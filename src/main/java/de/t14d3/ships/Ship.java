package de.t14d3.ships;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import io.papermc.paper.entity.TeleportFlag;
import java.util.List;
import java.util.UUID;

public class Ship {
    private final ArmorStand origin;
    private final List<BlockDisplay> blockDisplays;
    private final List<Shulker> shulkers;
    private final List<Vector> shulkerOffsets;
    private final List<ArmorStand> shulkerArmorStands;
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
        this.shulkerArmorStands = shulkerArmorStands;
        this.origin = origin;
        this.blockDisplays = blockDisplays;
        this.shulkers = shulkers;
        this.shulkerOffsets = shulkerOffsets;
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
        origin.teleport(newLocation, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        for (int i = 0; i < shulkerArmorStands.size(); i++) {
            ArmorStand shulkerArmorStand = shulkerArmorStands.get(i);
            Vector offset = shulkerOffsets.get(i);
            Location shulkerLoc = newLocation.clone().add(offset);
            shulkerArmorStand.teleport(shulkerLoc.clone().add(0.5, 0, 0.5), TeleportFlag.EntityState.RETAIN_PASSENGERS);
        }
    }

    public void move(Vector direction) {
        moveTo(origin.getLocation().add(direction));
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
}
