package pl.caltonek.simpleMarry.command;

import net.kyori.adventure.text.Component;
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

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MarryCommand implements CommandExecutor {

    private record Proposal(@NotNull UUID proposerUuid, long timestampMs) {
        private Proposal {
            Objects.requireNonNull(proposerUuid, "proposerUuid cannot be null");
        }
    }

    private final @NotNull CacheDatabase cacheDatabase;
    private final @NotNull ConfigManager configManager;
    private final Map<UUID, Proposal> activeProposals = new ConcurrentHashMap<>();

    public MarryCommand(final @NotNull CacheDatabase cacheDatabase, final @NotNull ConfigManager configManager) {
        this.cacheDatabase = Objects.requireNonNull(cacheDatabase, "cacheDatabase cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            renderChatMenu(player);
            return true;
        }

        final @Nullable Player targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null) {
            player.sendMessage(configManager.getMessage("player-offline"));
            return true;
        }

        processMarriageProposal(player, targetPlayer);
        return true;
    }

    private void renderChatMenu(final @NotNull Player player) {
        final @Nullable UUID partnerUuid = cacheDatabase.getPartner(player.getUniqueId());
        final String statusFormatted;

        if (partnerUuid != null) {
            final @Nullable Player onlinePartner = Bukkit.getPlayer(partnerUuid);
            if (onlinePartner != null) {
                statusFormatted = "<green>" + onlinePartner.getName();
            } else {
                final OfflinePlayer offlinePartner = Bukkit.getOfflinePlayer(partnerUuid);
                final String name = offlinePartner.getName();
                statusFormatted = "<green>" + (name != null ? name : partnerUuid.toString());
            }
        } else {
            statusFormatted = configManager.getRawMessage("marry.status-single");
        }

        for (Component line : configManager.getMessageList("marry.menu", "status", statusFormatted)) {
            player.sendMessage(line);
        }
    }

    private void processMarriageProposal(final @NotNull Player sender, final @NotNull Player target) {
        final UUID senderUuid = sender.getUniqueId();
        final UUID targetUuid = target.getUniqueId();

        if (senderUuid.equals(targetUuid)) {
            sender.sendMessage(configManager.getMessage("marry.self-proposal"));
            return;
        }

        if (cacheDatabase.hasDivorceCooldown(senderUuid)) {
            final long remainingSeconds = cacheDatabase.getRemainingDivorceCooldown(senderUuid);
            sender.sendMessage(configManager.getMessage("marry.divorce-cooldown", "seconds", String.valueOf(remainingSeconds)));
            return;
        }

        if (cacheDatabase.isMarried(senderUuid)) {
            sender.sendMessage(configManager.getMessage("marry.already-married-sender"));
            return;
        }

        if (cacheDatabase.isMarried(targetUuid)) {
            sender.sendMessage(configManager.getMessage("marry.already-married-target"));
            return;
        }

        final long nowMs = System.currentTimeMillis();
        final @Nullable Proposal pendingProposal = activeProposals.get(senderUuid);

        if (pendingProposal != null && pendingProposal.proposerUuid().equals(targetUuid)) {
            activeProposals.remove(senderUuid);
            if (nowMs - pendingProposal.timestampMs() <= configManager.getProposalExpirationMs()) {
                cacheDatabase.marry(senderUuid, targetUuid);
                Bukkit.broadcast(configManager.getMessage("marry.broadcast-success", "player1", sender.getName(), "player2", target.getName()));
                return;
            }
        }

        activeProposals.put(targetUuid, new Proposal(senderUuid, nowMs));
        sender.sendMessage(configManager.getMessage("marry.proposal-sent", "target", target.getName()));
        target.sendMessage(configManager.getMessage("marry.proposal-received", "sender", sender.getName()));
    }
}