package de.t14d3.ships;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.entity.TeleportFlag;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static de.t14d3.ships.ShipManager.shipDataKey;

public class PacketUtils {
    private final ProtocolManager protocolManager;
    private final FieldAccessor entityId = Accessors.getFieldAccessor(Entity.class, AtomicInteger.class, true);

    private final Map<Player, PlayerSelection> playerSelections = new HashMap<>();

    private final Ships plugin;
    private static final BlockData AIR = Bukkit.createBlockData(Material.AIR);

    public PacketUtils(Ships plugin) {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.plugin = plugin;
    }

    private int nextId() {
        return ((AtomicInteger) entityId.get(null)).incrementAndGet();
    }

    public int sendPacket(Location location, BlockState state, Vector3f translation) {
        final int entityId = nextId();
        PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawn.getEntityTypeModifier().write(0, EntityType.BLOCK_DISPLAY);
        spawn.getUUIDs().write(0, UUID.randomUUID());
        spawn.getIntegers().write(0, entityId);
        spawn.getDoubles().write(0, location.getX()).write(1, location.getY()).write(2, location.getZ());

        PacketContainer data = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        data.getDataValueCollectionModifier().write(0, Arrays.asList(
                new WrappedDataValue(23, WrappedDataWatcher.Registry.getBlockDataSerializer(false), ((CraftBlockState) state).getHandle()),
                new WrappedDataValue(11, WrappedDataWatcher.Registry.get(Vector3f.class), translation)
        ));
        data.getIntegers().write(0, entityId);

        protocolManager.broadcastServerPacket(spawn, location, 50);
        protocolManager.broadcastServerPacket(data, location, 50);

        return entityId;
    }

    public void createFromBlocklist(Player player, Location center) {
        PlayerSelection selection = playerSelections.get(player);
        if (selection == null) {
            return;
        }
        createFromBlocklist(List.copyOf(selection.getBlocks()), center);
        for (Block block : selection.getBlocks()) {
            block.setBlockData(AIR, false);
        }
        removeEntities(player);
    }

    public void createFromBlocklist(List<Block> blocks, Location center) {

        ArmorStand armorStand = (ArmorStand) center.getWorld().spawnEntity(center.clone().subtract(0, 2, 0), EntityType.ARMOR_STAND);

        center = center.add(0.5, 0, 0.5);

        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setInvisible(true);


        int[] entityIds = new int[blocks.size()];
        List<ShipBlock> shipBlocks = new ArrayList<>();
        JsonObject shipData = new JsonObject();
        int i = 0;
        for (Block block : blocks) {
            if (!Converter.isVisible(block)) {
                continue;
            }
            Vector3f translation = new Vector3f(
                    (float) (block.getLocation().getX() - center.getX()),
                    (float) (block.getLocation().getY() - center.getY()),
                    (float) (block.getLocation().getZ() - center.getZ())
            );
            entityIds[i] = sendPacket(block.getLocation(), block.getState(), translation);
            JsonObject temp = new JsonObject();
            temp.addProperty("state", block.getState().getBlockData().getAsString());
            temp.addProperty("translation", translation.toString());
            shipData.add(String.valueOf(i), temp);
            shipBlocks.add(new ShipBlock(translation, entityIds[i], block.getState()));
            i++;
        }

        Ship ship = new Ship(armorStand, shipBlocks);
        ship.setData(shipData);
        plugin.getShipManager().addShip(ship);
        sendMountPacket(armorStand, entityIds, null);
    }

    public Ship recreateShip(ArmorStand armorStand) {
        armorStand.setRotation(0, 0);
        armorStand.teleportAsync(armorStand.getLocation().subtract(0, 2, 0), PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);

        //noinspection ConstantConditions
        UUID uuid = UUID.fromString(armorStand.getPersistentDataContainer().get(ShipManager.shipDataKey, PersistentDataType.STRING));

        return plugin.getShipManager().loadShip(armorStand, uuid);
    }

    public Ship recreateShip(String data, ArmorStand armorStand, UUID uuid) {
        armorStand.setGravity(false);
        armorStand.setInvisible(true);
        armorStand.setRotation(0, 0);
        armorStand.teleportAsync(armorStand.getLocation().setRotation(0,0).subtract(0, 2, 0), PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        JsonObject shipData = JsonParser.parseString(data).getAsJsonObject();
        List<ShipBlock> blocks = new ArrayList<>();
        int[] entityIds = new int[shipData.keySet().size()];
        for (Map.Entry<String, JsonElement> element : shipData.entrySet()) {
            JsonObject block = element.getValue().getAsJsonObject();
            Vector3f translation = new Vector3f();
            String[] translationParts = block.get("translation").getAsString().replace('(', ' ').replace(')', ' ').trim().split("\\s+");
            plugin.getLogger().info(block.get("translation").getAsString());
            translation.x = Float.parseFloat(translationParts[0]);
            translation.y = Float.parseFloat(translationParts[1]);
            translation.z = Float.parseFloat(translationParts[2]);
            BlockState state = Bukkit.createBlockData(block.get("state").getAsString()).createBlockState();
            int entityId = sendPacket(armorStand.getLocation(), state, translation);
            blocks.add(new ShipBlock(translation, entityId, state));
            entityIds[Integer.parseInt(element.getKey())] = entityId;
        }

        Ship ship = new Ship(armorStand, blocks, uuid);
        ship.setData(shipData);
        plugin.getShipManager().addShip(ship);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            sendMountPacket(armorStand, entityIds, null);
        }, 1L);

