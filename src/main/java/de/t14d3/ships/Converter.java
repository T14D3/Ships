package de.t14d3.ships;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
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
    private final Ships plugin;
    private BukkitTask task;

    private static final BlockData AIR = Bukkit.createBlockData(Material.AIR);

    public Converter(Ships plugin) {
        this.plugin = plugin;
    }

    public void convert(Player player) {
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

        double centerX = min.getX() + (max.getX() - min.getX()) / 2.0;
        double centerY = min.getY() + (max.getY() - min.getY()) / 2.0;
        double centerZ = min.getZ() + (max.getZ() - min.getZ()) / 2.0;
        Location center = new Location(player.getWorld(), centerX, centerY, centerZ);

        // Create persistent data keys
        NamespacedKey shipKey = new NamespacedKey(plugin, "ship");
        NamespacedKey offsetKey = new NamespacedKey(plugin, "offset");

        ArmorStand marker = (ArmorStand) player.getWorld().spawnEntity(center, EntityType.ARMOR_STAND);
        marker.setGravity(false);
        marker.setInvulnerable(true);
        marker.setInvisible(true);
        List<ArmorStand> shulkerArmorStands = new ArrayList<>();

        // Tag the armor stand as ship origin
        marker.getPersistentDataContainer().set(shipKey, PersistentDataType.STRING, "SHIP-ORIGIN");

        List<BlockDisplay> blockDisplays = new ArrayList<>();
        List<Shulker> shulkers = new ArrayList<>();
        List<Vector> shulkerOffsets = new ArrayList<>();

        List<Block> blocksToRemove = new ArrayList<>();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = min.getWorld().getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {

                        BlockDisplay blockDisplay = (BlockDisplay) min.getWorld().spawnEntity(center, EntityType.BLOCK_DISPLAY);
                        blockDisplay.setBlock(block.getBlockData());

                        // Calculate offset relative to the center
                        float offsetX = (float) (x - centerX);
                        float offsetY = (float) (y - centerY);
                        float offsetZ = (float) (z - centerZ);
                        Vector3f offset = new Vector3f(offsetX, offsetY, offsetZ);

                        Transformation transformation = new Transformation(
                                offset,
                                new Quaternionf(),
                                blockDisplay.getTransformation().getScale(),
                                new Quaternionf()
                        );

                        blockDisplay.setTransformation(transformation);
                        marker.addPassenger(blockDisplay);
                        blockDisplays.add(blockDisplay);

                        // Tag block display with ship UUID
                        blockDisplay.getPersistentDataContainer().set(shipKey,
                                PersistentDataType.STRING,
                                marker.getUniqueId().toString());

                        Shulker shulker = (Shulker) block.getWorld().spawnEntity(block.getLocation(), EntityType.SHULKER);
                        ArmorStand shulkerArmorStand = (ArmorStand) block.getWorld().spawnEntity(block.getLocation(), EntityType.ARMOR_STAND);
                        shulkerArmorStand.setInvisible(true);
                        shulkerArmorStand.setGravity(false);
                        shulkerArmorStand.addPassenger(shulker);
                        shulker.setInvisible(true);
                        shulker.setAI(false);
                        shulker.setInvulnerable(true);
                        shulker.setCollidable(false);
                        shulker.setSilent(true);
                        shulker.setGravity(false);
                        shulker.setPeek(0);

                        // Calculate and store offset
                        Vector shulkerOffset = new Vector(
                                x - centerX,
                                y - centerY,
                                z - centerZ
                        );
                        shulkerOffsets.add(shulkerOffset);

                        // Store offset in shulker's persistent data container
                        String offsetString = shulkerOffset.getX() + "," +
                                shulkerOffset.getY() + "," +
                                shulkerOffset.getZ();
                        shulkerArmorStand.getPersistentDataContainer().set(offsetKey,
                                PersistentDataType.STRING,
                                offsetString);

                        // Tag shulker with ship UUID
                        shulker.getPersistentDataContainer().set(shipKey,
                                PersistentDataType.STRING,
                                marker.getUniqueId().toString());
                        shulkerArmorStand.getPersistentDataContainer().set(shipKey,
                                PersistentDataType.STRING,
                                marker.getUniqueId().toString());

                        shulkers.add(shulker);
                        shulkerArmorStands.add(shulkerArmorStand);
                        block.setBlockData(AIR, false);
                    }
                }
            }
        }

        Ship ship = new Ship(marker, blockDisplays, shulkers, shulkerOffsets, shulkerArmorStands);
        plugin.getShipManager().addShip(ship);
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
