/*
 * Prison is a Minecraft plugin for the prison game mode.
 * Copyright (C) 2017 The Prison Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tech.mcprison.prison.mines.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.file.FileIOData;
import tech.mcprison.prison.internal.World;
import tech.mcprison.prison.internal.block.PrisonBlock;
import tech.mcprison.prison.internal.block.PrisonBlockStatusData;
import tech.mcprison.prison.mines.MineException;
import tech.mcprison.prison.mines.PrisonMines;
import tech.mcprison.prison.mines.features.MineBlockEvent;
import tech.mcprison.prison.mines.features.MineLinerData;
import tech.mcprison.prison.mines.managers.MineManager;
import tech.mcprison.prison.output.Output;
import tech.mcprison.prison.placeholders.PlaceholderStringCoverter;
import tech.mcprison.prison.selection.Selection;
import tech.mcprison.prison.sorting.PrisonSortable;
import tech.mcprison.prison.store.Document;
import tech.mcprison.prison.util.Bounds;
import tech.mcprison.prison.util.Location;
import tech.mcprison.prison.util.ObsoleteBlockType;

/**
 * @author Dylan M. Perks
 */
@SuppressWarnings( "deprecation" )
public class Mine 
	extends MineScheduler 
	implements PrisonSortable, Comparable<Mine>, 
			PlaceholderStringCoverter,
			FileIOData {
	
	
	public static final int MINE_RESET__TIME_SEC__DEFAULT = 15 * 60; // 15 minutes
	public static final int MINE_RESET__TIME_SEC__MINIMUM = 30; // 30 seconds
	public static final long MINE_RESET__BROADCAST_RADIUS_BLOCKS = 150;
	
	public static final String MINE_NOTIFICATION_PERMISSION_PREFIX = "mines.notification.";	
	
	
	public enum MineType {
		primary,
		playerMines
		;
		
		public static MineType fromString( String mineType ) {
			MineType results = primary;
			
			if ( mineType != null && !mineType.trim().isEmpty() ) {
				for ( MineType mt : values() ) {
					if ( mt.name().equalsIgnoreCase( mineType ) ) {
						results = mt;
					}
				}
			}
			
			return results;
		}
	}
	
	public enum MineUnitTestUsage {
		TRUE;
	}

    public enum MineNotificationMode {
    	disabled,
    	disable,
    	within,
    	radius,
    	world,
    	server,
    	
    	displayOptions
    	;
    	
    	public static MineNotificationMode fromString(String mode) {
    		return fromString(mode, radius);
    	}
    	public static MineNotificationMode fromString(String mode, MineNotificationMode defaultValue) {
    		MineNotificationMode results = defaultValue;
    		
    		if ( mode != null && mode.trim().length() > 0 ) {
    			for ( MineNotificationMode mnm : values() ) {
    				if ( mnm.name().equalsIgnoreCase( mode )) {
    					results = mnm;
    				}
    			}
    		}
    		
    		if ( results == disable ) {
    			results = disabled;
    		}
    		
    		return results;
    	}
    }

    /**
     * Creates a new, empty mine instance
     */
    public Mine() {
        super();
        
        // Kick off the initialize:
        initialize();
    }

    /**
     * <p>This constructor should ONLY be used in unit test since it will NOT run
     * any of the initialization code so the automated services will not be started,
     * which are not needed for unit tests.
     * </p>
     * 
     * @param unitTestUsage
     */
    public Mine( MineUnitTestUsage unitTestUsage, String mineName ) {
    	super();
		
		setName( mineName );
		
		// Kick off the initialize:
		//initialize();
    }

    
    /**
     * <p>This is called when a mine is first created.
     * </p>
     * 
     * @param name
     * @param selection
     */
    public Mine(String name, Selection selection) {
    	this( name, selection, MineType.primary );
    }
    
    public Mine(String name, Selection selection, MineType mineType, boolean logInfo ) {
    	super();
    	
    	setName(name);
    	
    	setMineType( mineType );
    	
    	if ( selection == null ) {
    		setVirtual( true );
    	}
    	else {
    		
    		setBounds(selection.asBounds(), logInfo );
    		
    		setWorldName( getBounds().getMin().getWorld().getName());
    		
    		setEnabled( true );
    	}
        
        // Kick off the initialize:
        initialize();
    }
    
    public Mine(String name, Selection selection, MineType mineType) {
    	this( name, selection, mineType, true );
		
    }
    
    /**
     * <p>Loads a mine from a document.
     * </p>
     * 
     * <p>Note that the location where the loadFromDocument() occurs in the whole 
     * "create the objects" is in the "middle".  All classes that are extended
     * from are instantiated first due to the super() function call.  So when Mine
     * tries to be instantiated, it first drills all the way down to MineData and 
     * then runs all the initialization code within MineData and then works back 
     * through all of the classes, instantiating everything, one layer at a time.
     * </p>
     * 
     * <p>Then when it gets back up to this class, Mine, all parents have been fully
     * instantiated so all collections will have been assigned non-null values 
     * as an example.  Then this class loads the data from the document object.
     * This is important, since all parents have been initialized, now the document
     * loader is making it a "mine". 
     * </p>
     * 
     * <p>Then at that point, after the mine data is loaded, it once again drills all the
     * way down to the MineData ancestor class, using the initialize() functions, where
     * it then starts to initialize all classes from MineData, back up to Mine.
     * What this enables and allows, is when a class is initialized, it will have access
     * to the fully loaded mine data.  This is a perfect example of being able to start
     * submitting the mine reset jobs since all data has been loaded, and all lower 
     * functions have been ran.
     * </p>
     * 
     * <p>So the over all design of the Mine objects is that all ancestors instantiate
     * first, from MineData to Mine. Then the mine is loaded from the file system.
     * Then all ancestors are initialized from MineData to Mine.  This gives a high
     * degree of control over when actions can be ran over a mine, and have confidence the
     * data and conditions will be there.
     * </p>
     *
     * @param document The document to load from.
     * @throws MineException If the mine couldn't be loaded from the document.
     */
    public Mine(Document document) throws MineException {
    	super();
    	
        loadFromDocument( document );
        
        // Kick off the initialize:
        // This is critically vital to ensure the workflow is generated with the contents
        // from the document and not the defaults as set by the super().
        initialize();
    }

    
    /**
     * <p>This initialize function gets called after the classes are
     * instantiated, and is initiated from Mine class and propagates
     * to the MineData class.  Good for kicking off the scheduler.
     * </p>
     */
	@Override
	protected void initialize() {
    	super.initialize();
    	
    }

	/**
	 * <p>The loading of a mine checks to ensure if the world exists.  If not, then 
	 * traditionally, it would not load the mine.  The problem with this model is 
	 * the world may not exist yet, if running Multiverse-core (or another similar 
	 * plugin) and as such, may falsely cause mine failures.  This is the situation if
	 * the mine exists within a world that must be loaded by Multiverse-core.  If it was
	 * a standard world, then it would be fine. 
	 * </p>
	 * 
	 * <p>Soft dependencies do not provide a solution. One bad solution for this 
	 * situation, is to manually add a hard dependency to Multiverse-core. This
	 * should not be used.
	 * </p>
	 * 
	 * <p>As a better solution to this problem, mines will be loaded as normal, but
	 * if the world does not exist, then their initialization, or enablement, will be
	 * delayed until the world is available. 
	 * </p>
	 * 
	 * @param document
	 * @throws MineException
	 */
	@SuppressWarnings( { "unchecked" } )
	private void loadFromDocument( Document document )
			throws MineException {
		
		boolean dirty = false;
		boolean inconsistancy = false;
		
		String worldName = (String) document.get("world");
        setWorldName( worldName );
        setName((String) document.get("name")); // Mine name:

        
        String tag = (String) document.get("tag");
        setTag( tag );

        
        String mineType = (String) document.get("mineType");
        if ( mineType == null ) {
        	// If mineType was not saved, then it was a primary mine:
        	mineType = MineType.primary.name();
        	dirty = true;
        }
        setMineType( MineType.fromString( mineType ) );

        
        String accessPerm = (String) document.get("accessPermission");
        setAccessPermission( (accessPerm == null || accessPerm.trim().isEmpty() ? null : accessPerm) );
        
               
    	setTpAccessByRank( document.get("tpAccessByRank") == null ? false : (boolean) document.get("tpAccessByRank") );
    	setMineAccessByRank( document.get("mineAccessByRank") == null ? false : (boolean) document.get("mineAccessByRank") );

    	
    	setVirtual( document.get("isVirtual") == null ? false : (boolean) document.get("isVirtual") );
        
        
        Double sortOrder = (Double) document.get( "sortOrder" );
        setSortOrder( sortOrder == null ? 0 : sortOrder.intValue() );

        
		World world = null;
		
		if ( !isVirtual() ) {
			if ( worldName == null || "Virtually-Undefined".equalsIgnoreCase( worldName ) ) {
				Output.get().logInfo( "Mines.loadFromDocument: Failure: World does not exist in Mine " +
						"file. mine= %s  world= %s " +
						"Contact support on how to fix.",  
						getName(), (world == null ? "null" : world ));
			}
			
			Optional<World> worldOptional = Prison.get().getPlatform().getWorld(worldName);
			if (!worldOptional.isPresent()) {
				MineManager mineMan = PrisonMines.getInstance().getMineManager();
				
				// Store this mine and the world in MineManager's unavailableWorld for later
				// processing and hooking up to the world object. Print an error message upon
				// the first mine's world not existing.
				mineMan.addUnavailableWorld( worldName, this );
				
				setEnabled( false );
			}
			else {
				world = worldOptional.get();
				setEnabled( true );
			}

			//        World world = worldOptional.get();
			
		
			
			Location locMin = getLocation(document, world, "minX", "minY", "minZ");
			Location locMax = getLocation(document, world, "maxX", "maxY", "maxZ");
			
			setBounds( new Bounds( 
					locMin,
					locMax));
			
			setHasSpawn((boolean) document.get("hasSpawn"));
			if (isHasSpawn()) {
				setSpawn(getLocation(document, world, "spawnX", "spawnY", "spawnZ", "spawnPitch", "spawnYaw"));
			}
			
		}
        

        
        Double resetTimeDouble = (Double) document.get("resetTime");
        setResetTime( resetTimeDouble != null ? resetTimeDouble.intValue() : PrisonMines.getInstance().getConfig().resetTime );

        
        setNotificationMode( MineNotificationMode.fromString( (String) document.get("notificationMode")) ); 
        Double noteRadius = (Double) document.get("notificationRadius");
        setNotificationRadius( noteRadius == null ? MINE_RESET__BROADCAST_RADIUS_BLOCKS : noteRadius.longValue() );

        Double zeroBlockResetDelaySec = (Double) document.get("zeroBlockResetDelaySec");
        setZeroBlockResetDelaySec( zeroBlockResetDelaySec == null ? 0.0d : zeroBlockResetDelaySec.doubleValue() );
        
        Boolean skipResetEnabled = (Boolean) document.get( "skipResetEnabled" );
        setSkipResetEnabled( skipResetEnabled == null ? false : skipResetEnabled.booleanValue() );
        Double skipResetPercent = (Double) document.get( "skipResetPercent" );
        setSkipResetPercent( skipResetPercent == null ? 80.0D : skipResetPercent.doubleValue() );
        Double skipResetBypassLimit = (Double) document.get( "skipResetBypassLimit" );
        setSkipResetBypassLimit( skipResetBypassLimit == null ? 50 : skipResetBypassLimit.intValue() );

        Double resetThresholdPercent = (Double) document.get( "resetThresholdPercent" );
        setResetThresholdPercent( resetThresholdPercent == null ? 0 : resetThresholdPercent.doubleValue() );
 
        // When loading, skipResetBypassCount must be set to zero:
        setSkipResetBypassCount( 0 );
        
        
        String rankString = (String) document.get( "rank" );
        setRank( null );
        setRankString( rankString );
        
        
        long totalBlockCount = 0L;
        
        
        // This is a validation set to ensure only one block type is loaded file system.
        // Must keep the first one loaded.
        Set<String> validateBlockNames = new HashSet<>();
        getBlocks().clear();

        List<String> docBlocks = (List<String>) document.get("blocks");
		for (String docBlock : docBlocks) {
			
			// If the file is manually edited and a comma is added to the end of the block list,
			// then docBlock could be null.  Skip processing if null.
			if ( docBlock != null ) {
				
				String[] split = docBlock.split("-");
				String blockTypeName = split[0];
//				double chance = split.length > 1 ? Double.parseDouble(split[1]) : 0;
//				long blockCount = split.length > 2 ? Long.parseLong(split[2]) : 0;
//				int constraintMin = split.length > 3 ? Integer.parseInt(split[3]) : 0;
//				int constraintMax = split.length > 4 ? Integer.parseInt(split[4]) : 0;
				
				if ( blockTypeName != null && !validateBlockNames.contains( blockTypeName )) {
					// Use the BlockType.name() load the block type:
					ObsoleteBlockType blockType = ObsoleteBlockType.getBlock(blockTypeName);
					if ( blockType != null ) {
						
						/**
						 * <p>The following is code to correct the use of items being used as a
						 * block in a mine, which will cause a failure in trying to place an 
						 * item as a block.
						 * </p>
						 * 
						 * <p>This is intended for the old block model and is temp code to ensure 
						 * that there are less errors the end user will experience.
						 * </p>
						 */
						String errorMessage = "Warning! An invalid block type of %s was " +
								"detect when loading blocks for " +
								"mine %s. %s is not a valid block type. Using " +
								"%s instead. If this is incorrect please fix manually.";
						
						if ( blockType == ObsoleteBlockType.REDSTONE ) {
							ObsoleteBlockType itemType = blockType;
							blockType = ObsoleteBlockType.REDSTONE_ORE;
							
							Output.get().logError( 
									String.format( errorMessage, itemType.name(), getName(), 
											"Redstone dust", blockType.name()) );
							
							dirty = true;
						}
						else if ( blockType == ObsoleteBlockType.NETHER_BRICK ) {
							ObsoleteBlockType itemType = blockType;
							blockType = ObsoleteBlockType.DOUBLE_NETHER_BRICK_SLAB;
							
							Output.get().logError( 
									String.format( errorMessage, itemType.name(), getName(), 
											"Individual nether brick", blockType.name()) );
							
							dirty = true;
						}
						
						BlockOld block = new BlockOld(blockType);

						block.parseFromSaveFileFormatStats( docBlock );
						
						totalBlockCount += block.getBlockCountTotal();
								
//						BlockOld block = new BlockOld(blockType, chance, blockCount);
//						block.setConstraintMin( constraintMin );
//						block.setConstraintMax( constraintMax );

						getBlocks().add(block);
					}
					else {
						String message = String.format( "Failure in loading block type from %s mine's " +
								"save file. Block type %s has no mapping.", getName(),
								blockTypeName );
						Output.get().logError( message );
					}
					
					validateBlockNames.add( blockTypeName );
				}
				else if (validateBlockNames.contains( blockTypeName ) ) {
					// Detected and fixed a duplication so mark as dirty so fixed block list is saved:
					dirty = true;
					inconsistancy = true;
				}
			}
			
        }
        
        
		// Reset validation checks:
		validateBlockNames.clear();
		getPrisonBlocks().clear();
		
		List<String> docPrisonBlocks = (List<String>) document.get("prisonBlocks");
		if ( docPrisonBlocks != null ) {
			
			
			// If prisonBlocks are defined, then use these for the block counts:
			totalBlockCount = 0L;
			
			for (String docBlock : docPrisonBlocks) {
				
				if ( docBlock != null ) {
					
					PrisonBlock prisonBlock = PrisonBlockStatusData.parseFromSaveFileFormat( docBlock );
					
					// If the server version is less than 1.13.0, and if the block is a "_wood" block, 
					// then it needs to be remapped to "_planks" so the resulting block will work properly.
					// Versions prior to 1.13.0, _WOOD is identical to _PLANKS.
					if ( prisonBlock != null && 
							prisonBlock.getBlockName().toLowerCase().contains( "_wood" ) &&
							Prison.get().getPlatform().compareServerVerisonTo( "1.13.0" ) < 0 ) {
						String fixedName = docBlock.toLowerCase().replace( "_wood", "_planks" );
						
						prisonBlock = PrisonBlockStatusData.parseFromSaveFileFormat( fixedName );
						dirty = true;
					}
					
					if ( prisonBlock == null && "grass".equalsIgnoreCase(docBlock) ) {
						// For spigot v20.0.4 GRASS was changed to SHORT_GRASS
						
						String fixedName = "SHORT_GRASS".toLowerCase();
						
						prisonBlock = PrisonBlockStatusData.parseFromSaveFileFormat( fixedName );
						dirty = true;
						
						Output.get().logInfo( "NOTE: Block named GRASS has ben changed to SHORT_GRASS "
								+ "due to XMaterial's support for spigot v20.0.4. Please verify that "
								+ "it's spawning correctly on mine resets." );
					}
					
					if ( prisonBlock != null ) {
						
						totalBlockCount += prisonBlock.getBlockCountTotal();
						
						if ( !validateBlockNames.contains( prisonBlock.getBlockName() )) {
							
							if ( prisonBlock.isLegacyBlock() ) {
								dirty = true;
							}
							addPrisonBlock( prisonBlock );
							
							validateBlockNames.add( prisonBlock.getBlockName() );
						}
						else if ( validateBlockNames.contains( prisonBlock.getBlockName() ) ) {
							// Detected and fixed a duplication so mark as dirty so fixed block list is saved:
							dirty = true;
							inconsistancy = true;
						}
						
					}
					
					
					
//					String[] split = docBlock.split("-");
//					String blockTypeName = split[0];
//					double chance = split.length > 1 ? Double.parseDouble(split[1]) : 0;
//					long blockCount = split.length > 2 ? Long.parseLong(split[2]) : 0;
//					int constraintMin = split.length > 3 ? Integer.parseInt(split[3]) : 0;
//					int constraintMax = split.length > 4 ? Integer.parseInt(split[4]) : 0;
//					int constraintExcludeTopLayers = split.length > 5 ? Integer.parseInt(split[5]) : 0;
//					int constraintExcludeBottomLayers = split.length > 6 ? Integer.parseInt(split[6]) : 0;
//
//					if ( blockTypeName != null ) {
//						// The new way to get the PrisonBlocks:  
//						//   The blocks return are cloned so they have their own instance:
//						PrisonBlock prisonBlock = Prison.get().getPlatform().getPrisonBlock( blockTypeName );
//						
//						if ( prisonBlock != null && !validateBlockNames.contains( blockTypeName )) {
//							prisonBlock.setChance( chance );
//							prisonBlock.setBlockCountTotal( blockCount );
//							prisonBlock.setConstraintMin( constraintMin );
//							prisonBlock.setConstraintMax( constraintMax );
//							prisonBlock.setConstraintExcludeTopLayers( constraintExcludeTopLayers );
//							prisonBlock.setConstraintExcludeBottomLayers( constraintExcludeBottomLayers );
//
//							
//							if ( prisonBlock.isLegacyBlock() ) {
//								dirty = true;
//							}
//							addPrisonBlock( prisonBlock );
//							
//							validateBlockNames.add( blockTypeName );
//						}
//						else if (validateBlockNames.contains( blockTypeName ) ) {
//							// Detected and fixed a duplication so mark as dirty so fixed block list is saved:
//							dirty = true;
//							inconsistancy = true;
//						}
//						
//					}
					
					
				}
			}
			
		}
		
		
		// Set the total blocks mined count:
		setTotalBlocksMined( totalBlockCount );

		
		// Check if one of the blocks is effected by gravity, and if so, set that indicator.
		checkGravityAffectedBlocks();

        
		// Using the Obsolete old block model for conversion to the new block model
		// NOTE: This is the ONLY place were we are allowed to use the old block model! ;)
        if ( // isUseNewBlockModel() && 
        		getPrisonBlocks().size() == 0 && getBlocks().size() > 0 ) {
        	// Need to perform the initial conversion: 
        	
        	for ( BlockOld blockOld : getBlocks() ) {
        		PrisonBlock prisonBlock = Prison.get().getPlatform().getPrisonBlock( blockOld.getType().name() );

        		if ( prisonBlock == null ) {
        			for ( String altName : blockOld.getType().getXMaterialAltNames() ) {
        				
        				prisonBlock = Prison.get().getPlatform().getPrisonBlock( altName );
        				if ( prisonBlock != null ) {
        					break;
        				}
        			}
        		}
        		
        		if ( prisonBlock != null ) {
            		
            		// This transfers all the stats over so none are lost.
            		prisonBlock.transferStats( blockOld );
            		
            		addPrisonBlock( prisonBlock );

            		dirty = true;
            	}
        		
			}
        	Output.get().logInfo( "Notice: Mine: " + getName() + ": Existing prison block model has " +
        			"been converted to the new block model and will be saved." );
        }
        
        
        List<String> commands = (List<String>) document.get("commands");
        setResetCommands( commands == null ? new ArrayList<>() : commands );
        
        
//        Boolean usePagingOnReset = (Boolean) document.get( "usePagingOnReset" );
//        setUsePagingOnReset( usePagingOnReset == null ? false : usePagingOnReset.booleanValue() );

        
        List<String> mineBlockEvents = (List<String>) document.get("mineBlockEvents");
        if ( mineBlockEvents != null ) {
        	for ( String blockEvent : mineBlockEvents ) {
        		if ( blockEvent != null ) {
        			
        			MineBlockEvent bEvent = MineBlockEvent.fromSaveString( blockEvent, this.getName() );
        			
        			if ( bEvent != null ) {
        				
        				getBlockEvents().add( bEvent );
        			}
        			else {
        				Output.get().logInfo( "Notice: Mine: " + getName() + ": Error trying to parse a blockEvent. "
        						+ "BlockEvent is lost: raw BlockEvent= [" + blockEvent + "]" );
        			}
        		}
        	}
        }
        
        
        String mineLinerData = (String) document.get("mineLinerData");
        setLinerData( MineLinerData.fromSaveString( mineLinerData ) );

        Boolean mineSweeperEnabled = (Boolean) document.get( "mineSweeperEnabled" );
        setMineSweeperEnabled( mineSweeperEnabled == null ? false : mineSweeperEnabled.booleanValue() );

        
        if ( dirty ) {
			
        	// Resave the mine data since an update to the mine format was detected and
        	// needs to be saved. Otherwise the bad data will always need to be converted
        	// every time the mine is loaded which may lead to other issues.
        	
        	// This is enabled since the original is not modified.
        	
        	// If dirty, then make a backup since these are automatic changes:
        	PrisonMines.getInstance().getMineManager().backupMine( this );

        	PrisonMines.getInstance().getMineManager().saveMine( this );
        	
        	if ( inconsistancy ) {
        		
        		Output.get().logInfo( "Notice: Mine: " + getName() + ": During the loading of this mine an " +
        				"inconsistancy was detected and was fixed then saved." );
        	}
        	else {
        		Output.get().logInfo( "Notice: Mine: " + getName() + ": Updated mine data was successfully saved." );
        		
        	}
        }
	}

    
	public Document toDocument() {
        Document ret = new Document();
        
        // If world name is not set, try to get it from the bounds:
        String worldName = getWorldName();
        if ( (worldName == null || worldName.trim().length() == 0 ||
        		"Virtually-Undefined".equalsIgnoreCase( worldName )) &&
        		getBounds() != null && getBounds().getMin() != null &&
        		getBounds().getMin().getWorld() != null ) {
        	worldName = getBounds().getMin().getWorld().getName();
        	setWorldName( worldName );
        }
        ret.put("world", worldName );
        ret.put("name", getName());
        
        ret.put( "isVirtual", isVirtual() );

        ret.put( "tpAccessByRank", isTpAccessByRank() );
        ret.put( "mineAccessByRank", isMineAccessByRank() );
        
        String accessPerm = getAccessPermission();
        ret.put( "accessPermission", accessPerm == null || accessPerm.trim().isEmpty() ? "" : accessPerm );

        ret.put( "tag", getTag() );
        ret.put( "sortOrder", getSortOrder() );

        
        ret.put( "mineType", getMineType().name() );
        
        
        if ( !isVirtual() ) {
        	ret.put("minX", getBounds().getMin().getX());
        	ret.put("minY", getBounds().getMin().getY());
        	ret.put("minZ", getBounds().getMin().getZ());
        	ret.put("maxX", getBounds().getMax().getX());
        	ret.put("maxY", getBounds().getMax().getY());
        	ret.put("maxZ", getBounds().getMax().getZ());
        	ret.put("hasSpawn", isHasSpawn());
        	
        }
        
        ret.put("resetTime", getResetTime() );
        ret.put("notificationMode", getNotificationMode().name() );
        ret.put("notificationRadius", Long.valueOf( getNotificationRadius() ));

        ret.put( "zeroBlockResetDelaySec", Double.valueOf( getZeroBlockResetDelaySec() ) );
        
        ret.put( "skipResetEnabled", isSkipResetEnabled() );
        ret.put( "skipResetPercent", getSkipResetPercent() );
        ret.put( "skipResetBypassLimit", getSkipResetBypassLimit() );
        
        ret.put( "resetThresholdPercent", getResetThresholdPercent() );
        
        if (isHasSpawn()) {
            ret.put("spawnX", getSpawn().getX());
            ret.put("spawnY", getSpawn().getY());
            ret.put("spawnZ", getSpawn().getZ());
            ret.put("spawnPitch", getSpawn().getPitch());
            ret.put("spawnYaw", getSpawn().getYaw());
        }

        // This is a validation set to ensure only one block is written to file system:
        Set<String> validateBlockNames = new HashSet<>();

        // NOTE: This is using the obsolete old block model!!
        // This is the ONLY SECOND place where we can use the old block model!
        // We want to "preserve" the old blocks that may have been setup up in the mines
        // originally.  In a future release, these may be purged.
        List<String> blockStrings = new ArrayList<>();
        for (BlockOld block : getBlocks()) {
        	if ( !validateBlockNames.contains( block.getType().name() )) {
        		
        		blockStrings.add( block.toSaveFileFormat() );
        		
//        		// Use the BlockType.name() to save the block type to the file:
//        		blockStrings.add(block.getType().name() + "-" + block.getChance());
//            blockStrings.add(block.getType().getId() + "-" + block.getChance());
        		validateBlockNames.add( block.getType().name() );
        	}
        }
        
        ret.put("blocks", blockStrings);

        // reset validation for next block list:
        validateBlockNames.clear();
        
        List<String> prisonBlockStrings = new ArrayList<>();
        for (PrisonBlock pBlock : getPrisonBlocks() ) {
        	if ( !validateBlockNames.contains( pBlock.getBlockName()) ) {
        		
        		prisonBlockStrings.add( pBlock.toSaveFileFormat() );
        		
//        		prisonBlockStrings.add(pBlock.getBlockNameFormal() + "-" + pBlock.getChance());
        		validateBlockNames.add( pBlock.getBlockNameFormal() );
        	}
        }
        
        ret.put("prisonBlocks", prisonBlockStrings);

        ret.put("commands", getResetCommands());
        
        
//        ret.put( "usePagingOnReset", isUsePagingOnReset() );
        
        
        if ( getRank() != null ) {
        	String rank = getRank().getModuleElementType() + "," + getRank().getName() + "," + 
        			getRank().getId() + "," + getRank().getTag();
        	ret.put("rank", rank );
        }

        List<String> mineBlockEvents = new ArrayList<>();
        for ( MineBlockEvent blockEvent : getBlockEvents() ) {
			mineBlockEvents.add( blockEvent.toSaveString() );
		}
        ret.put("mineBlockEvents", mineBlockEvents);
        
        
        String mineLinerData = getLinerData().toSaveString();
        ret.put("mineLinerData", mineLinerData );
        
        ret.put( "mineSweeperEnabled", isMineSweeperEnabled() );

        
        return ret;
    }

    @Override
    public String toString() {
    	return getName() + "  " + getTotalBlocksMined();
    }
    
    /**
     * <p>Even if world is null, it will allow you to create a location, but
     * the location will be invalid.  In order to use this location, 
     * the world will have to be set to a valid world.
     * </p>
     * 
     * @param doc
     * @param world
     * @param x
     * @param y
     * @param z
     * @return
     */
    private Location getLocation(Document doc, World world, String x, String y, String z) {
    	Location results = null;
    	
//    	if ( world != null ) {
//    		
//    		
//    	}
    	Object xD = doc.get(x);
    	Object yD = doc.get(y);
    	Object zD = doc.get(z);
    	
    	if ( xD != null && yD != null && zD != null ) {
    		
    		results = new Location(world, (double) xD, (double) yD, (double) zD );
    	}
    	
    	return results;
    }
    
    private Location getLocation(Document doc, World world, String x, String y, String z, String pitch, String yaw) {
    	Location loc = getLocation(doc, world, x, y, z);
    	
    	Object pitchD = doc.get(pitch);
    	Object yawD = doc.get(yaw);
    	
    	if ( pitchD != null ) {
    		
    		loc.setPitch( ((Double) pitchD ).floatValue() );
    	}
    	
    	if ( yawD != null ) {
    		
    		loc.setYaw( ((Double) yawD ).floatValue() );
    	}
    	return loc;
    }
    
    
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Mine) && (((Mine) obj).getName()).equals(getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }


	public String getBlockListString()
	{
		StringBuilder sb = new StringBuilder();

		for ( PrisonBlock block : getPrisonBlocks()) {
			if ( sb.length() > 0 ) {
				sb.append( ", " );
			}
			sb.append( block.toString() );
		}


		sb.insert( 0, ": [" );
		sb.append( "]" );
		sb.insert( 0, getName() );
		sb.insert( 0, "Mine " );
		
		return sb.toString();
	}

	@Override
	public int compareTo( Mine o ) {
		return getName().toLowerCase().compareTo( o.getName().toLowerCase() );
	}
	
	

	@Override
	public String getStringPlaceholders() {
		return "{mine} {mine_tag} {mine_world} {mine_type} {mine_group} " +
				"{mine_rank} {mine_air_count} {mine_player_count} " +
				"{mine_block_break_count} {mine_block_remaining_count} {mine_block_remaining_percent} " +
				"{mine_blocks_mined_total} {mine_reset_time_sec} {mine_reset_time_remaining_sec} " +
				"{mine_block_[block]_name} {mine_block_[block]_type} {mine_block_[block]_placed} " +
				"{mine_block_[block]_remaining} {mine_block_[block]_total} {mine_block_[block]_chance}";
	}

	
    /**
     * <p>This function will provide support for secondary placeholders for all mine related placeholders.
     * These secondary placeholders are in addition to the preexisting positional placeholders
     * that are hard coded for the specific parameters. 
     * </p>
     * 
     * <p>These secondary placeholders can be inserted anywhere in the message.
     * </p>
     * 
     * <ul>
     * 	<li>{mine}</li>
     * 	<li>{mine_tag}</li>
     * 	<li>{mine_world}</li>
     * 	<li>{mine_type}</li>
     * 	<li>{mine_group}</li>
     * 
     * 	<li>{mine_rank}</li>
     * 	<li>{mine_air_count}</li>
     * 	<li>{mine_player_count}</li>
     * 
     * 	<li>{mine_block_break_count}</li>
     *  <li>{mine_block_remaining_count}</li>
     *  <li>{mine_block_remaining_percent}</li>
     *  <li>{mine_blocks_mined_total}</li>
     *  <li>{mine_reset_time_sec}</li>
     *  <li>{mine_reset_time_remaining_sec}</li>
     *  
     *  <li>{mine_block_<block>_name}</li>
     *  <li>{mine_block_<block>_type}</li>
     *  <li>{mine_block_<block>_placed}</li>
     *  <li>{mine_block_<block>_remaining}</li>
     *  <li>{mine_block_<block>_total}</li>
     *  <li>{mine_block_<block>_chance}</li>

     * 
     *  <li>{mine} {mine_tag} {mine_world} {mine_type} {mine_group}
	 *		{mine_rank} {mine_air_count} {mine_player_count} 
	 *		{mine_block_break_count} {mine_block_remaining_count} {mine_block_remaining_percent}
	 *		{mine_blocks_mined_total} {mine_reset_time_sec} {mine_reset_time_remaining_sec}
	 *		{mine_block_[block]_name} {mine_block_[block]_type} {mine_block_[block]_placed}
	 *		{mine_block_[block]_remaining} {mine_block_[block]_total} {mine_block_[block]_chance}
	 *  </li>
     * 
     * </ul>
     * 
     * @param rankPlayer
     * @param results
     * @return
     */
	@Override
	public String convertStringPlaceholders(String results) {

		Mine mine = this;
		
		if ( mine != null ) {
			
			results = applySecondaryPlaceholdersCheck( "{mine}", mine.getName(), results );
			results = applySecondaryPlaceholdersCheck( "{mine_tag}", mine.getTag(), results );
			results = applySecondaryPlaceholdersCheck( "{mine_world}", mine.getWorldName(), results );
			results = applySecondaryPlaceholdersCheck( "{mine_type}", mine.getMineType().name(), results );
			results = applySecondaryPlaceholdersCheck( "{mine_group}", mine.getMineGroup().getName(), results );

			results = applySecondaryPlaceholdersCheck( "{mine_rank}", mine.getRankString(), results );
			
			results = applySecondaryPlaceholdersCheck( "{mine_air_count}", 
					Integer.toString( mine.getAirCount()), results );
			results = applySecondaryPlaceholdersCheck( "{mine_player_count}", 
					Integer.toString( mine.getPlayerCount()), results );
			
			results = applySecondaryPlaceholdersCheck( "{mine_block_break_count}", 
					Integer.toString( mine.getBlockBreakCount()), results );
			results = applySecondaryPlaceholdersCheck( "{mine_block_remaining_count}", 
					Integer.toString( mine.getRemainingBlockCount()), results );
			results = applySecondaryPlaceholdersCheck( "{mine_block_remaining_percent}", 
					Double.toString( (mine.getPercentRemainingBlockCount() * 100.0d )), results );
			results = applySecondaryPlaceholdersCheck( "{mine_blocks_mined_total}", 
					Long.toString( mine.getTotalBlocksMined()), results );
			
			results = applySecondaryPlaceholdersCheck( "{mine_reset_time_sec}", 
					Integer.toString( mine.getResetTime()), results );
			results = applySecondaryPlaceholdersCheck( "{mine_reset_time_remaining_sec}", 
					Double.toString( mine.getRemainingTimeSec()), results );
			
			
			Set<String> keys = mine.getBlockStats().keySet();
			for (String key : keys) {
				PrisonBlockStatusData blockStat = mine.getBlockStats().get( key );
				
				String bName = blockStat.getBlockName().toLowerCase();
				String type = blockStat.getBlockType().name();
				
				long placed = blockStat.getBlockPlacedCount();
				long remaining = blockStat.getBlockPlacedCount() - blockStat.getBlockCountUnsaved();
				long total = blockStat.getBlockCountTotal();
				
				Double chance = blockStat.getChance() * 100.0d;
				
				results = applySecondaryPlaceholdersCheck( "{mine_block_" + bName + "_name}", 
						bName, results );
				results = applySecondaryPlaceholdersCheck( "{mine_block_" + bName + "_type}", 
						type, results );

				results = applySecondaryPlaceholdersCheck( "{mine_block_" + bName + "_placed}", 
						Long.toString( placed ), results );
				results = applySecondaryPlaceholdersCheck( "{mine_block_" + bName + "_remaining}", 
						Long.toString( remaining ), results );
				results = applySecondaryPlaceholdersCheck( "{mine_block_" + bName + "_total}", 
						Long.toString( total ), results );
				results = applySecondaryPlaceholdersCheck( "{mine_block_" + bName + "_chance}", 
						Double.toString( chance ), results );
				
			}
			

			
		}
		
		return results;
	}
	

	/**
	 * <p>Ths function will perform individual replacements of the given placeholders, but
	 * if the placeholder does not exist, then it will not change anything with the results
	 * and it will be just passed through.
	 * </p>
	 * 
	 * @param placeholder
	 * @param value
	 * @param results
	 * @return
	 */
	private String applySecondaryPlaceholdersCheck( String placeholder, String value, String results) {

		if ( results.contains( placeholder ) && value != null ) {
			results = results.replace( placeholder, value );
		}
		
		return results;
	}


}
