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
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PacketUtils {
    private final ProtocolManager protocolManager;
    private final WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.getBlockDataSerializer(false);
    private final FieldAccessor entityId = Accessors.getFieldAccessor(Entity.class, AtomicInteger.class, true);

    public PacketUtils() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    private int nextId() {
        return ((AtomicInteger) entityId.get(null)).incrementAndGet();
    }

    public int sendPacket(Location location, BlockState state) {
        final int entityId = nextId();
        PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawn.getEntityTypeModifier().write(0, EntityType.BLOCK_DISPLAY);
        spawn.getUUIDs().write(0, UUID.randomUUID());
        spawn.getIntegers().write(0, entityId);
        spawn.getDoubles().write(0, location.getX()).write(1, location.getY()).write(2, location.getZ());

        PacketContainer data = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        data.getDataValueCollectionModifier().write(0, Arrays.asList(
                new WrappedDataValue(23, serializer, ((CraftBlockState) state).getHandle())
        ));
        data.getIntegers().write(0, entityId);

        protocolManager.broadcastServerPacket(spawn, location, 50);
        protocolManager.broadcastServerPacket(data, location, 50);

        return entityId;
    }

    public int multi(Player player) {
        final int entityId = nextId();
        PacketContainer armorStand = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        armorStand.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
        armorStand.getUUIDs().write(0, UUID.randomUUID());
        armorStand.getIntegers().write(0, entityId);
        armorStand.getDoubles().write(0, player.getLocation().getX()).write(1, player.getLocation().getY()).write(2, player.getLocation().getZ());

        PacketContainer passenger = protocolManager.createPacket(PacketType.Play.Server.MOUNT);
        passenger.getIntegers().write(0, entityId);

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


        int[] entityIds = new int[blocks.size()];
        int i = 0;
        for (Block block : blocks) {
            entityIds[i] = sendPacket(block.getLocation(), block.getState());
            i++;
        }
        passenger.getIntegerArrays().write(0, entityIds);


        protocolManager.broadcastServerPacket(armorStand, player.getLocation(), 50);
        protocolManager.broadcastServerPacket(passenger, player.getLocation(), 50);

        Bukkit.getScheduler().runTaskLater(Ships.getInstance(), () -> {
            sendMovePacket(entityId, player.getLocation());
        }, 20L);

        return entityId;
    }

    public void sendMovePacket(int entityId, Location location) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getIntegers().write(0, entityId);
        packet.getStructures().write(0, InternalStructure.getConverter().getSpecific(new PositionMoveRotation(new Vec3(location.getX(), location.getY(), location.getZ()), new Vec3(0, 0, 0), 0f, 0f)));
        protocolManager.broadcastServerPacket(packet, location, 50);
    }
}
