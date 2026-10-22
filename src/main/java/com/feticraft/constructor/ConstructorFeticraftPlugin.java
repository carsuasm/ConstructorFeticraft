package com.feticraft.constructor;

import com.feticraft.constructor.builder.BuildingExecutor;
import com.feticraft.constructor.gui.CatalogMenu;
import com.feticraft.constructor.gui.MenuListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConstructorFeticraftPlugin extends JavaPlugin implements CommandExecutor {

    private static ConstructorFeticraftPlugin instance;
    private static Economy economy = null;

    @Override
    public void onEnable() {
        instance = this;

        // Guardar configuración por defecto si no existe
        saveDefaultConfig();

        // Verificar e inicializar Vault para la economía
        if (!setupEconomy()) {
            getLogger().warning("¡Vault no encontrado o sin plugin de economía! El sistema de pago por construcciones estará desactivado.");
        } else {
            getLogger().info("¡Economía de Vault vinculada correctamente!");
        }

        // Registrar comandos y eventos
        if (getCommand("constructor") != null) {
            getCommand("constructor").setExecutor(this);
        }
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        getLogger().info("ConstructorFeticraft ha sido habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        instance = null;
        getLogger().info("ConstructorFeticraft ha sido deshabilitado.");
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo lo puede usar un jugador.");
            return true;
        }

        // Comando para recargar configuración (/constructor reload)
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("constructorfeticraft.admin")) {
                player.sendMessage(ChatColor.RED + "No tienes permiso para recargar la configuración.");
                return true;
            }
            reloadConfig();
            player.sendMessage(ChatColor.GREEN + "[ConstructorFeticraft] Configuración recargada correctamente.");
            return true;
        }

        // Comando para pausar / reanudar (/constructor stop)
        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            BuildingExecutor.togglePause(player);
            return true;
        }

        // Comando para cancelar la obra por completo y retirar al obrero (/constructor cancelar)
        if (args.length > 0 && args[0].equalsIgnoreCase("cancelar")) {
            boolean cancelled = BuildingExecutor.cancelBuilding(player);
            if (!cancelled) {
                player.sendMessage(ChatColor.RED + "No tienes ninguna construcción en curso para cancelar.");
            }
            return true;
        }

        // Comando para crear el NPC Arquitecto del spawn (/constructor crear)
        if (args.length > 0 && args[0].equalsIgnoreCase("crear")) {
            if (!player.hasPermission("constructorfeticraft.admin")) {
                player.sendMessage(ChatColor.RED + "No tienes permisos para crear al Arquitecto.");
                return true;
            }

            player.getWorld().spawn(player.getLocation(), Villager.class, villager -> {
                villager.setProfession(Villager.Profession.LIBRARIAN);
                villager.setVillagerType(Villager.Type.SNOW);
                villager.setCustomName(ChatColor.GOLD + "Arquitecto Feticraft");
                villager.setCustomNameVisible(true);
                
                villager.setAI(false);
                villager.setInvulnerable(true);
                villager.setCollidable(false);
                villager.setSilent(true);
            });

            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡Has creado al Arquitecto Feticraft en esta ubicación!");
            return true;
        }

        // Si pones /constructor a secas, abre el catálogo visual
        CatalogMenu.openCatalog(player);
        return true;
    }
}
