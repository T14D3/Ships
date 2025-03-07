package de.t14d3.ships;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import com.comphenix.protocol.ProtocolManager;

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
                PacketType.Play.Server.VEHICLE_MOVE
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                    Player player = event.getPlayer();

                    Ship ship = thisplugin.getShipManager().getShip(player.getUniqueId());
                    if (ship == null) return;

                    // Extract steering and movement data
                    boolean left = event.getPacket().getBooleans().read(0);
                    boolean right = event.getPacket().getBooleans().read(1);
                    boolean forward = event.getPacket().getBooleans().read(2);
                    boolean backward = event.getPacket().getBooleans().read(3);

                    // Handle the steering direction
                    Vector direction = new Vector();
                    if (left) direction.add(new Vector(-1, 0, 0));
                    if (right) direction.add(new Vector(1, 0, 0));
                    if (forward) direction.add(new Vector(0, 0, 1));
                    if (backward) direction.add(new Vector(0, 0, -1));

                    ship.move(direction);

                }

        });
    }
}
