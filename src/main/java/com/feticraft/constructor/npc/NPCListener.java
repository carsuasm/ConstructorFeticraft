package com.feticraft.constructor.npc;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import com.feticraft.constructor.gui.CatalogMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NPCListener implements Listener {

    private final ConstructorFeticraftPlugin plugin;

    public NPCListener(ConstructorFeticraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        String name = villager.getCustomName();
        if (name != null && name.contains("Arquitecto")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            
            // Llamada correcta al método openCatalog que definimos en CatalogMenu
            CatalogMenu.openCatalog(player);
        }
    }
}
