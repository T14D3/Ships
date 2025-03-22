package de.t14d3.ships;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ShipManager {
    private final Map<UUID, Ship> ships = new HashMap<>();
    private final Ships plugin;
    public static NamespacedKey booleanShipKey;
    public static NamespacedKey shipDataKey;
    private final FileConfiguration config;

    public ShipManager(Ships plugin) {
        this.plugin = plugin;
        booleanShipKey = new NamespacedKey(plugin, "data");
        shipDataKey = new NamespacedKey(plugin, "ship");

        this.config = new YamlConfiguration();
        try {
            config.load(plugin.getDataFolder().toPath().resolve("ships.yml").toFile());
        } catch (FileNotFoundException e) {
            // Ships.yml does not exist, create it
            plugin.saveResource("ships.yml", false);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Ship loadShip(ArmorStand origin, UUID uuid) {
        String data = config.getString(uuid.toString());
        if (data == null) {
            return null;
        }
        Ship ship = plugin.getPacketUtils().recreateShip(data, origin, uuid);
        if (ship != null) {
            ships.put(uuid, ship);
        }
        plugin.getSLF4JLogger().info("Loaded ship {}", uuid);
        return ship;
    }

    public void saveShip(UUID uuid, String data) {
        config.set(uuid.toString(), data);
        plugin.getSLF4JLogger().info("Saved ship {}", uuid);
    }



    public void tick() {
        for (Ship ship : ships.values()) {
            if (!ship.getVector().isZero()) {
                ship.move(ship.getVector());
            }
            if (!ship.getOrientation().equals(new Quaternionf())) {
                ship.rotate(ship.getVectorToRotate());
            }
        }
        updatePlayerFloorEntitiesAsync();
    }

    private void updatePlayerFloorEntitiesAsync() {
        for (Ship ship : ships.values()) {
            if (ship.getShipBlocks() == null || ship.getShipBlocks().isEmpty()) {
                continue;
            }

            // Retrieve the ship's current orientation
            Quaternionf shipOrientation = ship.getOrientation();

            for (ShipBlock shipBlock : ship.getShipBlocks()) {
                Vector3f relativePosition = new Vector3f();
                relativePosition.set(shipBlock.getOffset());

                // Rotate the relative position using the ship's orientation
                relativePosition.add(0.5f, 0, 0.5f);
                relativePosition.rotate(shipOrientation);

                // Determine the new world position of the block
                Location shipOrigin = ship.getOrigin().getLocation();
                Location blockWorldLocation = shipOrigin.clone().add(relativePosition.x, relativePosition.y + 2, relativePosition.z);
                if (blockWorldLocation.getNearbyPlayers(3).isEmpty()) {
                    if (shipBlock.getFloor() == null) {
                        continue;
                    }
                    shipBlock.getFloor().getPassengers().forEach(Entity::remove);
                    shipBlock.getFloor().remove();
                    shipBlock.setFloor(null);
                    continue;
                }
                if (shipBlock.getFloor() == null || !shipBlock.getFloor().isValid()) {
                    World world = ship.getOrigin().getWorld();

                    // Spawn the Armor Stand
                    ArmorStand armorStand = (ArmorStand) world.spawnEntity(blockWorldLocation, EntityType.ARMOR_STAND);
                    armorStand.setInvisible(true);
                    armorStand.setSmall(true);
                    armorStand.setMarker(true);
                    armorStand.setGravity(false);
                    armorStand.setAI(false);
                    armorStand.getPersistentDataContainer().set(booleanShipKey, PersistentDataType.BOOLEAN, true);

                    // Spawn the Shulker and set it to ride the Armor Stand
                    Shulker shulker = (Shulker) world.spawnEntity(blockWorldLocation, EntityType.SHULKER);
                    shulker.setAI(false);
                    shulker.setInvisible(true);
                    shulker.setGravity(false);
                    shulker.setNoPhysics(true);
                    shulker.setInvulnerable(true);
                    shulker.setCollidable(false);
                    shulker.setAware(false);
                    armorStand.addPassenger(shulker);

                    // Store the Armor Stand (since it controls movement)
                    shipBlock.setFloor(armorStand);
                } else {
                    ArmorStand armorStand = shipBlock.getFloor();
                    armorStand.teleportAsync(blockWorldLocation, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
                }
            }

        }
    }

    public void addShip(Ship ship) {
        if (ships.containsKey(ship.getUuid())) {
            return;
        }
        ships.put(ship.getUuid(), ship);
        saveShip(ship.getUuid(), ship.getData().toString());
    }

    public Ship getShip(UUID uuid) {
        return ships.get(uuid);
    }

    public Ship getShip(ArmorStand armorStand) {
        return ships.values().stream().filter(ship -> ship.getOrigin().equals(armorStand)).findFirst().orElse(null);
    }

    public Ship getControlledBy(Player player) {
        for (Ship ship : ships.values()) {
            if (ship.getController() == player) {
                return ship;
            }
        }
        return null;
    }

    public CompletableFuture<Ship> getShipFromShulker(Shulker shulker) {
        return CompletableFuture.supplyAsync(() -> {
            for (Ship ship : ships.values()) {
                if (ship.getShipBlocks() != null) {
                    for (ShipBlock shipBlock : ship.getShipBlocks()) {
                        if (shipBlock.getFloor() == null) {
                            continue;
                        }
                        if (shipBlock.getFloor().getPassengers().contains(shulker)) {
                            return ship;
                        }
                    }
                }
            }
            return null;
        });
    }

    public CompletableFuture<Ship> getShipFromSeat(ArmorStand armorStand) {
        return CompletableFuture.supplyAsync(() -> {
            for (Ship ship : ships.values()) {
                if (ship.getShipBlocks() != null) {
                    for (ShipBlock shipBlock : ship.getShipBlocks()) {
                        if (shipBlock.getSeat().equals(armorStand)) {
                            return ship;
                        }
                    }
                }
            }
            return null;
        });
    }

    public void cleanup(List<Entity> entities) {
        entities.forEach(entity -> {
            if (entity instanceof ArmorStand armorStand) {
                if (armorStand.getPersistentDataContainer().has(booleanShipKey, PersistentDataType.BOOLEAN)) {
                    if (getShipFromSeat(armorStand).join() == null) {
                        plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                            armorStand.getPassengers().forEach(Entity::remove);
                            armorStand.remove();
                            return null;
                        });
                    }
                }
            }
        });
    }

    public void removeShip(UUID uuid) {
        Ship target = ships.get(uuid);
        if (target != null) {
            removeShip(target);
        }
    }
    public void removeShip(Ship ship) {
        ship.getShipBlocks().forEach(shipBlock -> {
            if (shipBlock.getFloor() != null) {
                shipBlock.getFloor().getPassengers().forEach(Entity::remove);
                shipBlock.getFloor().remove();
                shipBlock.setFloor(null);
            }
        });
        plugin.getPacketUtils().removeEntities(ship.getEntityIds());
        if (ship.getOrigin() != null && ship.getOrigin().isValid()) {
            ship.getOrigin().remove();
        }
        ships.remove(ship.getUuid());
    }

    public void save() {
        for (Ship ship : ships.values()) {
            saveShip(ship.getOrigin().getUniqueId(), ship.getData().toString());
        }
        try {
            config.save(plugin.getDataFolder().toPath().resolve("ships.yml").toFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save ships.yml: " + e.getMessage());
        }
    }
}
