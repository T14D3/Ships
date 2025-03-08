package de.t14d3.ships;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import io.papermc.paper.entity.TeleportFlag;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Ship {
    private final ArmorStand origin;
    private List<ShipBlock> shipBlocks;

    private Vector velocity;
    private Vector vectorToRotate;
    private Quaternionf orientation;
    private Player controller;

    private List<Player> playersInShip = new ArrayList<>();

    public Ship(ArmorStand origin, List<ShipBlock> shipBlocks) {
        NamespacedKey key = new NamespacedKey(Ships.getInstance(), "ship");
        UUID uuid = origin.getUniqueId();
        origin.getPersistentDataContainer().set(key, PersistentDataType.STRING, "SHIP-ORIGIN");
        this.shipBlocks = shipBlocks;
        for (ShipBlock shipBlock : shipBlocks) {
            BlockDisplay blockDisplay = shipBlock.getBlockDisplay();
            blockDisplay.getPersistentDataContainer().set(key, PersistentDataType.STRING, uuid.toString());
            blockDisplay.setTeleportDuration(1);
            blockDisplay.setInterpolationDelay(1);
            blockDisplay.setInterpolationDuration(1);

            Transformation transformation = new Transformation(
                    new Vector3f(-0.5f, 0, -0.5f),
                    blockDisplay.getTransformation().getLeftRotation(),
                    blockDisplay.getTransformation().getScale(),
                    blockDisplay.getTransformation().getRightRotation()
            );
            blockDisplay.setTransformation(transformation);

        }
        this.origin = origin;
        this.velocity = new Vector(0, 0, 0);
        this.vectorToRotate = new Vector(0, 0, 0);
        this.orientation = new Quaternionf();
    }

    public List<ShipBlock> getShipBlocks() {
        return shipBlocks;
    }

    public ArmorStand getOrigin() {
        return origin;
    }

    public BlockDisplay getBlockDisplay(Shulker shulker) {
        for (ShipBlock shipBlock : shipBlocks) {
            if (shipBlock.getShulker() != null && shipBlock.getShulker().equals(shulker)) {
                return shipBlock.getBlockDisplay();
            }
        }
        return null;
    }

    public void moveTo(Location newLocation) {
        origin.teleportAsync(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        for (ShipBlock shipBlock : shipBlocks) {
            Vector offset = shipBlock.getOffset();
            Location dest = newLocation.clone().add(offset);
            if (shipBlock.getShulker() != null) {
                shipBlock.getShulker().teleportAsync(dest, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
            }
            shipBlock.getBlockDisplay().teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
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

    public void setOrientation(Vector orientation) {
        Quaternionf targetOrientation = new Quaternionf();
        targetOrientation.rotateY((float) Math.toRadians(orientation.getX()));
        targetOrientation.rotateX((float) Math.toRadians(orientation.getY()));
        targetOrientation.rotateZ((float) Math.toRadians(orientation.getZ()));

        while (this.orientation.dot(targetOrientation) < 0) {
            Vector deltaEulerAngles = new Vector(0, 0, 0);
            deltaEulerAngles.setX(Math.signum(targetOrientation.x()) * Math.PI);
            deltaEulerAngles.setY(Math.signum(targetOrientation.y()) * Math.PI);
            deltaEulerAngles.setZ(Math.signum(targetOrientation.z()) * Math.PI);
            this.rotate(deltaEulerAngles);
        }
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

    protected void addShipBlocks(List<ShipBlock> shipBlocks) {
        this.shipBlocks.addAll(shipBlocks);
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

    public void rotate(Vector deltaEulerAngles) {
        // Convert delta Euler angles (degrees) to radians
        float pitchRad = (float) Math.toRadians(deltaEulerAngles.getX());
        float yawRad = (float) Math.toRadians(deltaEulerAngles.getY());
        float rollRad = (float) Math.toRadians(deltaEulerAngles.getZ());

        // Create a delta quaternion from the rotation angles
        Quaternionf deltaQuat = new Quaternionf()
                .rotateY(yawRad)    // Yaw around Y-axis
                .rotateX(pitchRad) // Pitch around X-axis
                .rotateZ(rollRad); // Roll around Z-axis

        // Update the ship's cumulative orientation
        orientation.mul(deltaQuat); // Multiply delta into current orientation

        // Rotate each BlockDisplay
        for (ShipBlock shipBlock : shipBlocks) {
            BlockDisplay blockDisplay = shipBlock.getBlockDisplay();
            Transformation oldTransformation = blockDisplay.getTransformation();
            Vector3f translation = new Vector3f(oldTransformation.getTranslation());

            // Apply delta rotation to the BlockDisplay's translation (relative to origin)
            translation.rotate(deltaQuat);

            // Create new transformation with updated translation and cumulative rotation
            Transformation newTransformation = new Transformation(
                    translation,
                    new Quaternionf(orientation), // Use accumulated orientation
                    oldTransformation.getScale(),
                    new Quaternionf()
            );
            blockDisplay.setTransformation(newTransformation);
        }

        // Update Shulker offsets relative to the origin
        Location originLocation = origin.getLocation();
        for (ShipBlock shipBlock : shipBlocks) {
            Vector originalOffset = shipBlock.getOffset();
            Vector3f offset3f = new Vector3f(
                    (float) originalOffset.getX(),
                    (float) originalOffset.getY(),
                    (float) originalOffset.getZ()
            );

            // Apply delta rotation to the Shulker's offset
            offset3f.rotate(deltaQuat);
            Vector rotatedOffset = new Vector(offset3f.x, offset3f.y, offset3f.z);
            shipBlock.setOffset(rotatedOffset);

            // Teleport Shulker to new position
            Shulker shulker = shipBlock.getShulker();
            Location newLocation = originLocation.clone()
                    .add(rotatedOffset);
            if (shulker != null) {
                shulker.teleportAsync(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
            }
            shipBlock.getBlockDisplay().teleportAsync(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        }
    }
}
