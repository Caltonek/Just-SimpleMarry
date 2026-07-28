package pl.caltonek.simpleMarry.command;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MarryCommand implements CommandExecutor {

    private record Proposal(UUID proposerUuid, long timestampMs) {}

    private final CacheDatabase cacheDatabase;
    private final ConfigManager configManager;
    private final Map<UUID, Proposal> activeProposals = new ConcurrentHashMap<>();

    public MarryCommand(CacheDatabase cacheDatabase, ConfigManager configManager) {
        this.cacheDatabase = cacheDatabase;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            renderMenuAndStatus(player);
            return true;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null) {
            player.sendMessage(configManager.getMessage("player-offline"));
            return true;
        }

        processMarriageProposal(player, targetPlayer);
        return true;
    }

    private void renderMenuAndStatus(Player player) {
        UUID partnerUuid = cacheDatabase.getPartner(player.getUniqueId());
        String statusFormatted;

        if (partnerUuid != null) {
            Player onlinePartner = Bukkit.getPlayer(partnerUuid);
            if (onlinePartner != null) {
                statusFormatted = "<green>" + onlinePartner.getName();
            } else {
                OfflinePlayer offlinePartner = Bukkit.getOfflinePlayer(partnerUuid);
                String name = offlinePartner.getName();
                statusFormatted = "<green>" + (name != null ? name : partnerUuid.toString());
            }
        } else {
            statusFormatted = configManager.getMessage("marry.status-single").toString();
        }

        for (Component line : configManager.getMessageList("marry.menu", "status", statusFormatted)) {
            player.sendMessage(line);
        }
    }

    private void processMarriageProposal(Player sender, Player target) {
        UUID senderUuid = sender.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        if (senderUuid.equals(targetUuid)) {
            sender.sendMessage(configManager.getMessage("marry.self-proposal"));
            return;
        }

        if (cacheDatabase.hasDivorceCooldown(senderUuid)) {
            long remainingSeconds = cacheDatabase.getRemainingDivorceCooldown(senderUuid);
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

        long nowMs = System.currentTimeMillis();
        Proposal pendingProposal = activeProposals.get(senderUuid);

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