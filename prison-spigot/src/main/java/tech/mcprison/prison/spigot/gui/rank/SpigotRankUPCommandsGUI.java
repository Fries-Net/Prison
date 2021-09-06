package tech.mcprison.prison.spigot.gui.rank;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.entity.Player;

import tech.mcprison.prison.output.Output;
import tech.mcprison.prison.ranks.data.Rank;
import tech.mcprison.prison.spigot.SpigotPrison;
import tech.mcprison.prison.spigot.configs.NewMessagesConfig;
import tech.mcprison.prison.spigot.game.SpigotPlayer;
import tech.mcprison.prison.spigot.gui.guiutility.Button;
import tech.mcprison.prison.spigot.gui.guiutility.ButtonLore;
import tech.mcprison.prison.spigot.gui.guiutility.PrisonGUI;
import tech.mcprison.prison.spigot.gui.guiutility.SpigotGUIComponents;

/**
 * @author GABRYCA
 */
public class SpigotRankUPCommandsGUI extends SpigotGUIComponents {

    private final Player p;
    private final Rank rank;

    // Global Strings.
    private final String shiftRightClickToDelete = newMessages.getString(NewMessagesConfig.StringID.spigot_gui_lore_click_right_and_shift_to_delete);
    private final String loreInfo = newMessages.getString(NewMessagesConfig.StringID.spigot_gui_lore_info);
    private final String loreCommand = newMessages.getString(NewMessagesConfig.StringID.spigot_gui_lore_command);

    public SpigotRankUPCommandsGUI(Player p, Rank rank) {
        this.p = p;
        this.rank = rank;
    }

    public void open() {

        // Check if Ranks are enabled
        if (!(checkRanks(p))){
            return;
        }

        if (rank.getRankUpCommands().size() == 0){
            Output.get().sendWarn(new SpigotPlayer(p), messages.getString("Message.NoRankupCommands"));
            return;
        }

        // Get the dimensions and if needed increases them
        int dimension = (int) Math.ceil(rank.getRankUpCommands().size() / 9D) * 9;

        // If the inventory is empty
        if (dimension == 0){
            Output.get().sendWarn(new SpigotPlayer(p), messages.getString("Message.EmptyGui"));
            p.closeInventory();
            return;
        }

        // If the dimension's too big, don't open the GUI
        if (dimension > 54){
            Output.get().sendWarn(new SpigotPlayer(p), messages.getString("Message.TooManyRankupCommands"));
            p.closeInventory();
            return;
        }

        PrisonGUI gui = new PrisonGUI(p, dimension, "&3RankManager -> RankUPCommands");

        // For every command make a button
        for (String command : rank.getRankUpCommands()) {

            ButtonLore commandsLore = new ButtonLore(shiftRightClickToDelete, loreInfo);

            commandsLore.addLineLoreDescription(SpigotPrison.format(loreCommand + " " + command));

            // Add the button to the inventory
            gui.addButton(new Button(null, XMaterial.TRIPWIRE_HOOK, commandsLore, SpigotPrison.format("&3" + rank.getName() + " " + command)));

        }

        // Open the GUI.
        gui.open();
    }
}
