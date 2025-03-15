package de.t14d3.ships;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

public final class Ships extends JavaPlugin {
    private static Ships instance;
    private CommandListener commandListener;
    private DeathListener deathListener;
    private InteractListener interactListener;
    private ShipManager shipManager;
    private ChunkLoadEvent chunkLoadEvent;
    private ProtocolManager protocolManager;
    private MoveListener moveListener;
    private Team collisionTeam;
    private PacketUtils packetUtils;

    public static Ships getInstance() {
        return instance;
    }
    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        protocolManager = ProtocolLibrary.getProtocolManager();

        commandListener = new CommandListener();
        getCommand("ships").setExecutor(commandListener);
        getCommand("ships").setTabCompleter(commandListener);
        deathListener = new DeathListener(this);
        getServer().getPluginManager().registerEvents(deathListener, this);
        interactListener = new InteractListener(this);
        getServer().getPluginManager().registerEvents(interactListener, this);
        chunkLoadEvent = new ChunkLoadEvent(this);
        getServer().getPluginManager().registerEvents(chunkLoadEvent, this);

        shipManager = new ShipManager(this);
        moveListener = new MoveListener(this);

        packetUtils = new PacketUtils(this);

        getServer().getPluginManager().registerEvents(new CollisionTeamListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public ShipManager getShipManager() {
        return shipManager;
    }

    public Team getCollisionTeam() {
        return collisionTeam;
    }

    private class CollisionTeamListener implements Listener {
        @EventHandler
        private void onWorldLoad(WorldLoadEvent event) {
            if (collisionTeam != null) {
                return;
            }
            collisionTeam = getServer().getScoreboardManager().getMainScoreboard().getTeam("ships-collision");
            if (collisionTeam == null) {
                collisionTeam = getServer().getScoreboardManager().getMainScoreboard().registerNewTeam("ships-collision");
                collisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OTHER_TEAMS);
            }
        }
    }

    public PacketUtils getPacketUtils() {
        return packetUtils;
    }
}
