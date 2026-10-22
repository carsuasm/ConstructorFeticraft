package com.feticraft.constructor.selection;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import com.feticraft.constructor.ConstructorFeticraftPlugin;

import java.util.ArrayList;
import java.util.List;

public class ConstructionSelector {

    public static ItemStack getSelectionTool(ConstructorFeticraftPlugin plugin, String idDiseno, String nombreDiseno) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Herramienta del Arquitecto");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Diseño seleccionado: " + ChatColor.YELLOW + nombreDiseno);
            lore.add("");
            lore.add(ChatColor.AQUA + "▶ Haz Clic Derecho en el suelo para marcar la esquina inicial.");
            lore.add(ChatColor.AQUA + "▶ Haz Shift + Clic Derecho para cancelar.");
            meta.setLore(lore);

            // Guardar ID del diseño en PDC
            NamespacedKey key = new NamespacedKey(plugin, "selected_building_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, idDiseno);

            item.setItemMeta(meta);
        }

        return item;
    }
}
