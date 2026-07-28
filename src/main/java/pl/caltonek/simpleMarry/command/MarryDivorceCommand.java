package pl.caltonek.simpleMarry.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;

import java.util.UUID;

public final class MarryDivorceCommand implements CommandExecutor {

    private final CacheDatabase cacheDatabase;
    private final ConfigManager configManager;

    public MarryDivorceCommand(CacheDatabase cacheDatabase, ConfigManager configManager) {
        this.cacheDatabase = cacheDatabase;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        UUID playerUuid = player.getUniqueId();
        if (!cacheDatabase.isMarried(playerUuid)) {
            player.sendMessage(configManager.getMessage("divorce.not-married"));
            return true;
        }

        UUID partnerUuid = cacheDatabase.getPartner(playerUuid);
        String partnerName = "Nieznany";

        if (partnerUuid != null) {
            Player onlinePartner = Bukkit.getPlayer(partnerUuid);
            if (onlinePartner != null) {
                partnerName = onlinePartner.getName();
            } else {
                OfflinePlayer offlinePartner = Bukkit.getOfflinePlayer(partnerUuid);
                String name = offlinePartner.getName();
                partnerName = (name != null) ? name : partnerUuid.toString();
            }
            cacheDatabase.setDivorceCooldown(partnerUuid);
        }

        cacheDatabase.setDivorceCooldown(playerUuid);
        cacheDatabase.divorce(playerUuid);

        player.sendMessage(configManager.getMessage("divorce.success-sender", "target", partnerName));

        if (partnerUuid != null) {
            Player partnerPlayer = Bukkit.getPlayer(partnerUuid);
            if (partnerPlayer != null && partnerPlayer.isOnline()) {
                partnerPlayer.sendMessage(configManager.getMessage("divorce.success-target", "sender", player.getName()));
            }
        }

        Bukkit.broadcast(configManager.getMessage("divorce.broadcast-success", "player1", player.getName(), "player2", partnerName));
        return true;
    }
}