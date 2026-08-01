package pl.caltonek.simpleMarry.listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;
import pl.caltonek.simpleMarry.util.ColorUtil;

import java.util.Objects;
import java.util.UUID;

public final class PaperMarryChatListener implements Listener {

    private final @NotNull CacheDatabase cacheDatabase;
    private final @NotNull ConfigManager configManager;

    public PaperMarryChatListener(final @NotNull CacheDatabase cacheDatabase, final @NotNull ConfigManager configManager) {
        this.cacheDatabase = Objects.requireNonNull(cacheDatabase, "cacheDatabase cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(final @NotNull AsyncChatEvent event) {
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

            final String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

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

        final ChatRenderer originalRenderer = event.renderer();
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            final Component rendered = originalRenderer.render(source, sourceDisplayName, message, viewer);
            return replacePlaceholders(rendered, source);
        });
    }

    private @NotNull Component replacePlaceholders(final @NotNull Component component, final @NotNull Player player) {
        final String rawSymbol = cacheDatabase.isMarried(player.getUniqueId())
                ? configManager.getMarriedSymbol()
                : configManager.getSingleSymbol();

        final Component symbolComponent = ColorUtil.color(rawSymbol);

        return component.replaceText(TextReplacementConfig.builder()
                .matchLiteral("{marriage-status}")
                .replacement(symbolComponent)
                .build());
    }
}