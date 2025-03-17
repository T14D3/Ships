package de.t14d3.ships;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Converter {
    private final Ships plugin;

    public Converter(Ships plugin) {
        this.plugin = plugin;
    }

    public void convert(Player player) {
        List<Block> blocks = new ArrayList<>();
        Location pos1 = (Location) player.getMetadata("pos1").get(0).value();
        Location pos2 = (Location) player.getMetadata("pos2").get(0).value();

        Location min = new Location(player.getWorld(),
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()));
        Location max = new Location(player.getWorld(),
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ()));
        Location center = new Location(player.getWorld(),
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2);

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = min.getWorld().getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {
                        blocks.add(block);
                    }
                }
            }
        }
        plugin.getPacketUtils().createFromBlocklist(blocks, center);
    }

    /**
     * Returns a list of blocks that are connected to the given block. <br>
     * {@link warn <b>Warning:</b> Recursive method, can easily crash if the range is too high! Can only be used asynchronously}
     * @param block The block to start from
     * @param range The maximum distance to search
     * @return A list of connected blocks
     */
    @SuppressWarnings("JavadocReference")
    public List<Block> getConnectedBlocks(Block block, int range) {
        if (Thread.currentThread().threadId() == plugin.getMainThreadId()) {
            throw new IllegalStateException("Cannot be called from the main thread!");
        }
        Set<Block> connectedBlocks = new HashSet<>();
        connectedBlocks.add(block);
        getConnectedBlocksRecursive(block, range, connectedBlocks);
        return new ArrayList<>(connectedBlocks);
    }

    private void getConnectedBlocksRecursive(Block block, int range, Set<Block> connectedBlocks) {
        if (range <= 0) {
            return;
        }
        BlockFace[] faces = BlockFace.values();
        for (BlockFace face : faces) {
            Block connectedBlock = block.getRelative(face);
            if (connectedBlock.getType().isCollidable() && !connectedBlock.isLiquid() && connectedBlocks.add(connectedBlock)) {
                getConnectedBlocksRecursive(connectedBlock, range - 1, connectedBlocks);
            }
        }
    }

}
