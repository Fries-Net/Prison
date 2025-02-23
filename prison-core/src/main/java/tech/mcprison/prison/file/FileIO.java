package tech.mcprison.prison.file;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.error.Error;
import tech.mcprison.prison.error.ErrorManager;
import tech.mcprison.prison.internal.Player;
import tech.mcprison.prison.modules.ModuleStatus;
import tech.mcprison.prison.output.Output;

public abstract class FileIO
	extends FileVirtualDelete
{

	public static final String FILE_SUFFIX_JSON = ".json";
	public static final String FILE_PREFIX_BACKUP = ".backup_";
	public static final String FILE_SUFFIX_BACKUP = ".bu";
	public static final String FILE_SUFFIX_TEMP = ".temp";
	public static final String FILE_SUFFIX_TXT = ".txt";
	public static final String FILE_TIMESTAMP_FORMAT = "_yyyy-MM-dd_HH-mm-ss";
	
	public static final String PLAYER_PATH = "data_storage/ranksDb/players/";
	public static final String CACHE_PATH = "data_storage/playerCache/";

	
	private final SimpleDateFormat sdf;
	
	private final ErrorManager errorManager;
	private final ModuleStatus status;

	public FileIO()
	{
		this(null, null);
	}
	
	/**
	 * 
	 * @param errorManager Optional; set to null if used outside of a module.
	 * @param status Optional; set to null if used outside of a module.
	 */
	public FileIO(ErrorManager errorManager, ModuleStatus status)
	{
		super();
	
		this.errorManager = errorManager;
		this.status = status;

		this.sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	}

	
	public File getProjectRootDiretory() {
		return Prison.get().getDataFolder();
	}
	
	public File getTempFile( File file ) {
		String tempFileName = file.getName() + "." + getTimestampFormat() + ".tmp";
		File tempFile = new File(file.getParentFile(), tempFileName);

		return tempFile;
	}
	
	/**
	 * <p>This generates a new File with the filename of the backup file.
	 * This function only generates the File object and does not modify
	 * or save anything on the file system.
	 * </p>
	 * 
	 * @param file The original file name
	 * @param backupTag A no-spaced tag name to identify the type of backup. 
	 * 				This is inserted after the original file name.
	 * @param suffix File suffix to use for the backup, not including the dot.
	 * @return File objct of the target backup file.
	 */
	public File getBackupFile( File file, String backupTag, String suffix ) {
		
		String tempFileName = file.getName() + "." + backupTag + "_" + 
							getTimestampFormat() + "." + suffix;
		File tempFile = new File(file.getParentFile(), tempFileName);
		
		return tempFile;
	}
	
	protected void saveFile( File file, String data ) 
	{
		if ( file != null && data != null )
		{
			File tempFile = getTempFile( file );
//			String tempFileName = file.getName() + "." + getTimestampFormat() + ".tmp";
//			File tempFile = new File(file.getParentFile(), tempFileName);
			
			boolean disableAdvancedSaves = 
					Prison.get().getPlatform().getConfigBooleanFalse( 
							"storage.file.disable-advanced-saves.enabled" );
			
			if ( !disableAdvancedSaves ) {
				
				try
				{
					// Write as a .tmp file:
					
					// Add json data to lines, splitting on \n:
					List<String> lines = Arrays.asList( data.split( "\n" ));
					
					// Write as an UTF-8 stream:
					Files.write( tempFile.toPath(), lines, StandardCharsets.UTF_8 );
					
//				Files.write( tempFile.toPath(), data.getBytes() );
					
					// If original target exists, then delete it:
					if ( file.exists() )
					{
						file.delete();
					}
					
					tempFile.renameTo( file );
				}
				catch ( IOException e )
				{
					logException( "Failed to create file", file, e );
				}
			}
			else {
				
				boolean keepTempFiles = 
						Prison.get().getPlatform().getConfigBooleanFalse( 
								"storage.file.disable-advanced-saves.debug-keep-temp-files" );
				
				try
				{
					// Write as a .tmp file:
					
					// Add json data to lines, splitting on \n:
					List<String> lines = Arrays.asList( data.split( "\n" ));
					
					
					// Write to both temp file and the actual file:
					
					// Write as an UTF-8 stream:
					Files.write( tempFile.toPath(), lines, StandardCharsets.UTF_8 );
					
					boolean exists = file.exists();
					
					StandardOpenOption sooW = StandardOpenOption.WRITE;
					StandardOpenOption sooTe = exists ?
									StandardOpenOption.TRUNCATE_EXISTING : 
									StandardOpenOption.CREATE;
					
					Files.write( file.toPath(), lines, StandardCharsets.UTF_8, sooW, sooTe );
					
					
//				Files.write( tempFile.toPath(), data.getBytes() );
					
					// If original target exists, then delete it:
//					if ( file.exists() )
//					{
//						file.delete();
//					}
//					
//					tempFile.renameTo( file );
					
					
					if ( !keepTempFiles ) {
						tempFile.delete();
					}
					
				}
				catch ( IOException e )
				{
					logException( "Failed to create file", file, e );
				}
			}
			
			
		}
	}

	protected String readFile( File file )
	{
		StringBuilder results = new StringBuilder();
//		String results = null;
		
		if ( file.exists() ) {
			
			try
			{
				List<String> lines = Files.readAllLines( file.toPath(), StandardCharsets.UTF_8 );
				
				for ( String line : lines ) {
					results.append( line ).append( "\n" );
				}
				
//			byte[] bytes = Files.readAllBytes( file.toPath() );
//			results = new String(bytes);
			}
			catch ( IOException e )
			{
				logException( "Failed to load file", file, e );
			}
		}
		
		return results.toString();
	}

	private void logException( String description, File file, IOException e )
	{
		String message = description + " " + file.getAbsolutePath();
		
		if ( getStatus() != null )
		{
			getStatus().toFailed(message);
		}
		
		if ( getErrorManager() != null )
		{
			getErrorManager().throwError(
					new Error(message).appendStackTrace("Additional info:", e));
		}
		else
		{
			Output.get().logError(message, e);
		}
	}
	
	
	/**
	 * <p>This function generate a partial user file name. This is based upon
	 * the UUID-fragment (first and last parts of the player UUID), 
	 * plus the player's name, and the file suffix, which is '.json'.
	 * This function does not add any prefix such as 'player_' or 
	 * 'cache_'.
	 * </p>
	 * 
	 * @param player
	 * @return
	 */
    private static String getPlayerFileNameNewVersion( Player player ) {
    	
		String uuidFragment = getFileNamePrefixNew( player );
		
		return uuidFragment + "_" + player.getName() + FILE_SUFFIX_JSON;
    }
	
    /**
     * <p>This generates the fragment UUID that is used within file names.
     * This is based upon the first 8 UUID digits, plus the hyphen.  Followed
     * by the last segment of the UUID, which starts at character 25.
     * </p>
     * 
     * <p>This uses the start and end of the UUID because bedrock players 
     * only have zeros for the first part of the UUID, so the ending must
     * be included too.
     * </p>
     * 
     * <p>Examples:
     * </p>
     * '22cacd8c-d0ff-4dd7-a8ba-2a6a8b46be92'
     * ''
     * 
     * @param player
     * @return
     */
	private static String getFileNamePrefixNew( Player player ) {
		String uuid = player.getUUID().toString();
		return uuid.substring( 0, 9 ) + 
				uuid.substring( 25 );
	}
	
	/**
	 * <p>This function extracts the UUID fragment from player file names.
	 * </p>
	 * 
	 * @param filename
	 * @return
	 */
	public static String getFileNameUUIDFragment( String filename ) {
		String uuid = null;
		
		if ( filename != null && filename.trim().length() > 0 ) {
			
			String uuidTmp = filename.replace("cache_", "").replace("player_", "");
			
			uuid = uuidTmp.indexOf("_") > 0 ?
						uuidTmp.substring(0, uuidTmp.indexOf("_")) 
						: uuidTmp;
		}
		return uuid;
		
//		String uuid = player.getUUID().toString();
//		return uuid.substring( 0, 9 ) + 
//				uuid.substring( 25 );
	}
	
//	/**
//     * <p>This is a helper function to ensure that the given file name is 
//     * always generated correctly and consistently.
//     * </p>
//     * 
//     * @return "player_" plus the least significant bits of the UID
//     */
//    public static String filenamePlayer( Player player )
//    {
//    	boolean useNewFormat = useFriendlyUserFileNames();
//    	
//    	return useNewFormat ? filenamePlayerNew( player ) : filenamePlayerOld( player );
//    }
    
//    public static String filenameCache( Player player )
//    {
//    	boolean useNewFormat = useFriendlyUserFileNames();
//    	
//    	return useNewFormat ? filenameCacheNew( player ) : filenameCacheOld( player );
//    }
    
    public String filenamePrefix( String filename, String prefixDeliminator ) {
    	int idx = filename.lastIndexOf( prefixDeliminator ) + 1;
    	String prefix = idx > 0 ? filename.substring(0, idx) : null;
    	return prefix;
    }
    
    /**
     * Using the path and file name, this will check to see if any files 
     * preexist with the given file prefix so if the player changes their 
     * name, it will still load the correct file.
     * 
     * If no file is found, then it will crate a new File object based 
     * upon the path and filename.
     * 
     * If existing file are found, there should only be at most one, but
     * if there is more than one, then use the first one.
     * 
     * @param path
     * @param filename
     * @return
     */
    public File checkFile( File path, String filename, String prefixDeliminator ) {
    	
    	String prefix = filenamePrefix( filename, prefixDeliminator );
    	
    	List<File> files = getFilesFromPrefix( prefix, path );
    	
    	return files.size() > 0 ? files.get(0) : new File( path, filename );
    }
    
    public static File filePlayer( Player player ) {
    	FileIO fIO = new FileIO() {};
    	return fIO.checkFiles( filenamePlayerNew( player ), filenamePlayerOld( player ), PLAYER_PATH );
    }
    
    public static File fileCache( Player player ) {
    	FileIO fIO = new FileIO() {};
    	return fIO.checkFiles( filenameCacheNew( player ), filenameCacheOld( player ), CACHE_PATH );
    }
    
    private File checkFiles( String newFileName, String oldFileName, String pathName )
    {
    	File results = null;
    	
    	File path = new File( Prison.get().getDataFolder(), pathName );
    	
    	File newPlayerFile = checkFile( path, newFileName, "_" );
    	
    	if ( newPlayerFile.exists() ) {
    		results = newPlayerFile;
    	}
    	else {
    		File oldPlayerFile = checkFile( path, oldFileName, "." );
    		
    		if ( oldPlayerFile.exists() ) {
    			results = oldPlayerFile;
    		}
    	}
    	
    	if ( results == null ) {
    		// Did not find a new format file, or an old file format, so use the new format:
    		results = newPlayerFile;
    	}
    	
    	// If the file chosen is not equal to the newFileName, then rename it:
    	else if ( !newFileName.equals(results.getName()) ) {
    		File newFile = new File( path, newFileName );
    		results.renameTo(newFile);
    		
    		// Rename does not change the original file path in results so have to reassign it:
    		results = newFile;
    	}

    	return results;
    }
    
    
//    /**
//     * <p>This function will check two things.  First will be the config settings within 
//     * 'config.yml' to see if the new format is enabled. Secondly, it checks to see if 
//     * the PrisonSystemSettings has recorded if the conversion has taken place.
//     * Both have to be true in order for this function to return a value of true.,
//     * </p>
//     * 
//     * @return
//     */
//    public static boolean useFriendlyUserFileNames() {
//    	
//    	return PrisonSystemSettings.useFriendlyUserFileNames();
//    }
    
    /**
     * Do not use.  Use 'filenameCache( player )'.
     * 
     * @param player
     * @return
     */
    public static String filenameCacheNew( Player player ) {
    	
    	return "cache_" + getPlayerFileNameNewVersion( player );
    }
    /**
     * Do not use.  Use 'filenameCache( player )'.
     * 
     * @param player
     * @return
     */
    public static String filenameCacheOld( Player player ) {
    	
    	return getPlayerFileNameShortVersion( player );
    }
    
    /**
     * Do not use.  Use 'filenamePlayer( player )'.
     * 
     * @param player
     * @return
     */
    public static String filenamePlayerNew( Player player )
    {
    	return "player_" + getPlayerFileNameNewVersion( player );
    }
    /**
     * Do not use.  Use 'filenamePlayer( player )'.
     * 
     * @param player
     * @return
     */
    public static String filenamePlayerOld( Player player )
    {
    	return "player_" + player.getUUID().getLeastSignificantBits() + FILE_SUFFIX_JSON;
    }
    
	
    /**
     * <p>Do not use. This version is not compatible with bedrock players 
     * because all bedrock UUIDs are just zeros when using this formmat.
     * The newer format also includes the trailing 
     * 
     * <p>This constructs a player file named based upon the UUID followed 
     * by the player's name.  This format is used so it's easier to identify
     * the correct player.
     * </p>
     * 
     * <p>The format should be UUID-PlayerName.json.  The UUID is a shortened 
     * format, which should still produce a unique id.  The name, when read, 
     * is based upon the UUID and not the player's name, which may change.
     * This format includes the player's name to make it easier to identify
     * who's record is whom's.
     * </p>
     * 
     * @return
     */
	@Deprecated
    public static String getPlayerFileNameShortVersion( Player player ) {
    	
    	String UUIDString = player.getUUID().toString();
		String uuidFragment = getFileNamePrefixObsolete( UUIDString );
		
		return uuidFragment + "_" + player.getName() + FILE_SUFFIX_JSON;
    }
    
	/**
	 * <p>Do not use.  This does not support bedrock players because
	 * the prefix of bedrock UUIDs is all zeros.
	 * </p>
	 * 
	 * <p>This function returns the first 13 characters of the supplied
	 * file name, or UUID String. The hyphen is around the 12 or 13th position, 
	 * so it may or may not include it.
	 * </p>
	 * 
	 * @param playerFileName
	 * @return
	 */
	@Deprecated
	private static String getFileNamePrefixObsolete( String UUIDString ) {
		return UUIDString.substring( 0, 14 );
	}
	
	
	public List<File> getFilesFromPrefix( String filePrefix, File path ) {
		List<File> results = new ArrayList<>();
    	
    	FileFilter fFilter = getFilePrefixFilter( filePrefix );
    	
    	
    	File[] collectionFiles = path.listFiles( fFilter );
    	if ( collectionFiles != null ) {
    		
    		for (File file : collectionFiles ) {
    			results.add(file);
    		}
    	}

    	return results;
	}
	
	
	public FileFilter getFilePrefixFilter( String filePrefix ) {
		
		FileFilter fileFilter = (file) -> {
			
			String fname = file.getName();
			boolean isTemp = fname.startsWith( FILE_PREFIX_BACKUP ) ||
							 fname.endsWith( FILE_SUFFIX_BACKUP ) ||
							 fname.endsWith( FILE_SUFFIX_TEMP ) ||
							 fname.endsWith( FILE_SUFFIX_TXT );
			
			return 
					fname.toLowerCase().startsWith( filePrefix.toLowerCase() ) &&
					!file.isDirectory() && !isTemp &&
					fname.endsWith( FILE_SUFFIX_JSON );
		};
		
		return fileFilter;
	}
	
	public static FileFilter getPrisonFileFilter() {
		
		FileFilter fileFilter = (file) -> {
			
			String fname = file.getName();
			boolean isTemp = fname.startsWith( FILE_PREFIX_BACKUP ) ||
							 fname.endsWith( FILE_SUFFIX_BACKUP ) ||
							 fname.endsWith( FILE_SUFFIX_TEMP ) ||
							 fname.endsWith( FILE_SUFFIX_TXT );
			
			return !file.isDirectory() && !isTemp &&
						fname.endsWith( FILE_SUFFIX_JSON );
		};
		
		return fileFilter;
	}
	
	private String getTimestampFormat()
	{
		return sdf.format( new Date() );
	}
	
	public ErrorManager getErrorManager()
	{
		return errorManager;
	}

	public ModuleStatus getStatus()
	{
		return status;
	}

}
