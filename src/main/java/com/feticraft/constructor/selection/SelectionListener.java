package com.feticraft.constructor.selection;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

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
            String idDiseno = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

            // Leer dimensiones reales desde catalogo.yml (con un tamaño generoso por defecto si no se encuentran)
            int width = 12;   // Ancho predeterminado grande para una casa
            int length = 12;  // Largo predeterminado grande
            int height = 8;   // Altura de 2 pisos / techos altos

            File catalogFile = new File(plugin.getDataFolder(), "catalogo.yml");
            if (catalogFile.exists()) {
                YamlConfiguration catalog = YamlConfiguration.loadConfiguration(catalogFile);
                if (catalog.contains("disenos." + idDiseno)) {
                    width = catalog.getInt("disenos." + idDiseno + ".dimensiones.ancho", 12);
                    length = catalog.getInt("disenos." + idDiseno + ".dimensiones.largo", 12);
                    height = catalog.getInt("disenos." + idDiseno + ".dimensiones.alto", 8);
                }
            }

            // Iniciar animación de partículas grandes durante 15 segundos
            startParticleTask(player, baseLoc, width, length, height, 15);

            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡Ubicación de la obra fijada!");
            player.sendMessage(ChatColor.YELLOW + "Mostrando perímetro de la casa (" + width + "x" + length + "x" + height + ") durante 15 segundos.");

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

                // Dibujar aristas y contorno con separación fluida
                for (double x = 0; x <= width; x += 1.0) {
                    for (double z = 0; z <= length; z += 1.0) {
                        // Solo los bordes externos para formar el cubo/perímetro de la casa
                        if (x == 0 || x == width || z == 0 || z == length) {
                            // Suelo y esquinas inferiores
                            player.spawnParticle(Particle.HAPPY_VILLAGER, corner.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
                            // Techo y esquinas superiores
                            player.spawnParticle(Particle.HAPPY_VILLAGER, corner.clone().add(x, height, z), 1, 0, 0, 0, 0);
                        }
                    }
                }

                // Dibujar pilares en las 4 esquinas verticales para marcar la altura claramente
                for (double y = 0; y <= height; y += 1.0) {
                    player.spawnParticle(Particle.FLAME, corner.clone().add(0, y, 0), 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.FLAME, corner.clone().add(width, y, 0), 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.FLAME, corner.clone().add(0, y, length), 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.FLAME, corner.clone().add(width, y, length), 1, 0, 0, 0, 0);
                }

                ticksRun += 10; // Actualizar cada medio segundo
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
}
