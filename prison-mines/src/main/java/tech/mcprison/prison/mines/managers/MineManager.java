/*
 *  Copyright (C) 2017 The Prison Team
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tech.mcprison.prison.mines.managers;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.cache.PlayerCache;
import tech.mcprison.prison.internal.Player;
import tech.mcprison.prison.internal.World;
import tech.mcprison.prison.internal.block.PrisonBlock;
import tech.mcprison.prison.internal.block.PrisonBlock.PrisonBlockType;
import tech.mcprison.prison.mines.PrisonMines;
import tech.mcprison.prison.mines.data.Mine;
import tech.mcprison.prison.mines.data.OnStartupRefreshBlockBreakCountSyncTask;
import tech.mcprison.prison.mines.data.MineScheduler.MineResetActions;
import tech.mcprison.prison.mines.data.MineScheduler.MineResetScheduleType;
import tech.mcprison.prison.mines.data.PrisonSortableResults;
import tech.mcprison.prison.output.Output;
import tech.mcprison.prison.placeholders.ManagerPlaceholders;
import tech.mcprison.prison.placeholders.PlaceHolderKey;
import tech.mcprison.prison.placeholders.PlaceholderAttribute;
import tech.mcprison.prison.placeholders.PlaceholderAttributeBar;
import tech.mcprison.prison.placeholders.PlaceholderAttributeNumberFormat;
import tech.mcprison.prison.placeholders.PlaceholderAttributeText;
import tech.mcprison.prison.placeholders.PlaceholderAttributeTime;
import tech.mcprison.prison.placeholders.PlaceholderIdentifier;
import tech.mcprison.prison.placeholders.PlaceholderManager;
import tech.mcprison.prison.placeholders.PlaceholderManager.PlaceholderFlags;
import tech.mcprison.prison.placeholders.PlaceholderManager.PrisonPlaceHolders;
import tech.mcprison.prison.placeholders.PlaceholderManagerUtils;
import tech.mcprison.prison.placeholders.PlaceholdersUtil;
import tech.mcprison.prison.store.Collection;
import tech.mcprison.prison.store.Document;
import tech.mcprison.prison.tasks.PrisonDispatchCommandTask;
import tech.mcprison.prison.tasks.PrisonTaskSubmitter;

/**
 * Manages the creation, removal, and management of mines.
 *
 * @author Dylan M. Perks
 */
