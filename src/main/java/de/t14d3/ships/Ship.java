package de.t14d3.ships;

import com.google.gson.JsonObject;
import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class Ship {
    private ArmorStand origin;
    private List<ShipBlock> shipBlocks = new ArrayList<>();

    private Vector velocity;
    private Vector vectorToRotate;
    private Quaternionf orientation;

    private Player controller;

    private JsonObject data;

    private UUID uuid;

    public Ship(ArmorStand origin, List<ShipBlock> shipBlocks, UUID uuid) {
        this.origin = origin;
        this.shipBlocks = shipBlocks;

        this.velocity = new Vector(0, 0, 0);
        this.vectorToRotate = new Vector(0, 0, 0);
        this.orientation = new Quaternionf();
        this.uuid = uuid;
    }

    public Ship(ArmorStand origin, List<ShipBlock> shipBlocks) {
        this(origin, shipBlocks, UUID.randomUUID());
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
        Map<ShipBlock, Vector3f> newTranslations = new HashMap<>();
        for (ShipBlock shipBlock : shipBlocks) {
            // Rotate the original offset by the current orientation to get the new translation
            Vector3f newTranslation = new Vector3f(shipBlock.getOffset()).rotate(this.orientation);
            if (getOrigin().getLocation().add(Vector.fromJOML(newTranslation).add(new Vector(1, 2, 1))).getBlock().isCollidable()) {
                newTranslations.clear();
                return;
            }
            newTranslations.put(shipBlock, newTranslation);
        }
        for (ShipBlock shipBlock : shipBlocks) {
            Ships.getInstance().getPacketUtils().sendEntityMetadataUpdate(shipBlock.getEntityId(), newTranslations.get(shipBlock), this.orientation, shipBlock.getLocation(this));
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
        for (ShipBlock shipBlock : shipBlocks) {
            Location offsetLocation = newLocation.clone().add(Vector.fromJOML(shipBlock.getOffset()).add(new Vector(1, 2, 1)));
            if (offsetLocation.getBlock().isCollidable()) {
                return;
            }
            if (shipBlock.getSeat() != null) {
                shipBlock.getSeat().teleportAsync(
                        offsetLocation,
                        PlayerTeleportEvent.TeleportCause.PLUGIN,
                        TeleportFlag.EntityState.RETAIN_PASSENGERS);
            }
        }
        origin.teleportAsync(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
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

    public void setData(JsonObject data) {
        this.data = data;
    }

    public JsonObject getData() {
        return data;
    }

    public UUID getUuid() {
        return uuid;
    }
}
