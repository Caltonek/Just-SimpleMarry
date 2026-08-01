package pl.caltonek.simpleMarry.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

public final class MarryDivorceCommand implements CommandExecutor {

    private final @NotNull CacheDatabase cacheDatabase;
    private final @NotNull ConfigManager configManager;

    public MarryDivorceCommand(final @NotNull CacheDatabase cacheDatabase, final @NotNull ConfigManager configManager) {
        this.cacheDatabase = Objects.requireNonNull(cacheDatabase, "cacheDatabase cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;

        final UUID playerUuid = player.getUniqueId();
        if (!cacheDatabase.isMarried(playerUuid)) {
            player.sendMessage(configManager.getMessage("divorce.not-married"));
            return true;
        }

        final @Nullable UUID partnerUuid = cacheDatabase.getPartner(playerUuid);
        String partnerName = "Nieznany";

        if (partnerUuid != null) {
            final @Nullable Player onlinePartner = Bukkit.getPlayer(partnerUuid);
            if (onlinePartner != null) {
                partnerName = onlinePartner.getName();
            } else {
                final OfflinePlayer offlinePartner = Bukkit.getOfflinePlayer(partnerUuid);
                final String name = offlinePartner.getName();
                partnerName = (name != null) ? name : partnerUuid.toString();
            }
            cacheDatabase.setDivorceCooldown(partnerUuid);
        }

        cacheDatabase.setDivorceCooldown(playerUuid);
        cacheDatabase.divorce(playerUuid);

        player.sendMessage(configManager.getMessage("divorce.success-sender", "target", partnerName));

        if (partnerUuid != null) {
            final @Nullable Player partnerPlayer = Bukkit.getPlayer(partnerUuid);
            if (partnerPlayer != null && partnerPlayer.isOnline()) {
                partnerPlayer.sendMessage(configManager.getMessage("divorce.success-target", "sender", player.getName()));
            }
        }

        Bukkit.broadcast(configManager.getMessage("divorce.broadcast-success", "player1", player.getName(), "player2", partnerName));
        return true;
    }
}