package pl.caltonek.simpleMarry.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;

import java.util.Objects;
import java.util.UUID;

public final class MarryTpCommand implements CommandExecutor {

    private final @NotNull CacheDatabase cacheDatabase;
    private final @NotNull ConfigManager configManager;

    public MarryTpCommand(final @NotNull CacheDatabase cacheDatabase, final @NotNull ConfigManager configManager) {
        this.cacheDatabase = Objects.requireNonNull(cacheDatabase, "cacheDatabase cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;

        if (!configManager.isTpEnabled()) {
            player.sendMessage(configManager.getMessage("tp.disabled"));
            return true;
        }

        final UUID playerUuid = player.getUniqueId();
        if (!cacheDatabase.isMarried(playerUuid)) {
            player.sendMessage(configManager.getMessage("tp.not-married"));
            return true;
        }

        final @Nullable UUID partnerUuid = cacheDatabase.getPartner(playerUuid);
        final @Nullable Player partnerPlayer = (partnerUuid != null) ? Bukkit.getPlayer(partnerUuid) : null;

        if (partnerPlayer == null || !partnerPlayer.isOnline()) {
            player.sendMessage(configManager.getMessage("tp.partner-offline"));
            return true;
        }

        player.teleportAsync(partnerPlayer.getLocation()).thenAccept(teleportSuccess -> {
            if (teleportSuccess) {
                player.sendMessage(configManager.getMessage("tp.success-sender", "target", partnerPlayer.getName()));
                partnerPlayer.sendMessage(configManager.getMessage("tp.success-target", "sender", player.getName()));
            } else {
                player.sendMessage(configManager.getMessage("tp.failed"));
            }
        });

        return true;
    }
}