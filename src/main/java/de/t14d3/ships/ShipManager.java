package de.t14d3.ships;

import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShipManager {
    private final Map<UUID, Ship> ships = new HashMap<>();

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
