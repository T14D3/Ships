package de.t14d3.ships;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.UUID;

public class InteractListener implements Listener {
    private final Ships plugin;

    public InteractListener(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand armorStand) {
            if (armorStand.getPersistentDataContainer().has(new NamespacedKey(plugin, "ship"), PersistentDataType.STRING)) {
                Ship ship = plugin.getShipManager().getShip(armorStand.getUniqueId());
                if (ship != null) {
                    ship.setController(event.getPlayer());
                    ShipBlock shipBlock = ship.getClosestBlock(event.getPlayer().getLocation());
                    if (shipBlock != null) {
                        ArmorStand seat = shipBlock.getSeat();
                        if (seat != null) {
                            seat.addPassenger(event.getPlayer());
                        } else {
                            seat = (ArmorStand) event.getPlayer().getWorld().spawnEntity(shipBlock.getLocation(ship).add(0.5, 0, 0.5), EntityType.ARMOR_STAND);
                            seat.setGravity(false);
                            seat.setInvulnerable(true);
                            seat.setInvisible(true);
                            seat.setSmall(true);
                            seat.customName(Component.text("Seat"));
                            shipBlock.setSeat(seat);
                            seat.addPassenger(event.getPlayer());
                        }
                    }
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
                    if (Objects.equals(seat.customName(), Component.text("Seat"))) {
                        seat.remove();
                        player.teleportAsync(player.getLocation().add(0, 1, 0));
                    }
                }
            }
        }
    }
}
