package pl.caltonek.simpleMarry.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;

import java.util.Map;
import java.util.UUID;

public final class MarryGiftCommand implements CommandExecutor {

    private final CacheDatabase cacheDatabase;
    private final ConfigManager configManager;

    public MarryGiftCommand(CacheDatabase cacheDatabase, ConfigManager configManager) {
        this.cacheDatabase = cacheDatabase;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        UUID playerUuid = player.getUniqueId();
        if (!cacheDatabase.isMarried(playerUuid)) {
            player.sendMessage(configManager.getMessage("gift.not-married"));
            return true;
        }

        UUID partnerUuid = cacheDatabase.getPartner(playerUuid);
        Player partnerPlayer = (partnerUuid != null) ? Bukkit.getPlayer(partnerUuid) : null;

        if (partnerPlayer == null || !partnerPlayer.isOnline()) {
            player.sendMessage(configManager.getMessage("gift.partner-offline"));
            return true;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR || handItem.getAmount() <= 0) {
            player.sendMessage(configManager.getMessage("gift.no-item"));
            return true;
        }

        ItemStack giftStack = handItem.clone();
        player.getInventory().setItemInMainHand(null);

        Map<Integer, ItemStack> overflowItems = partnerPlayer.getInventory().addItem(giftStack);
        if (!overflowItems.isEmpty()) {
            for (ItemStack item : overflowItems.values()) {
                partnerPlayer.getWorld().dropItemNaturally(partnerPlayer.getLocation(), item);
            }
        }

        player.sendMessage(configManager.getMessage("gift.sent", "target", partnerPlayer.getName()));
        partnerPlayer.sendMessage(configManager.getMessage("gift.received", "sender", player.getName()));
        return true;
    }
}