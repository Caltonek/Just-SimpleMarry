package pl.caltonek.simpleMarry.config;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.caltonek.simpleMarry.SimpleMarry;
import pl.caltonek.simpleMarry.util.ColorUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConfigManager {

    private final @NotNull SimpleMarry plugin;
    private @Nullable FileConfiguration messagesConfig;

    private int batchIntervalSeconds;
    private int batchThreshold;

    private long divorceCooldownMs;
    private long proposalExpirationMs;
    private boolean tpEnabled;
    private boolean giftEnabled;

    private boolean kissEnabled;
    private long kissCooldownMs;
    private double kissMaxDistanceSq;
    private boolean kissSoundEnabled;
    private @NotNull String kissSound = "entity.chicken.step";
    private float kissSoundVolume;
    private float kissSoundPitch;

    private @NotNull String marriedSymbol = "&#C21E56❤ ";
    private @NotNull String singleSymbol = "";

    public ConfigManager(final @NotNull SimpleMarry plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        final FileConfiguration mainConfig = plugin.getConfig();
        this.batchIntervalSeconds = mainConfig.getInt("database.batch-interval-seconds", 30);
        this.batchThreshold = mainConfig.getInt("database.batch-threshold", 10);

        this.divorceCooldownMs = mainConfig.getLong("settings.divorce-cooldown-seconds", 10) * 1000L;
        this.proposalExpirationMs = mainConfig.getLong("settings.proposal-expiration-seconds", 300) * 1000L;
        this.tpEnabled = mainConfig.getBoolean("settings.tp-enabled", true);
        this.giftEnabled = mainConfig.getBoolean("settings.gift-enabled", true);

        this.kissEnabled = mainConfig.getBoolean("settings.kiss-enabled", true);
        this.kissCooldownMs = mainConfig.getLong("settings.kiss-cooldown-seconds", 2) * 1000L;
        final double kissDistance = mainConfig.getDouble("settings.kiss-max-distance", 3.0);
        this.kissMaxDistanceSq = kissDistance * kissDistance;

        this.kissSoundEnabled = mainConfig.getBoolean("settings.kiss-sound-enabled", true);
        this.kissSound = mainConfig.getString("settings.kiss-sound", "entity.chicken.step");
        this.kissSoundVolume = (float) mainConfig.getDouble("settings.kiss-sound-volume", 1.0);
        this.kissSoundPitch = (float) mainConfig.getDouble("settings.kiss-sound-pitch", 1.8);

        this.marriedSymbol = mainConfig.getString("placeholders.married-symbol", "&#C21E56❤ ");
        this.singleSymbol = mainConfig.getString("placeholders.single-symbol", "");

        final File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public @NotNull String getRawMessage(final @NotNull String path) {
        if (messagesConfig == null) return "<red>Missing messages.yml configuration!";
        return messagesConfig.getString(path, "<red>Missing message: " + path);
    }

    public @NotNull Component getMessage(final @NotNull String path, final @NotNull String... placeholders) {
        String rawMessage = getRawMessage(path);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            rawMessage = rawMessage.replace("<" + placeholders[i] + ">", placeholders[i + 1]);
        }
        return ColorUtil.color(rawMessage);
    }

    public @NotNull List<Component> getMessageList(final @NotNull String path, final @NotNull String... placeholders) {
        if (messagesConfig == null) return List.of();

        final List<String> rawList = messagesConfig.getStringList(path);
        final List<Component> formattedList = new ArrayList<>(rawList.size());

        for (String line : rawList) {
            for (int i = 0; i < placeholders.length - 1; i += 2) {
                line = line.replace("<" + placeholders[i] + ">", placeholders[i + 1]);
            }
            formattedList.add(ColorUtil.color(line));
        }
        return formattedList;
    }

    public int getBatchIntervalSeconds() { return batchIntervalSeconds; }
    public int getBatchThreshold() { return batchThreshold; }
    public long getDivorceCooldownMs() { return divorceCooldownMs; }
    public long getProposalExpirationMs() { return proposalExpirationMs; }
    public boolean isTpEnabled() { return tpEnabled; }
    public boolean isGiftEnabled() { return giftEnabled; }

    public boolean isKissEnabled() { return kissEnabled; }
    public long getKissCooldownMs() { return kissCooldownMs; }
    public double getKissMaxDistanceSq() { return kissMaxDistanceSq; }
    public boolean isKissSoundEnabled() { return kissSoundEnabled; }
    public @NotNull String getKissSound() { return kissSound; }
    public float getKissSoundVolume() { return kissSoundVolume; }
    public float getKissSoundPitch() { return kissSoundPitch; }

    public @NotNull String getMarriedSymbol() { return marriedSymbol; }
    public @NotNull String getSingleSymbol() { return singleSymbol; }
}