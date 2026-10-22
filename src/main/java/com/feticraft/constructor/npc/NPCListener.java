package com.feticraft.constructor.npc;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NPCListener implements Listener {

    private final ArchitectNPCManager npcManager;

    public NPCListener(ArchitectNPCManager npcManager) {
        this.npcManager = npcManager;
    }

    @EventHandler
    public void onPlayerInteractNPC(PlayerInteractEntityEvent event) {
        if (npcManager.isArchitect(event.getRightClicked())) {
            event.setCancelled(true); // Evitar menú vanilla del aldeano

            Player player = event.getPlayer();
            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡Hola " + player.getName() + "! Próximamente abriré el catálogo de construcciones.");
        }
    }
}
