package pl.caltonek.simpleMarry.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;

import java.util.Objects;

public final class MarryChatCommand implements CommandExecutor {

    private final @NotNull CacheDatabase cacheDatabase;
    private final @NotNull ConfigManager configManager;

    public MarryChatCommand(final @NotNull CacheDatabase cacheDatabase, final @NotNull ConfigManager configManager) {
        this.cacheDatabase = Objects.requireNonNull(cacheDatabase, "cacheDatabase cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!cacheDatabase.isMarried(player.getUniqueId())) {
            player.sendMessage(configManager.getMessage("marrychat.not-married"));
            return true;
        }

        final boolean enabled = cacheDatabase.toggleMarryChat(player.getUniqueId());
        if (enabled) {
            player.sendMessage(configManager.getMessage("marrychat.enabled"));
        } else {
            player.sendMessage(configManager.getMessage("marrychat.disabled"));
        }

        return true;
    }
}