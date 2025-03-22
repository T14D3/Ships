package de.t14d3.ships;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import net.minecraft.world.entity.player.Input;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

public class MoveListener implements Listener {
    private final Ships thisplugin;
    private final ProtocolManager protocolManager;

    public MoveListener(Ships plugin) {
        this.thisplugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerPacketListener();
    }

    private void registerPacketListener() {
        protocolManager.addPacketListener(new PacketAdapter(
                thisplugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Client.STEER_VEHICLE
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();

                if (!player.isInsideVehicle()) {
                    return;
                }

                Ship ship = thisplugin.getShipManager().getControlledBy(player);
                if (ship == null) return;

                Input input = (Input) event.getPacket().getModifier().readSafely(0);

                Vector direction = new Vector();
                Vector rotation = new Vector();
                if (input.left())
                    rotation.add(new Vector(0, 1, 0)); // Rotate left
                if (input.right())
                    rotation.add(new Vector(0, 1, 0).multiply(-1)); // Rotate right
                if (input.forward())
                    direction.add(player.getLocation().getDirection().clone().setY(0).normalize().multiply(0.1));
                if (input.backward())
                    direction.add(player.getLocation().getDirection().clone().setY(0).normalize().multiply(-0.1));
                if (input.jump()) direction.add(new Vector(0, 0.1, 0));
                if (input.sprint()) direction.add(new Vector(0, -0.1, 0));

                ship.setVectorToRotate(rotation);
                ship.setVector(direction);
                ship.rotate(rotation);
            }

        });
    }

    @EventHandler
    public void onLoad(PlayerTrackEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand armorStand) {
            if (armorStand.getPersistentDataContainer().has(ShipManager.shipDataKey, PersistentDataType.STRING)) {
                Ship ship = thisplugin.getShipManager().getShip(armorStand);
                if (ship != null) {
                    int[] entityIds = new int[ship.getShipBlocks().size()];
                    for (int i = 0; i < ship.getShipBlocks().size(); i++) {
                        entityIds[i] = ship.getShipBlocks().get(i).getEntityId();
                    }
                    thisplugin.getServer().getScheduler().runTaskLater(thisplugin, () -> {
                        thisplugin.getPacketUtils().sendMountPacket(armorStand, entityIds, event.getPlayer());
                    }, 10L);
                }
            }
        }
    }

}
