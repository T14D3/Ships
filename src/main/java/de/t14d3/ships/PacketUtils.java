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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.entity.TeleportFlag;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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

    private final Ships plugin;

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

    public void createFromBlocklist(List<Block> blocks, Location center) {

        ArmorStand armorStand = (ArmorStand) center.getWorld().spawnEntity(center.clone().subtract(0, 2, 0), EntityType.ARMOR_STAND);

        center = center.add(0.5, 0, 0.5);

        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setInvisible(true);
        int entityId = armorStand.getEntityId();

        PacketContainer passenger = protocolManager.createPacket(PacketType.Play.Server.MOUNT);
        passenger.getIntegers().write(0, entityId);


        int[] entityIds = new int[blocks.size()];
        List<ShipBlock> shipBlocks = new ArrayList<>();
        JsonObject shipData = new JsonObject();
        int i = 0;
        for (Block block : blocks) {
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
        armorStand.getPersistentDataContainer().set(shipDataKey, PersistentDataType.STRING, shipData.toString());
        passenger.getIntegerArrays().write(0, entityIds);

        plugin.getShipManager().addShip(new Ship(armorStand, shipBlocks));

        protocolManager.broadcastServerPacket(passenger, center, 50);
    }
    public Ship recreateShip(ArmorStand armorStand) {
        return recreateShip(armorStand, null);
    }

    public Ship recreateShip(ArmorStand armorStand, Player player) {
        armorStand.setRotation(0, 0);
        armorStand.teleportAsync(armorStand.getLocation().subtract(0, 2, 0), PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        String temp = armorStand.getPersistentDataContainer().get(shipDataKey, PersistentDataType.STRING);
        assert temp != null;
        JsonObject shipData = JsonParser.parseString(temp).getAsJsonObject();
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

        PacketContainer passenger = protocolManager.createPacket(PacketType.Play.Server.MOUNT);
        passenger.getIntegers().write(0, armorStand.getEntityId());
        passenger.getIntegerArrays().write(0, entityIds);
        if (player != null) {
            protocolManager.sendServerPacket(player, passenger);
        } else {
            protocolManager.broadcastServerPacket(passenger, armorStand.getLocation(), 50);
        }

        Ship ship = new Ship(armorStand, blocks);
        plugin.getShipManager().addShip(ship);
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

    public void removeEntities(int[] entityIds) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        IntList intList = new IntArrayList(entityIds);
        packet.getStructures().write(0, InternalStructure.getConverter().getSpecific(intList));
        protocolManager.broadcastServerPacket(packet);
    }
}
