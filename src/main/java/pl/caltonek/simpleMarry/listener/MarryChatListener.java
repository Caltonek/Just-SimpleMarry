package pl.caltonek.simpleMarry.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;
import pl.caltonek.simpleMarry.util.ColorUtil;

public final class MarryChatListener implements Listener {

    private final CacheDatabase cacheDatabase;
    private final ConfigManager configManager;

    public MarryChatListener(CacheDatabase cacheDatabase, ConfigManager configManager) {
        this.cacheDatabase = cacheDatabase;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String format = event.getFormat();

        if (format.contains("{marriage-status}")) {
            String rawSymbol = cacheDatabase.isMarried(event.getPlayer().getUniqueId())
                    ? configManager.getMarriedSymbol()
                    : configManager.getSingleSymbol();

            String symbol = ColorUtil.colorize(rawSymbol);

            event.setFormat(format.replace("{marriage-status}", symbol));
        }
    }
}