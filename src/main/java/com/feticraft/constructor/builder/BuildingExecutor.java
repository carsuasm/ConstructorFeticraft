package com.feticraft.constructor.builder;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

        String nombre = catalog.getString("disenos." + buildingId + ".nombre", "Estructura");
        String esquemaPath = catalog.getString("disenos." + buildingId + ".esquema", "");

        File schemFile = new File(plugin.getDataFolder(), "disenos/" + esquemaPath);
        if (!schemFile.exists()) {
            player.sendMessage(ChatColor.RED + "Error: No se encuentra el archivo de diseño (" + esquemaPath + ").");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡El ayudante ha comenzado a levantar " + nombre + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_MASON, 1.0f, 1.0f);

        Location npcSpawnLoc = startLoc.clone().add(3.0, 0, -2.0);
        Villager builderNpc = startLoc.getWorld().spawn(npcSpawnLoc, Villager.class, villager -> {
            villager.setProfession(Villager.Profession.MASON);
            villager.setVillagerType(Villager.Type.PLAINS);
            villager.setCustomName(ChatColor.YELLOW + "Obrero de " + player.getName());
            villager.setCustomNameVisible(true);
            villager.setAI(true);
            villager.setInvulnerable(true);
            villager.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));
        });

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
            if (format == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Error: Formato de esquema no reconocido.");
                    if (builderNpc.isValid()) builderNpc.remove();
                });
                return;
            }

            try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                Clipboard clipboard = reader.read();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    executeBlockPlacement(plugin, player, builderNpc, startLoc, clipboard, nombre);
                });

            } catch (IOException e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Error al leer el archivo .schem.");
                    if (builderNpc.isValid()) builderNpc.remove();
                });
                e.printStackTrace();
            }
        });
    }

    private static void executeBlockPlacement(ConstructorFeticraftPlugin plugin, Player player, Villager builderNpc, Location startLoc, Clipboard clipboard, String nombre) {
        BlockVector3 minimumPoint = clipboard.getMinimumPoint();
        BlockVector3 dimensions = clipboard.getDimensions();

        new BukkitRunnable() {
            int x = 0;
            int y = 0;
            int z = 0;

            final int maxX = dimensions.x();
            final int maxY = dimensions.y();
            final int maxZ = dimensions.z();

            @Override
            public void run() {
                if (!player.isOnline() || builderNpc.isDead()) {
                    if (builderNpc.isValid()) builderNpc.remove();
                    cancel();
                    return;
                }

                int blocksPlaced = 0;
                while (blocksPlaced < 3) {
                    if (y >= maxY) {
                        builderNpc.remove();
                        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡La construcción de " + nombre + " ha finalizado con éxito!");
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        cancel();
                        return;
                    }

                    BlockVector3 relPos = BlockVector3.at(minimumPoint.x() + x, minimumPoint.y() + y, minimumPoint.z() + z);
                    com.sk89q.worldedit.world.block.BlockState blockState = clipboard.getBlock(relPos);

                    Material mat = BukkitAdapter.adapt(blockState).getMaterial();

                    if (mat != null && !mat.isAir()) {
                        Location targetLoc = startLoc.clone().add(x, y, z);
                        Block block = targetLoc.getBlock();
                        
                        if (!block.getChunk().isLoaded()) {
                            block.getChunk().load();
                        }

                        block.setType(mat, false);

                        Location npcLoc = builderNpc.getLocation();
                        npcLoc.setDirection(targetLoc.toVector().subtract(npcLoc.toVector()));
                        builderNpc.teleport(npcLoc);
                    }

                    x++;
                    if (x >= maxX) {
                        x = 0;
                        z++;
                        if (z >= maxZ) {
                            z = 0;
                            y++;
                        }
                    }
                    blocksPlaced++;
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
}
