package de.t14d3.ships;

import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.*;

public class ShipManager {
    private final List<Ship> ships = new ArrayList<>();
    private BukkitTask tickTask;

    public ShipManager(Ships plugin) {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Ship ship : ships) {
                    if (!ship.getVector().isZero()) {
                        ship.move(ship.getVector());
                    }
                    if (!ship.getOrientation().equals(new Quaternionf())) {
                        ship.rotate(ship.getVectorToRotate());
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void addShip(Ship ship) {
        ships.add(ship);
    }

    public Ship getShip(UUID markerUuid) {
        return ships.stream().filter(ship -> ship.getOrigin().getUniqueId() == markerUuid).findFirst().orElse(null);
    }



    public Ship getControlledBy(Player player) {
        for (Ship ship : ships) {
            if (ship.getController() == player) {
                return ship;
            }
        }
        return null;
    }
}
