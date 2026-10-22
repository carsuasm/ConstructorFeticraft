package com.feticraft.constructor.selection;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class SelectionListener implements Listener {

    private final ConstructorFeticraftPlugin plugin;

    public SelectionListener(ConstructorFeticraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == org.bukkit.Material.AIR || !item.hasItemMeta()) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "selected_building_id");
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true); // Cancela el uso normal de la pluma

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;

            Location baseLoc = clickedBlock.getLocation().add(0, 1, 0);
            String idDiseno = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

            // Mostrar partículas del perímetro
            drawAreaParticles(player, baseLoc, 5, 5, 4);

            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡Ubicación de la obra fijada!");
            player.sendMessage(ChatColor.YELLOW + "Puntos de la estructura marcados con partículas.");

            // Retirar herramienta de marcado
            player.getInventory().removeItem(item);
        }
    }

    private void drawAreaParticles(Player player, Location corner, int width, int length, int height) {
        for (double x = 0; x <= width; x += 0.5) {
            for (double z = 0; z <= length; z += 0.5) {
                if (x == 0 || x == width || z == 0 || z == length) {
                    player.spawnParticle(Particle.HAPPY_VILLAGER, corner.clone().add(x, 0.1, z), 1);
                    player.spawnParticle(Particle.HAPPY_VILLAGER, corner.clone().add(x, height, z), 1);
                }
            }
        }
    }
}
