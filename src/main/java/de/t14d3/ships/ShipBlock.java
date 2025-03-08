package de.t14d3.ships;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Shulker;
import org.bukkit.util.Vector;

public class ShipBlock {
    private final BlockDisplay blockDisplay;
    private Block block;
    private Vector offset;

    public static final BlockData AIR = Bukkit.createBlockData(Material.AIR);
    public static final BlockData BARRIER = Bukkit.createBlockData(Material.BARRIER);

    public ShipBlock(BlockDisplay blockDisplay, Block block, Vector offset) {
        this.blockDisplay = blockDisplay;
        this.block = block;
        this.offset = offset;
    }

    public BlockDisplay getBlockDisplay() {
        return blockDisplay;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public Vector getOffset() {
        return offset;
    }

    public void setOffset(Vector offset) {
        this.offset = offset;
    }
}
