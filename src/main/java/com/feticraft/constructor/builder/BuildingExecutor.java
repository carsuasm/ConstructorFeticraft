package com.feticraft.constructor.builder;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
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

        int width = catalog.getInt("disenos." + buildingId + ".dimensiones.ancho", 6);
        int length = catalog.getInt("disenos." + buildingId + ".dimensiones.largo", 6);
        int height = catalog.getInt("disenos." + buildingId + ".dimensiones.alto", 5);
        String nombre = catalog.getString("disenos." + buildingId + ".nombre", "Estructura");

        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡El ayudante ha comenzado a levantar " + nombre + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_MASON, 1.0f, 1.0f);

        // Spawnear un NPC Obrero temporal justo al lado de la construcción
        Location npcSpawnLoc = startLoc.clone().add(width / 2.0, 0, -2.0);
        npcSpawnLoc.setYaw(0);
        npcSpawnLoc.setPitch(0);

        Villager builderNpc = startLoc.getWorld().spawn(npcSpawnLoc, Villager.class, villager -> {
            villager.setProfession(Villager.Profession.MASON);
            villager.setVillagerType(Villager.Type.PLAINS);
            villager.setCustomName(ChatColor.YELLOW + "Obrero de " + player.getName());
            villager.setCustomNameVisible(true);
            villager.setAI(true); // Permitir que mueva la cabeza/cuerpo
            villager.setInvulnerable(true);
            villager.setSilent(false);
            villager.setRemoveWhenFarAway(false);
            // Darle una herramienta en la mano
            villager.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));
        });

        // Tarea de construcción bloque a bloque a velocidad pausada y visual
        new BukkitRunnable() {
            int currentX = 0;
            int currentY = 0;
            int currentZ = 0;

            @Override
            public void run() {
                // Si el jugador se desconecta o el NPC muere/desaparece, cancelar
                if (!player.isOnline() || builderNpc.isDead()) {
                    if (builderNpc.isValid()) builderNpc.remove();
                    cancel();
                    return;
                }

                // Construir 2 bloques por tick o 1 bloque cada poco para un ritmo perfecto (~1-2 bloques por segundo reales)
                int blocksPlaced = 0;
                int maxBlocksPerInterval = 2; 

                while (blocksPlaced < maxBlocksPerInterval) {
                    if (currentY > height) {
                        // Fin de la construcción
                        builderNpc.remove();
                        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡La construcción de " + nombre + " ha finalizado con éxito!");
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        cancel();
                        return;
                    }

                    // Definir material temporal o según diseño
                    Material materialToPlace = (currentY == 0) ? Material.STONE_BRICKS : Material.OAK_PLANKS;
                    if (currentY == height) materialToPlace = Material.OAK_SLAB;

                    // Construir contorno/paredes
                    if (currentX == 0 || currentX == width || currentZ == 0 || currentZ == length) {
                        Location blockLoc = startLoc.clone().add(currentX, currentY, currentZ);
                        Block block = blockLoc.getBlock();
                        
                        if (!block.getChunk().isLoaded()) {
                            block.getChunk().load();
                        }
                        block.setType(materialToPlace, false);

                        // Hacer que el NPC obrero mire hacia el bloque que se está colocando
                        Location npcLoc = builderNpc.getLocation();
                        npcLoc.setDirection(blockLoc.toVector().subtract(npcLoc.toVector()));
                        builderNpc.teleport(npcLoc);

                        blocksPlaced++;
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
        }.runTaskTimer(plugin, 0L, 10L); // Intervalo de 10 ticks (0.5 segundos por ciclo, logrando un ritmo genial)
    }
}
