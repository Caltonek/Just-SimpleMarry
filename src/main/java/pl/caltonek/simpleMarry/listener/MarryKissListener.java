package pl.caltonek.simpleMarry.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.database.CacheDatabase;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MarryKissListener implements Listener {

    private final @NotNull CacheDatabase cacheDatabase;
    private final @NotNull ConfigManager configManager;
    private final Map<UUID, Long> kissCooldowns = new ConcurrentHashMap<>();

    public MarryKissListener(final @NotNull CacheDatabase cacheDatabase, final @NotNull ConfigManager configManager) {
        this.cacheDatabase = Objects.requireNonNull(cacheDatabase, "cacheDatabase cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    @EventHandler
    public void onSneak(final @NotNull PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        if (!configManager.isKissEnabled()) return;

        final Player player = event.getPlayer();
        final UUID playerUuid = player.getUniqueId();

        final @Nullable UUID partnerUuid = cacheDatabase.getPartner(playerUuid);
        if (partnerUuid == null) return;

        final @Nullable Player partner = Bukkit.getPlayer(partnerUuid);
        if (partner == null || !partner.isOnline()) return;

        if (!player.getWorld().equals(partner.getWorld())) return;

        final Location playerEye = player.getEyeLocation();
        final Location partnerEye = partner.getEyeLocation();

        if (playerEye.distanceSquared(partnerEye) > configManager.getKissMaxDistanceSq()) return;

        final Vector dirToPartner = partnerEye.toVector().subtract(playerEye.toVector()).normalize();
        final Vector playerDirection = playerEye.getDirection().normalize();

        if (playerDirection.dot(dirToPartner) < 0.7) return;

        final long now = System.currentTimeMillis();
        final Long lastKiss = kissCooldowns.get(playerUuid);
        if (lastKiss != null && (now - lastKiss) < configManager.getKissCooldownMs()) return;

        kissCooldowns.put(playerUuid, now);

        final Location kissLocation = playerEye.clone().add(partnerEye).multiply(0.5);

        player.getWorld().spawnParticle(
                Particle.HEART,
                kissLocation,
                6,
                0.25, 0.25, 0.25,
                0.0
        );

        if (configManager.isKissSoundEnabled() && !configManager.getKissSound().isBlank()) {
            player.playSound(
                    playerEye,
                    configManager.getKissSound(),
                    configManager.getKissSoundVolume(),
                    configManager.getKissSoundPitch()
            );
            partner.playSound(
                    partnerEye,
                    configManager.getKissSound(),
                    configManager.getKissSoundVolume(),
                    configManager.getKissSoundPitch()
            );
        }
    }
}