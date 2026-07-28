package pl.caltonek.simpleMarry.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;
import pl.caltonek.simpleMarry.model.Marriage;

import java.util.List;
import java.util.UUID;

public final class MarryListCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 10;

    private final CacheDatabase cacheDatabase;
    private final ConfigManager configManager;

    public MarryListCommand(CacheDatabase cacheDatabase, ConfigManager configManager) {
        this.cacheDatabase = cacheDatabase;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<Marriage> marriages = cacheDatabase.getOrderedMarriages();

        if (marriages.isEmpty()) {
            sender.sendMessage(configManager.getMessage("marrylist.empty"));
            return true;
        }

        int totalItems = marriages.size();
        int maxPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(configManager.getMessage("marrylist.invalid-page", "max_page", String.valueOf(maxPages)));
                return true;
            }
        }

        if (page < 1 || page > maxPages) {
            sender.sendMessage(configManager.getMessage("marrylist.invalid-page", "max_page", String.valueOf(maxPages)));
            return true;
        }

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalItems);

        sender.sendMessage(configManager.getMessage("marrylist.header",
                "page", String.valueOf(page),
                "max_page", String.valueOf(maxPages)
        ));

        for (int i = startIndex; i < endIndex; i++) {
            Marriage marriage = marriages.get(i);
            String p1Name = resolvePlayerName(marriage.player1());
            String p2Name = resolvePlayerName(marriage.player2());

            sender.sendMessage(configManager.getMessage("marrylist.format",
                    "index", String.valueOf(i + 1),
                    "player1", p1Name,
                    "player2", p2Name
            ));
        }

        sender.sendMessage(configManager.getMessage("marrylist.footer",
                "page", String.valueOf(page),
                "max_page", String.valueOf(maxPages)
        ));

        return true;
    }

    private String resolvePlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        return (name != null) ? name : uuid.toString();
    }
}