package tech.mcprison.prison.spigot.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SpigotMineNotificationRadiusGUI extends SpigotGUIComponents{

    private int dimension = 45;
    private Player p;
    private String minename;
    private long val;
    private String typeNotification;

    public SpigotMineNotificationRadiusGUI(Player p, Long val, String typeNotification, String minename){
        this.p = p;
        this.val = val;
        this.minename = minename;
        this.typeNotification = typeNotification;
    }

    public void open() {

        Inventory inv = Bukkit.createInventory(null, dimension, "§3MineNotifications -> Radius");

        List<String> changeDecreaseValueLore = createLore(
                "§8Click to decrease"
        );

        ItemStack decreaseOf1 = createButton(Material.REDSTONE_BLOCK, 1, changeDecreaseValueLore, "§3" + minename + " " + val + " - 1 " + typeNotification );
        inv.setItem(1, decreaseOf1);

        ItemStack decreaseOf5 = createButton(Material.REDSTONE_BLOCK, 5, changeDecreaseValueLore, "§3" + minename + " " + val + " - 5 " + typeNotification);
        inv.setItem(10, decreaseOf5);

        ItemStack decreaseOf10 = createButton(Material.REDSTONE_BLOCK, 10, changeDecreaseValueLore, "§3" + minename + " " + val + " - 10 " + typeNotification);
        inv.setItem(19, decreaseOf10);

        ItemStack decreaseOf50 = createButton(Material.REDSTONE_BLOCK, 50, changeDecreaseValueLore, "§3" + minename + " " + val + " - 50 " + typeNotification);
        inv.setItem(28, decreaseOf50);

        ItemStack decreaseOf100 = createButton(Material.REDSTONE_BLOCK, 1, changeDecreaseValueLore, "§3" + minename + " " + val + " - 100 " + typeNotification);
        inv.setItem(37, decreaseOf100);



        List<String> confirmButtonLore = createLore(
                "§aLeft-Click to confirm",
                "§8Radius: " + val,
                "§cRight-Click to cancel"
        );

        ItemStack confirmButton = createButton(Material.WATCH, 1, confirmButtonLore, "§3" + "Confirm: " + minename + " " + val + " " + typeNotification);
        inv.setItem(22, confirmButton);



        List<String> changeIncreaseValueLore = createLore(
                "§8Click to increase"
        );

        ItemStack increseOf1 = createButton(Material.EMERALD_BLOCK, 1, changeIncreaseValueLore, "§3" + minename + " " + val + " + 1 " + typeNotification);
        inv.setItem(7, increseOf1);

        ItemStack increaseOf5 = createButton(Material.EMERALD_BLOCK, 5, changeIncreaseValueLore, "§3" + minename + " " + val + " + 5 " + typeNotification);
        inv.setItem(16, increaseOf5);

        ItemStack increaseOf10 = createButton(Material.EMERALD_BLOCK, 10, changeIncreaseValueLore, "§3" + minename + " " + val + " + 10 " + typeNotification);
        inv.setItem(25, increaseOf10);

        ItemStack increaseOf50 = createButton(Material.EMERALD_BLOCK, 50, changeIncreaseValueLore, "§3" + minename + " " + val + " + 50 " + typeNotification);
        inv.setItem(34, increaseOf50);

        ItemStack increaseOf100 = createButton(Material.EMERALD_BLOCK, 1, changeIncreaseValueLore, "§3" + minename + " " + val + " + 100 " + typeNotification);
        inv.setItem(43, increaseOf100);

        this.p.openInventory(inv);
    }

}
