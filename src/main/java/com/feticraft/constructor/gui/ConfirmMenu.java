package com.feticraft.constructor.gui;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.List;

public class ConfirmMenu {

    public static final String CONFIRM_TITLE = ChatColor.DARK_GRAY + "¿Confirmar Construcción?";

    public static void openConfirmMenu(ConstructorFeticraftPlugin plugin, Player player, String buildingId, Location startLoc) {
        Inventory gui = Bukkit.createInventory(null, 27, CONFIRM_TITLE);

        File catalogFile = new File(plugin.getDataFolder(), "catalogo.yml");
        YamlConfiguration catalog = YamlConfiguration.loadConfiguration(catalogFile);

        String nombre = catalog.getString("disenos." + buildingId + ".nombre", "Construcción");
        double precio = catalog.getDouble("disenos." + buildingId + ".precio", 50000.0);

        // Ítem Verde: CONFIRMAR
        ItemStack acceptItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta acceptMeta = acceptItem.getItemMeta();
        if (acceptMeta != null) {
            acceptMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "¡CONFIRMAR Y CONSTRUIR!");
            acceptMeta.setLore(List.of(
                ChatColor.GRAY + "Proyecto: " + ChatColor.GOLD + nombre,
                ChatColor.GRAY + "Coste total: " + ChatColor.GREEN + "$" + precio,
                "",
                ChatColor.YELLOW + "▶ Haz clic para cobrar e iniciar la obra aquí."
            ));

            // Guardar datos en el ítem verde
            NamespacedKey idKey = new NamespacedKey(plugin, "confirm_building_id");
            NamespacedKey locKey = new NamespacedKey(plugin, "confirm_building_loc");
            
            acceptMeta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, buildingId);
            acceptMeta.getPersistentDataContainer().set(locKey, PersistentDataType.STRING, 
                startLoc.getWorld().getName() + "," + startLoc.getBlockX() + "," + startLoc.getBlockY() + "," + startLoc.getBlockZ()
            );

            acceptItem.setItemMeta(acceptMeta);
        }

        // Ítem Rojo: CANCELAR
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "CANCELAR Y REUBICAR");
            cancelMeta.setLore(List.of(
                ChatColor.GRAY + "Cancela la ubicación actual.",
                ChatColor.GRAY + "Podrás elegir otra zona o cambiar de diseño."
            ));
            cancelItem.setItemMeta(cancelMeta);
        }

        gui.setItem(11, acceptItem);
        gui.setItem(15, cancelItem);

        player.openInventory(gui);
    }
}
