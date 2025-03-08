package de.t14d3.ships;

import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Shulker;
import org.bukkit.util.Vector;

public class ShipBlock {
    private final BlockDisplay blockDisplay;
    private Shulker shulker;
    private Vector offset;

    public ShipBlock(BlockDisplay blockDisplay, Shulker shulker, Vector offset) {
        this.blockDisplay = blockDisplay;
        this.shulker = shulker;
        this.offset = offset;
    }

    public BlockDisplay getBlockDisplay() {
        return blockDisplay;
    }

    public Shulker getShulker() {
        return shulker;
    }

    public Vector getOffset() {
        return offset;
    }

    public void setOffset(Vector offset) {
        this.offset = offset;
    }

    public void setShulker(Shulker shulker) {
        this.shulker = shulker;
    }
}
