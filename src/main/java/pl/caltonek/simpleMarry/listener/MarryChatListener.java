package pl.caltonek.simpleMarry.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;
import pl.caltonek.simpleMarry.util.ColorUtil;

import java.util.Objects;
import java.util.UUID;

public final class MarryChatListener implements Listener {

    private final @NotNull CacheDatabase cacheDatabase;
    private final @NotNull ConfigManager configManager;

    public MarryChatListener(final @NotNull CacheDatabase cacheDatabase, final @NotNull ConfigManager configManager) {
        this.cacheDatabase = Objects.requireNonNull(cacheDatabase, "cacheDatabase cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(final @NotNull AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();

        if (cacheDatabase.isMarryChatToggled(player.getUniqueId())) {
            event.setCancelled(true);

            final UUID partnerUuid = cacheDatabase.getPartner(player.getUniqueId());
            if (partnerUuid == null) {
                player.sendMessage(configManager.getMessage("divorce.not-married"));
                return;
            }

            final Player partner = Bukkit.getPlayer(partnerUuid);
            if (partner == null || !partner.isOnline()) {
                player.sendMessage(configManager.getMessage("gift.partner-offline"));
                return;
            }

            final String rawMessage = event.getMessage();

            final Component senderMsg = configManager.getMessage("marrychat.format-sender",
                    "sender", player.getName(),
                    "target", partner.getName(),
                    "message", rawMessage);
            final Component receiverMsg = configManager.getMessage("marrychat.format-receiver",
                    "sender", player.getName(),
                    "target", partner.getName(),
                    "message", rawMessage);

            player.sendMessage(senderMsg);
            partner.sendMessage(receiverMsg);
            return;
        }

        final String format = event.getFormat();
        if (format.contains("{marriage-status}")) {
            final String rawSymbol = cacheDatabase.isMarried(player.getUniqueId())
                    ? configManager.getMarriedSymbol()
                    : configManager.getSingleSymbol();

            final String symbol = ColorUtil.colorize(rawSymbol);
            event.setFormat(format.replace("{marriage-status}", symbol));
        }
    }
}