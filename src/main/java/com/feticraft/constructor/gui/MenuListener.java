package com.feticraft.constructor.gui;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import com.feticraft.constructor.selection.ConstructionSelector;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(CatalogMenu.MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String itemName = event.getCurrentItem().getItemMeta().getDisplayName();

        player.closeInventory();
        
        // Entregar la herramienta de marcado
        ItemStack tool = ConstructionSelector.getSelectionTool(ConstructorFeticraftPlugin.getInstance(), "choza_madera", itemName);
        player.getInventory().addItem(tool);

        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "Has seleccionado: " + itemName);
        player.sendMessage(ChatColor.YELLOW + "Te he entregado la Herramienta del Arquitecto. Haz clic derecho en el suelo para ubicar tu obra.");
    }
}
