package com.feticraft.constructor.selection;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import com.feticraft.constructor.gui.ConfirmMenu;
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

        String idDiseno = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        // Cancelar si el jugador está agachado (Shift + Clic Derecho)
        if (player.isSneaking() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            NamespacedKey locKey = new NamespacedKey(plugin, "marked_location");
            var meta = item.getItemMeta();
            meta.getPersistentDataContainer().remove(locKey);
            item.setItemMeta(meta);
            
            player.sendMessage(ChatColor.RED + "[Arquitecto] Selección cancelada. Puedes marcar otra ubicación.");
            return;
        }

        NamespacedKey locKey = new NamespacedKey(plugin, "marked_location");
        boolean hasLocation = item.getItemMeta().getPersistentDataContainer().has(locKey, PersistentDataType.STRING);

        // Si hace clic en un bloque y NO tenía ubicación marcada (o quiere cambiarla)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null) {
                Location baseLoc = clickedBlock.getLocation().add(0, 1, 0);

                int width = 7;
                int length = 7;
                int height = 6;

                File catalogFile = new File(plugin.getDataFolder(), "catalogo.yml");
                if (catalogFile.exists()) {
                    YamlConfiguration catalog = YamlConfiguration.loadConfiguration(catalogFile);
                    if (catalog.contains("disenos." + idDiseno)) {
                        width = catalog.getInt("disenos." + idDiseno + ".dimensiones.ancho", 7);
                        length = catalog.getInt("disenos." + idDiseno + ".dimensiones.largo", 7);
                        height = catalog.getInt("disenos." + idDiseno + ".dimensiones.alto", 6);
                    }
                }

                // Guardar la ubicación elegida en el ítem
                String locData = baseLoc.getWorld().getName() + "," + baseLoc.getBlockX() + "," + baseLoc.getBlockY() + "," + baseLoc.getBlockZ();
                var meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(locKey, PersistentDataType.STRING, locData);
                item.setItemMeta(meta);

                // Mostrar partículas 3D durante 20 segundos
                startParticleTask(player, baseLoc, width, length, height, 20);

                player.sendMessage(ChatColor.GOLD + "--------------------------------------------------");
                player.sendMessage(ChatColor.GREEN + "✔ Perímetro de obra delimitado con partículas.");
                player.sendMessage(ChatColor.YELLOW + "▶ Mira bien el terreno desde la distancia.");
                player.sendMessage(ChatColor.AQUA + "▶ Haz clic derecho en el aire o bloque para abrir el menú de confirmación.");
                player.sendMessage(ChatColor.GOLD + "--------------------------------------------------");
                return;
            }
        }

        // Si ya tiene la ubicación marcada y hace clic (en aire o bloque), abre el menú de confirmación
        if (hasLocation) {
            String locRaw = item.getItemMeta().getPersistentDataContainer().get(locKey, PersistentDataType.STRING);
            String[] parts = locRaw.split(",");
            Location targetLoc = new Location(
                org.bukkit.Bukkit.getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
            );

            ConfirmMenu.openConfirmMenu(plugin, player, idDiseno, targetLoc);
        } else {
            player.sendMessage(ChatColor.YELLOW + "[Arquitecto] Primero haz clic derecho en un bloque para marcar dónde quieres construir.");
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

                for (double x = 0; x <= width; x += 1.0) {
                    for (double z = 0; z <= length; z += 1.0) {
                        if (x == 0 || x == width || z == 0 || z == length) {
                            player.spawnParticle(Particle.HAPPY_VILLAGER, corner.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
                            player.spawnParticle(Particle.HAPPY_VILLAGER, corner.clone().add(x, height, z), 1, 0, 0, 0, 0);
                        }
                    }
                }

                for (double y = 0; y <= height; y += 1.0) {
                    player.spawnParticle(Particle.FLAME, corner.clone().add(0, y, 0), 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.FLAME, corner.clone().add(width, y, 0), 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.FLAME, corner.clone().add(0, y, length), 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.FLAME, corner.clone().add(width, y, length), 1, 0, 0, 0, 0);
                }

                ticksRun += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
}
