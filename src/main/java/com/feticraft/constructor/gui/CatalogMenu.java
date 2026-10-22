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

    private static ConstructorFeticraftPlugin plugin;
    public static final String MENU_TITLE = ChatColor.DARK_GREEN + "Catálogo de Construcciones";

    public CatalogMenu(ConstructorFeticraftPlugin plugin) {
        CatalogMenu.plugin = plugin;
    }

    // Sobrecarga para admitir tanto openCatalog(player) como openCatalog(plugin, player)
    public static void openCatalog(Player player) {
        openCatalog(plugin, player);
    }

    public static void openCatalog(ConstructorFeticraftPlugin pl, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MENU_TITLE);

        File catalogFile = new File(pl.getDataFolder(), "catalogo.yml");
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

                        NamespacedKey key = new NamespacedKey(pl, "catalog_id");
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
        if (event.getView().getTitle() == null || !event.getView().getTitle().equals(MENU_TITLE)) {
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

        File catalogFile = new File(plugin.getDataFolder(), "catalogo.yml");
        double precio = 0.0;
        String nombreDiseno = idDiseno;
        if (catalogFile.exists()) {
            YamlConfiguration catalog = YamlConfiguration.loadConfiguration(catalogFile);
            precio = catalog.getDouble("disenos." + idDiseno + ".precio", 0.0);
            nombreDiseno = catalog.getString("disenos." + idDiseno + ".nombre", idDiseno);
        }

        Economy econ = plugin.getEconomy();
        if (econ != null) {
            if (econ.has(player, precio)) {
                econ.withdrawPlayer(player, precio); // Cobro mediante Vault
                player.sendMessage(ChatColor.GREEN + "¡Has comprado el diseño '" + nombreDiseno + "' por " + precio + " monedas!");
            } else {
                player.sendMessage(ChatColor.RED + "[Arquitecto] No tienes suficiente dinero para comprar este diseño.");
                player.closeInventory();
                return;
            }
        }

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

            NamespacedKey designKey = new NamespacedKey(plugin, "selected_building_id");
            featherMeta.getPersistentDataContainer().set(designKey, PersistentDataType.STRING, idDiseno);
            feather.setItemMeta(featherMeta);
        }

        player.closeInventory();
        player.getInventory().addItem(feather);
        player.sendMessage(ChatColor.YELLOW + "[Arquitecto] Se ha añadido la pluma a tu inventario. ¡Ve a marcar la zona!");
    }
}
