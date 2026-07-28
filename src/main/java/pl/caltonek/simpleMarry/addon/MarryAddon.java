package pl.caltonek.simpleMarry.addon;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.caltonek.simpleMarry.SimpleMarry;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;

import java.util.UUID;

public final class MarryAddon extends PlaceholderExpansion {

    private final SimpleMarry plugin;
    private final CacheDatabase cacheDatabase;
    private final ConfigManager configManager;

    public MarryAddon(SimpleMarry plugin, CacheDatabase cacheDatabase, ConfigManager configManager) {
        this.plugin = plugin;
        this.cacheDatabase = cacheDatabase;
        this.configManager = configManager;
    }

    @Override
    public String getIdentifier() {
        return "simplemarry";
    }

    @Override
    public String getAuthor() {
        return "caltonek";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null) return "";

        UUID playerUuid = offlinePlayer.getUniqueId();

        if (params.equalsIgnoreCase("status") || params.equalsIgnoreCase("marriage-status")) {
            return cacheDatabase.isMarried(playerUuid)
                    ? configManager.getMarriedSymbol()
                    : configManager.getSingleSymbol();
        }

        if (params.equalsIgnoreCase("partner")) {
            UUID partnerUuid = cacheDatabase.getPartner(playerUuid);
            if (partnerUuid == null) return "";

            Player onlinePartner = Bukkit.getPlayer(partnerUuid);
            if (onlinePartner != null) return onlinePartner.getName();

            OfflinePlayer offlinePartner = Bukkit.getOfflinePlayer(partnerUuid);
            String partnerName = offlinePartner.getName();
            return partnerName != null ? partnerName : partnerUuid.toString();
        }

        return null;
    }
}