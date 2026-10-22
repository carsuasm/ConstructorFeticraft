package com.feticraft.constructor;

import com.feticraft.constructor.builder.BuildingExecutor;
import com.feticraft.constructor.gui.CatalogMenu;
import com.feticraft.constructor.gui.MenuListener;
import com.feticraft.constructor.selection.SelectionListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().warning("¡Vault no encontrado o sin plugin de economía! El sistema de pago por construcciones estará desactivado.");
        } else {
            getLogger().info("¡Economía de Vault vinculada correctamente!");
        }

        if (getCommand("constructor") != null) {
            getCommand("constructor").setExecutor(this);
        }
        
        // Registramos todos los listeners correctamente
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new CatalogMenu(this), this);
        getServer().getPluginManager().registerEvents(new SelectionListener(this), this);

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

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("constructorfeticraft.admin")) {
                player.sendMessage(ChatColor.RED + "No tienes permiso para recargar la configuración.");
                return true;
            }
            reloadConfig();
            player.sendMessage(ChatColor.GREEN + "[ConstructorFeticraft] Configuración recargada correctamente.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            BuildingExecutor.togglePause(player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("cancelar")) {
            boolean cancelled = BuildingExecutor.cancelBuilding(player);
            if (!cancelled) {
                player.sendMessage(ChatColor.RED + "No tienes ninguna construcción en curso para cancelar.");
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("crear")) {
            if (!player.hasPermission("constructorfeticraft.admin")) {
                player.sendMessage(ChatColor.RED + "No tienes permisos para crear al Arquitecto.");
                return true;
            }

            Location loc = player.getLocation();
            if (!loc.getChunk().isLoaded()) {
                loc.getChunk().load();
            }

            Villager villager = loc.getWorld().spawn(loc, Villager.class, v -> {
                v.setProfession(Villager.Profession.LIBRARIAN);
                v.setVillagerType(Villager.Type.SNOW);
                v.setCustomName(ChatColor.GOLD + "Arquitecto Feticraft");
                v.setCustomNameVisible(true);
                v.setAI(false);
                v.setInvulnerable(true);
                v.setCollidable(false);
                v.setSilent(true);
            });

            if (villager != null && villager.isValid()) {
                player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "¡Has creado al Arquitecto Feticraft con éxito en esta ubicación!");
            } else {
                player.sendMessage(ChatColor.RED + "[Arquitecto] Error: El servidor o una protección ha bloqueado la creación del NPC.");
            }
            return true;
        }

        CatalogMenu.openCatalog(player);
        return true;
    }
}
