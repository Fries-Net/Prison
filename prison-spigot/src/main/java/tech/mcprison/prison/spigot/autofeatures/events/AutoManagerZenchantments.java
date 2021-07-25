package tech.mcprison.prison.spigot.autofeatures.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.autofeatures.AutoFeaturesFileConfig.AutoFeatures;
import tech.mcprison.prison.output.ChatDisplay;
import tech.mcprison.prison.output.LogLevel;
import tech.mcprison.prison.output.Output;
import tech.mcprison.prison.spigot.SpigotPrison;
import tech.mcprison.prison.spigot.autofeatures.AutoManagerFeatures;
import tech.mcprison.prison.spigot.block.OnBlockBreakEventListener;
import tech.mcprison.prison.spigot.block.OnBlockBreakEventListener.BlockBreakPriority;
import tech.mcprison.prison.spigot.game.SpigotHandlerList;
import zedly.zenchantments.BlockShredEvent;

public class AutoManagerZenchantments
	extends AutoManagerFeatures
	implements PrisonEventManager {

	public AutoManagerZenchantments() {
		super();
	}
	
	
	@Override
	public void registerEvents() {
	
		initialize();

	}

	  
    public class AutoManagerBlockShredEventListener
	    extends AutoManagerBlockBreakEvents
	    implements Listener {
    	
    	@EventHandler(priority=EventPriority.NORMAL) 
    	public void onBlockShredBreak(BlockShredEvent e) {
    		genericBlockEventAutoManager( e );
    	}
    }
 
    public class OnBlockBreakBlockShredEventListener 
	    extends OnBlockBreakEventListener
	    implements Listener {
		
		@EventHandler(priority=EventPriority.NORMAL) 
		public void onBlockShredBreak(BlockShredEvent e) {
			genericBlockEvent( e );
		}
	}
    
    public class OnBlockBreakBlockShredEventListenerMonitor 
    extends OnBlockBreakEventListener
    implements Listener {
    	
    	@EventHandler(priority=EventPriority.MONITOR) 
    	public void onBlockShredBreakMonitor(BlockShredEvent e) {
    		genericBlockEventMonitor( e );
    	}
    }
    
    
    @Override
    public void initialize() {
    	boolean isEventEnabled = isBoolean( AutoFeatures.isProcessZenchantsBlockExplodeEvents );
    	
    	if ( !isEventEnabled ) {
    		return;
    	}
    	
    	// Check to see if the class BlastUseEvent even exists:
    	try {
    		Output.get().logInfo( "AutoManager: checking if loaded: Zenchantments" );
    		
    		Class.forName( "zedly.zenchantments.BlockShredEvent", false, 
    				this.getClass().getClassLoader() );
    		
    		Output.get().logInfo( "AutoManager: Trying to register Zenchantments" );
    		
    		
    		String eP = getMessage( AutoFeatures.ZenchantmentsBlockShredEventPriority );
    		BlockBreakPriority eventPriority = BlockBreakPriority.fromString( eP );
    		
    		if ( eventPriority != BlockBreakPriority.DISABLED ) {
    			
    			EventPriority ePriority = EventPriority.valueOf( eventPriority.name().toUpperCase() );           
    			
    			
    			OnBlockBreakBlockShredEventListener normalListener = 
												new OnBlockBreakBlockShredEventListener();
    			OnBlockBreakBlockShredEventListenerMonitor normalListenerMonitor = 
												new OnBlockBreakBlockShredEventListenerMonitor();
    			
    			
    			SpigotPrison prison = SpigotPrison.getInstance();
    			
    			PluginManager pm = Bukkit.getServer().getPluginManager();
    			
    			if ( isBoolean( AutoFeatures.isAutoFeaturesEnabled )) {
    				
    				AutoManagerBlockShredEventListener autoManagerlListener = 
    											new AutoManagerBlockShredEventListener();
    				
    				pm.registerEvent(BlockShredEvent.class, autoManagerlListener, ePriority,
    						new EventExecutor() {
    					public void execute(Listener l, Event e) { 
        					if ( l instanceof OnBlockBreakBlockShredEventListenerMonitor && 
           						 e instanceof BlockShredEvent ) {
           						OnBlockBreakBlockShredEventListenerMonitor lmon = 
           											(OnBlockBreakBlockShredEventListenerMonitor) l;
           						BlockShredEvent event = (BlockShredEvent) e;
           						lmon.onBlockShredBreakMonitor( event );
           					}
    					}
    				},
    				prison);
    				prison.getRegisteredBlockListeners().add( autoManagerlListener );
    			}
    			
    			pm.registerEvent(BlockShredEvent.class, normalListener, ePriority,
    					new EventExecutor() {
    				public void execute(Listener l, Event e) { 
    					if ( l instanceof OnBlockBreakBlockShredEventListenerMonitor && 
       						 e instanceof BlockShredEvent ) {
       						OnBlockBreakBlockShredEventListenerMonitor lmon = 
       											(OnBlockBreakBlockShredEventListenerMonitor) l;
       						BlockShredEvent event = (BlockShredEvent) e;
       						lmon.onBlockShredBreakMonitor( event );
       					}
    				}
    			},
    			prison);
    			prison.getRegisteredBlockListeners().add( normalListener );
    			
    			pm.registerEvent(BlockShredEvent.class, normalListenerMonitor, EventPriority.MONITOR,
    					new EventExecutor() {
    				public void execute(Listener l, Event e) { 
    					if ( l instanceof OnBlockBreakBlockShredEventListenerMonitor && 
    						 e instanceof BlockShredEvent ) {
    						OnBlockBreakBlockShredEventListenerMonitor lmon = 
    											(OnBlockBreakBlockShredEventListenerMonitor) l;
    						BlockShredEvent event = (BlockShredEvent) e;
    						lmon.onBlockShredBreakMonitor( event );
    					}
    				}
    			},
    			prison);
    			prison.getRegisteredBlockListeners().add( normalListenerMonitor );
    		}
    		
    	}
    	catch ( ClassNotFoundException e ) {
    		// Zenchantments is not loaded... so ignore.
    		Output.get().logInfo( "AutoManager: Zenchantments is not loaded" );
    	}
    	catch ( Exception e ) {
    		Output.get().logInfo( "AutoManager: Zenchantments failed to load. [%s]", e.getMessage() );
    	}
    }
    
    public void unregisterListeners() {
    	
    	SpigotPrison prison = SpigotPrison.getInstance();
    	
    	while ( prison.getRegisteredBlockListeners().size() > 0 ) {
    		Listener listener = prison.getRegisteredBlockListeners().remove( 0 );
    		
    		if ( listener != null ) {
    			
    			HandlerList.unregisterAll( listener );
    		}
    	}
    	
//    	AutoManagerBlockShredEventListener listener = null;
//    	for ( RegisteredListener lstnr : BlastUseEvent.getHandlerList().getRegisteredListeners() )
//		{
//			if ( lstnr.getListener() instanceof AutoManagerBlockShredEventListener ) {
//				listener = (AutoManagerBlockShredEventListener) lstnr.getListener();
//				break;
//			}
//		}
//
//    	if ( listener != null ) {
//    		
//			HandlerList.unregisterAll( listener );
//    	}
    	
    }
	
    
    @Override
    public void dumpEventListeners() {
    	boolean isEventEnabled = isBoolean( AutoFeatures.isProcessZenchantsBlockExplodeEvents );
    	
    	if ( !isEventEnabled ) {
    		return;
    	}
    	
    	// Check to see if the class BlastUseEvent even exists:
    	try {
    		
    		Class.forName( "zedly.zenchantments.BlockShredEvent", false, 
    				this.getClass().getClassLoader() );
    		
    		
    		ChatDisplay eventDisplay = Prison.get().getPlatform().dumpEventListenersChatDisplay( 
    				"BlockShredEvent", 
    				new SpigotHandlerList( BlockShredEvent.getHandlerList()) );
    		
    		if ( eventDisplay != null ) {
    			Output.get().logInfo( "" );
    			eventDisplay.toLog( LogLevel.INFO );
    		}
    	}
    	catch ( ClassNotFoundException e ) {
    		// Zenchantments is not loaded... so ignore.
    	}
    	catch ( Exception e ) {
    		Output.get().logInfo( "AutoManager: zenchantments failed to load. [%s]", e.getMessage() );
    	}
    }

}
