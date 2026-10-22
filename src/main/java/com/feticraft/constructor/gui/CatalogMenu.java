package com.feticraft.constructor.gui;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CatalogMenu {

    public static final String MENU_TITLE = ChatColor.DARK_GREEN + "Catálogo de Construcciones";

    // Método que abre el inventario del catálogo al jugador
    public static void openCatalog(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MENU_TITLE);

        ConstructorFeticraftPlugin plugin = ConstructorFeticraftPlugin.getInstance();
        File catalogFile = new File(plugin.getDataFolder(), "catalogo.yml");
        
        if (!catalogFile.exists()) {
            player.sendMessage(ChatColor.RED + "El archivo catalogo.yml no existe.");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(catalogFile);
        ConfigurationSection disenosSection = config.getConfigurationSection("disenos");

        if (disenosSection != null) {
            int slot = 10; // Posición inicial en el inventario
            for (String key : disenosSection.getKeys(false)) {
                String name = disenosSection.getString(key + ".nombre", "Estructura");
                double price = disenosSection.getDouble(key + ".precio", 0.0);
                String desc = disenosSection.getString(key + ".descripcion", "Sin descripción.");

                // Asignar icono según categoría o diseño
                Material mat = Material.OAK_DOOR;
                if (key.contains("honeypot")) mat = Material.HONEYCOMB_BLOCK;
                else if (key.contains("pumpkin")) mat = Material.PUMPKIN;
                else if (key.contains("survival")) mat = Material.HAY_BLOCK;
                else if (key.contains("granja")) mat = Material.WHEAT;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                    
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + desc);
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "Precio: " + ChatColor.GREEN + "$" + price);
                    lore.add("");
                    lore.add(ChatColor.AQUA + "▶ Haz clic para seleccionar");
                    
                    meta.setLore(lore);

                    // Guardar ID del diseño en el PersistentDataContainer
                    NamespacedKey catalogKey = new NamespacedKey(plugin, "catalog_building_key");
                    meta.getPersistentDataContainer().set(catalogKey, PersistentDataType.STRING, key);

                    item.setItemMeta(meta);
                }

                if (slot < 17) {
                    inv.setItem(slot, item);
                    slot++;
                }
            }
        }

        player.openInventory(inv);
    }
}
