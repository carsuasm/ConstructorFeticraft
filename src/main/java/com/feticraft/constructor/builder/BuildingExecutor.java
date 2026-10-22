package com.feticraft.constructor.builder;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

public class BuildingExecutor {

    public static void buildStructure(ConstructorFeticraftPlugin plugin, Player player, Location startLoc, String buildingId) {
        File catalogFile = new File(plugin.getDataFolder(), "catalogo.yml");
        if (!catalogFile.exists()) {
            player.sendMessage(ChatColor.RED + "Error: No se encuentra el catálogo de construcciones.");
            return;
        }

        YamlConfiguration catalog = YamlConfiguration.loadConfiguration(catalogFile);
        if (!catalog.contains("disenos." + buildingId)) {
            player.sendMessage(ChatColor.RED + "Error: El diseño seleccionado no existe en el catálogo.");
            return;
        }

        int width = catalog.getInt("disenos." + buildingId + ".dimensiones.ancho", 5);
        int length = catalog.getInt("disenos." + buildingId + ".dimensiones.largo", 5);
        int height = catalog.getInt("disenos." + buildingId + ".dimensiones.alto", 5);
        String nombre = catalog.getString("disenos." + buildingId + ".nombre", "Estructura");

        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡Comenzando la construcción de " + nombre + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_MASON, 1.0f, 1.0f);

        // Tarea asíncrona / por ticks para colocar bloques sin lagear el servidor
        new BukkitRunnable() {
            int currentX = 0;
            int currentY = 0;
            int currentZ = 0;

            @Override
            public void run() {
                // Bloques colocados por ciclo para mantener el servidor fluido
                int blocksPerTick = 10;
                int placedThisTick = 0;

                while (placedThisTick < blocksPerTick) {
                    if (currentY > height) {
                        // Construcción finalizada
                        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡La construcción de " + nombre + " ha finalizado con éxito!");
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        cancel();
                        return;
                    }

                    // Determinar el material base temporal (ejemplo: ladrillos o madera según altura)
                    Material materialToPlace = (currentY == 0) ? Material.STONE_BRICKS : Material.OAK_PLANKS;
                    if (currentY == height) materialToPlace = Material.OAK_STAIRS; // Techo básico

                    // Solo construir el contorno o paredes por ahora para el ejemplo funcional
                    if (currentX == 0 || currentX == width || currentZ == 0 || currentZ == length) {
                        Location blockLoc = startLoc.clone().add(currentX, currentY, currentZ);
                        Block block = blockLoc.getBlock();
                        
                        // Forzar carga de chunk si es necesario y colocar bloque
                        if (!block.getChunk().isLoaded()) {
                            block.getChunk().load();
                        }
                        block.setType(materialToPlace, false);
                        placedThisTick++;
                    }

                    // Avanzar coordenadas
                    currentX++;
                    if (currentX > width) {
                        currentX = 0;
                        currentZ++;
                        if (currentZ > length) {
                            currentZ = 0;
                            currentY++;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Se ejecuta cada 2 ticks
    }
}
