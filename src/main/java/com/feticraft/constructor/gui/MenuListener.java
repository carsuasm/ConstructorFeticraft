package com.feticraft.constructor.gui;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import com.feticraft.constructor.selection.ConstructionSelector;
import org.bukkit.ChatColor;
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
        if (!event.getView().getTitle().equals(CatalogMenu.MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemMeta meta = clickedItem.getItemMeta();
        ConstructorFeticraftPlugin plugin = ConstructorFeticraftPlugin.getInstance();

        // Extraer la Key real del diseño guardada en el menú
        NamespacedKey catalogKey = new NamespacedKey(plugin, "catalog_building_key");
        if (!meta.getPersistentDataContainer().has(catalogKey, PersistentDataType.STRING)) {
            return;
        }

        String buildingId = meta.getPersistentDataContainer().get(catalogKey, PersistentDataType.STRING);
        String itemName = meta.getDisplayName();

        player.closeInventory();
        
        // Entregar la herramienta con el ID exacto del diseño seleccionado
        ItemStack tool = ConstructionSelector.getSelectionTool(plugin, buildingId, itemName);
        player.getInventory().addItem(tool);

        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "Has seleccionado: " + itemName);
        player.sendMessage(ChatColor.YELLOW + "Te he entregado la Herramienta del Arquitecto. Haz clic derecho en el suelo para ver sus dimensiones exactas.");
    }
}
