package de.t14d3.ships;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ChunkLoadEvent implements Listener {
    private final Ships plugin;

    public ChunkLoadEvent(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                NamespacedKey shipKey = new NamespacedKey(plugin, "ship");
                NamespacedKey offsetKey = new NamespacedKey(plugin, "offset");

                NamespacedKey legacyShipKey = new NamespacedKey("blocktoentity", "ship");
                NamespacedKey legacyOffsetKey = new NamespacedKey("blocktoentity", "offset");

                for (Entity entity : event.getChunk().getEntities()) {
                    if (entity.getPersistentDataContainer().has(legacyShipKey, PersistentDataType.STRING)) {
                        entity.getPersistentDataContainer().set(shipKey, PersistentDataType.STRING, entity.getPersistentDataContainer().get(legacyShipKey, PersistentDataType.STRING));
                        entity.getPersistentDataContainer().remove(legacyShipKey);
                    }
                    if (entity.getPersistentDataContainer().has(legacyOffsetKey, PersistentDataType.STRING)) {
                        entity.getPersistentDataContainer().set(offsetKey, PersistentDataType.STRING, entity.getPersistentDataContainer().get(legacyOffsetKey, PersistentDataType.STRING));
                        entity.getPersistentDataContainer().remove(legacyOffsetKey);
                    }


                    if (entity instanceof ArmorStand armorStand &&
                            armorStand.getPersistentDataContainer().has(shipKey, PersistentDataType.STRING) &&
                            armorStand.getPersistentDataContainer().get(shipKey, PersistentDataType.STRING).equals("SHIP-ORIGIN")) {

                        ShipManager shipManager = plugin.getShipManager();
                        UUID shipUuid = armorStand.getUniqueId();

                        plugin.getLogger().info("Ship UUID: " + shipUuid);

                        List<BlockDisplay> blockDisplays = new ArrayList<>();
                        List<Shulker> shulkers = new ArrayList<>();
                        List<Vector> shulkerOffsets = new ArrayList<>();
                        List<ArmorStand> shulkerArmorStands = new ArrayList<>();

                        List<Entity> nearbyEntities = new ArrayList<>();

                        try {
                            nearbyEntities = plugin.getServer().getScheduler().callSyncMethod(plugin, () -> armorStand.getNearbyEntities(50, 50, 50)).get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }

                        for (Entity nearbyEntity : nearbyEntities) {
                            // Find block displays
                            if (nearbyEntity instanceof BlockDisplay display &&
                                    nearbyEntity.getPersistentDataContainer().has(shipKey, PersistentDataType.STRING) &&
                                    nearbyEntity.getPersistentDataContainer().get(shipKey, PersistentDataType.STRING).equals(shipUuid.toString())) {

                                blockDisplays.add(display);
                                armorStand.addPassenger(display);
                            }

                            // Find shulkers and their offsets
                            if (nearbyEntity instanceof Shulker shulker &&
                                    nearbyEntity.getPersistentDataContainer().has(shipKey, PersistentDataType.STRING) &&
                                    nearbyEntity.getPersistentDataContainer().get(shipKey, PersistentDataType.STRING).equals(shipUuid.toString())) {
                                shulkers.add(shulker);
                            }

                            // Find shulker armor stands and their shulkers
                            if (nearbyEntity instanceof ArmorStand shulkerArmorStand &&
                                    nearbyEntity.getPersistentDataContainer().has(shipKey, PersistentDataType.STRING) &&
                                    nearbyEntity.getPersistentDataContainer().get(shipKey, PersistentDataType.STRING).equals(shipUuid.toString())) {
                                shulkerArmorStands.add(shulkerArmorStand);

                                if (shulkerArmorStand.getPersistentDataContainer().has(offsetKey, PersistentDataType.STRING)) {
                                    String[] offsetParts = shulkerArmorStand.getPersistentDataContainer()
                                            .get(offsetKey, PersistentDataType.STRING)
                                            .split(",");

                                    Vector offset = new Vector(
                                            Double.parseDouble(offsetParts[0]),
                                            Double.parseDouble(offsetParts[1]),
                                            Double.parseDouble(offsetParts[2])
                                    );
                                    shulkerOffsets.add(offset);
                                }
                            }
                        }

                        plugin.getLogger().info("Block Displays: " + blockDisplays.size());
                        plugin.getLogger().info("Shulkers: " + shulkers.size());
                        plugin.getLogger().info("Shulker Armor Stands: " + shulkerArmorStands.size());

                        // Recreate the ship
                        Ship existingShip = shipManager.getShip(shipUuid);
                        if (existingShip != null) {

                            existingShip.addBlockDisplays(blockDisplays);
                            existingShip.addShulkers(shulkers);
                            existingShip.addShulkerArmorStands(shulkerArmorStands);
                            return;
                        }
                        Ship restoredShip = new Ship(armorStand, blockDisplays, shulkers, shulkerOffsets, shulkerArmorStands);
                        shipManager.addShip(restoredShip);
                    }
                }
            }
        };
        task.runTaskAsynchronously(plugin);
    }
}
