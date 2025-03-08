package de.t14d3.ships;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import io.papermc.paper.entity.TeleportFlag;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class Ship {
    private final ArmorStand origin;
    private final List<BlockDisplay> blockDisplays;
    private final List<Shulker> shulkers;
    private final List<Vector> shulkerOffsets;
    private final List<ArmorStand> shulkerArmorStands;

    private Vector velocity;
    private Vector vectorToRotate;
    private Quaternionf orientation;

    private Player controller;

    public Ship(ArmorStand origin, List<BlockDisplay> blockDisplays, List<Shulker> shulkers, List<Vector> shulkerOffsets, List<ArmorStand> shulkerArmorStands) {
        NamespacedKey key = new NamespacedKey(Ships.getInstance(), "ship");
        UUID uuid = origin.getUniqueId();
        origin.getPersistentDataContainer().set(key, PersistentDataType.STRING, "SHIP-ORIGIN");
        for (BlockDisplay blockDisplay : blockDisplays) {
            blockDisplay.getPersistentDataContainer().set(key, PersistentDataType.STRING, uuid.toString());
        }
        for (Shulker shulker : shulkers) {
            shulker.getPersistentDataContainer().set(key, PersistentDataType.STRING, uuid.toString());
        }
        for (ArmorStand shulkerArmorStand : shulkerArmorStands) {
            shulkerArmorStand.getPersistentDataContainer().set(key, PersistentDataType.STRING, uuid.toString());
        }
        this.shulkerArmorStands = shulkerArmorStands;
        this.origin = origin;
        this.blockDisplays = blockDisplays;
        this.shulkers = shulkers;
        this.shulkerOffsets = shulkerOffsets;
        this.velocity = new Vector(0, 0, 0);
        this.vectorToRotate = new Vector(0, 0, 0);
        this.orientation = new Quaternionf();
    }

    public List<BlockDisplay> getBlockDisplays() {
        return blockDisplays;
    }

    public List<Shulker> getShulkers() {
        return shulkers;
    }

    public ArmorStand getOrigin() {
        return origin;
    }

    public void moveTo(Location newLocation) {
        origin.teleportAsync(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        for (int i = 0; i < shulkerArmorStands.size(); i++) {
            ArmorStand shulkerArmorStand = shulkerArmorStands.get(i);
            Vector offset = shulkerOffsets.get(i);
            Location shulkerLoc = newLocation.clone().add(offset);
            shulkerArmorStand.teleportAsync(shulkerLoc.clone().add(0.5, 0, 0.5), PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
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

    public List<ArmorStand> getShulkerArmorStands() {
        return shulkerArmorStands;
    }

    protected void addBlockDisplays(List<BlockDisplay> blockDisplays) {
        this.blockDisplays.addAll(blockDisplays);
    }

    protected void addShulkers(List<Shulker> shulkers) {
        this.shulkers.addAll(shulkers);
    }

    protected void addShulkerArmorStands(List<ArmorStand> shulkerArmorStands) {
        this.shulkerArmorStands.addAll(shulkerArmorStands);
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
        for (BlockDisplay blockDisplay : blockDisplays) {
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
        for (int i = 0; i < shulkerOffsets.size(); i++) {
            Vector originalOffset = shulkerOffsets.get(i);
            Vector3f offset3f = new Vector3f(
                    (float) originalOffset.getX(),
                    (float) originalOffset.getY(),
                    (float) originalOffset.getZ()
            );

            // Apply delta rotation to the Shulker's offset
            offset3f.rotate(deltaQuat);
            Vector rotatedOffset = new Vector(offset3f.x, offset3f.y, offset3f.z);
            shulkerOffsets.set(i, rotatedOffset);

            // Teleport Shulker armor stand to new position
            ArmorStand armorStand = shulkerArmorStands.get(i);
            Location newLocation = originLocation.clone()
                    .add(rotatedOffset)
                    .add(0.5, 0, 0.5); // Center on block
            armorStand.teleportAsync(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        }
    }
}
