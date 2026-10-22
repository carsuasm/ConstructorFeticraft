package com.feticraft.constructor.builder;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
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
        double precio = catalog.getDouble("disenos." + buildingId + ".precio", 0.0);
        String esquemaPath = catalog.getString("disenos." + buildingId + ".esquema", "");

        // 1. Cobro mediante Vault
        if (plugin.getEconomy() != null && precio > 0) {
            if (!plugin.getEconomy().has(player, precio)) {
                player.sendMessage(ChatColor.RED + "No tienes suficiente dinero. Necesitas $" + precio);
                return;
            }
            EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, precio);
            if (!response.transactionSuccess()) {
                player.sendMessage(ChatColor.RED + "Error al procesar el pago: " + response.errorMessage);
                return;
            }
            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "Se han debitado $" + precio + " de tu cuenta.");
        }

        File schemFile = new File(plugin.getDataFolder(), "disenos/" + esquemaPath);
        if (!schemFile.exists()) {
            player.sendMessage(ChatColor.RED + "Error: No se encuentra el archivo (" + esquemaPath + ").");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡El obrero ha comenzado a construir " + nombre + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_MASON, 1.0f, 1.0f);

        // 2. Spawn del Obrero fijado sin IA errática
        Villager builderNpc = startLoc.getWorld().spawn(startLoc.clone().add(1, 0, 1), Villager.class, villager -> {
            villager.setProfession(Villager.Profession.MASON);
            villager.setVillagerType(Villager.Type.PLAINS);
            villager.setCustomName(ChatColor.YELLOW + "Obrero de " + player.getName());
            villager.setCustomNameVisible(true);
            villager.setAI(false); // Quitar IA libre para controlar movimiento
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
            int y = 0; // Empieza desde la capa más baja (incluyendo subterráneos)
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
                while (blocksPlaced < 4) { // Coloca 4 bloques por ciclo
                    if (y >= maxY) {
                        builderNpc.remove();
                        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡La construcción de " + nombre + " ha finalizado!");
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        cancel();
                        return;
                    }

                    BlockVector3 relPos = BlockVector3.at(minimumPoint.x() + x, minimumPoint.y() + y, minimumPoint.z() + z);
                    com.sk89q.worldedit.world.block.BlockState blockState = clipboard.getBlock(relPos);
                    Material mat = BukkitAdapter.adapt(blockState).getMaterial();

                    Location targetLoc = startLoc.clone().add(x, y, z);

                    if (mat != null && !mat.isAir()) {
                        Block block = targetLoc.getBlock();
                        if (!block.getChunk().isLoaded()) {
                            block.getChunk().load();
                        }

                        block.setType(mat, false);

                        // Teletransportar al obrero cerca del bloque actual para la animación
                        Location npcPos = targetLoc.clone().add(1.2, 0, 1.2);
                        npcPos.setDirection(targetLoc.toVector().subtract(npcPos.toVector()));
                        builderNpc.teleport(npcPos);

                        // Efectos de sonido y partículas de construcción
                        targetLoc.getWorld().spawnParticle(Particle.BLOCK, targetLoc.clone().add(0.5, 0.5, 0.5), 6, mat.createBlockData());
                        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_STONE_PLACE, 0.5f, 1.2f);
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
        }.runTaskTimer(plugin, 0L, 3L); // Animación fluida
    }
}
