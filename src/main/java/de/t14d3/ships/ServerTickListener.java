package de.t14d3.ships;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ServerTickListener implements Listener {
    private final Ships plugin;

    public ServerTickListener(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerTick(ServerTickStartEvent event) {
        plugin.getShipManager().tick();
    }
}
