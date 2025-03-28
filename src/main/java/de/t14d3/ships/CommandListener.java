package de.t14d3.ships;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class CommandListener implements CommandExecutor, TabCompleter {
    private final Ships plugin;

    public CommandListener() {
        this.plugin = Ships.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (args[0]) {
            case "pos1":
                if (sender instanceof Player player) {
                    Location loc = player.getLocation();
                    player.setMetadata("pos1", new FixedMetadataValue(plugin, loc));
                }
                break;
            case "pos2":
                if (sender instanceof Player player) {
                    Location loc = player.getLocation();
                    player.setMetadata("pos2", new FixedMetadataValue(plugin, loc));
                }
                break;
            case "center":
                if (sender instanceof Player player) {
                    Location loc = player.getLocation();
                    player.setMetadata("center", new FixedMetadataValue(plugin, loc));
                }
                break;
            case "multi":
                plugin.getConverter().convert((Player) sender);
                break;
            case "recreate":
                plugin.getPacketUtils().recreateShip((ArmorStand) ((Player) sender).rayTraceEntities(10).getHitEntity());
                break;
            case "save":
                plugin.getShipManager().save();
                break;
            default:
                sender.sendMessage("Invalid command");
                return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = List.of("pos1", "pos2" ,"center", "multi", "recreate");
        return completions;
    }
}
