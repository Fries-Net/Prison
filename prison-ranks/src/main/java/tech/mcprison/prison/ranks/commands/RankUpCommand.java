/*
 * Copyright (C) 2017-2020 The MC-Prison Team
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

package tech.mcprison.prison.ranks.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.commands.Arg;
import tech.mcprison.prison.commands.Command;
import tech.mcprison.prison.internal.CommandSender;
import tech.mcprison.prison.internal.Player;
import tech.mcprison.prison.internal.Scheduler;
import tech.mcprison.prison.internal.platform.Platform;
import tech.mcprison.prison.output.Output;
import tech.mcprison.prison.output.Output.DebugTarget;
import tech.mcprison.prison.ranks.PrisonRanks;
import tech.mcprison.prison.ranks.RankUtil;
import tech.mcprison.prison.ranks.RankUtil.PromoteForceCharge;
import tech.mcprison.prison.ranks.RankUtil.RankupModes;
import tech.mcprison.prison.ranks.RankUtil.RankupStatus;
import tech.mcprison.prison.ranks.RankupResults;
import tech.mcprison.prison.ranks.data.PlayerRank;
import tech.mcprison.prison.ranks.data.Rank;
import tech.mcprison.prison.ranks.data.RankLadder;
import tech.mcprison.prison.ranks.data.RankPlayer;
import tech.mcprison.prison.ranks.data.RankPlayerFactory;
import tech.mcprison.prison.ranks.managers.LadderManager;
import tech.mcprison.prison.ranks.managers.PlayerManager;
import tech.mcprison.prison.tasks.PrisonCommandTaskData;
import tech.mcprison.prison.tasks.PrisonCommandTasks;

/**
 * The commands for this module.
 *
 * @author Faizaan A. Datoo
 * @author GABRYCA
 * @author RoyalBlueRanger
 */
