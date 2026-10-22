package com.feticraft.constructor;

import com.feticraft.constructor.builder.BuildingExecutor;
import com.feticraft.constructor.gui.CatalogMenu;
import com.feticraft.constructor.gui.MenuListener;
import com.feticraft.constructor.selection.SelectionListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ConstructorFeticraftPlugin extends JavaPlugin implements CommandExecutor {

    private static ConstructorFeticraftPlugin instance;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        // Guardar archivos por defecto
        saveDefaultConfig();
        saveResourceIfNotExists("catalogo.yml");
        saveResourceIfNotExists("npc.yml");

        File disenosFolder = new File(getDataFolder(), "disenos/casas");
        if (!disenosFolder.exists()) {
            disenosFolder.mkdirs();
        }

        // Configurar Economía con Vault
        if (!setupEconomy()) {
            getLogger().warning("Vault no encontrado o sin plugin de economía compatible. Los cobros no funcionarán.");
        } else {
            getLogger().info("Vault conectado con éxito a ConstructorFeticraft.");
        }

        // Registrar Eventos de Listeners
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new SelectionListener(this), this);

        // Registrar Comando
        if (getCommand("constructor") != null) {
            getCommand("constructor").setExecutor(this);
        }

        getLogger().info("ConstructorFeticraft habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ConstructorFeticraft deshabilitado.");
    }

    public static ConstructorFeticraftPlugin getInstance() {
        return instance;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
        }
    }

    // MANEJO DE COMANDOS (/constructor)
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo lo puede usar un jugador.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("constructorfeticraft.admin")) {
                player.sendMessage(ChatColor.RED + "No tienes permiso para recargar la configuración.");
                return true;
            }
            reloadConfig();
            player.sendMessage(ChatColor.GREEN + "[ConstructorFeticraft] Configuración recargada correctamente.");
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("cancelar") || args[0].equalsIgnoreCase("stop"))) {
            boolean cancelled = BuildingExecutor.cancelBuilding(player);
            if (!cancelled) {
                player.sendMessage(ChatColor.RED + "No tienes ninguna construcción en curso para cancelar.");
            }
            return true;
        }

        // Si pone /constructor sin argumentos, abre el catálogo directamente
        CatalogMenu.openCatalog(player);
        return true;
    }
}
