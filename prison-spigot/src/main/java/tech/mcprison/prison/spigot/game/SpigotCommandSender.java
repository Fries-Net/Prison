/*
 *  Prison is a Minecraft plugin for the prison game mode.
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

package tech.mcprison.prison.spigot.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.PermissionAttachmentInfo;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.PrisonAPI;
import tech.mcprison.prison.commands.CommandHandler;
import tech.mcprison.prison.integration.PermissionIntegration;
import tech.mcprison.prison.internal.CommandSender;
import tech.mcprison.prison.internal.Player;
import tech.mcprison.prison.ranks.PrisonRanks;
import tech.mcprison.prison.ranks.data.RankPlayer;
import tech.mcprison.prison.spigot.SpigotPrison;
import tech.mcprison.prison.spigot.sellall.SellAllUtil;
import tech.mcprison.prison.util.Text;

/**
 * @author Faizaan A. Datoo
 */
public class SpigotCommandSender 
		implements CommandSender {

    private org.bukkit.command.CommandSender bukkitSender;

    public SpigotCommandSender(org.bukkit.command.CommandSender sender) {
        this.bukkitSender = sender;
    }
    

    /**
     * We have both this function, getUniqueId() and getUUID(), because different
     * sources (player vs entity) have different requirements and expectations for
     * field names.
     * 
     * @return
     */
    public UUID getUniqueId() {
		return getUUID();
	}
    
    public UUID getUUID() {
    	UUID uuid = null;
    	if ( isPlayer() ) {
    		uuid = ((org.bukkit.entity.Player) bukkitSender).getUniqueId();
    	}
        return uuid;
    }

    @Override 
    public String getName() {
        return bukkitSender.getName();
    }

    /**
     * <p>This function will dispatch a command and run it as command sender.
     * But before it is ran, this function looks up within the Prison command handler
     * to see if it's commands have been remapped to another command, and if it has,
     * it then uses the mapped command.
     * </p>
     */
    @Override 
    public void dispatchCommand(String command) {
    	
    	command = CommandHandler.remapRootCmdIdentifiers( command );
    	
    	String registeredCmd = Prison.get().getCommandHandler()
    					.findRegisteredCommand( command );
    	
        Bukkit.getServer().dispatchCommand(bukkitSender, registeredCmd);
    }

    @Override 
    public boolean doesSupportColors() {
        return (this instanceof ConsoleCommandSender) && Bukkit.getConsoleSender() != null;
    }

    @Override 
    public void sendMessage(String message) {
    	
    	String[] msgs = Text.translateAmpColorCodes(message).split( "\\{br\\}" );
    	
    	for ( String msg : msgs ) {
    		bukkitSender.sendMessage(msg);
		}
    }

    @Override 
    public void sendMessage(String[] messages) {
        for (String message : messages) {

        	String[] msgs = Text.translateAmpColorCodes(message).split( "\\{br\\}" );
        	for ( String msg : msgs ) {
        		
        		sendMessage(msg);
        	}
        }
    }
    
    @Override 
    public void sendMessage(List<String> messages) {
    	for (String message : messages) {
    		
    		String[] msgs = Text.translateAmpColorCodes(message).split( "\\{br\\}" );
    		for ( String msg : msgs ) {
    			
    			sendMessage(msg);
    		}
    	}
    }

    @Override 
    public void sendRaw(String json) {
        if (bukkitSender instanceof org.bukkit.entity.Player) {
            json = Text.translateAmpColorCodes(json);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + bukkitSender.getName() + " " + json);
        }
    }

    @Override
    public boolean isOp() {
    	return bukkitSender.isOp();
    }
   
	@Override
	public void recalculatePermissions() {
		bukkitSender.recalculatePermissions();
	}
	

    @Override
    public boolean hasPermission(String perm) {
        return bukkitSender.hasPermission(perm);
    }
    
    
    @Override
    public List<String> getPermissions() {
    	List<String> results = new ArrayList<>();
    	
    	Set<PermissionAttachmentInfo> perms = bukkitSender.getEffectivePermissions();
    	for ( PermissionAttachmentInfo perm : perms )
		{
			results.add( perm.getPermission() );
		}
    	
    	return results;
    }
    
    
    @Override
    public List<String> getPermissions( String prefix ) {
    	
    	return getPermissions( prefix, getPermissions() );
    }
    
    @Override
    public List<String> getPermissions( String prefix, List<String> perms ) {
    	List<String> results = new ArrayList<>();
    	
    	for ( String perm : perms ) {
    		if ( perm.startsWith( prefix ) ) {
    			results.add( perm );
    		}
    	}
    	
    	return results;
    }
    
