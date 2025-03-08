package de.t14d3.ships;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class DeathListener implements Listener {
    private final Ships plugin;

    public DeathListener(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Ship ship = plugin.getShipManager().getShip(event.getEntity().getUniqueId());
        if (ship != null) {
            ship.getOrigin().remove();
            ship.getShipBlocks().forEach(shipBlock -> {
                shipBlock.getBlockDisplay().remove();
                if (shipBlock.getBlock() != null) {
                    shipBlock.getBlock().setBlockData(ShipBlock.AIR, false);
                }
            });
            plugin.getShipManager().removeShip(event.getEntity().getUniqueId());
        }
    }
}
