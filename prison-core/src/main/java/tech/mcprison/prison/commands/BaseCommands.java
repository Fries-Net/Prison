package tech.mcprison.prison.commands;

import java.util.Optional;
import java.util.UUID;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.internal.CommandSender;
import tech.mcprison.prison.internal.Player;

public abstract class BaseCommands
{
	private String cmdGroup;
	
	public BaseCommands( String cmdGroup ) {
		this.cmdGroup = cmdGroup;
	}

	public String getCmdGroup() {
		return cmdGroup;
	}
	public void setCmdGroup( String cmdGroup ) {
		this.cmdGroup = cmdGroup;
	}
	

//	public Player getPlayer( CommandSender sender ) {
//		Optional<Player> player = Prison.get().getPlatform().getPlayer( sender.getName() );
//		return player.isPresent() ? player.get() : null;
//	}
    
    /**
     * <p>Gets a player by name.  If the player is not online, then try to get them from 
     * the offline player list. If not one is found, then return a null.
     * </p>
     * 
     * <p>The getOfflinePlayer() will now include RankPlayer as a fall back to help
     * ensure a player is always returned, if its a valid player.
     * </p>
     * 
     * <p>Never should this function return a Player based upon sender.
     * </p>
     * 
     * @param sender
     * @param playerName is optional, if not supplied, then sender will be used
     * @return Player if found, or null.
     */
	public Player getPlayerByName( // CommandSender sender, 
			String playerName ) {
		return getPlayerByName( // sender, 
				playerName, null );
	}
//	public Player getPlayer( CommandSender sender ) {
//		return getPlayer( sender, null, null );
//	}
	
	/**
	 * <p>This function should only return a Player based upon either the player's name
	 * or their UUID.  It should never return a Player based upon sender.
	 * </p>
	 * 
	 * <p>Gets a player by name.  If the player is not online, then try to get them from 
     * the offline player list. If not one is found, then return a null.
     * </p>
     * 
     * <p>The getOfflinePlayer() will now include RankPlayer as a fall back to help
     * ensure a player is always returned, if its a valid player.
     * </p>
     * 
     * 
	 * @param sender
	 * @param playerName
	 * @param uuid
	 * @return
	 */
	public Player getPlayerByName( // CommandSender sender, 
			String playerName, UUID uuid ) {
		Player result = null;
		
//		boolean console = sender == null ? true : "CONSOLE".equalsIgnoreCase( sender.getName() );
//		
//		if ( !console ) {
//			playerName = sender.getName();
//		}
		
		result = Prison.get().getPlatform().getRankPlayer( uuid, playerName );

		
//		if ( playerName != null ) {
//			Optional<Player> opt = Prison.get().getPlatform().getPlayer( playerName );
//			
//			result = opt.orElse( null );
//			
////			if ( !opt.isPresent() ) {
////				opt = Prison.get().getPlatform().getOfflinePlayer( playerName );
////			}
////			if ( opt.isPresent() ) {
////				result = opt.get();
////			}
//		}
//		if ( result == null && uuid != null ) {
//			
////			result = Prison.get().getPlatform().getRankPlayer( uuid, playerName );
//			
////			Optional<Player> opt = Prison.get().getPlatform().getOfflinePlayer( uuid );
////			if ( opt.isPresent() ) {
////				result = opt.get();
////			}
//		}
		
		
//		playerName = playerName != null && !playerName.trim().isEmpty() ? 
//								playerName : sender != null ? sender.getName() : null;
//		
//		//Output.get().logInfo("RanksCommands.getPlayer :: playerName = " + playerName );
//		
//		// Do not try to get the player name if 
//		if ( playerName != null ) {
//			Optional<Player> opt = Prison.get().getPlatform().getPlayer( playerName );
//			if ( !opt.isPresent() ) {
//				opt = Prison.get().getPlatform().getOfflinePlayer( playerName );
//			}
//			if ( !opt.isPresent() ) {
//				opt = Prison.get().getPlatform().getOfflinePlayer( uuid );
//			}
//			if ( opt.isPresent() ) {
//				result = opt.get();
//			}
//			
//		}
		return result;
	}
	
	public Player getOnlinePlayer( CommandSender sender, String playerName ) {
		Player result = null;
		
		playerName = playerName != null && !playerName.trim().isEmpty() ? 
				playerName : sender != null ? sender.getName() : null;
		
		//Output.get().logInfo("RanksCommands.getPlayer :: playerName = " + playerName );
		
		if ( playerName != null ) {
			Optional<Player> opt = Prison.get().getPlatform().getPlayer( playerName );

			if ( opt.isPresent() ) {
				result = opt.get();
			}
			
		}
		return result;
	}
	
	
//	public double getPlayerBalance( Player player ) {
//		
//		EconomyIntegration economy = PrisonAPI.getIntegrationManager().getEconomy();
//		
//		return economy.getBalance( player );
//	}
//    
//	public double getPlayerBalance( Player player, String currency ) {
//		
//
//		EconomyCurrencyIntegration currencyEcon = PrisonAPI.getIntegrationManager()
//						.getEconomyForCurrency( currency );
//		if ( currencyEcon == null ) {
//			// ERROR: currency is not supported
//			Output.get().logInfo( "The currency %s is not supported.  Therefore there is no blance.",
//					currency );
//			return 0;
//		}
//		else {
//			return currencyEcon.getBalance( player, currency );
//		}
//
//	}
	
	
}
