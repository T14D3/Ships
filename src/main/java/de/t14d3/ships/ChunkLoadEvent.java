package de.t14d3.ships;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChunkLoadEvent implements Listener {
    private final Ships plugin;

    public ChunkLoadEvent(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        NamespacedKey shipKey = new NamespacedKey(plugin, "ship");
        NamespacedKey offsetKey = new NamespacedKey(plugin, "offset");

        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ArmorStand armorStand &&
                    armorStand.getPersistentDataContainer().has(shipKey, PersistentDataType.STRING) &&
                    armorStand.getPersistentDataContainer().get(shipKey, PersistentDataType.STRING).equals("SHIP-ORIGIN")) {

                ShipManager shipManager = plugin.getShipManager();
                UUID shipUuid = armorStand.getUniqueId();

                List<BlockDisplay> blockDisplays = new ArrayList<>();
                List<Shulker> shulkers = new ArrayList<>();
                List<Vector> shulkerOffsets = new ArrayList<>();
                List<ArmorStand> shulkerArmorStands = new ArrayList<>();

                // Find all related entities in the chunk
                for (Entity chunkEntity : event.getChunk().getEntities()) {
                    // Find block displays
                    if (chunkEntity instanceof BlockDisplay display &&
                            chunkEntity.getPersistentDataContainer().has(shipKey, PersistentDataType.STRING) &&
                            chunkEntity.getPersistentDataContainer().get(shipKey, PersistentDataType.STRING).equals(shipUuid.toString())) {

                        blockDisplays.add(display);
                        armorStand.addPassenger(display);
                    }

                    // Find shulkers and their offsets
                    if (chunkEntity instanceof Shulker shulker &&
                            chunkEntity.getPersistentDataContainer().has(shipKey, PersistentDataType.STRING) &&
                            chunkEntity.getPersistentDataContainer().get(shipKey, PersistentDataType.STRING).equals(shipUuid.toString())) {
                    }

                    // Find shulker armor stands and their shulkers
                    if (chunkEntity instanceof ArmorStand shulkerArmorStand &&
                            chunkEntity.getPersistentDataContainer().has(shipKey, PersistentDataType.STRING) &&
                            chunkEntity.getPersistentDataContainer().get(shipKey, PersistentDataType.STRING).equals(shipUuid.toString())) {
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

                            shulkerArmorStands.add(shulkerArmorStand);
                            shulkerOffsets.add(offset);
                        }
                    }
                }

                // Recreate the ship
                Ship existingShip = shipManager.getShip(shipUuid);
                if (existingShip != null) {

                    existingShip.addBlockDisplays(blockDisplays);
                    existingShip.addShulkers(shulkers);
                    existingShip.addShulkerArmorStands(shulkerArmorStands);
                }
                Ship restoredShip = new Ship(armorStand, blockDisplays, shulkers, shulkerOffsets, shulkerArmorStands);
                shipManager.addShip(restoredShip);
            }
        }
    }
}
