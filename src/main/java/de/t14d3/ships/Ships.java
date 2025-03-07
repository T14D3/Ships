package de.t14d3.ships;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Ships extends JavaPlugin {
    private static Ships instance;
    private CommandListener commandListener;
    private Converter converter;
    private DeathListener deathListener;
    private InteractListener interactListener;
    private ShipManager shipManager;
    private ChunkLoadEvent chunkLoadEvent;
    private ProtocolManager protocolManager;
    private MoveListener moveListener;

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
        converter = new Converter(this);
        deathListener = new DeathListener(this);
        getServer().getPluginManager().registerEvents(deathListener, this);
        interactListener = new InteractListener(this);
        getServer().getPluginManager().registerEvents(interactListener, this);
        chunkLoadEvent = new ChunkLoadEvent(this);
        getServer().getPluginManager().registerEvents(chunkLoadEvent, this);

        shipManager = new ShipManager();
        moveListener = new MoveListener(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Converter getConverter() {
        return converter;
    }

    public ShipManager getShipManager() {
        return shipManager;
    }
}
