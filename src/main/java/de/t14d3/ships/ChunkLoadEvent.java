package de.t14d3.ships;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

                for (Entity entity : event.getChunk().getEntities()) {

                    if (entity instanceof ArmorStand armorStand &&
                            armorStand.getPersistentDataContainer().has(shipKey, PersistentDataType.STRING) &&
                            armorStand.getPersistentDataContainer().get(shipKey, PersistentDataType.STRING).equals("SHIP-ORIGIN")) {

                        UUID shipUuid = armorStand.getUniqueId();

                        List<ShipBlock> shipBlocks = new ArrayList<>();

                        List<Entity> nearbyEntities;

                        try {
                            nearbyEntities = plugin.getServer().getScheduler().callSyncMethod(plugin, () -> armorStand.getNearbyEntities(50, 50, 50)).get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }

                        for (Entity nearbyEntity : nearbyEntities) {
                            if (nearbyEntity instanceof BlockDisplay display) {
                                Vector offset = new Vector(0, 0, 0);
                                if (display.getPersistentDataContainer().has(offsetKey, PersistentDataType.STRING)) {
                                    String[] offsetParts = display.getPersistentDataContainer().get(offsetKey, PersistentDataType.STRING).split(",");
                                    offset = new Vector(Double.parseDouble(offsetParts[0]), Double.parseDouble(offsetParts[1]), Double.parseDouble(offsetParts[2]));
                                }
                                shipBlocks.add(new ShipBlock(display, display.getLocation().getBlock(), offset));
                            }
                        }

                        plugin.getLogger().info("Ship UUID: " + shipUuid);
                        plugin.getLogger().info("Ship Blocks: " + shipBlocks.size());

                        Ship ship = plugin.getShipManager().getShip(shipUuid);
                        if (ship == null) {
                            ship = new Ship(armorStand, shipBlocks);
                            plugin.getShipManager().addShip(ship);
                        } else {
                            ship.addShipBlocks(shipBlocks);
                        }
                    }
                }

            }
        };
        task.runTaskAsynchronously(plugin);
    }
}
