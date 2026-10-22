package com.feticraft.constructor.gui;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import com.feticraft.constructor.builder.BuildingExecutor;
import com.feticraft.constructor.selection.ConstructionSelector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // 1. Manejo del menú del catálogo
        if (title.equals(CatalogMenu.MENU_TITLE)) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            Player player = (Player) event.getWhoClicked();
            ItemMeta meta = clickedItem.getItemMeta();
            ConstructorFeticraftPlugin plugin = ConstructorFeticraftPlugin.getInstance();

            NamespacedKey catalogKey = new NamespacedKey(plugin, "catalog_building_key");
            if (!meta.getPersistentDataContainer().has(catalogKey, PersistentDataType.STRING)) return;

            String buildingId = meta.getPersistentDataContainer().get(catalogKey, PersistentDataType.STRING);
            String itemName = meta.getDisplayName();

            player.closeInventory();
            
            ItemStack tool = ConstructionSelector.getSelectionTool(plugin, buildingId, itemName);
            player.getInventory().addItem(tool);

            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "Has seleccionado: " + itemName);
            player.sendMessage(ChatColor.YELLOW + "Usa la Herramienta en el suelo para previsualizar el área.");
            return;
        }

        // 2. Manejo del menú de confirmación de obra
        if (title.equals(ConfirmMenu.CONFIRM_TITLE)) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            Player player = (Player) event.getWhoClicked();
            ConstructorFeticraftPlugin plugin = ConstructorFeticraftPlugin.getInstance();

            if (clickedItem.getType() == Material.RED_WOOL) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "[Arquitecto] Ubicación cancelada. Puedes volver a usar la herramienta cuando quieras.");
                return;
            }

            if (clickedItem.getType() == Material.LIME_WOOL) {
                ItemMeta meta = clickedItem.getItemMeta();
                NamespacedKey idKey = new NamespacedKey(plugin, "confirm_building_id");
                NamespacedKey locKey = new NamespacedKey(plugin, "confirm_building_loc");

                if (!meta.getPersistentDataContainer().has(idKey, PersistentDataType.STRING)) return;

                String buildingId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
                String locRaw = meta.getPersistentDataContainer().get(locKey, PersistentDataType.STRING);
                
                String[] parts = locRaw.split(",");
                Location targetLoc = new Location(
                    Bukkit.getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
                );

                player.closeInventory();

                // Iniciar la construcción con el obrero
                BuildingExecutor.buildStructure(plugin, player, targetLoc, buildingId);
            }
        }
    }
}
