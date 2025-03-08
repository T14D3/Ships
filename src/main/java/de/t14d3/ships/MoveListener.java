package de.t14d3.ships;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.minecraft.world.entity.player.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.util.Vector;

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
                if (input.left())
                    direction.add(player.getLocation().getDirection().clone().setY(0).normalize().crossProduct(new Vector(0, 1, 0)).multiply(-0.1));
                if (input.right())
                    direction.add(player.getLocation().getDirection().clone().setY(0).normalize().crossProduct(new Vector(0, 1, 0)).multiply(0.1));
                if (input.forward())
                    direction.add(player.getLocation().getDirection().clone().setY(0).normalize().multiply(0.1));
                if (input.backward())
                    direction.add(player.getLocation().getDirection().clone().setY(0).normalize().multiply(-0.1));
                if (input.jump()) direction.add(new Vector(0, 0.1, 0));
                if (input.sprint()) direction.add(new Vector(0, -0.1, 0));

                ship.setVector(direction);
            }

        });
    }

}
