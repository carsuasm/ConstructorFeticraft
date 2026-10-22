if (clickedItem.getType() == Material.LIME_WOOL) {
                ItemMeta meta = clickedItem.getItemMeta();
                NamespacedKey idKey = new NamespacedKey(plugin, "confirm_building_id");
                NamespacedKey locKey = new NamespacedKey(plugin, "confirm_building_loc");

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

                // Retirar la herramienta del inventario solo tras confirmar
                player.getInventory().removeItem(player.getInventory().getItemInMainHand());

                // Iniciar la construcción con el obrero
                BuildingExecutor.buildStructure(plugin, player, targetLoc, buildingId);
            }
