package com.feticraft.constructor.npc;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ArchitectNPCManager {

    private final ConstructorFeticraftPlugin plugin;
    private final NamespacedKey npcKey;
    private File npcFile;
    private FileConfiguration npcConfig;

    public ArchitectNPCManager(ConstructorFeticraftPlugin plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "architect_npc");
        loadNPCConfig();
    }

    private void loadNPCConfig() {
        npcFile = new File(plugin.getDataFolder(), "npc.yml");
        if (!npcFile.exists()) {
            plugin.saveResource("npc.yml", false);
        }
        npcConfig = YamlConfiguration.loadConfiguration(npcFile);
    }

    public void spawnArchitect(Location loc) {
        removeArchitect(); // Eliminar anterior si existe

        // Asegurar que el chunk está cargado
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load();
        }

        // Forzar el spawn ignorando restricciones de protección del mundo
        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        
        villager.setProfession(Villager.Profession.MASON);
        villager.setVillagerType(Villager.Type.PLAINS);
        villager.setCustomName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("npc.name", "&e&lArquitecto Feticraft")));
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setRemoveWhenFarAway(false);
        villager.setPersistent(true);

        // Marcar la entidad con PDC propia
        villager.getPersistentDataContainer().set(npcKey, PersistentDataType.BYTE, (byte) 1);

        // Guardar persistencia
        saveNPCToConfig(loc, villager.getUniqueId());
        plugin.getLogger().info("NPC Arquitecto propio creado con éxito en zona protegida.");
    }

    public void removeArchitect() {
        if (!npcConfig.contains("architect.uuid")) return;

        try {
            UUID uuid = UUID.fromString(npcConfig.getString("architect.uuid"));
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null && isArchitect(entity)) {
                    entity.remove();
                    break;
                }
            }
        } catch (Exception ignored) {}

        npcConfig.set("architect", null);
        saveFile();
    }

    public boolean isArchitect(Entity entity) {
        if (!(entity instanceof Villager)) return false;
        return entity.getPersistentDataContainer().has(npcKey, PersistentDataType.BYTE);
    }

    private void saveNPCToConfig(Location loc, UUID uuid) {
        npcConfig.set("architect.uuid", uuid.toString());
        npcConfig.set("architect.world", loc.getWorld().getName());
        npcConfig.set("architect.x", loc.getX());
        npcConfig.set("architect.y", loc.getY());
        npcConfig.set("architect.z", loc.getZ());
        npcConfig.set("architect.yaw", loc.getYaw());
        npcConfig.set("architect.pitch", loc.getPitch());
        saveFile();
    }

    private void saveFile() {
        try {
            npcConfig.save(npcFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar npc.yml: " + e.getMessage());
        }
    }
}
