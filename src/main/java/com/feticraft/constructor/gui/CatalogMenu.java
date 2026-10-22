package com.feticraft.constructor.gui;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CatalogMenu implements Listener {

    private final ConstructorFeticraftPlugin plugin;

    public CatalogMenu(ConstructorFeticraftPlugin plugin) {
        this.plugin = plugin;
    }

    public static void openCatalog(ConstructorFeticraftPlugin plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Catálogo de Construcciones");

        File catalogFile = new File(plugin.getDataFolder(), "catalogo.yml");
        if (catalogFile.exists()) {
            YamlConfiguration catalog = YamlConfiguration.loadConfiguration(catalogFile);
            if (catalog.contains("disenos")) {
                for (String id : catalog.getConfigurationSection("disenos").getKeys(false)) {
                    String name = catalog.getString("disenos." + id + ".nombre", id);
                    double price = catalog.getDouble("disenos." + id + ".precio", 0.0);

                    ItemStack item = new ItemStack(Material.OAK_SIGN);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.GREEN + name);
                        List<String> lore = new ArrayList<>();
                        lore.add(ChatColor.YELLOW + "Precio: " + price + " monedas");
                        lore.add(ChatColor.AQUA + "▶ Haz clic para comprar y obtener la pluma.");
                        meta.setLore(lore);

                        // Guardar ID del diseño en el ítem del menú
                        NamespacedKey key = new NamespacedKey(plugin, "catalog_id");
                        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
                        item.setItemMeta(meta);
                    }
                    inv.addItem(item);
                }
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null || !event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Catálogo de Construcciones")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || !clickedItem.hasItemMeta()) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "catalog_id");
        ItemMeta meta = clickedItem.getItemMeta();
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return;
        }

        String idDiseno = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        Player player = (Player) event.getWhoClicked();

        // Obtener precio y nombre del archivo catalogo.yml
        File catalogFile = new File(plugin.getDataFolder(), "catalogo.yml");
        double precio = 0.0;
        String nombreDiseno = idDiseno;
        if (catalogFile.exists()) {
            YamlConfiguration catalog = YamlConfiguration.loadConfiguration(catalogFile);
            precio = catalog.getDouble("disenos." + idDiseno + ".precio", 0.0);
            nombreDiseno = catalog.getString("disenos." + idDiseno + ".nombre", idDiseno);
        }

        // Comprobar y cobrar el dinero usando Vault
        Economy econ = plugin.getEconomy(); // Asegúrate de que tu clase principal tenga el método getEconomy()
        if (econ != null) {
            if (econ.has(player, precio)) {
                econ.withdrawPlayer(player, precio); // ¡Aquí es donde se efectúa el cobro!
                player.sendMessage(ChatColor.GREEN + "¡Has comprado el diseño '" + nombreDiseno + "' por " + precio + " monedas!");
            } else {
                player.sendMessage(ChatColor.RED + "[Arquitecto] No tienes suficiente dinero para comprar este diseño.");
                player.closeInventory();
                return;
            }
        }

        // Crear la pluma del arquitecto con el diseño seleccionado
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta featherMeta = feather.getItemMeta();
        if (featherMeta != null) {
            featherMeta.setDisplayName(ChatColor.GOLD + "Herramienta del Arquitecto");
            List<String> featherLore = new ArrayList<>();
            featherLore.add(ChatColor.GRAY + "Minecraft");
            featherLore.add(ChatColor.GREEN + "Diseño seleccionado: " + ChatColor.WHITE + nombreDiseno);
            featherLore.add(ChatColor.AQUA + "▶ Haz Clic Derecho en el suelo para marcar la esquina inicial.");
            featherLore.add(ChatColor.AQUA + "▶ Haz Shift + Clic Derecho para cancelar.");
            featherMeta.setLore(featherLore);

            // Guardar la clave del diseño para que el SelectionListener la reconozca al hacer clic
            NamespacedKey designKey = new NamespacedKey(plugin, "selected_building_id");
            featherMeta.getPersistentDataContainer().set(designKey, PersistentDataType.STRING, idDiseno);
            feather.setItemMeta(featherMeta);
        }

        player.closeInventory();
        player.getInventory().addItem(feather);
        player.sendMessage(ChatColor.YELLOW + "[Arquitecto] Se ha añadido la pluma a tu inventario. ¡Ve a marcar la zona!");
    }
}
