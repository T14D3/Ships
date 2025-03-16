package de.t14d3.ships;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Ship {
    private ArmorStand origin;
    private List<ShipBlock> shipBlocks = new ArrayList<>();

    private Vector velocity;
    private Vector vectorToRotate;
    private Quaternionf orientation;

    private Player controller;

    private List<Player> playersInShip = new ArrayList<>();

    public Ship(ArmorStand origin, List<ShipBlock> shipBlocks) {
        this.origin = origin;
        this.shipBlocks = shipBlocks;

        this.velocity = new Vector(0, 0, 0);
        this.vectorToRotate = new Vector(0, 0, 0);
        this.orientation = new Quaternionf();
    }

    public void rotate(Vector rotationVector) {
        // Create the rotation quaternion based on the input vector
        Quaternionf rotation = new Quaternionf();
        rotation.rotateY((float) Math.toRadians(rotationVector.getY()));
        rotation.rotateX((float) Math.toRadians(rotationVector.getX()));
        rotation.rotateZ((float) Math.toRadians(rotationVector.getZ()));

        // Update the ship's orientation
        this.orientation.mul(rotation);

        // Calculate new transformations for each block
        for (ShipBlock shipBlock : shipBlocks) {
            Vector3f originalOffset = shipBlock.getOffset();

            // Rotate the original offset by the current orientation to get the new translation
            Vector3f newTranslation = new Vector3f(originalOffset).rotate(this.orientation);

            Ships.getInstance().getPacketUtils().sendEntityMetadataUpdate(shipBlock.getEntityId(), newTranslation, this.orientation, shipBlock.getLocation(this));
        }
    }

    public ArmorStand getOrigin() {
        return origin;
    }

    public ShipBlock getClosestBlock(Location location) {
        double minDistance = Double.MAX_VALUE;
        ShipBlock closestBlock = null;
        for (ShipBlock shipBlock : shipBlocks) {
            double distance = location.distanceSquared(origin.getLocation().add(Vector.fromJOML(shipBlock.getOffset())));
            if (distance < minDistance) {
                minDistance = distance;
                closestBlock = shipBlock;
            }
        }
        return closestBlock;
    }

    public void moveTo(Location newLocation) {
        origin.teleportAsync(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        for (ShipBlock shipBlock : shipBlocks) {
            if (shipBlock.getSeat() != null) {
                shipBlock.getSeat().teleportAsync(
                        newLocation.add(shipBlock.getSeat().getLocation().subtract(origin.getLocation())),
                        PlayerTeleportEvent.TeleportCause.PLUGIN,
                        TeleportFlag.EntityState.RETAIN_PASSENGERS);
            }
        }
    }

    public void move(Vector direction) {
        moveTo(origin.getLocation().add(direction));
    }

    public void setVector(Vector vector) {
        this.velocity = vector;
    }

    public Vector getVector() {
        return velocity;
    }

    public Quaternionf getOrientation() {
        return orientation;
    }

    public Vector getVectorToRotate() {
        return vectorToRotate;
    }

    public void setVectorToRotate(Vector vectorToRotate) {
        this.vectorToRotate = vectorToRotate;
    }

    public void setController(Player player) {
        this.controller = player;
    }

    public Player getController() {
        return controller;
    }

    public void addPlayer(Player player) {
        this.playersInShip.add(player);
    }

    public void removePlayer(Player player) {
        this.playersInShip.remove(player);
    }

    public List<Player> getPlayersInShip() {
        return playersInShip;
    }

    public boolean isPlayerInShip(Player player) {
        return playersInShip.contains(player);
    }

    public List<ShipBlock> getShipBlocks() {
        return shipBlocks;
    }

    public ShipBlock getFloorBlock(Player player) {
        for (ShipBlock shipBlock : shipBlocks) {
            if (shipBlock.getFloor() != null && shipBlock.getFloor().getLocation().distanceSquared(player.getLocation()) < 1) {
                return shipBlock;
            }
        }
        return null;
    }

    public int[] getEntityIds() {
        int[] entityIds = new int[shipBlocks.size()];
        for (int i = 0; i < shipBlocks.size(); i++) {
            entityIds[i] = shipBlocks.get(i).getEntityId();
        }
        return entityIds;
    }
}