public class RankUpCommand 
				extends RankUpCommandMessages {

	public RankUpCommand() {
		super( "RankUpCommand" );
	}
	
    /*
     * /rankup command
     */
	
    @Command( identifier = "rankupMax", 
    			description = "Ranks up to the max rank that the player can afford. If the player has the " +
    					"perm ranks.rankupmax.prestige it will try to rankup prestige once it maxes out " +
    					"on the default ladder. " +
    					"By default, no player has access to this command. The following perms must be used."
    					, 
    			altPermissions = {"ranks.rankupmax.default", "ranks.rankupmax.prestige", "ranks.rankupmax.[ladderName]"},
    			onlyPlayers = false) 
    public void rankUpMax(CommandSender sender,
    		@Arg(name = "ladder", description = "The ladder to rank up on.", def = "default")  String ladder 
    		) {

    	String perms = "ranks.rankupmax.";
    	String permsLadder = perms + ladder;
    	
		boolean isPrestigesEnabled = Prison.get().getPlatform().getConfigBooleanFalse( "prestiges" ) || 
				Prison.get().getPlatform().getConfigBooleanFalse( "prestige.enabled" );
		
		boolean isLadderPrestiges = ladder.equalsIgnoreCase(LadderManager.LADDER_PRESTIGES);
//		boolean isLadderDefault = ladder.equalsIgnoreCase(LadderManager.LADDER_DEFAULT);
    	
		RankPlayer rPlayer = sender.getRankPlayer();
		
    	if ( (isPrestigesEnabled && isLadderPrestiges ||
    			!isLadderPrestiges ) && 
    			sender.hasPermission( permsLadder) 
    			) {
    		Output.get().logDebug( DebugTarget.rankup, 
    				"Rankup: cmd '/rankupmax %s'  Passed perm check: %s", 
    				ladder, permsLadder );
    		
    		boolean success = false;
    		
    		List<PrisonCommandTaskData> cmdTasks = new ArrayList<>();
    		StringBuilder sbRanks = new StringBuilder();
    		
    		
    		RankupModes mode = RankupModes.MAX_RANKS;
    		
    		if ( !LadderManager.LADDER_PRESTIGES.equalsIgnoreCase( ladder ) && 
    				!LadderManager.LADDER_DEFAULT.equalsIgnoreCase( ladder )) {
    			
    			success = rankUpPrivate(sender, rPlayer, ladder, mode, perms, cmdTasks, sbRanks );
    		}
    		else {
    			
    			// Run rankupmax on the default ladder only:
    			success = rankUpPrivate(sender, rPlayer, LadderManager.LADDER_DEFAULT, mode, perms, cmdTasks, sbRanks );
    			
    			// If they specified the prestiges ladder, then try to prestige that one rank:
    			if ( success && LadderManager.LADDER_PRESTIGES.equalsIgnoreCase( ladder ) ) {
    				
    				success = rankUpPrivate(sender, rPlayer, LadderManager.LADDER_PRESTIGES, RankupModes.ONE_RANK, perms, cmdTasks, sbRanks );
    			}
    		}
    		
			
//    		RankPlayer rPlayer = sender.getRankPlayer();
//    		Player player = getPlayerByName( sender.getName());
//    		Player player = getPlayer( sender, null );
			
			// submit cmdTasks
			if ( cmdTasks.size() > 0 ) {
				
				submitCmdTasks( rPlayer, cmdTasks );
			}
			
			
			if ( sbRanks.length() > 0 ) {
				ranksRankupMaxSuccessMsg( sender, sbRanks, rPlayer );
//				ranksRankupMaxSuccessMsg( sender, sbRanks, player.getRankPlayer() );
			}
			
			// If the ran rankupmax for prestiges, and the last prestige was successful, then
			// try it all again!
			if ( success && LadderManager.LADDER_PRESTIGES.equalsIgnoreCase( ladder ) ) {
				rankUpMax( sender, ladder );
			}
		}
    	else {
//    		RankPlayer rPlayer = sender.getRankPlayer();
//    		Player player = getPlayerByName( sender.getName() );
//    		Player player = getPlayer( sender, null );
    		Output.get().logDebug( DebugTarget.rankup, 
    				"Rankup: Failed: cmd '/rankupmax %s'  Does not have the permission ranks.rankupmax.%s", 
    				ladder, ladder );
    		rankupMaxNoPermissionMsg( sender, "ranks.rankupmax." + ladder, rPlayer );
//    		rankupMaxNoPermissionMsg( sender, "ranks.rankupmax." + ladder, player.getRankPlayer() );
    	}
    }


	@Command(identifier = "rankup", 
			description = "Ranks up to the next rank. All players have access to " +
			"the ability to rankup on the default ladder so no perms are required, but any other ladder " +
			"requires the correct perms. " +
			"All players much have at least the perm 'ranks.user' to use this command. To use this " +
			"command with other ladders, the users must have the correct perms as listed with this " +
			"command's help information. "
			, 
//			permissions = "ranks.user", 
			altPermissions = {"ranks.user", "ranks.rankup.default", "ranks.rankup.prestiges", 
					"ranks.rankup.[ladderName]"}, 
			onlyPlayers = false) 
    public void rankUp(CommandSender sender,
		@Arg(name = "ladder", description = "The ladder to rank up on. Defaults to 'default'.", def = "default")  String ladder,
		@Arg(name = "playerName", description = "Provides the player's name for the rankup, but" +
				"this can only be provided by a non-player such as console or ran from a script.", def = "")  String playerName
//		,
//    	@Arg(name = "confirm", def = "",
//    			description = "If confirmations are enabled, then the rankup command " + 
//    			"must be repeated with the addition of 'confirm'. If the rankup command is ran by a "
//    			+ "non-player, such as console or from script, then the confirmation will be skipped, or "
//    			+ "the word 'confirm' can be appended to the initial command. If 'confirm' is supplied, even "
//    			+ "if the setting 'prison-ranks.confirmation-enabled: true' is enabled, then confirmations will "
//    			+ "be skipped (acts like a bypass)." )
//    			String confirm
		) {
        
		boolean isPlayer = sender.isPlayer();
		
		if ( isPlayer && sender.isOp() && playerName != null && playerName.trim().length() > 0 ) {
			// use playerName as is since the OP'd player is using this command on someone else:
		}
		else if ( isPlayer ) {
			playerName = "";
		}
		
		
		// NOTE: If this is being ran from the console, the a 'playerName' parameter must be supplied:
		else if ( !isPlayer && playerName.length() == 0 ) {
        	Output.get().logInfo( rankupCannotRunFromConsoleMsg() );
        	return;
        }
        
        
        
		
        RankPlayer rPlayer = isPlayer ? 
        		sender.getRankPlayer() :
        		getRankPlayer(sender, null, playerName);
        
        if ( rPlayer == null ) {
        	rankupInvalidPlayerNameMsg( sender, playerName );
        	return;
        }
        
        
//        Player player = getPlayerByName( playerName );
//        
//        if ( player == null ) {
//        	rankupInvalidPlayerNameMsg( sender, playerName );
//        	return;
//        }

//        RankPlayer rPlayer = getRankPlayer(sender, null, playerName);
        
//		boolean isConfirmed = ( confirm != null && confirm.toLowerCase().contains("confirm") );
//		if ( !isConfirmed && playerName != null && playerName.toLowerCase().equals( "confirm" ) ) {
//			isConfirmed = true;
//			playerName = "";
//		}
//		else if ( !isConfirmed && ladder != null && ladder.toLowerCase().equals( "confirm" ) ) {
//			isConfirmed = true;
//			ladder = "";
//		}
		


//		boolean isConfirmationEnabled = Prison.get().getPlatform().getConfigBooleanFalse( "ranks.confirmation-enabled" );
//		if ( isConfirmationEnabled && !isConfirmed ) {
//			if ( isConfirmGUI && isPlayer ) {
//				
//				// call the gui prestige confirmation:
////				callGuiPrestigeConfirmation( sender, );
//				String guiConfirmParms = 
//						prestigeConfirmationGUIMsg( sender, rPlayer, nextRank, isResetDefaultLadder, isResetMoney );
//				
//				String guiConfirmCmd = "gui prestigeConfirm " + guiConfirmParms;
////				Output.get().logInfo( guiConfirmCmd );
//				
//				submitCmdTask( rPlayer, guiConfirmCmd );
//				
//			}
//			else {
//				
//				prestigeConfirmationMsg( sender, rPlayer, nextRank, isResetDefaultLadder, isResetMoney, isPlayer );
//			}
//			return;
//		}
        
        boolean isLadderDefault = ladder.equalsIgnoreCase(LadderManager.LADDER_DEFAULT);
        boolean isLadderPrestiges = ladder.equalsIgnoreCase(LadderManager.LADDER_PRESTIGES);

        boolean isPrestigesEnabled = Prison.get().getPlatform().getConfigBooleanFalse( "prestiges" ) || 
        		Prison.get().getPlatform().getConfigBooleanFalse( "prestige.enabled" );
        
        
        boolean isDefaultBypassPermCheck = isLadderDefault &&
        		Prison.get().getPlatform().getConfigBooleanTrue( 
        				"ranks.rankup-bypass-perm-check" ); 
        
        boolean hasDefaultPerm = sender.hasPermission( "ranks.user" ) || 
        							sender.hasPermission( "ranks.rankup.default");
        
        boolean isPrestigeBypassPermCheck = isLadderPrestiges &&
        		isPrestigesEnabled &&
        		Prison.get().getPlatform().getConfigBooleanTrue( 
        				"prestige.prestige-bypass-perm-check" ); 
        
        
        String perms = "ranks.rankup.";
        String permsLadder = perms + ladder;
        
        boolean hasLadderPerm = sender.hasPermission(permsLadder);
    	
        String permsCheck = String.format( 
        		"isDefaultBypassPermCheck: %s, isPrestigeBypassPermCheck: %s, 'ranks.user': %s, '%s': %s ", 
        		Boolean.toString(isDefaultBypassPermCheck),
        		Boolean.toString(isPrestigeBypassPermCheck),
        		Boolean.toString( sender.hasPermission( "ranks.user" ) ),
        		
        		permsLadder,
        		Boolean.toString(hasLadderPerm)
        		);
        
    	
    	if ( isDefaultBypassPermCheck || 
    			isPrestigeBypassPermCheck ||
    			
    			isLadderDefault && hasDefaultPerm ||
    			
    			isPrestigesEnabled && isLadderPrestiges &&
    					hasLadderPerm ||
    					
    					
    			!isLadderDefault && !isLadderPrestiges &&
    					hasLadderPerm 
    			
    			) {
    		
    		Output.get().logDebug( DebugTarget.rankup, 
    				"Rankup: cmd '/rankup %s%s'  Passed perm check: %s  [%s]]", 
    				ladder, 
    				( playerName.length() == 0 ? "" : " " + playerName ),
    				permsLadder,
    				permsCheck );
        
        	
//        	Output.get().logDebug( DebugTarget.rankup, 
//        			"Rankup: cmd '/rankup %s%s'  Processing %s", 
//        			ladder, 
//        			( playerName.length() == 0 ? "" : " " + playerName ),
//        			permsLadder
//        			);
        	
        	List<PrisonCommandTaskData> cmdTasks = new ArrayList<>();
        	
        	rankUpPrivate(sender, rPlayer, ladder, RankupModes.ONE_RANK, perms, cmdTasks, null );
//        	rankUpPrivate(sender, playerName, ladder, RankupModes.ONE_RANK, perms, cmdTasks, null );
        	
        	// submit cmdTasks
//        	Player player = getPlayerByName( sender, playerName );
//        	Player player = getPlayer( sender, playerName );
        	submitCmdTasks( rPlayer, cmdTasks );
        }
    	else {
//    		Player player = getPlayer( sender, playerName );
    		Output.get().logDebug( DebugTarget.rankup, 
    				"Rankup: Failed: cmd '/rankup %s'  Does not have the permission %s. [%s]", 
    				ladder, permsLadder, permsCheck );
    		rankupMaxNoPermissionMsg( sender, permsLadder, rPlayer );
    	}
        
    }
	

	@Command(identifier = "prestige", 
			description = "This will prestige the player. Prestiging is generally when "
			+ "the player's default rank is reset to the lowest rank, their money is rest to zero, and they "
			+ "start the mining and the ranking up process all over again. As a trade off for their reset "
			+ "of ranks, they get a new prestige rank with the costs of the default ranks being increased. "
			+ "All players must have the perms 'ranks.user' plus the optional perm 'ranks.rankup.prestiges' "
			+ "if the config.yml setting 'prestige.enable__ranks_rankup_prestiges__permission` is set to a "
			+ "value of 'true' (defaults to 'false'). "
			+ "Examples: '/prestige', '/presetige confirm', '/prestige <playerName> confirm'.", 
//			permissions = "ranks.user", 
			altPermissions = {"ranks.user", "ranks.rankup.prestiges"}, 
			onlyPlayers = false) 
    public void prestigeCmd(CommandSender sender,
		@Arg(name = "playerName", def = "", 
				description = "Provides the player's name for the prestige, but " +
				"this can only be provided by a non-player such as console or ran from a script. " +
				"Players cannot run Prestige for other players.") 
    			String playerName,
    	@Arg(name = "confirm", def = "",
    			description = "If confirmations are enabled, then the prestige command " + 
    			"must be repeated with the addition of 'confirm'. If the prestige command is ran by a "
    			+ "non-player, such as console or from script, then the confirmation will be skipped, or "
    			+ "the word 'confirm' can be appended to the initial command. If 'confirm' is supplied, even "
    			+ "if the setting 'prestige.confirmation-enabled: true' is enabled, then confirmations will "
    			+ "be skipped (acts like a bypass)." )
    			String confirm
		) {
        
		boolean isConfirmed = ( confirm != null && confirm.toLowerCase().contains("confirm") );
		if ( !isConfirmed && playerName != null && playerName.toLowerCase().equals( "confirm" ) ) {
			isConfirmed = true;
			playerName = "";
		}
		
		boolean isPlayer = sender.isPlayer();
		
		if ( isPlayer ) {
			playerName = sender.getName();
		}
		
		
        if ( !isPlayer && playerName.length() == 0 ) {
        	Output.get().logInfo( rankupCannotRunFromConsoleMsg() );
        	return;
        }
        
        RankPlayer rPlayer = isPlayer ?
        		sender.getRankPlayer() :
        		getRankPlayer(sender, null, playerName);
        
        if ( rPlayer == null ) {
        	rankupInvalidPlayerNameMsg( sender, playerName );
        	return;
        }
        
        

        boolean isPrestigesEnabled = Prison.get().getPlatform().getConfigBooleanFalse( "prestiges" ) || 
        		Prison.get().getPlatform().getConfigBooleanFalse( "prestige.enabled" );
        
        
        boolean hasPrestigePerm = sender.hasPermission( "ranks.user" ) || 
        							sender.hasPermission( "ranks.rankup.prestiges");
        
        boolean isPrestigeBypassPermCheck = 
        		isPrestigesEnabled &&
        		Prison.get().getPlatform().getConfigBooleanTrue( 
        				"prestige.prestige-bypass-perm-check" ); 
       
        String permsCheck = String.format( 
        		"isPrestigesEnabled: %s, isPrestigeBypassPermCheck: %s, 'ranks.user': %s, 'ranks.rankup.prestiges': %s ", 
        		Boolean.toString(isPrestigesEnabled),
        		Boolean.toString(isPrestigeBypassPermCheck),
        		Boolean.toString( sender.hasPermission( "ranks.user" ) ),
        		
        		Boolean.toString( sender.hasPermission( "ranks.rankup.prestiges") )
        		);

        
        String perms = "ranks.rankup.";
//        String permsLadder = perms + LadderManager.LADDER_PRESTIGES;
//        boolean hasPermsLadder = sender.hasPermission(permsLadder);
//        boolean usePerms = Prison.get().getPlatform().getConfigBooleanFalse( "enable__ranks_rankup_prestiges__permission" );
//        boolean hasAcessToPrestige = usePerms && hasPermsLadder || !usePerms;

//		boolean isPrestigesEnabled = Prison.get().getPlatform().getConfigBooleanFalse( "prestiges" ) || 
//				Prison.get().getPlatform().getConfigBooleanFalse( "prestige.enabled" );
		
		boolean isResetDefaultLadder = Prison.get().getPlatform().getConfigBooleanFalse( "prestige.resetDefaultLadder" );
		boolean isResetMoney = Prison.get().getPlatform().getConfigBooleanFalse( "prestige.resetMoney" );
		boolean isConfirmationEnabled = Prison.get().getPlatform().getConfigBooleanFalse( "prestige.confirmation-enabled" );
		boolean isConfirmGUI = Prison.get().getPlatform().getConfigBooleanFalse( "prestige.prestige-confirm-gui" );
		//boolean isforceSellall  = Prison.get().getPlatform().getConfigBooleanFalse( "prestige.force-sellall" );
		
//		boolean isLadderPrestiges = ladder.equalsIgnoreCase(LadderManager.LADDER_PRESTIGES);
    	
		PlayerRank nextRank = rPlayer.getNextPlayerRank();
		Rank nRank = nextRank == null ? null : nextRank.getRank();
		
		if ( nRank == null ) {
			// You're at the last possible rank.
			rankupNotAbleToPrestigeMsg( sender, rPlayer );
			rankupAtLastRankMsg( sender, rPlayer );
			return;
		}
		
		if ( !nRank.getLadder().getName().equals( LadderManager.LADDER_PRESTIGES ) ) {
			
			// your next rank is not a Prestiges rank
			rankupNotAbleToPrestigeMsg( sender, rPlayer );
			rankupNotAtLastRankMsg( sender, rPlayer );
			
			return;
		}
		
		String currency = nextRank.getCurrency();
		
		if ( rPlayer.getBalance( currency ) < nextRank.getRankCost() ) {
			
			// You do not have enough to prestige yet
			ranksRankupCannotAffordMsg( sender, nextRank, rPlayer );
			ranksRankupPlayerBalanceMsg( sender, rPlayer.getBalance( currency ), currency, rPlayer);
			return;
		}

		if ( isConfirmationEnabled && !isConfirmed ) {
			if ( isConfirmGUI && isPlayer ) {
				
				// call the gui prestige confirmation:
//				callGuiPrestigeConfirmation( sender, );
				String guiConfirmParms = 
						prestigeConfirmationGUIMsg( sender, rPlayer, nextRank, isResetDefaultLadder, isResetMoney );
				
				String guiConfirmCmd = "gui prestigeConfirm " + guiConfirmParms;
//				Output.get().logInfo( guiConfirmCmd );
				
				submitCmdTask( rPlayer, guiConfirmCmd );
				
			}
			else {
				
				prestigeConfirmationMsg( sender, rPlayer, nextRank, isResetDefaultLadder, isResetMoney, isPlayer );
			}
			return;
		}
		
		if ( isPrestigeBypassPermCheck ||
				
				isPrestigesEnabled && hasPrestigePerm  ) {
			
    		Output.get().logDebug( DebugTarget.rankup, 
    				"Rankup: cmd '/prestige %s%s'  [%s]", 
    				(playerName.length() == 0 ? "" : " " + playerName ),
    				(confirm == null ? "" : " " + confirm ),
    				permsCheck );
        
        	
        	List<PrisonCommandTaskData> cmdTasks = new ArrayList<>();
        	
        	String ladder = nRank.getLadder().getName();
        	rankUpPrivate(sender, rPlayer, ladder, RankupModes.ONE_RANK, perms, cmdTasks, null );
//        	rankUpPrivate(sender, playerName, ladder, RankupModes.ONE_RANK, perms, cmdTasks, null );
        	
        	// submit cmdTasks
//        	Player player = getPlayer( sender, playerName );
        	submitCmdTasks( rPlayer, cmdTasks );
        }
		else {
//    		Player player = getPlayer( sender, playerName );
    		Output.get().logDebug( DebugTarget.rankup, 
    				"Rankup: Failed: cmd '/prestige %s%s'  Does not have the permission %s. [%s]", 
    				(playerName.length() == 0 ? "" : " " + playerName ),
    				(confirm == null ? "" : " " + confirm ),
    				permsCheck );
    		
    		//prestigeNoPermissionMsg( sender, "ranks.rankup.prestiges", player.getRankPlayer() );
    	}
        
    }
	
	
//	private boolean rankUpPrivateX(CommandSender sender, 
//    		String playerName, 
//			String ladder, RankupModes mode, 
//			String permission, 
//			List<PrisonCommandTaskData> cmdTasks, 
//			StringBuilder sbRanks ) {
//
//		boolean rankupSuccess = false;
//
//		if ( sender.isPlayer() ) {
//			playerName = "";
//		}
//		
//        if ( !sender.isPlayer() && playerName.length() == 0 ) {
//        	Output.get().logInfo( rankupCannotRunFromConsoleMsg() );
//        	return false;
//        }
//        
//
//        // Player will always be the player since they have to be online and must be a player:
//        Player player = getPlayerByName( playerName );
//        
//        if ( player == null ) {
//        	rankupInvalidPlayerNameMsg( sender, playerName );
//        	return false;
//        }
//
//		
//		return rankupSuccess;
//	}
	
    private boolean rankUpPrivate(CommandSender sender, 
    		RankPlayer rankPlayer, 
//    		String playerName, 
    		String ladder, RankupModes mode, 
    		String permission, 
    		List<PrisonCommandTaskData> cmdTasks, 
    		StringBuilder sbRanks ) {

    	boolean rankupSuccess = false;
    	
        // RETRIEVE THE LADDER
    	
//    	// Perms have already been checked on the RankupModes.MAX_RANKS:
//    	if ( mode != RankupModes.MAX_RANKS ) {
//    		
//    		boolean isPrestigesEnabled = Prison.get().getPlatform().getConfigBooleanFalse( "prestiges" ) || 
//    				Prison.get().getPlatform().getConfigBooleanFalse( "prestige.enabled" );
//    		
//    		boolean isLadderPrestiges = ladder.equalsIgnoreCase(LadderManager.LADDER_PRESTIGES);
//    		boolean isLadderDefault = ladder.equalsIgnoreCase(LadderManager.LADDER_DEFAULT);
//    		
//    		String permCheck = permission + ladder.toLowerCase();
//    		
//    		// This player has to have permission to rank up on this ladder, but
//    		// ignore if either the default or prestiges ladder.  This only is to check for
//    		//  other ladders.
//    		if (!( isLadderPrestiges && isPrestigesEnabled ) && 
//    				!isLadderDefault && 
//    				!sender.hasPermission( permCheck )) {
//    			
//    			Output.get().logDebug( DebugTarget.rankup, 
//    					"Rankup: rankUpPrivate: failed rankup perm check. Missing perm: %s",
//    					permCheck );
//    			
//    			rankupMaxNoPermissionMsg( sender, permCheck );
//    			return false;
//    		}
//    		
//    	}

        
        // 
        if ( mode == null ) {
        	Output.get().logInfo( rankupInternalFailureMsg() );
        	return false;
        }
        
        
//		if ( sender.isPlayer() ) {
//			playerName = "";
//		}
//		
//        if ( !sender.isPlayer() && playerName.length() == 0 ) {
//        	Output.get().logInfo( rankupCannotRunFromConsoleMsg() );
//        	return false;
//        }
//        
//
//        // Player will always be the player since they have to be online and must be a player:
//        Player player = getPlayer( sender, playerName );
//        
//        if ( player == null ) {
//        	rankupInvalidPlayerNameMsg( sender, playerName );
//        	return false;
//        }

        
        
        
        //UUID playerUuid = player.getUUID();
//        RankPlayer rankPlayer = 
//        		player == null ? 
//        				sender.getRankPlayer() :
//		        		player instanceof RankPlayer ? (RankPlayer) player : 
//		        			player.getRankPlayer();
        
        
		ladder = confirmLadder( sender, ladder, rankPlayer );
		if ( ladder == null ) {
			// ladder cannot be null, 
			return false;
		}

		LadderManager lm = PrisonRanks.getInstance().getLadderManager();
		RankLadder targetLadder = lm.getLadder( ladder );
		
       	if ( targetLadder == null ){
    		rankupErrorNoLadderMsg( sender, ladder );
    		return false;
    	}
    	
    	if (!targetLadder.getLowestRank().isPresent()){
    		rankupErrorNoRankOnLadderMsg( sender, ladder );
    		return false;
    	}
		

    	


        PlayerRank rankCurrent = rankPlayer.getPlayerRank(ladder);
        
        
        // If the player has a rank on the target ladder, mmake sure the next rank is not null
        if ( rankCurrent != null && rankCurrent.getRank().getRankNext() == null ) {
        	rankupAtLastRankMsg(sender, rankPlayer );
        	return false;
        }
        
        // If at last rank on ladder, then cannot /rankup
//        if ( rankPlayer.getran)
        
        
        // Get the player's next rank on default ladder, or if at end then it will return the next
        // prestiges rank.
        PlayerRank playerRankTarget = null;
        
        if ( targetLadder.isDefault() || targetLadder.isPrestiges() ) {
        	
        	playerRankTarget = rankPlayer.getNextPlayerRank();
        	
        	// If the nextRank is null or the ladder does not match selected ladder, then exit:
        	if ( playerRankTarget == null || playerRankTarget.getRank() == null ) {
        		
        		if ( Output.get().isDebug() ) {
        			Output.get().logInfo( 
        					"RankUp  ladder= %s  currentRank= %s   "
        					+ "No next default or prestige rank. At end of both ladders?", 
        					ladder, rankCurrent.getRank().getName() );
        		}
        		
        		return false;
        	}
        }
        
        
        
        RankPlayerFactory rankPlayerFactory = new RankPlayerFactory();
        
        
        
        // If ranking up on neither default or prestige ladders:
        if ( !targetLadder.isDefault() && !targetLadder.isPrestiges() ) {
        	

        	PlayerRank playerRankCurrent = rankPlayerFactory.getRank( rankPlayer, ladder );
//        PlayerRank playerRankCurrent = rankPlayer.getPlayerRankDefault();

        	
        	// If the player does not have a rank on the current ladder, then assign the
        	// default rank for the ladder to be their next rank.
        	if ( playerRankCurrent == null ) {
        		
        		playerRankTarget = rankPlayer.calculateTargetPlayerRank( 
        				targetLadder.getLowestRank().get() );
        		
//        	playerRankTarget = rankPlayerFactory.createPlayerRank( 
//        			targetLadder.getLowestRank().get() );
        	}
        	else {
        		
        		playerRankTarget = rankPlayer.calculateTargetPlayerRank( playerRankCurrent.getRank() );
//        	playerRankTarget = playerRankCurrent.getTargetPlayerRankForPlayer( rankPlayer, 
//        					playerRankCurrent.getRank() );
        	}
        
        }
        

        
        
        
        		
        
        Output.get().logDebug( DebugTarget.rankup, 
    			"Rankup: rankUpPrivate: RankPlayer %s  playerRankTarget %s", 
    					(rankPlayer == null ? "null" : "true"), 
    					(playerRankTarget == null ? "null" : "true") );
        
        // If a player has a rank on the ladder get their rank, otherwise null:
        Rank pRankTarget = playerRankTarget.getRank();
        
//        Rank pRankAfter = null;
        boolean canPrestige = false;
        
        // If the player is trying to prestige, then the following must be ran to setup the prestige checks:
        if (ladder.equalsIgnoreCase(LadderManager.LADDER_PRESTIGES)) {

        	RankLadder rankLadder = lm.getLadder(LadderManager.LADDER_DEFAULT);
        	
        	if ( rankLadder == null ){
        		rankupErrorNoDefaultLadderMsg( sender );
        		return false;
        	}
        	
        	if (!rankLadder.getLowestRank().isPresent()){
        		rankupErrorNoLowerRankMsg( sender );
        		return false;
        	}
        	
        	// gets the rank on the default ladder. Used if ladder is not default.
        	PlayerRank pRankDefaultLadder = rankPlayerFactory.getRank( rankPlayer, LadderManager.LADDER_DEFAULT);
        	if ( pRankDefaultLadder == null ) {
        		rankupErrorPlayerNotOnDefaultLadder( sender, rankPlayer );
        	}
        	
        	Rank playersRankOnDefaultLadder = pRankDefaultLadder.getRank();
        	// On the default ladder, the player must be at the last rank:
        	// The last rank will never have a rankNext (it will be null):
        	if ( playersRankOnDefaultLadder.getRankNext() != null ) {
        		rankupNotAtLastRankMsg( sender, rankPlayer );
        		return false;
        	}
        	
        	// If force sellall, then perform the sellall here:
        	if ( Prison.get().getPlatform().getConfigBooleanFalse( "prestige.force-sellall" ) ) {
        		
        		Prison.get().getPlatform().sellall( rankPlayer );
        	}
        	
        	
        	// IF everything's ready, this will be true if and only if pRank is not null,
        	// and the prestige method will start
        	canPrestige = true;
        }
        
        
        // Get currency if it exists, otherwise it will be null if the Rank has no currency:
        String currency = rankPlayer == null || pRankTarget == null ? null : pRankTarget.getCurrency();
        
        
        if (rankPlayer != null ) {
        	
        	// Performs the actual rankup here:
        	RankupResults results = new RankUtil().rankupPlayer(rankPlayer, rankPlayer, ladder, 
        						sender.getName(), cmdTasks );
//        	RankupResults results = new RankUtil().rankupPlayer(player, rankPlayer, ladder, 
//        			sender.getName(), cmdTasks );
        	
        	
        	processResults( sender, rankPlayer.getName(), results, null, ladder, currency, sbRanks, mode );
        	
        	// If the last rankup attempt was successful and they are trying to rankup as many times as possible: 
        	// Note they used to restrict rankupmax from working on prestige ladder... 
        	if (results.getStatus() == RankupStatus.RANKUP_SUCCESS && mode == RankupModes.MAX_RANKS ) {
//    		if (results.getStatus() == RankupStatus.RANKUP_SUCCESS && mode == RankupModes.MAX_RANKS && 
//    				!ladder.equals(LadderManager.LADDER_PRESTIGES)) {
        		rankUpPrivate( sender, rankPlayer, ladder, mode, permission, cmdTasks, sbRanks );
        	}
        	if (results.getStatus() == RankupStatus.RANKUP_SUCCESS){
        		rankupSuccess = true;
        	}
        	
        	
        	// Get the player rank after
//        	PlayerRank playerRankAfter = rankPlayer.getNextPlayerRank();
//        	PlayerRank playerRankAfter = rankPlayerFactory.getRank( rankPlayer, ladder );
        	
//        	if ( playerRankAfter != null ) {
//        		
//        		pRankAfter = playerRankAfter.getRank();
//        	}
        	
        	// Prestige method if canPrestige and a successful rankup. 
        	// pRankTarget now contains the target rank prior to processing the rankup.  SO it should be
        	// the same as pRankAfter, but if it is wrong, then rankupWithSuccess will not be true.  So ignore...
        	if ( canPrestige && rankupSuccess ) {
        		prestigePlayer( sender, rankPlayer, lm, cmdTasks, sbRanks );
//        		prestigePlayer( sender, player, rankPlayer, pRankAfter, lm );
        		
        	}
        	else if ( canPrestige ) {
        		rankupNotAbleToPrestigeMsg( sender, rankPlayer );
        	}
        	
        }
        
        return rankupSuccess;
	}

    /**
     * <p>Perform the final prestige actions if prestige is requested (canPrestige) and if the 
     * rankup was successful.  It also assumes that pRankAfter is not null and not the same
     * as pRank, which would indicate something went wrong with the rankup.
     * </p>
     * 
     * <p>This function will reset the player's default ladder, if the configuration setting
     * 'prestige.resetDefaultLadder' has been enabled.  Otherwise the default ladder is not 
     * modified.
     * </p>
     * 
     * <p>This function will also reset the player's balance if the configuration 
     * 'prestige.resetMoney' is enabled.
     * </p>
     * 
     * 
     * @param sender
     * @param player
     * @param rankPlayer
     * @param pRankAfter
     * @param lm
     */
	private void prestigePlayer(CommandSender sender, RankPlayer rankPlayer, 
//						Rank pRankAfter, 
						LadderManager lm, 
						List<PrisonCommandTaskData> cmdTasks, StringBuilder sbRanks ) {
		
       	Output.get().logDebug( DebugTarget.rankup, "Rankup: prestigePlayer: ");

		Platform platform = Prison.get().getPlatform();
		boolean resetBalance = platform.getConfigBooleanTrue( "prestige.resetMoney" );
		boolean resetDefaultLadder = platform.getConfigBooleanTrue( "prestige.resetDefaultLadder" );
		
		boolean success = true;
		
		if ( resetDefaultLadder ) {
			
			// Get the player rank after, just to check if it has success Conditions
//			if (willPrestige && rankupWithSuccess && pRankAfter != null && pRank != pRankAfter) {
				// Set the player rank to the first one of the default ladder
				
			
			RankLadder ladder = lm.getLadder(LadderManager.LADDER_DEFAULT);
			Rank dRank = ladder.getLowestRank().get();
			setPlayerRank( rankPlayer, ladder, dRank, sender, cmdTasks, sbRanks );

			
//			String ladderName = LadderManager.LADDER_DEFAULT;
//			String defaultRank = lm.getLadder(LadderManager.LADDER_DEFAULT).getLowestRank().get().getName();
//			
//			setPlayerRank( rankPlayer, defaultRank, ladderName, sender );
			
				// Call the function directly and skip using dispatch commands:
//				setRank( sender, player.getName(), 
//						lm.getLadder(LadderManager.LADDER_DEFAULT).getLowestRank().get().getName(), 
//							LadderManager.LADDER_DEFAULT );
				
				
				PlayerRank playerRankSecond = rankPlayer.getPlayerRankDefault();
				

				
				if ( playerRankSecond != null ) {
					
					Rank pRankSecond = playerRankSecond.getRank();
					// Check if the ranks match
					
					if ( !pRankSecond.equals( lm.getLadder(LadderManager.LADDER_DEFAULT).getLowestRank().get()) ) {
						
						rankupNotAbleToResetRankMsg( sender, rankPlayer );
						success = false;
					}
				}
				else {
					
					rankupNotAbleToResetRankMsg( sender, rankPlayer );
					success = false;
				}
//			}
		}
		
		if ( success && resetBalance ) {
			
			// set the player's balance to zero:
			rankPlayer.setBalance( 0 );
			prestigePlayerBalanceSetToZeroMsg( sender, rankPlayer );
		}
		
		
		
		PlayerRank newPrestigeRank = rankPlayer.getPlayerRankPrestiges();
		Rank newPRank = newPrestigeRank == null || newPrestigeRank.getRank() == null ? 
							null :
							newPrestigeRank.getRank();
		
		String title = newPRank == null || newPRank.getTag() == null ? 
							newPRank.getName() : 
							newPRank.getTag();
		if ( success ) {
			// Send a message to the player because he did prestige!

			prestigePlayerSucessfulMsg( sender, title, rankPlayer );
		}
		else {
			
			prestigePlayerFailureMsg( sender, title, rankPlayer );
		}

	}



	/**
	 * <p>This command requires that a target player be specified and that this can only be 
	 * ran by an admin.  The act of promoting a player cannot ever fall back on the person 
	 * issuing the command.
	 * </p>
	 * 
	 * @param sender
	 * @param playerName
	 * @param ladder
	 * @param chargePlayer
	 */
	@Command(identifier = "ranks promote", 
			description = "Promotes a player to the next rank. This is an admin command. " +
					"This command can be used from the console. There is an " +
					"option to charge the player for the cost of the next rank, which if enabled, will " +
					"then be same as if the player used the command '/rankup'.",
    		permissions = "ranks.promote", 
    		onlyPlayers = false) 
    public void promotePlayer(CommandSender sender,
    	@Arg(name = "playerName", def = "", description = "Player name") String playerName,
        @Arg(name = "ladder", description = "The ladder to promote on.", def = "default") String ladder,
        @Arg(name = "chargePlayers", description = "Force the player to pay for the rankup (no_charge, charge_player)", 
        					def = "no_charge") String chargePlayer
    		) {

		
		
        RankPlayer rPlayer = getRankPlayer(sender, null, playerName);
        
        if ( rPlayer == null ) {
        	rankupInvalidPlayerNameMsg( sender, playerName );
        	return;
        }

		
//    	Player player = getPlayerByName( playerName );
//    	
////    	if (player == null) {
////    		ranksPromotePlayerMustBeOnlineMsg( sender );
////    		return;
////    	}

//    	UUID playerUuid = player.getUUID();
    	
    	RankPlayerFactory rankPlayerFactory = new RankPlayerFactory();
//    	RankPlayer rankPlayer = getRankPlayer( sender, playerUuid, player.getName() );
    	
    	PromoteForceCharge pForceCharge = PromoteForceCharge.fromString( chargePlayer );
    	if ( pForceCharge == null|| pForceCharge == PromoteForceCharge.refund_player ) {
    		
    		ranksPromotePlayerInvalidChargeValueMsg( sender, rPlayer );
    		return;
    	}

        
		ladder = confirmLadder( sender, ladder, rPlayer );
		if ( ladder == null ) {
			return;
		}

		
        PlayerRank playerRank = rankPlayerFactory.getRank( rPlayer, ladder );
        
        if ( rPlayer != null && playerRank != null ) {
        	
        	Rank pRank = playerRank.getRank();
        	if ( pRank == null ) {
        		sender.sendMessage( "Promote: There was an error trying to access the "
        				+ "current rank of the player. Try viewing the player's "
        				+ "details: '/ranks player help'.");
        		return;
        	}
        	
        	// Get currency if it exists, otherwise it will be null if the Rank has no currency:
        	String currency = pRank.getCurrency();
        	
        	List<PrisonCommandTaskData> cmdTasks = new ArrayList<>();
        	
        	RankupResults results = new RankUtil().promotePlayer( rPlayer, rPlayer, ladder, 
        			rPlayer.getName(), sender.getName(), pForceCharge, cmdTasks );
        	
        	// submit cmdTasks...
        	submitCmdTasks( rPlayer, cmdTasks );
        	
        	processResults( sender, rPlayer.getName(), results, null, ladder, currency, null, RankupModes.ONE_RANK );
        	
        }
        else {
        	// Message: Player is not on the ladder
        	sender.sendMessage( "Promote: Player is not on the specified ladder. "
        			+ "Try using '/ranks set rank' to add them.");
        }
    }


    @Command(identifier = "ranks demote",
    		description = "Demotes a player to the next lower rank. " +
    				"This is an admin command. This command can be used from the console. " +
    				"There is an option to refund the rankup cost to the player, of which the " +
    				"player will get a refund and it will be as if they did not perform a " +
    				"'/rankup' command.  Please be aware that if an admin uses '/ranks promote' on a " +
    				"player, then '/ranks demote <player> refund_player' the player will still get " +
    				"the refund although they did not pay for the rank.", 
    			permissions = "ranks.demote", onlyPlayers = false) 
    public void demotePlayer(CommandSender sender,
    	@Arg(name = "playerName", def = "", description = "Player name") String playerName,
        @Arg(name = "ladder", description = "The ladder to demote on.", def = "default") String ladder,
        @Arg(name = "chargePlayers", description = "Refund the player for the demotion (no_charge, refund_player)", 
        				def = "no_charge") String refundPlayer
        ) {

		
        RankPlayer rPlayer = getRankPlayer(sender, null, playerName);
        
        if ( rPlayer == null ) {
        	rankupInvalidPlayerNameMsg( sender, playerName );
        	return;
        }

        
//    	Player player = getPlayer( sender, playerName );
//    	
//    	if (player == null) {
//    		ranksPromotePlayerMustBeOnlineMsg( sender );
//    		return;
//    	}
    	
//    	UUID playerUuid = player.getUUID();
//    	RankPlayer rankPlayer = getRankPlayer( sender, playerUuid, player.getName() );
    	
    	PromoteForceCharge pForceCharge = PromoteForceCharge.fromString( refundPlayer );
    	if ( pForceCharge == null || pForceCharge == PromoteForceCharge.charge_player ) {
    		ranksDemotePlayerInvalidRefundValueMsg( sender, rPlayer );
    		return;
    	}
    	
		ladder = confirmLadder( sender, ladder, rPlayer );
		if ( ladder == null ) {
			// Already displayed error message about ladder not existing:
			return;
		}

		RankPlayerFactory rankPlayerFactory = new RankPlayerFactory();
		
        PlayerRank playerRank = rankPlayerFactory.getRank( rPlayer, ladder );
        
        if ( rPlayer != null && playerRank != null ) {
        	
        	Rank pRank = playerRank.getRank();
        	if ( pRank == null ) {
        		sender.sendMessage( "Demote: There was an error trying to access the "
        				+ "current rank of the player. Try viewing the player's "
        				+ "details: '/ranks player help'.");
        		return;
        	}
        	
        	// Get currency if it exists, otherwise it will be null if the Rank has no currency:
        	String currency = pRank.getCurrency();
        	
        	List<PrisonCommandTaskData> cmdTasks = new ArrayList<>();
        	
       	RankupResults results = new RankUtil().demotePlayer( rPlayer, rPlayer, ladder, 
        			rPlayer.getName(), sender.getName(), pForceCharge, cmdTasks );
        	
        	// submit cmdTasks
        	submitCmdTasks( rPlayer, cmdTasks );
        	
        	processResults( sender, rPlayer.getName(), results, null, ladder, currency, null, RankupModes.ONE_RANK );
        	
        }
        else {
        	// Message: Player is not on the ladder
        	sender.sendMessage( "Demote: Player is not on the specified ladder. "
        			+ "Try using '/ranks set rank' to add them.");
        }
    }


    @Command(identifier = "ranks set rank", description = "Sets a player to a specified rank on a ladder, " +
    		"or remove a player from a ladder (delete player rank).  Or if you use '*all*' for player name, " +
    		"then it will run this command on all players registered with Prison. When *all* is combined with " +
    		"the rankName '*same*' it will reapply the same rank to each player which will rerun the rank " +
    		"commands for the players.  If rank 'A' is your starting rank, you can use '*all*' and 'A' to " +
    		"reset all players to the starting rank; next you will need to -remove- all prestige ladders " +
    		"from all players too.", 
    			permissions = "ranks.setrank", onlyPlayers = false) 
    public void setRank(CommandSender sender,
    	@Arg(name = "playerName", def = "", description = "Player name, or [*all*]") String playerName,
    	@Arg(name = "rankName", description = "The rank to assign to the player, or [-remove-, *same*] " +
    						"to deleete the player from the rank.") String rank,
        @Arg(name = "ladder", description = "The ladder to demote on.", def = "default") String ladder) {

    	if ( "*all*".equalsIgnoreCase( playerName )) {
    		PlayerManager pm = PrisonRanks.getInstance().getPlayerManager();
    		
    		RankPlayerFactory rankPlayerFactory = new RankPlayerFactory();

    		for ( RankPlayer player : pm.getPlayers() ) {
    			
//    			Player targetPlayer = getPlayerByName( player.getName() );
//    			if ( targetPlayer != null ) {
    				
    				boolean isSameRank = rank.equalsIgnoreCase("*same*");

    				PlayerRank pRank = rankPlayerFactory.getRank( player, ladder );
    				String rankNameCurrent = isSameRank && 
    						pRank != null && 
    						pRank.getRank() != null ? 
    								pRank.getRank().getName() : "";
    				
    				String targetRank = isSameRank ? rankNameCurrent : rank;
    				setPlayerRank( player, targetRank, ladder, sender );
//    			}
    		}
    		
    	}
    	else {
    		
    		Player player = getPlayerByName( playerName );
    		
    		if (player == null) {
    			ranksPromotePlayerMustBeOnlineMsg( sender );
    			return;
    		}
    		
    		setPlayerRank( player.getRankPlayer(), rank, ladder, sender );
    	}
    }

    
    @Command(identifier = "ranks removeRank", description = "Removes a player from a specified ladder " +
    		"(delete player rank). This is an alias for /ranks set rank <playerName> -remove- <ladder>.", 
    		permissions = "ranks.setrank", onlyPlayers = false) 
    public void removeRank(CommandSender sender,
    		@Arg(name = "playerName", def = "", description = "Player name") String playerName,
    		@Arg(name = "ladder", description = "The ladder to demote on.", def = "default") String ladder) {
    	
    	setRank( sender, playerName, "-remove-", ladder );
    	
    }
    
    
    public void setPlayerRank( RankPlayer rankPlayer, Rank pRank ) {
        
        if ( rankPlayer != null ) {
        	
        	List<PrisonCommandTaskData> cmdTasks = new ArrayList<>();
        	
        	RankupResults results = 
        			new RankUtil().setRank(rankPlayer, rankPlayer, 
        						pRank.getLadder().getName(), pRank.getName(), 
        												rankPlayer.getName(), rankPlayer.getName(), 
        												cmdTasks );
        	
        	// submit cmdTasks
//        	Player player = getPlayerByName( rankPlayer.getName() );
			submitCmdTasks( rankPlayer, cmdTasks );
        	
        	processResults( rankPlayer, rankPlayer.getName(), results, 
        			pRank.getName(), pRank.getLadder().getName(), 
        			pRank.getCurrency(), null, RankupModes.ONE_RANK );
        }
    }
    
    /**
     * Added on 2022-07-04... called from: 
     * tech.mcprison.prison.ranks.data.RankPlayerFactory.firstJoin(RankPlayer)
     * 
     * @param rankPlayer
     * @param pRank
     */
    public void setPlayerRankFirstJoin( RankPlayer rankPlayer, Rank pRank ) {
    	
    	if ( rankPlayer != null ) {
    		
    		List<PrisonCommandTaskData> cmdTasks = new ArrayList<>();
    		
    		RankupResults results = 
    				new RankUtil().setRank(rankPlayer, rankPlayer, 
    						pRank.getLadder().getName(), pRank.getName(), 
    						rankPlayer.getName(), "FirstJoinEvent", 
    						cmdTasks );
    		
    		// submit cmdTasks
//    		Player player = getPlayer( null, rankPlayer.getName() );
    		submitCmdTasks( rankPlayer, cmdTasks );
    		
    		processResults( rankPlayer, rankPlayer.getName(), results, 
    				pRank.getName(), pRank.getLadder().getName(), 
    				pRank.getCurrency(), null, RankupModes.ONE_RANK );
    	}
    }
    
    
    private void setPlayerRank( RankPlayer rankPlayer, RankLadder ladder, Rank pRank,
    		CommandSender sender, 
    		List<PrisonCommandTaskData> cmdTasks, StringBuilder sbRanks ) {
    	
    	// Get currency if it exists, otherwise it will be null if the Rank has no currency:
    	String currency = rankPlayer == null || pRank == null ? null : pRank.getCurrency();
    	
    	
    	RankupResults results = new RankUtil().setRank( rankPlayer, rankPlayer, 
    			ladder.getName(), pRank.getName(), 
    			rankPlayer.getName(), sender.getName(), cmdTasks );
    	
    	
    	processResults( sender, rankPlayer.getName(), results, pRank.getName(), ladder.getName(), 
    			currency, sbRanks, RankupModes.ONE_RANK );
    	
    }
    
	private void setPlayerRank( RankPlayer rankPlayer, String rank, String ladderName, CommandSender sender ) {
//		UUID playerUuid = player.getUUID();
        
       	Output.get().logDebug( DebugTarget.rankup, "Rankup: setPlayerRank: ");
	
//       	RankPlayer rankPlayer = getRankPlayer( sender, playerUuid, player.getName() );

       	ladderName = confirmLadder( sender, ladderName, rankPlayer );
        
        if ( ladderName != null && rankPlayer != null ) {

//        	RankLadder rLadder = PrisonRanks.getInstance().getLadderManager().getLadder( ladderName );
        	
        	Rank pRank = PrisonRanks.getInstance().getRankManager().getRank( rank );
        	
        	// Get currency if it exists, otherwise it will be null if the Rank has no currency:
        	String currency = rankPlayer == null || pRank == null ? null : pRank.getCurrency();
        	
        	List<PrisonCommandTaskData> cmdTasks = new ArrayList<>();
        	
        	RankupResults results = new RankUtil().setRank(rankPlayer, rankPlayer, ladderName, rank, 
        			rankPlayer.getName(), sender.getName(), cmdTasks );
        	
        	// submit cmdTasks
			submitCmdTasks( rankPlayer, cmdTasks );
        	
        	processResults( sender, rankPlayer.getName(), results, rank, ladderName, currency, null, RankupModes.ONE_RANK );
        }
	}



	private String confirmLadder( CommandSender sender, String ladderName, RankPlayer rPlayer ) {
		String results = null;
		RankLadder ladder = PrisonRanks.getInstance().getLadderManager().getLadder(ladderName);

        // The ladder doesn't exist
        if ( ladder == null ) {
        	
        	ranksConfirmLadderMsg( sender, ladderName, rPlayer );
        }
        else {
        	results = ladder.getName();
        }
        return results;
	}


	/**
	 * This gets the RankPlayer for the given UUID or playerName.  The 'sender' generally is the 
	 * console, or an admin that is trying to run the command for a player.
	 * So although the sender has a 'getRankPlayer()' function, it may be for the wrong player.
	 * 

	 * 
	 * @param sender The one who should get an messages, but is not the one for RankPlayer.
	 * @param playerUuid
	 * @param playerName
	 * @return
	 */
	public RankPlayer getRankPlayer( CommandSender sender, UUID playerUuid, String playerName ) {
		
		RankPlayer player = PrisonRanks.getInstance().getPlayerManager().getPlayer(playerUuid, playerName);

        // Well, this isn't supposed to happen...
        if ( player == null ) {
        	ranksRankupFailureToGetRankPlayerMsg( sender );
        }

        return player;
	}


	public void processResults( CommandSender sender, String playerName, 
					RankupResults results, 
					String rank, String ladder, String currency, 
					StringBuilder sbRanks, 
					RankupModes mode ) {
	
		switch (results.getStatus()) {
            case RANKUP_SUCCESS:
            	
            	ranksRankupSuccessMsg( sender, playerName, results, sbRanks );
            	
            	break;
            	
            case DEMOTE_SUCCESS:
            	ranksRankupSuccessMsg( sender, playerName, results, null );

                break;
            case RANKUP_CANT_AFFORD:

            	// If mode is MAX_RANKS and was already successful, then do not display
            	// cannot afford message when it hits the end of the max rankups...
            	if ( mode != RankupModes.MAX_RANKS || 
            			mode == RankupModes.MAX_RANKS && sbRanks.length() == 0 ) {
            		
            		ranksRankupCannotAffordMsg( sender, results );
            	}
	            	
                break;
            case RANKUP_LOWEST:
            	ranksRankupLowestRankMsg( sender, playerName, results );
            	
            	break;
            case RANKUP_HIGHEST:
            	ranksRankupHighestRankMsg( sender, playerName, results );
            	
                break;
            case RANKUP_FAILURE:
            	ranksRankupFailureMsg( sender, results.getRankPlayer() );

            	break;
            case RANKUP_FAILURE_COULD_NOT_LOAD_PLAYER:
            	ranksRankupFailureCouldNotLoadPlayerMsg( sender, results.getRankPlayer() );
            	
            	break;
            case RANKUP_FAILURE_COULD_NOT_LOAD_LADDER:
            	ranksRankupFailureCouldNotLoadLadderMsg( sender, results.getRankPlayer() );
            	
            	break;
            case RANKUP_FAILURE_UNABLE_TO_ASSIGN_RANK:
            	ranksRankupFailureUnableToAssignRankMsg( sender, results.getRankPlayer() );
            	
            	break;
            case RANKUP_FAILURE_COULD_NOT_SAVE_PLAYER_FILE:
            	ranksRankupFailureCouldNotSavePlayerFileMsg( sender, results.getRankPlayer() );
            	
            	break;
            case RANKUP_NO_RANKS:
            	ranksRankupFailureNoRanksMsg( sender, results.getRankPlayer() );
            	
                break;
            case RANKUP_FAILURE_RANK_DOES_NOT_EXIST:
            	ranksRankupFailureRankDoesNotExistMsg( sender, rank, results.getRankPlayer() );
            	
            	break;
			case RANKUP_FAILURE_RANK_IS_NOT_IN_LADDER:
				ranksRankupFailureRankIsNotInLadderMsg( sender, rank, ladder, results.getRankPlayer() );
				
				break;
            
			case RANKUP_FAILURE_CURRENCY_IS_NOT_SUPPORTED:
				ranksRankupFailureCurrencyIsNotSupportedMsg( sender, results.getTargetRank().getCurrency(), 
						results.getRankPlayer() );
				
				break;
				
			case RANKUP_FAILURE_ECONOMY_FAILED:
				ranksRankupFailureEconomyFailedMsg( sender, results.getRankPlayer() );
				//String msg = "Failed to adjust player's balance. Could be an issue with vault or " +
				//		"a cache timing issue. Try again.";
				//sender.sendMessage( results.getRankPlayer().convertStringPlaceholders( msg ) );
				
				break;
				
			case RANKUP_LADDER_REMOVED:
				ranksRankupFailureLadderRemovedMsg( sender, ladder, results.getRankPlayer() );
				
				break;
				
			case RANKUP_FAILURE_REMOVING_LADDER:
				ranksRankupFailureRemovingLadderMsg( sender, ladder, results.getRankPlayer() );
				
				break;
				
			case IN_PROGRESS:
				ranksRankupFailureInProgressMsg( sender, results.getRankPlayer() );
				
				break;
			default:
				break;
        }
	}

	private void submitCmdTask( Player player, String command ) {
		
		Scheduler scheduler = Prison.get().getPlatform().getScheduler();
		
		scheduler.performCommand( player, command );
//		scheduler.dispatchCommand( player, command );
		
//		PrisonCommandTaskData task = new PrisonCommandTaskData( errorPrefix, command );
//		PrisonCommandTasks.submitTasks( player, task );
	}
	
    private void submitCmdTasks( Player player, List<PrisonCommandTaskData> cmdTasks )
	{
    	
    	PrisonCommandTasks.submitTasks( player, cmdTasks );
		
	}
    
}
