package pl.caltonek.simpleMarry.listener;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;

public final class MarryChatListener implements Listener {

    private final CacheDatabase cacheDatabase;
    private final ConfigManager configManager;

    public MarryChatListener(CacheDatabase cacheDatabase, ConfigManager configManager) {
        this.cacheDatabase = cacheDatabase;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String format = event.getFormat();

        if (format.contains("{marriage-status}") || format.contains("{status}")) {
            String rawSymbol = cacheDatabase.isMarried(event.getPlayer().getUniqueId())
                    ? configManager.getMarriedSymbol()
                    : configManager.getSingleSymbol();

            String symbol = ChatColor.translateAlternateColorCodes('&', rawSymbol);

            format = format.replace("{marriage-status}", symbol)
                    .replace("{status}", symbol);

            event.setFormat(format);
        }
    }
}