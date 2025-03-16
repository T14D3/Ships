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
    private ArmorStand floor;

    private Ship ship;

    public ShipBlock(Vector3f offset, int entityId, BlockState state) {
        this.entityId = entityId;
        this.offset = offset;
        this.state = state;
        this.seat = null;
        this.floor = null;
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

    public ArmorStand getFloor() {
        return floor;
    }

    public void setFloor(ArmorStand floor) {
        this.floor = floor;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    public Ship getShip() {
        return ship;
    }
}
