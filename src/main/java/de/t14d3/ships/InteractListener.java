package de.t14d3.ships;

import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.Display;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.entity.CraftBlockDisplay;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.UUID;

public class InteractListener implements Listener {
    private final Ships plugin;

    public InteractListener(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Shulker shulker) {
            UUID shipUuid = UUID.fromString(event.getRightClicked().getPersistentDataContainer().get(new NamespacedKey(plugin, "ship"), PersistentDataType.STRING));
            Ship ship = plugin.getShipManager().getShip(shipUuid);
            if (ship != null) {
                if (ship.getController() == null) {
                    ship.setController(event.getPlayer());
                    ArmorStand seat = (ArmorStand) event.getRightClicked().getWorld().spawnEntity(event.getRightClicked().getLocation(), EntityType.ARMOR_STAND);
                    seat.setGravity(false);
                    seat.setInvulnerable(true);
                    seat.setInvisible(true);
                    seat.setSmall(true);
                    seat.customName(Component.text("Seat"));
                    ship.getBlockDisplay(shulker).addPassenger(seat);
                    seat.addPassenger(event.getPlayer());
                } else {
                    event.getPlayer().sendMessage("Ship is already controlled by " + ship.getController().getName());
                }
            }
        }
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            Ship ship = plugin.getShipManager().getControlledBy(player);
            if (ship != null) {
                ship.setVector(new Vector(0, 0, 0));
                ship.setVectorToRotate(new Vector(0, 0, 0));
                ship.setController(null);
                if (event.getDismounted() instanceof ArmorStand seat) {
                    if (seat.customName().equals(Component.text("Seat"))) {
                        seat.remove();
                        player.teleportAsync(player.getLocation().add(0, 1, 0));
                    }
                }
            }
        }
    }
}