//    @Override
//    public List<String> getPermissions( String prefix ) {
//    	List<String> results = new ArrayList<>();
//    	
//    	for ( String perm : getPermissions() ) {
//			if ( perm.startsWith( prefix ) ) {
//				results.add( perm );
//			}
//		}
//    	
//    	return results;
//    }

    
    @Override
    public double getSellAllMultiplier() {
    	double results = 1.0;
    	
    	if ( isPlayer() ) {
    		
    		SellAllUtil sellall = SpigotPrison.getInstance().getSellAllUtil();
    		
    		if ( sellall != null && getWrapper() != null ) {
    			results = sellall.getPlayerMultiplier((org.bukkit.entity.Player) getWrapper());
    		}
    	}
    	
    	return results;

//    	Optional<Player> oPlayer = Prison.get().getPlatform().getPlayer( getName() );
//    	
//    	if ( oPlayer.isPresent() ) {
//    		results = oPlayer.get().getSellAllMultiplier();
//    	}
//    	
//    	return results;
    }
    
    @Override
    public double getSellAllMultiplierDebug() {
    	double results = 1.0;
    	
    	if ( isPlayer() ) {
    		
    		SellAllUtil sellall = SpigotPrison.getInstance().getSellAllUtil();
    		
    		if ( sellall != null && getWrapper() != null ) {
    			
    			Player player = getPlatformPlayer();
    			
    			results = sellall.getPlayerMultiplierDebug( player );
    		}
    	}
    	
    	return results;
    }
    
    @Override
    public List<String> getSellAllMultiplierListings() {
    	List<String> results = new ArrayList<>();
    	
    	if ( isPlayer() ) {
    		
    		SellAllUtil sellall = SpigotPrison.getInstance().getSellAllUtil();
    		
    		if ( sellall != null && getWrapper() != null ) {
    			results.addAll( sellall.getPlayerMultiplierList((org.bukkit.entity.Player) getWrapper()) );
    		}
    	}

    	
    	return results;
    }
    
    public List<String> getPermissionsIntegrations( boolean detailed ) {
    	List<String> results = new ArrayList<>();
    	
    	Optional<Player> oPlayer = Prison.get().getPlatform().getPlayer( getName() );
    	
    	if ( oPlayer.isPresent() ) {
    		
    		PermissionIntegration perms = PrisonAPI.getIntegrationManager() .getPermission();
    		if ( perms != null ) {
    			results = perms.getPermissions( oPlayer.get(), detailed );
    		}
    	}
    	
    	return results;
    }
    
    
    
    @Override 
    public boolean isPlayer() {
    	return bukkitSender != null && bukkitSender instanceof org.bukkit.entity.Player;
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( "SpigotCommandSender: " ).append( getName() )
    		.append( "  isOp=" ).append( isOp() )
    		.append( "  isPlayer=" ).append( isPlayer() );
    	
    	return sb.toString();
    }
    
    public org.bukkit.command.CommandSender getWrapper() {
        return bukkitSender;
    }

	@Override
	public Player getPlatformPlayer() {
		Player player = null;
		
		Optional<Player> oPlayer = Prison.get().getPlatform().getPlayer( getName() );
		
		if ( oPlayer.isPresent() ) {
			player = oPlayer.get();
		}
		
		return player;
	}
	
	@Override
	public RankPlayer getRankPlayer() {
		RankPlayer rankPlayer = null;
		
		if ( PrisonRanks.getInstance().isEnabled() ) {
			
			rankPlayer = PrisonRanks.getInstance().getPlayerManager()
					.getPlayer( (Player) this );
		}
		return rankPlayer;
	}

}
