package de.t14d3.ships;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

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
                Ship ship = plugin.getShipManager().getShip(armorStand);
                if (ship == null) {
                    plugin.getLogger().warning("Tried removing ship that doesn't exist: " + armorStand.getUniqueId());
                    return;
                }
                createItemDrop(ship, event.getEntity().getLocation());
                plugin.getShipManager().removeShip(ship);
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
                UUID uuid = UUID.fromString(meta.getPersistentDataContainer().get(ShipManager.shipDataKey, PersistentDataType.STRING));
                event.getPlayer().sendMessage(uuid.toString());
                Ship ship = plugin.getShipManager().loadShip(armorStand, uuid);
                if (ship != null) {
                    event.getPlayer().sendMessage("Ship created");
                }
            }
        }
    }

    @EventHandler
    public void onEntityHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Shulker shulker) {
            Ship ship = plugin.getShipManager().getShipFromShulker(shulker).join();
            if (ship != null) {
                event.setCancelled(true);
                createItemDrop(ship, event.getEntity().getLocation());
                plugin.getShipManager().removeShip(ship);
            }
        }
    }

    private static void createItemDrop(Ship ship, Location dropLocation) {
        ItemStack item = new ItemStack(Material.ARMOR_STAND);
        ItemMeta meta = item.getItemMeta();
        //noinspection ConstantConditions
        meta.getPersistentDataContainer().set(ShipManager.shipDataKey, PersistentDataType.STRING,
                ship.getUuid().toString());
        item.setItemMeta(meta);
        dropLocation.getWorld().dropItem(dropLocation, item);
    }
}
