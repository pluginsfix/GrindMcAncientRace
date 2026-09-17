package pluginsfix.grindmcancientrace;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import pluginsfix.grindmcancientrace.command.AncientRaceCommand;
import pluginsfix.grindmcancientrace.config.ConfigParser;
import pluginsfix.grindmcancientrace.config.PluginConfig;
import pluginsfix.grindmcancientrace.domain.EggManager;
import pluginsfix.grindmcancientrace.domain.TradeGenerator;
import pluginsfix.grindmcancientrace.gui.AncientRaceGui;
import pluginsfix.grindmcancientrace.hook.FancyHologramsHook;
import pluginsfix.grindmcancientrace.hook.PlaceholderApiHook;
import pluginsfix.grindmcancientrace.hook.PlayerPointsHook;
import pluginsfix.grindmcancientrace.hook.VaultEconomyHook;
import pluginsfix.grindmcancientrace.listener.InventoryListener;
import pluginsfix.grindmcancientrace.listener.PlayerEffectListener;
import pluginsfix.grindmcancientrace.listener.SpawnerEggListener;
import pluginsfix.grindmcancientrace.listener.TaskProgressListener;
import pluginsfix.grindmcancientrace.listener.VillagerInteractListener;
import pluginsfix.grindmcancientrace.listener.VillagerLifecycleListener;
import pluginsfix.grindmcancientrace.listener.VillagerSpawnListener;
import pluginsfix.grindmcancientrace.storage.Database;
import pluginsfix.grindmcancientrace.storage.EffectRepository;
import pluginsfix.grindmcancientrace.storage.TaskProgressRepository;
import pluginsfix.grindmcancientrace.storage.TradeCooldownRepository;
import pluginsfix.grindmcancientrace.storage.TradeRepository;
import pluginsfix.grindmcancientrace.text.Messages;

public final class GrindMcAncientRace extends JavaPlugin {
    private PluginConfig pluginConfig;
    private Messages messages;
    private Database database;
    private TradeRepository tradeRepository;
    private TradeCooldownRepository cooldownRepository;
    private EffectRepository effectRepository;
    private TaskProgressRepository taskRepository;
    private VaultEconomyHook vaultHook;
    private PlayerPointsHook pointsHook;
    private FancyHologramsHook hologramsHook;
    private EggManager eggManager;
    private TradeGenerator tradeGenerator;
    private AncientRaceGui gui;
    private PlayerEffectListener effectListener;
    private VillagerSpawnListener spawnListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = ConfigParser.parse(getConfig());
        this.messages = new Messages(this);

        this.database = new Database(getDataFolder(), pluginConfig);
        this.tradeRepository = new TradeRepository(database, pluginConfig, getLogger());
        this.cooldownRepository = new TradeCooldownRepository(database, getLogger());
        this.effectRepository = new EffectRepository(database, getLogger());
        this.taskRepository = new TaskProgressRepository(database, getLogger());

        this.vaultHook = new VaultEconomyHook(this);
        this.pointsHook = new PlayerPointsHook();
        this.hologramsHook = new FancyHologramsHook(this, pluginConfig, messages);

        this.eggManager = new EggManager(this, messages);
        this.tradeGenerator = new TradeGenerator();
        this.gui = new AncientRaceGui(pluginConfig, messages, cooldownRepository);

        this.spawnListener = new VillagerSpawnListener(this, pluginConfig, eggManager, tradeRepository, tradeGenerator, hologramsHook);
        this.effectListener = new PlayerEffectListener(this, pluginConfig, effectRepository);

        PlaceholderApiHook papiHook = null;
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papiHook = new PlaceholderApiHook(this, effectRepository, messages);
            papiHook.register();
        }

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(spawnListener, this);
        pm.registerEvents(new SpawnerEggListener(eggManager, messages), this);
        pm.registerEvents(new VillagerInteractListener(pluginConfig, messages, eggManager, tradeRepository, tradeGenerator, gui, papiHook), this);
        pm.registerEvents(new InventoryListener(this, pluginConfig, messages, vaultHook, pointsHook, effectRepository, taskRepository, cooldownRepository, gui), this);
        pm.registerEvents(effectListener, this);
        pm.registerEvents(new VillagerLifecycleListener(eggManager, hologramsHook, tradeRepository), this);
        pm.registerEvents(new TaskProgressListener(taskRepository), this);

        PluginCommand command = getCommand("ancientrace");
        if (command != null) {
            AncientRaceCommand cmdExecutor = new AncientRaceCommand(
                    this,
                    messages,
                    eggManager,
                    spawnListener,
                    hologramsHook,
                    this::reloadPlugin
            );
            command.setExecutor(cmdExecutor);
            command.setTabCompleter(cmdExecutor);
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        this.pluginConfig = ConfigParser.parse(getConfig());
        this.messages.reload();
        if (this.effectListener != null) {
            this.effectListener.startTask();
        }
        if (this.hologramsHook != null) {
            this.hologramsHook.checkAvailability();
        }
    }

    @Override
    public void onDisable() {
        if (effectListener != null) {
            effectListener.stopTask();
        }
        if (database != null) {
            database.close();
        }
    }
}
