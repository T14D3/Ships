package de.t14d3.ships;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class InteractListener implements Listener {
    private final Ships plugin;

    public InteractListener(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.SHULKER) {
            Ship ship = plugin.getShipManager().getNearestShip(event.getPlayer().getLocation());
            if (ship != null) {
                ship.setController(event.getPlayer());
                event.getRightClicked().addPassenger(event.getPlayer());
            }
        }
    }
}
