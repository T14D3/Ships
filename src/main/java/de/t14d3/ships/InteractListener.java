package de.t14d3.ships;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.UUID;

public class InteractListener implements Listener {
    private final Ships plugin;

    public InteractListener(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.SHULKER) {
            UUID shipUuid = UUID.fromString(event.getRightClicked().getPersistentDataContainer().get(new NamespacedKey(plugin, "ship"), PersistentDataType.STRING));
            Ship ship = plugin.getShipManager().getShip(shipUuid);
            if (ship != null) {
                if (ship.getController() == null) {
                    ship.setController(event.getPlayer());
                    event.getRightClicked().addPassenger(event.getPlayer());
                } else {
                    event.getPlayer().sendMessage("Ship is already controlled by " + ship.getController().getName());
                }
            }
        }
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            Ship ship = plugin.getShipManager().getControlledBy(player);
            if (ship != null) {
                ship.setVector(new Vector(0, 0, 0));
                ship.setVectorToRotate(new Vector(0, 0, 0));
                ship.setController(null);
            }
        }
    }
}
