package pl.caltonek.simpleMarry;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.caltonek.simpleMarry.command.*;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;
import pl.caltonek.simpleMarry.database.LocalDatabase;
import pl.caltonek.simpleMarry.listener.MarryChatListener;
import pl.caltonek.simpleMarry.listener.MarryKissListener;
import pl.caltonek.simpleMarry.listener.PaperMarryChatListener;

import java.util.concurrent.TimeUnit;

public final class SimpleMarry extends JavaPlugin {

    private @Nullable ConfigManager configManager;
    private @Nullable LocalDatabase localDatabase;
    private @Nullable CacheDatabase cacheDatabase;

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
        registerChatListeners();

        getServer().getPluginManager().registerEvents(new MarryKissListener(cacheDatabase, configManager), this);

        final int batchInterval = configManager.getBatchIntervalSeconds();
        getServer().getAsyncScheduler().runAtFixedRate(
                this,
                task -> {
                    if (cacheDatabase != null) {
                        cacheDatabase.flush();
                    }
                },
                batchInterval, batchInterval, TimeUnit.SECONDS
        );
    }

    private void registerChatListeners() {
        if (cacheDatabase == null || configManager == null) return;

        getServer().getPluginManager().registerEvents(new MarryChatListener(cacheDatabase, configManager), this);

        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            getServer().getPluginManager().registerEvents(new PaperMarryChatListener(cacheDatabase, configManager), this);
        } catch (ClassNotFoundException ignored) {}
    }

    private void registerCommands() {
        if (cacheDatabase == null || configManager == null) return;

        registerCommand("marry", new MarryCommand(cacheDatabase, configManager));
        registerCommand("marrydivorce", new MarryDivorceCommand(cacheDatabase, configManager));
        registerCommand("marrygift", new MarryGiftCommand(cacheDatabase, configManager));
        registerCommand("marrytp", new MarryTpCommand(cacheDatabase, configManager));
        registerCommand("marrylist", new MarryListCommand(cacheDatabase, configManager));
        registerCommand("marrychat", new MarryChatCommand(cacheDatabase, configManager));
        registerCommand("marryreload", new ReloadCommand(configManager));
    }

    private void registerCommand(final @NotNull String name, final @NotNull CommandExecutor executor) {
        final PluginCommand command = getCommand(name);
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