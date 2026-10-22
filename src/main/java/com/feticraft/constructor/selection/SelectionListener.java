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
import org.bukkit.scheduler.BukkitRunnable;

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

        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;

            Location baseLoc = clickedBlock.getLocation().add(0, 1, 0);

            // Iniciar animación de partículas durante 10 segundos (200 ticks)
            startParticleTask(player, baseLoc, 5, 5, 4, 10);

            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡Ubicación de la obra fijada!");
            player.sendMessage(ChatColor.YELLOW + "El perímetro estará marcado con partículas durante 10 segundos.");

            // Retirar herramienta de marcado
            player.getInventory().removeItem(item);
        }
    }

    private void startParticleTask(Player player, Location corner, int width, int length, int height, int seconds) {
        new BukkitRunnable() {
            int ticksRun = 0;
            final int maxTicks = seconds * 20;

            @Override
            public void run() {
                if (ticksRun >= maxTicks || !player.isOnline()) {
                    cancel();
                    return;
                }

                for (double x = 0; x <= width; x += 0.5) {
                    for (double z = 0; z <= length; z += 0.5) {
                        if (x == 0 || x == width || z == 0 || z == length) {
                            player.spawnParticle(Particle.HAPPY_VILLAGER, corner.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
                            player.spawnParticle(Particle.HAPPY_VILLAGER, corner.clone().add(x, height, z), 1, 0, 0, 0, 0);
                        }
                    }
                }
                ticksRun += 10; // Dibujar cada medio segundo
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
}
