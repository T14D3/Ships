package de.t14d3.ships;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ShipPickupListeners implements Listener {
    private final Ships plugin;

    public ShipPickupListeners(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof ArmorStand armorStand) {
            if (armorStand.getPersistentDataContainer().has(ShipManager.shipDataKey, PersistentDataType.STRING)) {
                event.getDrops().clear();
                ItemStack item = new ItemStack(Material.ARMOR_STAND);
                ItemMeta meta = item.getItemMeta();
                //noinspection ConstantConditions
                meta.getPersistentDataContainer().set(ShipManager.shipDataKey, PersistentDataType.STRING,
                        armorStand.getPersistentDataContainer().get(ShipManager.shipDataKey, PersistentDataType.STRING));
                item.setItemMeta(meta);
                event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), item);
                Ship ship = plugin.getShipManager().getShip(armorStand.getUniqueId());
                if (ship == null) {
                    plugin.getLogger().warning("Tried removing ship that doesn't exist: " + armorStand.getUniqueId());
                    return;
                }
                ship.getShipBlocks().forEach(shipBlock -> {
                    if (shipBlock.getFloor() != null) {
                        shipBlock.getFloor().getPassengers().forEach(Entity::remove);
                        shipBlock.getFloor().remove();
                        shipBlock.setFloor(null);
                    }
                });
                plugin.getPacketUtils().removeEntities(ship.getEntityIds());
                plugin.getShipManager().removeShip(armorStand.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlace(EntityPlaceEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() == Material.ARMOR_STAND) {
            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(ShipManager.shipDataKey, PersistentDataType.STRING)) {
                ArmorStand armorStand = (ArmorStand) event.getEntity();
                //noinspection ConstantConditions
                armorStand.getPersistentDataContainer().set(
                        ShipManager.shipDataKey,
                        PersistentDataType.STRING,
                        meta.getPersistentDataContainer().get(ShipManager.shipDataKey, PersistentDataType.STRING)
                );

                Ship ship = plugin.getPacketUtils().recreateShip(armorStand);
                if (ship != null) {
                    event.getPlayer().sendMessage("Ship created");
                }
            }

        }
    }
}
