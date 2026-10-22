package com.feticraft.constructor.gui;

import com.feticraft.constructor.ConstructorFeticraftPlugin;
import com.feticraft.constructor.builder.BuildingExecutor;
import com.feticraft.constructor.selection.ConstructionSelector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.UUID;

public class MenuListener implements Listener {

    private final ConstructorFeticraftPlugin plugin;
    public static final String CONTROL_MENU_TITLE = ChatColor.DARK_GRAY + "Gestión de Obra";

    public MenuListener(ConstructorFeticraftPlugin plugin) {
        this.plugin = plugin;
    }

    // INTERACCIÓN CON ALDEANOS (NPCs)
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        Player player = event.getPlayer();
        UUID ownerUUID = BuildingExecutor.getOwnerOfNpc(villager);

        // Si es un obrero activo haciendo una construcción
        if (ownerUUID != null && (ownerUUID.equals(player.getUniqueId()) || player.hasPermission("constructorfeticraft.admin"))) {
            if (player.isSneaking()) { // Shift + Clic Derecho -> Menú de Pausa/Cancelar
                event.setCancelled(true);
                openControlMenu(player, ownerUUID);
                return;
            }
        }

        // Si es el Arquitecto del Spawn (el aldeano decorativo/estático)
        String customName = villager.getCustomName();
        if (customName != null && customName.contains("Arquitecto")) {
            event.setCancelled(true); // EVITA QUE SE ABRA LA TIENDA DE VANILLA
            CatalogMenu.openCatalog(player); // Abre directamente el catálogo de casas del plugin
        }
    }

    public static void openControlMenu(Player player, UUID targetOwnerUUID) {
        Inventory gui = Bukkit.createInventory(null, 27, CONTROL_MENU_TITLE);

        boolean isPaused = BuildingExecutor.isPaused(targetOwnerUUID);

        ItemStack pauseItem = new ItemStack(isPaused ? Material.LIME_DYE : Material.ORANGE_DYE);
        ItemMeta pauseMeta = pauseItem.getItemMeta();
        if (pauseMeta != null) {
            pauseMeta.setDisplayName(isPaused ? ChatColor.GREEN + "▶ Reanudar Construcción" : ChatColor.GOLD + "⏸ Pausar Construcción");
            pauseMeta.setLore(Arrays.asList(ChatColor.GRAY + (isPaused ? "Haz clic para continuar la obra." : "Haz clic para detener el avance temporalmente.")));
            pauseItem.setItemMeta(pauseMeta);
        }

        ItemStack cancelItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ChatColor.RED + "🛑 Cancelar Construcción");
            cancelMeta.setLore(Arrays.asList(ChatColor.GRAY + "Detiene la obra por completo y", ChatColor.GRAY + "elimina al obrero actual."));
            cancelItem.setItemMeta(cancelMeta);
        }

        gui.setItem(11, pauseItem);
        gui.setItem(15, cancelItem);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // A) Catálogo de construcciones
        if (title.equals(CatalogMenu.MENU_TITLE)) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            Player player = (Player) event.getWhoClicked();
            ItemMeta meta = clickedItem.getItemMeta();
            ConstructorFeticraftPlugin pluginInstance = ConstructorFeticraftPlugin.getInstance();

            NamespacedKey catalogKey = new NamespacedKey(pluginInstance, "catalog_building_key");
            if (!meta.getPersistentDataContainer().has(catalogKey, PersistentDataType.STRING)) return;

            String buildingId = meta.getPersistentDataContainer().get(catalogKey, PersistentDataType.STRING);
            String itemName = meta.getDisplayName();

            player.closeInventory();
            
            ItemStack tool = ConstructionSelector.getSelectionTool(pluginInstance, buildingId, itemName);
            player.getInventory().addItem(tool);

            player.sendMessage(ChatColor.GOLD + "[Arquitecto] " + ChatColor.GREEN + "Has seleccionado: " + itemName);
            player.sendMessage(ChatColor.YELLOW + "Usa la Herramienta en el suelo para previsualizar el área.");
            return;
        }

        // B) Menú de confirmación de obra
        if (title.equals(ConfirmMenu.CONFIRM_TITLE)) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            Player player = (Player) event.getWhoClicked();
            ConstructorFeticraftPlugin pluginInstance = ConstructorFeticraftPlugin.getInstance();

            if (clickedItem.getType() == Material.RED_WOOL) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "[Arquitecto] Ubicación cancelada.");
                return;
            }

            if (clickedItem.getType() == Material.LIME_WOOL) {
                ItemMeta meta = clickedItem.getItemMeta();
                NamespacedKey idKey = new NamespacedKey(pluginInstance, "confirm_building_id");
                NamespacedKey locKey = new NamespacedKey(pluginInstance, "confirm_building_loc");

                if (!meta.getPersistentDataContainer().has(idKey, PersistentDataType.STRING)) return;

                String buildingId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
                String locRaw = meta.getPersistentDataContainer().get(locKey, PersistentDataType.STRING);
                
                String[] parts = locRaw.split(",");
                Location targetLoc = new Location(
                    Bukkit.getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
                );

                player.closeInventory();

                ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (mainHand != null) {
                    player.getInventory().removeItem(mainHand);
                }

                BuildingExecutor.buildStructure(pluginInstance, player, targetLoc, buildingId);
                return;
            }
        }

        // C) Menú de Gestión de Obra (Pausar / Cancelar)
        if (title.equals(CONTROL_MENU_TITLE)) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;

            ItemStack current = event.getCurrentItem();
            if (current == null || current.getType() == Material.AIR) return;

            if (current.getType() == Material.LIME_DYE || current.getType() == Material.ORANGE_DYE) {
                BuildingExecutor.togglePause(player);
                player.closeInventory();
            } else if (current.getType() == Material.REDSTONE_BLOCK) {
                BuildingExecutor.cancelBuilding(player);
                player.closeInventory();
            }
        }
    }
}
