package de.t14d3.ships;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Shulker;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class ShipBlock {
    private int entityId;
    private Vector3f offset;
    private BlockState state;

    private ArmorStand seat;
    private Shulker floor;

    public ShipBlock(Vector3f offset, int entityId, BlockState state) {
        this.entityId = entityId;
        this.offset = offset;
        this.state = state;
        this.seat = null;
        this.floor = null;
    }

    public ShipBlock(Vector3f offset, int entityId, BlockState state, ArmorStand seat) {
        this.entityId = entityId;
        this.offset = offset;
        this.state = state;
        this.seat = seat;
        this.floor = null;
    }

    public ShipBlock(Vector3f offset, int entityId, BlockState state, Shulker floor) {
        this.entityId = entityId;
        this.offset = offset;
        this.state = state;
        this.seat = null;
        this.floor = floor;
    }

    public ArmorStand getSeat() {
        if (seat != null && !seat.isValid()) {
            seat = null;
        }
        return seat;
    }

    public void setSeat(ArmorStand seat) {
        this.seat = seat;
    }

    public Vector3f getOffset() {
        return offset;
    }

    public Location getLocation(Ship ship) {
        return ship.getOrigin().getLocation().add(Vector.fromJOML(offset)).add(0, 2, 0);
    }

    public int getEntityId() {
        return entityId;
    }

    public BlockState getState() {
        return state;
    }

    public Shulker getFloor() {
        return floor;
    }

    public void setFloor(Shulker floor) {
        this.floor = floor;
    }
}
