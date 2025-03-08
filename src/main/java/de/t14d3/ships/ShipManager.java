package de.t14d3.ships;

import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShipManager {
    private final Map<UUID, Ship> ships = new HashMap<>();
    private BukkitTask tickTask;

    public ShipManager(Ships plugin) {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Ship ship : ships.values()) {
                    if (ship.getVector().isZero()) {
                        continue;
                    }
                    ship.move(ship.getVector());
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void addShip(Ship ship) {
        ships.put(ship.getOrigin().getUniqueId(), ship);
    }

    public Ship getShip(UUID markerUuid) {
        return ships.get(markerUuid);
    }

    public Ship getNearestShip(Location location) {
        Ship nearestShip = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Ship ship : ships.values()) {
            double distance = ship.getOrigin().getLocation().distance(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestShip = ship;
            }
        }
        return nearestShip;
    }

    public void removeShip(UUID markerUuid) {
        Ship ship = ships.remove(markerUuid);
        if (ship != null) {
            ship.getOrigin().remove();
            ship.getBlockDisplays().forEach(Entity::remove);
            ship.getShulkers().forEach(Entity::remove);
            ship.getShulkerArmorStands().forEach(Entity::remove);
        }
    }

    public Ship getControlledBy(Player player) {
        for (Ship ship : ships.values()) {
            if (ship.getController() == player) {
                return ship;
            }
        }
        return null;
    }
}
