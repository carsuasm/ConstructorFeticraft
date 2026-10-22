package com.feticraft.constructor.gui;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(CatalogMenu.MENU_TITLE)) {
            return;
        }

        event.setCancelled(true); // Cancela el movimiento de ítems del menú

        if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String itemName = event.getCurrentItem().getItemMeta().getDisplayName();

        player.closeInventory();
        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "Has seleccionado: " + itemName);
        player.sendMessage(ChatColor.YELLOW + "Próximamente se te entregará la herramienta de marcado de terreno para ubicar la obra.");
    }
}
