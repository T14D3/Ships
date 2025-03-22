package de.t14d3.ships;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class InteractListener implements Listener {
    private final Ships plugin;

    public InteractListener(Ships plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand armorStand) {
            if (armorStand.getPersistentDataContainer().has(new NamespacedKey(plugin, "ship"), PersistentDataType.STRING)) {
                Ship ship = plugin.getShipManager().getShip(armorStand);
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
        } else if (event.getRightClicked() instanceof Shulker shulker) {
            Ship ship = plugin.getShipManager().getShipFromShulker(shulker).join();
            if (ship != null) {
                if (ship.getController() == null) {
                    ship.setController(event.getPlayer());
                }
                shulker.addPassenger(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getItem() != null) {
            if (event.getItem().getType() == Material.PAPER) {
                if (event.getClickedBlock() != null) {
                    if (!event.getPlayer().isSneaking()) {
                        List<Block> blocks = CompletableFuture.supplyAsync(()
                                -> plugin.getConverter().getConnectedBlocks(event.getClickedBlock(), 20)).join();
                        if (!blocks.isEmpty()) {
                            plugin.getPacketUtils().highlightBlocks(event.getPlayer(), blocks);
                            event.getPlayer().sendMessage("Highlighted " + plugin.getPacketUtils().getPlayerSelection(event.getPlayer()).getBlocks().size() + " blocks");
                        }
                    }
                } else {
                    plugin.getPacketUtils().removeEntities(event.getPlayer());
                }
                if (event.getPlayer().isSneaking() && event.getClickedBlock() != null) {
                    plugin.getPacketUtils().createFromBlocklist(event.getPlayer(), event.getClickedBlock().getLocation());
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
            }
            if (event.getDismounted() instanceof ArmorStand seat) {
                if (Objects.equals(seat.customName(), Component.text("Seat"))) {
                    seat.remove();
                    player.teleportAsync(player.getLocation().add(0, 1, 0));
                }
            }
        }
    }
}
