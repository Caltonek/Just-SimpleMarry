package pl.caltonek.simpleMarry.listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;
import pl.caltonek.simpleMarry.util.ColorUtil;

import java.util.UUID;

public final class PaperMarryChatListener implements Listener {

    private final CacheDatabase cacheDatabase;
    private final ConfigManager configManager;

    public PaperMarryChatListener(CacheDatabase cacheDatabase, ConfigManager configManager) {
        this.cacheDatabase = cacheDatabase;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        ChatRenderer originalRenderer = event.renderer();

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component rendered = originalRenderer.render(source, sourceDisplayName, message, viewer);
            return replacePlaceholders(rendered, source);
        });
    }

    private Component replacePlaceholders(Component component, Player player) {
        String rawSymbol = cacheDatabase.isMarried(player.getUniqueId())
                ? configManager.getMarriedSymbol()
                : configManager.getSingleSymbol();

        Component symbolComponent = ColorUtil.color(rawSymbol);

        component = component.replaceText(TextReplacementConfig.builder()
                .matchLiteral("{marriage-status}")
                .replacement(symbolComponent)
                .build());

        UUID partnerUuid = cacheDatabase.getPartner(player.getUniqueId());
        String partnerName = "";
        if (partnerUuid != null) {
            Player onlinePartner = Bukkit.getPlayer(partnerUuid);
            if (onlinePartner != null) {
                partnerName = onlinePartner.getName();
            } else {
                OfflinePlayer offlinePartner = Bukkit.getOfflinePlayer(partnerUuid);
                if (offlinePartner.getName() != null) {
                    partnerName = offlinePartner.getName();
                }
            }
        }

        component = component.replaceText(TextReplacementConfig.builder()
                .matchLiteral("{partner}")
                .replacement(partnerName)
                .build());

        return component;
    }
}