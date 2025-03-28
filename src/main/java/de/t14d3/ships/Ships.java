package de.t14d3.ships;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Ships extends JavaPlugin {
    private static Ships instance;
    private CommandListener commandListener;
    private ShipPickupListeners shipPickupListeners;
    private InteractListener interactListener;
    private ShipManager shipManager;
    private ChunkLoadEvent chunkLoadEvent;
    private ProtocolManager protocolManager;
    private MoveListener moveListener;
    private Team collisionTeam;
    private PacketUtils packetUtils;
    private Converter converter;

    private long mainThreadId;

    public static Ships getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        mainThreadId = Thread.currentThread().threadId();
        // Plugin startup logic
        instance = this;

        protocolManager = ProtocolLibrary.getProtocolManager();

        commandListener = new CommandListener();
        getCommand("ships").setExecutor(commandListener);
        getCommand("ships").setTabCompleter(commandListener);
        shipPickupListeners = new ShipPickupListeners(this);
        getServer().getPluginManager().registerEvents(shipPickupListeners, this);
        interactListener = new InteractListener(this);
        getServer().getPluginManager().registerEvents(interactListener, this);
        chunkLoadEvent = new ChunkLoadEvent(this);
        getServer().getPluginManager().registerEvents(chunkLoadEvent, this);

        getServer().getPluginManager().registerEvents(new ServerTickListener(this), this);

        shipManager = new ShipManager(this);
        moveListener = new MoveListener(this);
        getServer().getPluginManager().registerEvents(moveListener, this);

        packetUtils = new PacketUtils(this);

        converter = new Converter(this);

        getServer().getPluginManager().registerEvents(new CollisionTeamListener(), this);
    }

    @Override
    public void onDisable() {
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

    public Converter getConverter() {
        return converter;
    }

    public long getMainThreadId() {
        return mainThreadId;
    }
}
