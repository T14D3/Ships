package de.t14d3.ships;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ChunkLoadEvent implements Listener {
    private final Ships plugin;

    public ChunkLoadEvent(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, ()
                -> plugin.getShipManager().cleanup(List.of(event.getChunk().getEntities()))
        );
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ArmorStand armorStand) {
                if (armorStand.getPersistentDataContainer().has(ShipManager.booleanShipKey, PersistentDataType.STRING)) {
                    plugin.getPacketUtils().recreateShip(armorStand);
                }
            }
        }
    }
}