        return ship;
    }

    public void sendTeleportPacket(int entityId, Location location) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getIntegers().write(0, entityId);
        packet.getStructures().write(0, InternalStructure.getConverter().getSpecific(new PositionMoveRotation(new Vec3(location.getX(), location.getY(), location.getZ()), new Vec3(0, 0, 0), 0f, 0f)));
        protocolManager.broadcastServerPacket(packet, location, 50);
    }

    public void sendMountPacket(ArmorStand armorStand, int[] passengers) {
        sendMountPacket(armorStand, passengers, null);
    }

    public void sendMountPacket(ArmorStand armorStand, int[] passengers, Player player) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.MOUNT);
        packet.getIntegers().write(0, armorStand.getEntityId());
        packet.getIntegerArrays().write(0, passengers);

        plugin.getServer().broadcast(Component.text("Sending mount packet for " + armorStand.getEntityId() + " with " + passengers.length + " passengers"));

        if (player != null) {
            protocolManager.sendServerPacket(player, packet);
        } else {
            protocolManager.broadcastServerPacket(packet, armorStand.getLocation(), 50);
        }
    }

    public void sendEntityMetadataUpdate(int entityId, Vector3f translation, Quaternionf rotation, Location location) {
        PacketContainer data = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        data.getDataValueCollectionModifier().write(0, Arrays.asList(
                new WrappedDataValue(11, WrappedDataWatcher.Registry.get(Vector3f.class), translation),
                new WrappedDataValue(13, WrappedDataWatcher.Registry.get(Quaternionf.class), rotation)
        ));
        data.getIntegers().write(0, entityId);
        protocolManager.broadcastServerPacket(data, location, 50);
    }

    public void removeEntities(Player player) {
        PlayerSelection selection = playerSelections.get(player);
        if (selection == null) {
            return;
        }
        int[] entityIds = new int[selection.getEntityIds().size()];
        for (int i = 0; i < selection.getEntityIds().size(); i++) {
            entityIds[i] = selection.getEntityIds().get(i);
        }
        removeEntities(entityIds);
        playerSelections.remove(player);
    }

    public void removeEntities(int[] entityIds) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        IntList intList = new IntArrayList(entityIds);
        packet.getStructures().write(0, InternalStructure.getConverter().getSpecific(intList));
        protocolManager.broadcastServerPacket(packet);
    }

    public void highlightBlocks(Player player, List<Block> blocks) {
        PlayerSelection selection = playerSelections.computeIfAbsent(player, p -> new PlayerSelection(new HashSet<>(), new ArrayList<>()));
        for (Block block : blocks) {
            if (selection.blocks.contains(block)) {
                continue;
            }
            final int entityId = nextId();
            // Create the SPAWN_ENTITY packet for the BLOCK_DISPLAY
            PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawn.getEntityTypeModifier().write(0, EntityType.BLOCK_DISPLAY);
            spawn.getUUIDs().write(0, UUID.randomUUID());
            spawn.getIntegers().write(0, entityId);

            spawn.getDoubles().write(0, block.getX() + 0.001);
            spawn.getDoubles().write(1,  block.getY() + 0.001);
            spawn.getDoubles().write(2,  block.getZ() + 0.001);

            // Create the ENTITY_METADATA packet to enable glowing
            PacketContainer metadata = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadata.getIntegers().write(0, entityId);

            // Set up the DataWatcher to make the entity glow
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)), (byte) 0x60);

            // Convert the DataWatcher entries to WrappedDataValues
            List<WrappedDataValue> dataValues = new ArrayList<>();
            for (WrappedWatchableObject entry : watcher.getWatchableObjects()) {
                if (entry != null) {
                    WrappedDataWatcher.WrappedDataWatcherObject obj = entry.getWatcherObject();
                    dataValues.add(new WrappedDataValue(
                            obj.getIndex(),
                            obj.getSerializer(),
                            entry.getRawValue()
                    ));
                }
            }
            dataValues.add(new WrappedDataValue(23, WrappedDataWatcher.Registry.getBlockDataSerializer(false), ((CraftBlockState) block.getState()).getHandle()));
            dataValues.add(new WrappedDataValue(12, WrappedDataWatcher.Registry.get(Vector3f.class), new Vector3f(0.998f, 0.998f, 0.998f)));

            // Apply the data values to the metadata packet
            metadata.getDataValueCollectionModifier().write(0, dataValues);

            // Send the packets to the player
            protocolManager.sendServerPacket(player, spawn);
            protocolManager.sendServerPacket(player, metadata);

            selection.addBlock(block);
            selection.addEntityId(entityId);
        }
    }

    public PlayerSelection getPlayerSelection(Player player) {
        return playerSelections.get(player);
    }

    static class PlayerSelection {
        private final Set<Block> blocks;
        private final List<Integer> entityIds;

        public PlayerSelection(Set<Block> blocks, List<Integer> entityIds) {
            this.blocks = blocks;
            this.entityIds = entityIds;
        }

        public Set<Block> getBlocks() {
            return blocks;
        }

        public List<Integer> getEntityIds() {
            return entityIds;
        }

        public void addBlock(Block block) {
            blocks.add(block);
        }

        public void addEntityId(int entityId) {
            entityIds.add(entityId);
        }
    }
}
