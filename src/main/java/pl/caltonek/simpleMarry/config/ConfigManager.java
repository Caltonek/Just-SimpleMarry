package pl.caltonek.simpleMarry.config;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.caltonek.simpleMarry.SimpleMarry;
import pl.caltonek.simpleMarry.util.ColorUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ConfigManager {

    private final SimpleMarry plugin;
    private FileConfiguration messagesConfig;

    private int flushIntervalMinutes;
    private long divorceCooldownMs;
    private long proposalExpirationMs;

    private String marriedSymbol;
    private String singleSymbol;

    public ConfigManager(SimpleMarry plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration mainConfig = plugin.getConfig();
        this.flushIntervalMinutes = mainConfig.getInt("database.flush-interval-minutes", 5);
        this.divorceCooldownMs = mainConfig.getLong("settings.divorce-cooldown-seconds", 10) * 1000L;
        this.proposalExpirationMs = mainConfig.getLong("settings.proposal-expiration-seconds", 300) * 1000L;

        this.marriedSymbol = mainConfig.getString("placeholders.married-symbol", "&#C21E56❤ ");
        this.singleSymbol = mainConfig.getString("placeholders.single-symbol", "");

        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getRawMessage(String path) {
        return messagesConfig.getString(path, "<red>Missing message: " + path);
    }

    public Component getMessage(String path, String... placeholders) {
        String rawMessage = messagesConfig.getString(path, "<red>Missing message: " + path);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            rawMessage = rawMessage.replace("<" + placeholders[i] + ">", placeholders[i + 1]);
        }
        return ColorUtil.color(rawMessage);
    }

    public List<Component> getMessageList(String path, String... placeholders) {
        List<String> rawList = messagesConfig.getStringList(path);
        List<Component> formattedList = new ArrayList<>(rawList.size());

        for (String line : rawList) {
            for (int i = 0; i < placeholders.length - 1; i += 2) {
                line = line.replace("<" + placeholders[i] + ">", placeholders[i + 1]);
            }
            formattedList.add(ColorUtil.color(line));
        }
        return formattedList;
    }

    public int getFlushIntervalMinutes() { return flushIntervalMinutes; }
    public long getDivorceCooldownMs() { return divorceCooldownMs; }
    public long getProposalExpirationMs() { return proposalExpirationMs; }
    public String getMarriedSymbol() { return marriedSymbol; }
    public String getSingleSymbol() { return singleSymbol; }
}