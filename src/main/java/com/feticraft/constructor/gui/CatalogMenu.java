package com.feticraft.constructor.gui;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CatalogMenu {

    public static final String MENU_TITLE = ChatColor.DARK_GRAY + "Catálogo de Construcciones";

    public static void openMenu(ConstructorFeticraftPlugin plugin, Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, MENU_TITLE);

        File catalogFile = new File(plugin.getDataFolder(), "catalogo.yml");
        if (!catalogFile.exists()) {
            player.sendMessage(ChatColor.RED + "Error: El catálogo de construcciones no está disponible.");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(catalogFile);
        ConfigurationSection disenosSection = config.getConfigurationSection("disenos");

        if (disenosSection != null) {
            int slot = 10;
            for (String key : disenosSection.getKeys(false)) {
                if (slot > 16) break; // Límite de ítems por fila para vista limpia

                String nombre = disenosSection.getString(key + ".nombre", "Construcción");
                String descripcion = disenosSection.getString(key + ".descripcion", "");
                double precio = disenosSection.getDouble(key + ".precio", 0.0);
                int ancho = disenosSection.getInt(key + ".dimensiones.ancho", 0);
                int largo = disenosSection.getInt(key + ".dimensiones.largo", 0);
                int alto = disenosSection.getInt(key + ".dimensiones.alto", 0);

                ItemStack item = new ItemStack(Material.HOUSE);
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + nombre);
                    
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + descripcion);
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "Dimensiones: " + ChatColor.WHITE + ancho + "x" + largo + "x" + alto);
                    lore.add(ChatColor.GREEN + "Precio: " + ChatColor.WHITE + "$" + precio);
                    lore.add("");
                    lore.add(ChatColor.AQUA + "▶ Haz clic para seleccionar esta construcción");

                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }

                gui.setItem(slot, item);
                slot += 2; // Espaciado entre ítems
            }
        }

        player.openInventory(gui);
    }
}