public class MineManager 
	implements ManagerPlaceholders {

    // Base list
    private List<Mine> mines;
    private TreeMap<String, Mine> minesByName;
    
    private TreeMap<String, List<Mine>> unavailableWorlds;

    private Collection coll;

    private List<PlaceHolderKey> translatedPlaceHolderKeys;
    
    private boolean mineStats = false;
    
    private List<String> mineResetCommands;
    private int mineResetCommandsCurrentTaskId = 0;
    private List<MineResetActions> mineResetActions;

    // private Pattern simpleNumberPattern = Pattern.compile("([0-9]+)");

	/**
	 * <p>These sort orders control how the mines are sorted, and which ones 
	 * are omitted from the result's included list of mines.
	 * </p>
	 * 
	 * <p>The type invalid is used to indicate that String value could not be
	 * converted to a MineSortOrder when using the fromString() function.
	 * </p>
	 * 
	 * <p>There are three primary sort orders: sortOrder, alpha, and active.
	 * Those primary three excludes any mine that has a sortOrder of -1.
	 * Each of these has a counter sort type that includes the excluded
	 * mines, and are: xSortOrder, xAplha, and xActive.
	 * </p>
	 * 
	 *
	 */
	public enum MineSortOrder {

		/**
		 * The sort order is based upon the mine's sortOrder field.  If more than
		 * one mine exists within a sortOrder, then they will be sub-sorted by
		 * alphabetical order. Mines are excluded if they have a sortOrder of -1, 
		 * but are placed within the exclude results set and they are sub-sorted 
		 * alphabetically.
		 */
		sortOrder,
		
		/**
		 * All mines are sorted by alphabetical order. Mines are excluded if they 
		 * have a sortOrder of -1, but are placed within the exclude results set 
		 * and they are sub-sorted alphabetically.
		 */
		alpha,
		
		/** 
		 * This provides a list of mines with the most active mines sorted to the
		 * top of the list.
		 * 
		 * All mines are sorted alphabetically, with the most active mines being
		 * placed at the very top of the list. The value of totalBlocksMined is 
		 * used to order the mines.  The value of totalBlocksMined resets to zero
		 * upon server restart.  So this provides the most active mines 
		 * since the server started. Mines are excluded if they have a 
		 * sortOrder of -1, but are placed within the exclude results set and 
		 * they are sub-sorted alphabetically.
		 */
		active,
		
		/**
		 * Same as sortOrder but ignores excluded mines.
		 */
		xSortOrder(true),
		
		/**
		 * Same as alpha but ignores excluded mines.
		 */
		xAlpha(true),
		/**
		 * Same as active but ignores excluded mines.
		 */
		xActive(true),
		
		/**
		 * Not a valid sort order, but is used within the fromString to indicate
		 * that the parameter sortOrder is invalid.
		 */
		invalid;
		
		private final boolean excluded;
		
		private MineSortOrder( boolean excluded ) {
			this.excluded = excluded;
			
		}
		private MineSortOrder() {
			this(false);
		}
		
		public boolean isExcluded() {
			return excluded;
		}
		
		public static MineSortOrder fromString( String sortOrder ) {
			MineSortOrder results = MineSortOrder.invalid;
			
			if ( sortOrder != null && sortOrder.trim().length() > 0 ) {
				for ( MineSortOrder so : values() ) {
					if ( so.name().equalsIgnoreCase( sortOrder ) ) {
						results = so;
						break;
					}
				}
				
			}
			
			return results;
		}
		
		/**
		 * Returns a space separated list of available sort orders, omitting
		 * invalid.
		 * 
		 * @return
		 */
		static String availableSortOrders() {
			StringBuilder sb = new StringBuilder();
			
			for ( MineSortOrder so : values() ) {
				if ( so != invalid ) {
					if ( sb.length() > 0 ) {
						sb.append( " " );
					}
					sb.append( so.name() );
				}
			}
			
			return sb.toString();
		}
	}
		
    /**
     * <p>MineManager must be fully instantiated prior to trying to load the mines,
     * otherwise if the mines cannot find the world they should be, they will be
     * unable to register that the world is unavailable.
     * </p>
     * 
     */
    public MineManager() {
    	this.mines = new ArrayList<>();
    	this.minesByName = new TreeMap<>();
    	
    	this.unavailableWorlds = new TreeMap<>();
    	
    	this.mineResetCommands = new ArrayList<>();
    	this.mineResetActions = new ArrayList<>();
    	
    	this.coll = null;
    	
    }
    

    public void loadFromDbCollection( PrisonMines pMines ) {
        
        Optional<Collection> collOptional = pMines.getDb().getCollection("mines");

        if (!collOptional.isPresent()) {
        	Output.get().logError("Could not create 'mines' collection.");
        	pMines.getStatus().toFailed("Could not create mines collection in storage.");
        	return;
        }

        this.coll = collOptional.get();

        
        // Default value of 5 seconds:
        //  NOTE: the parameter 'prison-mines-reset-gap' is the old version, so keep it
        //        for compatibility purposes.
        long offsetTimingMs = Prison.get().getPlatform()
        							.getConfigInt( "prison-mines-reset-gap", 5000 );
        offsetTimingMs = Prison.get().getPlatform()
        					.getConfigInt( "prison-mines.reset-gap-ms", (int) offsetTimingMs );

        loadMines(offsetTimingMs);
        

        Output.get().logInfo( String.format("Loaded %d mines and submitted with a %d " +
        		"millisecond offset timing for auto resets.", 
        			getMines().size(), offsetTimingMs));
        
        
        // Start task to count air blocks:
        OnStartupRefreshBlockBreakCountSyncTask.getInstance().submit( 60 );
        
        
//        // When finished loading the mines, then if there are any worlds that
//        // could not be loaded, dump the details:
//        List<String> unavailableWorlds = getUnavailableWorldsListings();
//        for ( String uWorld : unavailableWorlds ) {
//			Output.get().logInfo( uWorld );
//		}
        
        
//        // Submit all the loaded mines to run:
//        int offset = 0;
//        for ( Mine mine : mines )
//		{
//			mine.submit(offset);
//			offset += 5;
//		}
//        Output.get().logInfo("Mines are all queued to run auto resets.");
    }

//    public void loadMine(String mineFile) throws IOException, MineException {
//        Document document = coll.get(mineFile).orElseThrow(IOException::new);
//        Mine m = new Mine(document);
//        add(m, false, 0);
//    }

    /**
     * Adds a {@link Mine} to this {@link MineManager} instance.
     * 
     * Also saves the mine to the file system.
     *
     * @param mine the mine instance
     * @return if the add was successful
     */
    public boolean add(Mine mine) {
    	return add(mine, true, 0);
    }
    
    /**
     * Adds a {@link Mine} to this {@link MineManager} instance.
     * 
     * Also saves the mine to the file system.
     *
     * @param mine the mine instance
     * @param save - bypass the option to save. Useful for when initially loading the mines since
     *               no data has changed.
     * @param offsetTiming in milliseconds 
     * @return if the add was successful
     */
    private boolean add(Mine mine, boolean save, int offsetTimingMs ) {
    	boolean results = false;
    	
    	// not add if it already exists, or if the mine is null or does not have a valid name:
        if ( mine != null &&  mine.getName() != null && 
        		mine.getName().trim().length() > 0 && 
        		!getMines().contains(mine)) {
        	
        	if ( save ) {
        		saveMine( mine );
        	}
        	
            results = getMines().add(mine);
            getMinesByName().put( mine.getName().toLowerCase(), mine );
            
            // Start its scheduling:
            mine.submit( offsetTimingMs / 1000d );
        }
        return results;
    }


    public boolean removeMine(String mineName){
    	boolean results = false;
    	if ( mineName != null ) {
    		Mine mine = getMinesByName().get( mineName.toLowerCase() );
    		if ( mine != null ) {
    			results = removeMine(mine);
    		}
    	}
    	
    	return results;
    }

    public boolean removeMine(Mine mine) {
    	boolean success = false;
    	if ( mine != null ) {
    		coll.delete( mine.getName() );
    		getMinesByName().remove(mine.getName().toLowerCase());
    		success = getMines().remove(mine);
    	}
	    return success;
    }



    private void loadMines( long offsetTimingMs ) {
        List<Document> mineDocuments = coll.getAll();

        int offsetMs = 0;
        for (Document document : mineDocuments) {
            try {
                Mine m = new Mine(document);
                add(m, false, offsetMs);
                offsetMs += offsetTimingMs;
                
            } catch (Exception e) {
                Output.get()
                    .logError("&cFailed to load mine " + document.getOrDefault("name", "null"), e);
            }
        }
        
    }

    /**
     * Saves the specified mine. This should only be used for the instance created by {@link
     * PrisonMines}
     */
    public void saveMine(Mine mine) {
        coll.save( mine.getName(), mine.toDocument(), null, "Mine" );
    }

    public void saveMines(){
        for (Mine m : getMines()){
            saveMine(m);
        }
    }

    
    public void saveMinesIfUnsavedBlockCounts() {
    	for (Mine m : getMines()){
    		if ( m.hasUnsavedBlockCounts() ) {
    			saveMine( m );
    		}
    	}
    }
    

    /**
     * <p>Creates a backup of a mine's save file. Similar to a virtual delete
     * but is a copy.  If used, has to be manually renamed. 
     * </p>
     * 
     * @param mine
     */
	public File backupMine( Mine mine )
	{
		File backupFile = coll.backup( mine.getName() );
		
		return backupFile;
	}




	public void rename( Mine mine, String newName ) {
		
		String oldMineName = mine.getName();
		
		// Remove the old mine:
		removeMine( oldMineName );

		// rename the mine:
		mine.setName( newName );
		
		// Add the mine back with the new name:
		add( mine );
		
	}


    /**
     * Returns the mine with the specified name.
     *
     * @param name The mine's name, case-sensitive.
     * @return An optional containing either the {@link Mine} if it could be found, or empty if it
     * does not exist by the specified name.
     */
    public Mine getMine(String mineName) {
    	return (mineName == null ? null : getMinesByName().get( mineName.toLowerCase() ));
    	
        //return mines.stream().filter(mine -> mine.getName().equals(name)).findFirst();
    }

    public List<Mine> getMines() {
        return mines;
    }
    
    public PrisonSortableResults getMines( MineSortOrder sortOrder ) {
    	return getMines( sortOrder, getMines() );
    }
    
    protected PrisonSortableResults getMines( MineSortOrder sortOrder, List<Mine> mines ) {
    	PrisonSortableResults results = new PrisonSortableResults( sortOrder );
    	
    	
    	// if invalid, then that's invalid, so default to sortOrder:
    	if ( sortOrder == MineSortOrder.invalid ) {
    		sortOrder = MineSortOrder.sortOrder;
    	}

    	
    	for ( Mine mine : mines ) {
    		if ( mine.getSortOrder() < 0 ) {
    			results.getExclude().add( mine );
    		}
    		else {
    			results.getInclude().add( mine );
    		}
		}

    	// Sort first by name, then by other means if needed:
    	results.getInclude().sort( (a, b) -> a.getName().compareToIgnoreCase( b.getName()) );
    	results.getExclude().sort( (a, b) -> a.getName().compareToIgnoreCase( b.getName()) );

    	if ( sortOrder == MineSortOrder.sortOrder || sortOrder == MineSortOrder.xSortOrder ) {
    		results.getInclude().sort( (a, b) -> Integer.compare( a.getSortOrder(), b.getSortOrder()) );
    		results.getExclude().sort( (a, b) -> Integer.compare( a.getSortOrder(), b.getSortOrder()) );
    	}
    	
    	// for now hold off on sorting by total blocks mined.
    	else if ( sortOrder == MineSortOrder.active || sortOrder == MineSortOrder.xActive ) {
    		results.getInclude().sort( (a, b) -> Long.compare(b.getTotalBlocksMined(), a.getTotalBlocksMined()) );
    		results.getExclude().sort( (a, b) -> Long.compare(b.getTotalBlocksMined(), a.getTotalBlocksMined()) );
    	}
    	
    	return results;
    }
    
    
    

	public TreeMap<String, Mine> getMinesByName() {
		return minesByName;
	}

	public boolean isMineStats()
	{
		return mineStats;
	}
	public void setMineStats( boolean mineStats )
	{
		this.mineStats = mineStats;
	}
	
	

	/**
	 * <p>This command is the starting point for resetting all mines.
	 * </p>
	 * 
	 * <p>This will first cancel and prior resets that were queued. Then it will build the
	 * list of commands to run using all of the mines that are not virtual and are enabled.
	 * </p>
	 * 
	 * @param resetType
	 * @param resetActions 
	 */
	public void resetAllMines( MineResetScheduleType resetType, List<MineResetActions> resetActions ) {

		cancelResetAllMines();
		
		if ( getMineResetActions().contains( MineResetActions.DETAILS ) ) {
			Output.get().logInfo( "MineManager.resetAllMines: submitting all mines to be reset." );
		}
		
		// Save the resetActions:
		setMineResetActions( resetActions );
		
		// Build all the reset commands for all of the mines:
		for ( Mine mine : getMines() ) {
			
			// Only create reset commands if the mine is not a virtual mine and it is enabled:
			if ( !mine.isVirtual() && mine.isEnabled() ) {
				
				String command = String.format( "mines reset %s noCommands chained", mine.getName() );
				getMineResetCommands().add( command );
			}
		}

		// Submit the commands to run sync... 
		resetAllMinesNext();
		
	}
	
	
	/**
	 * <p>This is where the actual processing of the mine reset commands are processed to
	 * be submitted.  This should only be ran from within the MineReset functions when
	 * finished with resetting another mine. Trying to run this command from another
	 * location can cause problems or will result in nothing happening.  The 
	 * getMineResetCommands() List can only be built by resetAllMines().
	 * </p>
	 */
	public void resetAllMinesNext() {
		if ( getMineResetCommands().size() > 0 ) {

			List<String> tasks = new ArrayList<>();
			
			String command = getMineResetCommands().remove( 0 );
			tasks.add( command );
			
			String errorMessage = String.format( "&3MineManager: resetAllMinesNext: mines left= &7%s " + 
					 "  &3command= [&7%s&3]",
					 Integer.toString( getMineResetCommands().size( )), command );
			
			if ( getMineResetActions().contains( MineResetActions.DETAILS ) ) {
				Output.get().logInfo( errorMessage );
			}
			
			PrisonDispatchCommandTask task = 
					new PrisonDispatchCommandTask( tasks, errorMessage, null, false );

			// submit task: One tick in the future:
			int taskId = PrisonTaskSubmitter.runTaskLater(task, 1);
			
			// Store task ID so it can be canceled if needed:
			setMineResetCommandsCurrentTaskId( taskId );
		}
	}
	
	/**
	 * <p>Cancel the chained resetting of all the mines.
	 * Any mine that is in the middle of being reset 
	 * will continue to be reset, but this will prevent the other
	 * mines from being reset.  This will also cancel any job that has been submitted,
	 * but is yet to run.
	 * </p>
	 */
	public void cancelResetAllMines() {
		
		// If there is job yet to run, then cancel it:
		PrisonTaskSubmitter.cancelTask( getMineResetCommandsCurrentTaskId() );
		
		// Clear the queue of commands so there won't be any more left to run:
		getMineResetCommands().clear();
		
		setMineResetCommandsCurrentTaskId( 0 );
		
		getMineResetActions().clear();
	}
	
	public List<String> getMineResetCommands() {
		return mineResetCommands;
	}

	public int getMineResetCommandsCurrentTaskId() {
		return mineResetCommandsCurrentTaskId;
	}
	public void setMineResetCommandsCurrentTaskId( int mineResetCommandsCurrentTaskId ) {
		this.mineResetCommandsCurrentTaskId = mineResetCommandsCurrentTaskId;
	}

	public List<MineResetActions> getMineResetActions() {
		return mineResetActions;
	}
	public void setMineResetActions( List<MineResetActions> mineResetActions ) {
		this.mineResetActions = mineResetActions;
	}


	/**
	 * <p>Add the missing world and the associated mine to the collection. Create the
	 * base entries if needed.
	 * </p>
	 * 
	 * <p>Upon the first entry in to this collection of worlds and mines, an error
	 * message will be generated indicating the world does not exist.
	 * </p>
	 * 
	 * @param worldName
	 * @param mine
	 */
	public void addUnavailableWorld( String worldName, Mine mine ) {
		if ( worldName != null && worldName.trim().length() > 0 && mine != null ) {
			if ( !getUnavailableWorlds().containsKey( worldName )) {
				getUnavailableWorlds().put( worldName, new ArrayList<>() );
				
				Output.get().logWarn( "&7Mine Loader: &aWorld does not exist! " +
						"&7This maybe a temporary " +
	            		"condition until the world can be loaded. " +
	            		" &3worldName= " + worldName );
			}

			if ( !getUnavailableWorlds().get( worldName ).contains( mine )) {
				getUnavailableWorlds().get( worldName ).add( mine );
			}
		}
	}
    public TreeMap<String, List<Mine>> getUnavailableWorlds() {
		return unavailableWorlds;
	}
	public void setUnavailableWorlds( TreeMap<String, List<Mine>> unavailableWorlds ) {
		this.unavailableWorlds = unavailableWorlds;
	}

	public void assignAvailableWorld( String worldName ) {
    	if ( worldName != null && worldName.trim().length() > 0 ) {
    		
    		Optional<World> worldOptional = Prison.get().getPlatform().getWorld(worldName);
    		
    		if ( worldOptional.isPresent() && getUnavailableWorlds().containsKey( worldName )) {
    		
    			World world = worldOptional.get();
    			
    			
    			StringBuilder sb = new StringBuilder();
    			sb.append( "Enabling world: " ).append( worldName ).append( "  Mines: " );
    			
    			
    			// Store this mine and the world in MineManager's unavailableWorld for later
    			// processing and hooking up to the world object.
    			List<Mine> unenabledMines = getUnavailableWorlds().get( worldName );

//    			List<Mine> remove = new ArrayList<>();
    			
//    			long delay = 0;
    			for ( Mine mine : unenabledMines ) {
    				
    				if ( !mine.isEnabled() ) {
    					
    					if ( !mine.isVirtual() ) {
    						
    						sb.append( mine.getName() ).append( " " );
    						
    						mine.setWorld( world );
    						
    						// Make sure world is hooked up properly to all locations.
    						// Since world is an object, it may already be auto hooked:
    						
    						
    						if ( mine.getBounds() != null ) {
    							mine.getBounds().setWorld( world );
    						}
    						
    						
    						if ( mine.getSpawn() != null ) {
    							mine.getSpawn().setWorld( world );
    						}
    						
    						// Run the air-counts now that mine can be activated:
//    						mine.refreshBlockBreakCountUponStartup( delay++ );
    						
    					}
    					
    				}
//    				remove.add( mine );
    			}
    			
    			
    			// Count the blocks for the mines that were just loaded.. 2 sec delay
    			OnStartupRefreshBlockBreakCountSyncTask.getInstance().submit( 40 );
    			
    			
//    			// Purge all removed mines from the unenabledMines list:
//    			if ( remove.size() > 0 ) {
//    				unenabledMines.removeAll( remove );
//    			}
    			
//    			// If no mines remain, then remove this world from unavailableWorlds:
//    			if ( unenabledMines.size() == 0 ) {
//    				getUnavailableWorlds().remove( worldName );
//    			}
    			
    			Output.get().logInfo( sb.toString() );
    			
    			// Since the world is loaded and all available mines have been hooked up
    			// with the world, so remove these entries.
    			unenabledMines.clear();
    			getUnavailableWorlds().remove( worldName );
    		}
    	}
    	
    	getUnavailableWorldsListings();
	}
	
    public List<String> getUnavailableWorldsListings() {
    	List<String> results = new ArrayList<>();
    	
    	if ( getUnavailableWorlds().size() > 0 ) {
    		results.add( "&cUnavailable Worlds: &3Deferred loading of mines." );

    		Set<String> worlds = getUnavailableWorlds().keySet();
    		
    		for ( String worldName : worlds ) {
    			int enabledCount = 0;
    			
				List<Mine> mines = getUnavailableWorlds().get( worldName );
				for ( Mine mine : mines ) {
					if ( mine.isEnabled() ) {
						enabledCount++;
					}
				}
				results.add( 
						String.format( "&7    world: &3%s &7(&c%s mines enabled out &7of &c%s &7mines in the world) ", 
								worldName, Integer.toString( enabledCount ),
								Integer.toString( mines.size() )));
			}
    	}
    	
    	return results;
    }
	
	


    
    public String getTranslateMinesPlaceholder( PlaceholderIdentifier identifier ) {
    	
//    	// placeholder Attributes:
//    	PlaceholderManager pman = Prison.get().getPlaceholderManager();
    	
    	Player player = identifier.getPlayer();
    	
    	PlaceHolderKey placeHolderKey = identifier.getPlaceholderKey();
    	
		
    	Mine mine = null;
		if ( placeHolderKey.getPlaceholder().hasFlag( PlaceholderFlags.MINES ) || 
				placeHolderKey.getPlaceholder().hasFlag( PlaceholderFlags.STATSMINES ) ) {
			
			mine = getMine( placeHolderKey.getData() );
		}
		else {
			
			if ( player != null && player.getLocation() != null ) {
				
				mine = PrisonMines.getInstance().findMineLocation( player );
			}
		}
    	
    	
    	PlaceholderAttributeBar attributeBar = identifier.getAttributeBar();
    	PlaceholderAttributeNumberFormat attributeNFormat = identifier.getAttributeNFormat();
    	PlaceholderAttributeText attributeText = identifier.getAttributeText();
    	PlaceholderAttributeTime attributeTime = identifier.getAttributeTime();
    	
		
		int sequence = identifier.getSequence();
    	

		String results = null;

		if ( placeHolderKey != null ) {

			// If the mine is not provided, try to get it from the placeholder data: 
			if ( mine == null && placeHolderKey.getData() != null )  {
				mine = getMine( placeHolderKey.getData() );
			}

			if ( mine != null || 
					placeHolderKey.getPlaceholder().hasFlag( PlaceholderFlags.PLAYERBLOCKS ) || 
					placeHolderKey.getPlaceholder().hasFlag( PlaceholderFlags.MINEPLAYERS )) {
				
				DecimalFormat dFmt = Prison.get().getDecimalFormat("#,##0.00");
				DecimalFormat iFmt = Prison.get().getDecimalFormatInt();
//				DecimalFormat fFmt = Prison.get().getDecimalForma("#,##0.00");
				
				identifier.setFoundAMatch( true );
				
				switch ( placeHolderKey.getPlaceholder() ) {
					case prison_mn_minename:
					case prison_mines_name_minename:
					case prison_mn_pm:
					case prison_mines_name_playermines:
						if ( mine != null ) {
							results = mine.getName();
						}
						
						break;
						
					case prison_mt_minename:
					case prison_mines_tag_minename:
					case prison_mt_pm:
					case prison_mines_tag_playermines:
						// getTag() now defaults to the mine's name if it does not exist:
						if ( mine != null ) {
							results = mine.getTag();
						}
						
						break;
						
					case prison_mi_minename:
					case prison_mines_interval_minename:
					case prison_mi_pm:
					case prison_mines_interval_playermines:
						if ( mine != null ) {
	        				if ( attributeNFormat != null ) {

	        					results = attributeNFormat.format( (long) mine.getResetTime() );
	        				}
	        				else if ( attributeTime != null ) {
	        					
	        					results = attributeTime.format( (long) mine.getResetTime() );
	        				}
	        				else {
	        					
	        					results = iFmt.format( mine.getResetTime() );
	        				}
						}
						
						break;
						
					case prison_mif_minename:
					case prison_mines_interval_formatted_minename:
					case prison_mif_pm:
					case prison_mines_interval_formatted_playermines:
						
						if ( mine != null ) {
	        				if ( attributeNFormat != null ) {

	        					results = attributeNFormat.format( (long) mine.getResetTime() );
	        				}
	        				else if ( attributeTime != null ) {
	        					
	        					results = attributeTime.format( (long) mine.getResetTime() );
	        				}
	        				else {
	        					
	        					double timeMif = mine.getResetTime();
	        					results = PlaceholdersUtil.formattedTime( timeMif );
	        				}
						}
						break;
						
					case prison_mtl_minename:
					case prison_mines_timeleft_minename:
					case prison_mtl_pm:
					case prison_mines_timeleft_playermines:
						// NOTE: timeleft can vary based upon server loads:
						
						if ( mine != null && !mine.isVirtual() )
						{
	        				if ( attributeNFormat != null ) {

	        					results = attributeNFormat.format( (long) mine.getRemainingTimeSec() );
	        				}
	        				else if ( attributeTime != null ) {
	        					
	        					results = attributeTime.format( (long) mine.getResetTime() );
	        				}
	        				else {
	        					results = dFmt.format( mine.getRemainingTimeSec() );
	        				}
						}
						break;
						
					case prison_mtlb_minename:
					case prison_mines_timeleft_bar_minename:
					case prison_mtlb_pm:
					case prison_mines_timeleft_bar_playermines:
						// NOTE: timeleft can vary based upon server loads:
						if ( mine != null && !mine.isVirtual() ) {
							results = getRemainingTimeBar( mine, attributeBar );
						}
						
						break;
						
					case prison_mtlf_minename:
					case prison_mines_timeleft_formatted_minename:
					case prison_mtlf_pm:
					case prison_mines_timeleft_formatted_playermines:
						// NOTE: timeleft can vary based upon server loads:
						
						if ( mine != null && !mine.isVirtual() )
						{
	        				if ( attributeNFormat != null ) {

	        					results = attributeNFormat.format( (long) mine.getRemainingTimeSec() );
	        				}
	        				else {
	        					double timeMtlf = mine.getRemainingTimeSec();
	        					results = PlaceholdersUtil.formattedTime( timeMtlf );
	        				}
						}
						break;
						
					case prison_ms_minename:
					case prison_mines_size_minename:
					case prison_ms_pm:
					case prison_mines_size_playermines:
						
						if ( mine != null && !mine.isVirtual() )
						{
	        				if ( attributeNFormat != null ) {

	        					results = attributeNFormat.format( (long) mine.getBounds().getTotalBlockCount() );
	        				}
	        				else {
	        					
	        					results = iFmt.format( mine.getBounds().getTotalBlockCount() );
	        				}
						}
						
						break;
						
					case prison_mr_minename:
					case prison_mines_remaining_minename:
					case prison_mr_pm:
					case prison_mines_remaining_playermines:
						
						if ( mine != null && !mine.isVirtual() ) {
							
							int remainingBlocks = mine.getRemainingBlockCount();
							{
								if ( attributeNFormat != null ) {

									results = attributeNFormat.format( (long) remainingBlocks );
								}
								else {
									
									results = iFmt.format( remainingBlocks );
								}
							}
						}

						break;
						
					case prison_mrb_minename:
					case prison_mines_remaining_bar_minename:
					case prison_mrb_pm:
					case prison_mines_remaining_bar_playermines:
						
						if ( mine != null && !mine.isVirtual() ) {
							
							int totalBlocks = mine.getBounds().getTotalBlockCount();
							int blocksRemaining = mine.getRemainingBlockCount();
							
							results = PlaceholderManagerUtils.getInstance().
									getProgressBar( ((double) blocksRemaining), ((double) totalBlocks), 
											false, attributeBar );
						}
						break;
						
					case prison_mp_minename:
					case prison_mines_percent_minename:
					case prison_mp_pm:
					case prison_mines_percent_playermines:
						// mine.refreshAirCount(); // async & delayed : Very high cost
						
						if ( mine != null && !mine.isVirtual() ) {
							
							double percentRemaining = mine.getPercentRemainingBlockCount();
							results = dFmt.format( percentRemaining );
						}
						break;
						
					case prison_mpc_minename:
					case prison_mines_player_count_minename:
					case prison_mpc_pm:
					case prison_mines_player_count_playermines:
						if ( mine != null && !mine.isVirtual() )
						{
	        				if ( attributeNFormat != null ) {
	        					
	        					results = attributeNFormat.format( (long) mine.getPlayerCount() );
	        				}
	        				else {
	        					
	        					results = iFmt.format( mine.getPlayerCount() );
	        				}
						}
						break;
						
					case prison_mbm_minename:
					case prison_mines_blocks_mined_minename:
					case prison_mbm_pm:
					case prison_mines_blocks_mined_playermines:
						if ( mine != null && !mine.isVirtual() )
						{
//							getBlockBreakCount();
							
	        				if ( attributeNFormat != null ) {
	        					
	        					results = attributeNFormat.format( mine.getTotalBlocksMined() );
	        				}
	        				else {
	        					
	        					results = iFmt.format( mine.getTotalBlocksMined() );
	        				}
						}
						
						break;
						
					case prison_mrc_minename:
					case prison_mines_reset_count_minename:
					case prison_mrc_pm:
					case prison_mines_reset_count_playermines:
						if ( mine != null ){
	        				if ( attributeNFormat != null ) {
	        					
	        					results = attributeNFormat.format( (long) mine.getResetCount() );
	        				}
	        				else {
	        					
	        					results = iFmt.format( mine.getResetCount() );
	        				}
						}
					
						break;

						
						
					case prison_top_mine_block_line_header_minename:
					case prison_tmbl_header_minename:
						if ( !mine.isVirtual() )
						{
							results = "BlockName     Chance   Placed Remaining   Totals";
						}
						break;
						
						
					case prison_top_mine_block_line_nnn_minename:
					case prison_tmbl_nnn_minename:
						if ( !mine.isVirtual() )
						{
							List<PrisonBlock> blocks = mine.getPrisonBlocks();
							if ( sequence > 0 && sequence <= blocks.size() ) {
								PrisonBlock block = blocks.get( sequence - 1 );
								
								if ( block != null ) {
									
									String message = String.format( 
											"%-13s %6.2f  %7d   %7d %8s", 
											
											block.getBlockName(),
											block.getChance(),
											((long) block.getBlockPlacedCount() ),
											((long) block.getBlockPlacedCount() - block.getBlockCountUnsaved() ),
											
											PlaceholdersUtil.formattedKmbtSISize( block.getBlockCountTotal(), iFmt, "" )
											);
									
									results = message;
								}
							}
						}
						break;
						
					case prison_top_mine_block_line_totals_minename:
					case prison_tmbl_totals_minename:
						if ( !mine.isVirtual() )
						{
							List<PrisonBlock> blocks = mine.getPrisonBlocks();

							double tChance = 0d;
							int tPlaced = 0;
							int tRemoved = 0;
							long tTotals = 0;
							
							for ( PrisonBlock pBlock : blocks ) {
								tChance += pBlock.getChance();
								
								tPlaced += pBlock.getBlockPlacedCount();
								tRemoved += (pBlock.getBlockPlacedCount() - pBlock.getBlockCountUnsaved());
								tTotals += pBlock.getBlockCountTotal();
							}
							
							String message = String.format( 
									"              %6.2f  %7d   %7d %8s", 
									
									tChance,
									tPlaced,
									tRemoved,
									
									PlaceholdersUtil.formattedKmbtSISize( tTotals, iFmt, "" )
									);
							
							results = message;
						}
						break;

						
						
					case prison_top_mine_block_name_nnn_minename:
					case prison_tmbn_nnn_minename:
						if ( !mine.isVirtual() )
						{
							List<PrisonBlock> blocks = mine.getPrisonBlocks();
							if ( sequence > 0 && sequence <= blocks.size() ) {
								PrisonBlock block = blocks.get( sequence - 1 );
								
								if ( block != null ) {
									results = block.getBlockName();
								}
							}
						}
						break;
						
					case prison_top_mine_block_chance_nnn_minename:
					case prison_tmbc_nnn_minename:
						if ( !mine.isVirtual() )
						{
							List<PrisonBlock> blocks = mine.getPrisonBlocks();
							if ( sequence > 0 && sequence <= blocks.size() ) {
								PrisonBlock block = blocks.get( sequence - 1 );
								
								if ( block != null ) {
									if ( attributeNFormat != null ) {
										
			        					results = attributeNFormat.format( (long) block.getChance() );
			        				}
			        				else {
			        					
			        					results = dFmt.format( block.getChance() );
			        				}
								}
							}
						}
						break;
						
					case prison_top_mine_block_placed_nnn_minename:
					case prison_tmbpl_nnn_minename:
						if ( !mine.isVirtual() )
						{
							List<PrisonBlock> blocks = mine.getPrisonBlocks();
							if ( sequence > 0 && sequence <= blocks.size() ) {
								PrisonBlock block = blocks.get( sequence - 1 );
								
								if ( block != null ) {
									if ( attributeNFormat != null ) {

										results = attributeNFormat.format( (long) block.getBlockPlacedCount() );
			        				}
			        				else {
			        					
			        					results = iFmt.format( block.getBlockPlacedCount() );
			        				}
								}
							}
						}
						break;
						
					case prison_top_mine_block_remaing_nnn_minename:
					case prison_tmbr_nnn_minename:
						if ( !mine.isVirtual() )
						{
							List<PrisonBlock> blocks = mine.getPrisonBlocks();
							if ( sequence > 0 && sequence <= blocks.size() ) {
								PrisonBlock block = blocks.get( sequence - 1 );
								
								if ( block != null ) {
									if ( attributeNFormat != null ) {

										results = attributeNFormat.format( (long) 
			        								block.getBlockPlacedCount() - block.getBlockCountUnsaved() );
			        				}
			        				else {
			        					
			        					results = iFmt.format( block.getBlockPlacedCount() - block.getBlockCountUnsaved() );
			        				}
								}
							}
						}
						break;
						
					case prison_top_mine_block_remaing_bar_nnn_minename:
					case prison_tmbrb_nnn_minename:
						if ( !mine.isVirtual() )
						{
							List<PrisonBlock> blocks = mine.getPrisonBlocks();
							if ( sequence > 0 && sequence <= blocks.size() ) {
								PrisonBlock block = blocks.get( sequence - 1 );
								
								if ( block != null ) {
									int placed = block.getBlockPlacedCount();
									long removed = block.getBlockCountUnsaved();
									
									results = PlaceholderManagerUtils.getInstance().
											getProgressBar( ((double) removed), ((double) placed), 
													false, attributeBar );
								}
							}
						}
						break;
						
					case prison_top_mine_block_total_nnn_minename:
					case prison_tmbt_nnn_minename:
						if ( !mine.isVirtual() )
						{
							List<PrisonBlock> blocks = mine.getPrisonBlocks();
							if ( sequence > 0 && sequence <= blocks.size() ) {
								PrisonBlock block = blocks.get( sequence - 1 );
								
								if ( block != null ) {
									if ( attributeNFormat != null ) {
										
			        					results = attributeNFormat.format( (long) block.getBlockCountTotal() );
			        				}
			        				else {
			        					
			        					results = iFmt.format( block.getBlockCountTotal() );
			        				}
								}
							}
						}
						break;
						
						
//    				case prison_pbt:
//    				case prison_player_blocks_total:
//    					if ( !mine.isVirtual() && player != null )
//    					{
//    						long blocksTotal = PlayerCache.getInstance().getPlayerBlocksTotal( player );
//    						
//    						if ( attributeNFormat != null ) {
//    							results = attributeNFormat.format( blocksTotal );
//    						}
//    						else {
//    							
//    							results = iFmt.format( blocksTotal );
//    						}
//    					}
//    					break;
//    					
						
					case prison_pbt_minename:
					case prison_player_blocks_total_minename:
					case prison_pbtr_minename:
					case prison_player_blocks_total_raw_minename:
						if ( mine != null && !mine.isVirtual() && player != null )
						{
    						long blocksTotalByMine = PlayerCache.getInstance()
    												.getPlayerBlocksTotalByMine( player, mine.getName() );
    						
    						if ( placeHolderKey.getPlaceholder() == PrisonPlaceHolders.prison_pbtr_minename || 
    							 placeHolderKey.getPlaceholder() == PrisonPlaceHolders.prison_player_blocks_total_raw_minename ) {
    							results = Long.toString( blocksTotalByMine );
    						}
    						else if ( attributeNFormat != null ) {

    							results = attributeNFormat.format( blocksTotalByMine );
    						}
    						else {
    							
    							results = iFmt.format( blocksTotalByMine );
    						}
    					}
					
						break;

						
					case prison_ptb__blockname:
					case prison_player_total_blocks__blockname:
					case prison_ptbr__blockname:
					case prison_player_total_blocks_raw__blockname:
						{
							String blockName = placeHolderKey.getData().replace( "-", ":" );
							
							long blockCount = player.getPlayerCache().getPlayerBlocksTotalByBlockType( player, blockName );

    						if ( placeHolderKey.getPlaceholder() == PrisonPlaceHolders.prison_ptbr__blockname || 
       							 placeHolderKey.getPlaceholder() == PrisonPlaceHolders.prison_player_total_blocks_raw__blockname ) {
       							results = Long.toString( blockCount );
       						}
    						else if ( attributeNFormat != null ) {

    							results = attributeNFormat.format( blockCount );
    						}
    						else {
    							
    							results = iFmt.format( blockCount );
    						}
							
						}
						break;
						
					default:
						identifier.setFoundAMatch( false );
						
						Output.get().logInfo(
								"MineManager translateMinesPlaceholder: Warning: a placeholder '%s' was selected " +
								"to be processed in this manager, but the placeholder is not included in the swich. " +
								"Please report to support team.",
								identifier.getPlaceholderKey().getPlaceholder().name() );
						
						break;
				}
				
				if ( results != null && attributeText != null ) {
					
					results = attributeText.format( results );
				}
			}
			
		}
		
		// If results is null, but a PLAYERMINES then must return an empty string:
		if ( results == null && placeHolderKey.getPlaceholder().hasFlag( PlaceholderFlags.MINEPLAYERS ) ) {
			results = "";
		}
		
		identifier.setText(results);
		
		return results;
    }

    
    public String getTranslatePlayerMinesPlaceHolder( UUID playerUuid, String playerName, String identifier ) {
    	String results = null;

    	if ( playerUuid != null ) {
    		
    		List<PlaceHolderKey> placeHolderKeys = getTranslatedPlaceHolderKeys();
    		
    		
    		PlaceholderIdentifier phIdentifier = new PlaceholderIdentifier( identifier );
    		phIdentifier.setPlayer(playerUuid, playerName);
    		
    		
    		
    		
    		for ( PlaceHolderKey placeHolderKey : placeHolderKeys ) {
    			
    			if ( phIdentifier.checkPlaceholderKey(placeHolderKey) ) {
    				
    				results = getTranslateMinesPlaceholder( phIdentifier );
    				
    				break;
    			}
    			
    		}
    	}
    	
    	return results;
    }
    



	private String getRemainingTimeBar( Mine mine, PlaceholderAttribute attribute ) {

		PlaceholderAttributeBar attributeBar = ( attribute instanceof PlaceholderAttributeBar ? (PlaceholderAttributeBar) attribute : null );
		
    	double timeRemaining = mine.getRemainingTimeSec();
    	int time = mine.getResetTime();
    	
    	return PlaceholderManagerUtils.getInstance().
    					getProgressBar( timeRemaining, ((double) time), true, attributeBar );
	}





	/**
     * <p>Generates a list of all of the placeholder keys, which includes the
     * mine name, the placeholder enumeration, and then the actual translated
     * placeholder with the mine's name. This is generated only once, but if new
     * mines are added or mines removed, then just set the class variable to
     * null to auto regenerate with the new values. 
     * </p>
     */
    @Override
    public List<PlaceHolderKey> getTranslatedPlaceHolderKeys() {
    	if ( translatedPlaceHolderKeys == null ) {
    		translatedPlaceHolderKeys = new ArrayList<>();
    		
    		TreeSet<String> blockNames = new TreeSet<>();
    		
    		List<PrisonPlaceHolders> placeHolders = 
    				PrisonPlaceHolders.getTypes( PlaceholderFlags.MINES );
    		
    		placeHolders.addAll( PrisonPlaceHolders.getTypes( PlaceholderFlags.STATSMINES ) );
    		
    		
    		for ( Mine mine : getMines() ) {
    			for ( PrisonPlaceHolders ph : placeHolders ) {
    				String key = ph.name().replace( 
    						PlaceholderManager.PRISON_PLACEHOLDER_MINENAME_SUFFIX, "_" + mine.getName() ).
    						toLowerCase();
    				
    				PlaceHolderKey placeholder = new PlaceHolderKey(key, ph, mine.getName() );
    				if ( ph.getAlias() != null ) {
    					String aliasName = ph.getAlias().name().replace( 
    							PlaceholderManager.PRISON_PLACEHOLDER_MINENAME_SUFFIX, "_" + mine.getName() ).
    							toLowerCase();
    					placeholder.setAliasName( aliasName );
    				}
    				translatedPlaceHolderKeys.add( placeholder );
    				
    				// Getting too many placeholders... add back the extended prefix when looking up:

//    				// Now generate a new key based upon the first key, but without the prison_ prefix:
//    				String key2 = key.replace( 
//    						IntegrationManager.PRISON_PLACEHOLDER_PREFIX + "_", "" );
//    				PlaceHolderKey placeholder2 = new PlaceHolderKey(key2, ph, mine.getName(), false );
//    				translatedPlaceHolderKeys.add( placeholder2 );
    				
    				// capture all of the possible blocks used within the mines:
    				for ( PrisonBlock block : mine.getPrisonBlocks() ) {
    					
    					String blockName = block.getBlockType() == PrisonBlockType.minecraft ?
    											block.getBlockName().toLowerCase() :
    											block.getBlockNameFormal().replace( ":", "-" ).toLowerCase();
    					
    					if ( !blockNames.contains( blockName ) ) {
    						blockNames.add( blockName );
    					}
    				}
    			}
    		}
    		
    		
    		// Next we need to register all the PLAYERMINES.  The mines are dynamic, based upon which one
    		// the player is in.  So this is just a simple registration.
    		List<PrisonPlaceHolders> placeHoldersPM = 
    									PrisonPlaceHolders.getTypes( PlaceholderFlags.MINEPLAYERS );
    		
			for ( PrisonPlaceHolders ph : placeHoldersPM ) {
				String key = ph.name().toLowerCase();
				
				// There is a special condition when a MINEPLAYERS placeholder may have a suffix of 
				// _minename so they need to be expanded the same as a MINES placeholder.
				if ( key.endsWith( PlaceholderManager.PRISON_PLACEHOLDER_MINENAME_SUFFIX ) ) {
					
					for ( Mine mine : getMines() ) {
						String mineKey = ph.name().replace( 
	    						PlaceholderManager.PRISON_PLACEHOLDER_MINENAME_SUFFIX, "_" + mine.getName() ).
	    						toLowerCase();
	    				
	    				PlaceHolderKey placeholder = new PlaceHolderKey(mineKey, ph, mine.getName() );
	    				if ( ph.getAlias() != null ) {
	    					String aliasName = ph.getAlias().name().replace( 
	    							PlaceholderManager.PRISON_PLACEHOLDER_MINENAME_SUFFIX, "_" + mine.getName() ).
	    							toLowerCase();
	    					placeholder.setAliasName( aliasName );
	    				}
	    				translatedPlaceHolderKeys.add( placeholder );
					}
//					PlaceHolderKey placeholder = new PlaceHolderKey(key, ph );
//					if ( ph.getAlias() != null ) {
//						String aliasName = ph.getAlias().name().toLowerCase();
//						placeholder.setAliasName( aliasName );
//					}
//					translatedPlaceHolderKeys.add( placeholder );
				}
				else {
					
					PlaceHolderKey placeholder = new PlaceHolderKey(key, ph );
					if ( ph.getAlias() != null ) {
						String aliasName = ph.getAlias().name().toLowerCase();
						placeholder.setAliasName( aliasName );
					}
					translatedPlaceHolderKeys.add( placeholder );
				}
			}
			
			
			
			// Next we need to register all the PLAYERMINES.  The mines are dynamic, based upon which one
			// the player is in.  So this is just a simple registration.
			List<PrisonPlaceHolders> placeHoldersBN = 
					PrisonPlaceHolders.getTypes( PlaceholderFlags.PLAYERBLOCKS );
			
			for ( PrisonPlaceHolders bn : placeHoldersBN ) {
				String key = bn.name().toLowerCase();
				
				// There is a special condition when a MINEPLAYERS placeholder may have a suffix of 
				// _minename so they need to be expanded the same as a MINES placeholder.
				if ( key.endsWith( PlaceholderManager.PRISON_PLACEHOLDER_PLAYERBLOCK_SUFFIX ) ) {
					
					for ( String blockName : blockNames ) {
						String mineKey = bn.name().replace( 
								PlaceholderManager.PRISON_PLACEHOLDER_PLAYERBLOCK_SUFFIX, "__" + blockName ).
								toLowerCase();
						
						PlaceHolderKey placeholder = new PlaceHolderKey(mineKey, bn, blockName );
						if ( bn.getAlias() != null ) {
							String aliasName = bn.getAlias().name().replace( 
									PlaceholderManager.PRISON_PLACEHOLDER_PLAYERBLOCK_SUFFIX, "__" + blockName ).
									toLowerCase();
							placeholder.setAliasName( aliasName );
						}
						translatedPlaceHolderKeys.add( placeholder );
					}
//					PlaceHolderKey placeholder = new PlaceHolderKey(key, ph );
//					if ( ph.getAlias() != null ) {
//						String aliasName = ph.getAlias().name().toLowerCase();
//						placeholder.setAliasName( aliasName );
//					}
//					translatedPlaceHolderKeys.add( placeholder );
				}
				else {
					
					PlaceHolderKey placeholder = new PlaceHolderKey(key, bn );
					if ( bn.getAlias() != null ) {
						String aliasName = bn.getAlias().name().toLowerCase();
						placeholder.setAliasName( aliasName );
					}
					translatedPlaceHolderKeys.add( placeholder );
				}
			}

    	}
    	return translatedPlaceHolderKeys;
    }

    /**
     * <p>If new
     * mines are added or mines removed, then just set the class variable to
     * null to auto regenerate with the new values. 
     * </p>
     */
    public void resetTranslatedPlaceHolderKeys() {
    	translatedPlaceHolderKeys = null;
    }
    
    
    @Override
    public void reloadPlaceholders() {
    	
    	// clear the class variable so they will regenerate:
    	translatedPlaceHolderKeys = null;
    	
    	// Regenerate the translated placeholders:
    	getTranslatedPlaceHolderKeys();
    }




}
