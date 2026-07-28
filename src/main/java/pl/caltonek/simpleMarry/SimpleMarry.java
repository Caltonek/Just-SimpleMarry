package pl.caltonek.simpleMarry;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pl.caltonek.simpleMarry.addon.MarryAddon;
import pl.caltonek.simpleMarry.command.MarryCommand;
import pl.caltonek.simpleMarry.command.MarryDivorceCommand;
import pl.caltonek.simpleMarry.command.MarryGiftCommand;
import pl.caltonek.simpleMarry.command.MarryTpCommand;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;
import pl.caltonek.simpleMarry.database.LocalDatabase;
import pl.caltonek.simpleMarry.listener.MarryChatListener;

import java.util.concurrent.TimeUnit;

public final class SimpleMarry extends JavaPlugin {

    private ConfigManager configManager;
    private LocalDatabase localDatabase;
    private CacheDatabase cacheDatabase;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigs();

        this.localDatabase = new LocalDatabase(getDataFolder(), getLogger());
        try {
            this.localDatabase.init();
        } catch (Exception e) {
            getLogger().severe("DB initialization failed: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.cacheDatabase = new CacheDatabase(this, localDatabase, configManager);
        this.cacheDatabase.loadAsync();

        registerCommands();

        getServer().getPluginManager().registerEvents(new MarryChatListener(cacheDatabase, configManager), this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new MarryAddon(this, cacheDatabase, configManager).register();
        }

        long intervalMinutes = configManager.getFlushIntervalMinutes();
        getServer().getAsyncScheduler().runAtFixedRate(
                this,
                task -> cacheDatabase.flush(),
                intervalMinutes, intervalMinutes, TimeUnit.MINUTES
        );
    }

    private void registerCommands() {
        registerCommand("marry", new MarryCommand(cacheDatabase, configManager));
        registerCommand("marrydivorce", new MarryDivorceCommand(cacheDatabase, configManager));
        registerCommand("marrygift", new MarryGiftCommand(cacheDatabase, configManager));
        registerCommand("marrytp", new MarryTpCommand(cacheDatabase, configManager));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("Command '" + name + "' not found in plugin.yml!");
        }
    }

    @Override
    public void onDisable() {
        if (cacheDatabase != null) {
            cacheDatabase.flush();
        }
        if (localDatabase != null) {
            localDatabase.close();
        }
    }
}