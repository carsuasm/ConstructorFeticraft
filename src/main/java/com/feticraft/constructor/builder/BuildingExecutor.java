package com.feticraft.constructor.builder;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import net.milkbowl.vault.economy.EconomyResponse;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BuildingExecutor {

    private static final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private static final Map<UUID, Villager> activeNpcs = new HashMap<>();
    private static final Set<UUID> pausedPlayers = new HashSet<>();

    public static boolean hasActiveBuilding(UUID playerUUID) {
        return activeTasks.containsKey(playerUUID) || activeNpcs.containsKey(playerUUID);
    }

    public static boolean isPaused(UUID playerUUID) {
        return pausedPlayers.contains(playerUUID);
    }

    public static void togglePause(Player player) {
        UUID uuid = player.getUniqueId();
        if (!hasActiveBuilding(uuid)) return;

        if (pausedPlayers.contains(uuid)) {
            pausedPlayers.remove(uuid);
            Villager npc = activeNpcs.get(uuid);
            if (npc != null && npc.isValid()) {
                npc.setCustomName(ChatColor.YELLOW + "Obrero de " + player.getName());
            }
            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "Construcción reanudada.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        } else {
            pausedPlayers.add(uuid);
            Villager npc = activeNpcs.get(uuid);
            if (npc != null && npc.isValid()) {
                npc.setCustomName(ChatColor.RED + "[EN PAUSA] Obrero de " + player.getName());
            }
            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.YELLOW + "Construcción pausada.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    public static boolean cancelBuilding(Player player) {
        UUID uuid = player.getUniqueId();
        boolean cleaned = false;

        if (activeTasks.containsKey(uuid)) {
            activeTasks.get(uuid).cancel();
            activeTasks.remove(uuid);
            cleaned = true;
        }

        pausedPlayers.remove(uuid);

        Villager npc = activeNpcs.remove(uuid);
        if (npc != null) {
            if (npc.isValid()) {
                npc.remove();
            }
            cleaned = true;
        }

        for (org.bukkit.World world : player.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Villager villager && villager.getCustomName() != null) {
                    if (villager.getCustomName().contains(player.getName())) {
                        villager.remove();
                        cleaned = true;
                    }
                }
            }
        }

        if (cleaned) {
            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.RED + "Has cancelado la construcción activa y eliminado al obrero.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        return false;
    }

    public static Villager getNpcOfPlayer(UUID uuid) {
        return activeNpcs.get(uuid);
    }

    public static UUID getOwnerOfNpc(Villager villager) {
        for (Map.Entry<UUID, Villager> entry : activeNpcs.entrySet()) {
            if (entry.getValue().equals(villager)) {
                return entry.getKey();
            }
        }
        if (villager.getCustomName() != null) {
            for (Player p : villager.getServer().getOnlinePlayers()) {
                if (villager.getCustomName().contains(p.getName())) {
                    return p.getUniqueId();
                }
            }
        }
        return null;
    }

    public static void buildStructure(ConstructorFeticraftPlugin plugin, Player player, Location startLoc, String buildingId) {
        UUID uuid = player.getUniqueId();

        if (hasActiveBuilding(uuid)) {
            player.sendMessage(ChatColor.RED + "Ya tienes una construcción en curso. Usa /constructor cancelar si deseas detenerla.");
            return;
        }

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

        Villager builderNpc = startLoc.getWorld().spawn(startLoc.clone().add(1, 0, 1), Villager.class, villager -> {
            villager.setProfession(Villager.Profession.MASON);
            villager.setVillagerType(Villager.Type.PLAINS);
            villager.setCustomName(ChatColor.YELLOW + "Obrero de " + player.getName());
            villager.setCustomNameVisible(true);
            
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setCollidable(false);
            villager.setSilent(true);
            
            villager.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));
        });

        activeNpcs.put(uuid, builderNpc);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
            if (format == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Error: Formato de esquema no reconocido.");
                    if (builderNpc.isValid()) builderNpc.remove();
                    activeNpcs.remove(uuid);
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
                    activeNpcs.remove(uuid);
                });
                e.printStackTrace();
            }
        });
    }

    private static void executeBlockPlacement(ConstructorFeticraftPlugin plugin, Player player, Villager builderNpc, Location startLoc, Clipboard clipboard, String nombre) {
        BlockVector3 minimumPoint = clipboard.getMinimumPoint();
        BlockVector3 dimensions = clipboard.getDimensions();
        UUID uuid = player.getUniqueId();

        final int maxX = dimensions.x();
        final int maxY = dimensions.y();
        final int maxZ = dimensions.z();
        final int totalBlocks = maxX * maxY * maxZ;

        BukkitRunnable runnable = new BukkitRunnable() {
            int x = 0;
            int y = 0;
            int z = 0;

            @Override
            public void run() {
                if (!player.isOnline() || builderNpc.isDead()) {
                    if (builderNpc.isValid()) builderNpc.remove();
                    activeTasks.remove(uuid);
                    activeNpcs.remove(uuid);
                    pausedPlayers.remove(uuid);
                    cancel();
                    return;
                }

                if (pausedPlayers.contains(uuid)) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "⏸ Construcción PAUSADA (Shift+Click al obrero)"));
                    return;
                }

                int blocksPlaced = 0;
                while (blocksPlaced < 4) {
                    if (y >= maxY) {
                        builderNpc.remove();
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "✔ ¡Construcción de " + nombre + " finalizada al 100%!"));
                        player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡La construcción de " + nombre + " ha finalizado!");
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        activeTasks.remove(uuid);
                        activeNpcs.remove(uuid);
                        pausedPlayers.remove(uuid);
                        cancel();
                        return;
                    }

                    int currentStepIndex = (y * maxX * maxZ) + (z * maxX) + x;
                    int percent = totalBlocks > 0 ? (currentStepIndex * 100) / totalBlocks : 100;
                    if (percent > 100) percent = 100;

                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                            ChatColor.YELLOW + "🏗 Construyendo " + nombre + ": " + ChatColor.GREEN + percent + "%"
                    ));

                    BlockVector3 relPos = BlockVector3.at(minimumPoint.x() + x, minimumPoint.y() + y, minimumPoint.z() + z);
                    com.sk89q.worldedit.world.block.BlockState blockState = clipboard.getBlock(relPos);
                    Material mat = BukkitAdapter.adapt(blockState).getMaterial();

                    Location targetLoc = startLoc.clone().add(x, y, z);

                    if (mat != null) {
                        Block block = targetLoc.getBlock();
                        if (!block.getChunk().isLoaded()) {
                            block.getChunk().load();
                        }

                        // Reemplaza y destruye cualquier bloque anterior en el mundo
                        block.setType(mat, false);

                        Location npcPos = targetLoc.clone().add(1.2, 0, 1.2);
                        npcPos.setDirection(targetLoc.toVector().subtract(npcPos.toVector()));
                        builderNpc.teleport(npcPos);

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
        };

        // Velocidad ajustada a 12L para que vaya más pausado y natural
        BukkitTask task = runnable.runTaskTimer(plugin, 0L, 12L);
        activeTasks.put(uuid, task);
    }
}
