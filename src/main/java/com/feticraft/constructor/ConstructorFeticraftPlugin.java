package com.feticraft.constructor;

import com.feticraft.constructor.npc.ArchitectNPCManager;
import com.feticraft.constructor.npc.NPCListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ConstructorFeticraftPlugin extends JavaPlugin {

    private static ConstructorFeticraftPlugin instance;
    private ArchitectNPCManager npcManager;

    @Override
    public void onEnable() {
        instance = this;

        // Cargar/guardar configuraciones iniciales
        saveDefaultConfig();
        saveResource("catalogo.yml", false);
        saveResource("npc.yml", false);

        // Inicializar gestor de NPC y eventos
        this.npcManager = new ArchitectNPCManager(this);
        getServer().getPluginManager().registerEvents(new NPCListener(npcManager), this);

        getLogger().info(ChatColor.GREEN + "¡ConstructorFeticraft ha sido habilitado correctamente en Feticraft!");
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "ConstructorFeticraft deshabilitado.");
    }

    public static ConstructorFeticraftPlugin getInstance() {
        return instance;
    }

    public ArchitectNPCManager getNpcManager() {
        return npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("constructor")) {
            return false;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("ayuda")) {
            sender.sendMessage(ChatColor.GOLD + "=== Comandos de ConstructorFeticraft ===");
            sender.sendMessage(ChatColor.YELLOW + "/constructor crear " + ChatColor.WHITE + "- Crea o recoloca al NPC arquitecto.");
            sender.sendMessage(ChatColor.YELLOW + "/constructor eliminar " + ChatColor.WHITE + "- Elimina el NPC arquitecto.");
            sender.sendMessage(ChatColor.YELLOW + "/constructor mover " + ChatColor.WHITE + "- Mueve el NPC a tu posición.");
            sender.sendMessage(ChatColor.YELLOW + "/constructor recargar " + ChatColor.WHITE + "- Recarga la configuración.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo lo pueden usar jugadores dentro del juego.");
            return true;
        }

        if (!player.hasPermission("constructor.admin")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para ejecutar comandos administrativos de Constructor.");
            return true;
        }

        // Validación de mundos permitidos desde config.yml
        List<String> allowedWorlds = getConfig().getStringList("settings.allowed-worlds");
        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                getConfig().getString("messages.prefix", "&8[&6Feticraft&8] &7") + 
                "&cNo puedes usar este comando en este mundo. Solo está permitido en el mundo de supervivencia."
            ));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "crear", "mover":
                npcManager.spawnArchitect(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "¡El NPC Arquitecto Feticraft ha sido colocado en tu posición!");
                break;

            case "eliminar":
                npcManager.removeArchitect();
                player.sendMessage(ChatColor.RED + "El NPC Arquitecto ha sido eliminado del mapa.");
                break;

            case "recargar":
                reloadConfig();
                player.sendMessage(ChatColor.GREEN + "Configuración de ConstructorFeticraft recargada.");
                break;

            default:
                player.sendMessage(ChatColor.RED + "Comando desconocido. Usa /constructor ayuda.");
                break;
        }

        return true;
    }
}
