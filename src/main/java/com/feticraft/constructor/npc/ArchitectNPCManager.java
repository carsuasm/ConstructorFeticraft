package com.feticraft.constructor.npc;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
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

        // Cargar el chunk de forma síncrona
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load();
        }

        // Spawn con razón CUSTOM para sobrepasar restricciones de WorldGuard / LederProtections
        Villager villager = loc.getWorld().spawn(loc, Villager.class, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
            entity.setProfession(Villager.Profession.MASON);
            entity.setVillagerType(Villager.Type.PLAINS);
            entity.setCustomName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("npc.name", "&e&lArquitecto Feticraft")));
            entity.setCustomNameVisible(true);
            entity.setAI(false);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true);

            // Marcar la entidad con PDC propia
            entity.getPersistentDataContainer().set(npcKey, PersistentDataType.BYTE, (byte) 1);
        });

        // Guardar persistencia
        saveNPCToConfig(loc, villager.getUniqueId());
        plugin.getLogger().info("NPC Arquitecto propio creado con éxito en: " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ());
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
