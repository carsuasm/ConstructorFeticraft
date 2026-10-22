package com.feticraft.constructor;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConstructorFeticraftPlugin extends JavaPlugin {

    private static ConstructorFeticraftPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        // Guardar archivos de configuración por defecto si no existen
        saveDefaultConfig();
        saveResource("catalogo.yml", false);

        getLogger().info(ChatColor.GREEN + "¡ConstructorFeticraft ha sido habilitado correctamente en Feticraft!");
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "ConstructorFeticraft deshabilitado.");
    }

    public static ConstructorFeticraftPlugin getInstance() {
        return instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("constructor")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("ayuda")) {
                sender.sendMessage(ChatColor.GOLD + "=== Comandos de ConstructorFeticraft ===");
                sender.sendMessage(ChatColor.YELLOW + "/constructor crear " + ChatColor.WHITE + "- Crea el NPC arquitecto.");
                sender.sendMessage(ChatColor.YELLOW + "/constructor eliminar " + ChatColor.WHITE + "- Elimina el NPC seleccionado.");
                sender.sendMessage(ChatColor.YELLOW + "/constructor mover " + ChatColor.WHITE + "- Mueve el NPC a tu posición.");
                sender.sendMessage(ChatColor.YELLOW + "/constructor catalogo " + ChatColor.WHITE + "- Muestra información del catálogo.");
                sender.sendMessage(ChatColor.YELLOW + "/constructor obras " + ChatColor.WHITE + "- Lista las obras activas.");
                sender.sendMessage(ChatColor.YELLOW + "/constructor recargar " + ChatColor.WHITE + "- Recarga la configuración.");
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "[Feticraft] Usa " + ChatColor.WHITE + "/constructor ayuda" + ChatColor.YELLOW + " para ver los comandos disponibles.");
            return true;
        }
        return false;
    }
}
