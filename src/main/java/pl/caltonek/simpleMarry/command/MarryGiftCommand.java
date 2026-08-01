package pl.caltonek.simpleMarry.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MarryGiftCommand implements CommandExecutor {

    private final @NotNull CacheDatabase cacheDatabase;
    private final @NotNull ConfigManager configManager;

    public MarryGiftCommand(final @NotNull CacheDatabase cacheDatabase, final @NotNull ConfigManager configManager) {
        this.cacheDatabase = Objects.requireNonNull(cacheDatabase, "cacheDatabase cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;

        final UUID playerUuid = player.getUniqueId();

        if (!configManager.isGiftEnabled()) {
            player.sendMessage(configManager.getMessage("gift.disabled"));
            return true;
        }

        if (!cacheDatabase.isMarried(playerUuid)) {
            player.sendMessage(configManager.getMessage("gift.not-married"));
            return true;
        }

        final @Nullable UUID partnerUuid = cacheDatabase.getPartner(playerUuid);
        final @Nullable Player partnerPlayer = (partnerUuid != null) ? Bukkit.getPlayer(partnerUuid) : null;

        if (partnerPlayer == null || !partnerPlayer.isOnline()) {
            player.sendMessage(configManager.getMessage("gift.partner-offline"));
            return true;
        }

        final ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR || handItem.getAmount() <= 0) {
            player.sendMessage(configManager.getMessage("gift.no-item"));
            return true;
        }

        final ItemStack giftStack = handItem.clone();
        player.getInventory().setItemInMainHand(null);

        final Map<Integer, ItemStack> overflowItems = partnerPlayer.getInventory().addItem(giftStack);
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