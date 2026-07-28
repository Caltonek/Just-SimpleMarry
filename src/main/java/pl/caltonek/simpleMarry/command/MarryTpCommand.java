package pl.caltonek.simpleMarry.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;

import java.util.UUID;

public final class MarryTpCommand implements CommandExecutor {

    private final CacheDatabase cacheDatabase;
    private final ConfigManager configManager;

    public MarryTpCommand(CacheDatabase cacheDatabase, ConfigManager configManager) {
        this.cacheDatabase = cacheDatabase;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        UUID playerUuid = player.getUniqueId();
        if (!cacheDatabase.isMarried(playerUuid)) {
            player.sendMessage(configManager.getMessage("tp.not-married"));
            return true;
        }

        UUID partnerUuid = cacheDatabase.getPartner(playerUuid);
        Player partnerPlayer = (partnerUuid != null) ? Bukkit.getPlayer(partnerUuid) : null;

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