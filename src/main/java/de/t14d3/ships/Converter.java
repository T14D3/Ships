package de.t14d3.ships;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Converter {
    private static final BlockData AIR = Bukkit.createBlockData(Material.AIR);
    private final Ships plugin;
    private BukkitTask task;

    public Converter(Ships plugin) {
        this.plugin = plugin;
    }

    static boolean isHidden(Block block) {
        return
                (block.getRelative(BlockFace.DOWN).getType().isSolid()) &&
                        (block.getRelative(BlockFace.UP).getType().isSolid()) &&
                        (block.getRelative(BlockFace.NORTH).getType().isSolid()) &&
                        (block.getRelative(BlockFace.SOUTH).getType().isSolid()) &&
                        (block.getRelative(BlockFace.EAST).getType().isSolid()) &&
                        (block.getRelative(BlockFace.WEST).getType().isSolid());
    }

    public void convert(Player player) {
        Location pos1 = (Location) player.getMetadata("pos1").get(0).value();
        Location pos2 = (Location) player.getMetadata("pos2").get(0).value();
        Location temp = (Location) player.getMetadata("center").get(0).value();
        double centerX = temp.getX();
        double centerY = temp.getY();
        double centerZ = temp.getZ();
        Location center = new Location(player.getWorld(), centerX, centerY, centerZ);

        Location min = new Location(player.getWorld(),
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()));
        Location max = new Location(player.getWorld(),
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ()));

        // Create persistent data keys
        NamespacedKey shipKey = new NamespacedKey(plugin, "ship");
        NamespacedKey offsetKey = new NamespacedKey(plugin, "offset");
        NamespacedKey shulkerKey = new NamespacedKey(plugin, "shulker");

        ArmorStand marker = (ArmorStand) player.getWorld().spawnEntity(center, EntityType.ARMOR_STAND);
        marker.setGravity(false);
        marker.setInvulnerable(true);
        marker.setInvisible(true);

        // Tag the armor stand as ship origin
        marker.getPersistentDataContainer().set(shipKey, PersistentDataType.STRING, "SHIP-ORIGIN");

        List<Block> blocksToRemove = new ArrayList<>();
        List<ShipBlock> shipBlocks = new ArrayList<>();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = min.getWorld().getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {
                        if (!isHidden(block)) {
                            BlockDisplay blockDisplay = (BlockDisplay) min.getWorld().spawnEntity(block.getLocation(), EntityType.BLOCK_DISPLAY, CreatureSpawnEvent.SpawnReason.CUSTOM);
                            blockDisplay.setBlock(block.getBlockData());

                            // Calculate offset relative to the center
                            float offsetX = (float) (x - centerX);
                            float offsetY = (float) (y - centerY);
                            float offsetZ = (float) (z - centerZ);

                            // Tag block display with ship UUID
                            blockDisplay.getPersistentDataContainer().set(shipKey,
                                    PersistentDataType.STRING,
                                    marker.getUniqueId().toString());

                            // Store offset in block display's persistent data container
                            String offsetString = offsetX + "," + offsetY + "," + offsetZ;
                            blockDisplay.getPersistentDataContainer().set(offsetKey, PersistentDataType.STRING, offsetString);
                            Shulker shulker = null;
                            if (block.getType().isSolid() && !block.getRelative(BlockFace.UP).getType().isSolid()) {
                                shulker = (Shulker) block.getWorld().spawnEntity(block.getLocation(), EntityType.SHULKER);
                                shulker.setInvisible(true);
                                shulker.setAI(false);
                                shulker.setInvulnerable(true);
                                shulker.setCollidable(false);
                                shulker.setSilent(true);
                                shulker.setGravity(false);
                                shulker.setNoPhysics(true);
                                shulker.setPeek(0);

                                plugin.getCollisionTeam().addEntity(shulker);

                                // Store ship UUID in shulker's persistent data container
                                shulker.getPersistentDataContainer().set(shipKey,
                                        PersistentDataType.STRING,
                                        marker.getUniqueId().toString());

                                blockDisplay.getPersistentDataContainer().set(shulkerKey,
                                        PersistentDataType.STRING,
                                        shulker.getUniqueId().toString());
                            }
                            shipBlocks.add(new ShipBlock(blockDisplay, shulker, new Vector(offsetX, offsetY, offsetZ)));
                        }
                        blocksToRemove.add(block);
                    }
                }
            }
        }

        Ship ship = new Ship(marker, shipBlocks);
        plugin.getShipManager().addShip(ship);

        for (Block block : blocksToRemove) {
            block.setBlockData(AIR, false);
        }

        player.sendMessage("Ship created");
    }

    public void move(Player player) {
        if (player.getMetadata("moving").isEmpty()) {
            Ship ship = plugin.getShipManager().getControlledBy(player) != null ? plugin.getShipManager().getControlledBy(player) : plugin.getShipManager().getNearestShip(player.getLocation());
            if (ship == null) {
                player.sendMessage("No target ship selected");
                return;
            }
            ship.setController(player);
            player.setMetadata("moving", new FixedMetadataValue(plugin, true));
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                Location currentLoc = ship.getOrigin().getLocation();
                Vector direction = player.getLocation().getDirection().normalize().multiply(0.1);
                Location newLoc = currentLoc.clone().add(direction);
                ship.moveTo(newLoc);
            }, 1, 1);
        } else {
            if (task != null) {
                player.removeMetadata("moving", plugin);
                plugin.getShipManager().getControlledBy(player).setController(null);
                task.cancel();
            }
            player.sendMessage("Stopped moving");
        }
    }
}
