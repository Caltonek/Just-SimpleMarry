package pl.caltonek.simpleMarry.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.caltonek.simpleMarry.SimpleMarry;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.model.Marriage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CacheDatabase {

    private final @NotNull SimpleMarry plugin;
    private final @NotNull LocalDatabase localDatabase;
    private final @NotNull ConfigManager configManager;

    private final Map<UUID, UUID> marriageCache = new ConcurrentHashMap<>();
    private final List<Marriage> orderedMarriages = new CopyOnWriteArrayList<>();

    private final Map<UUID, UUID> pendingAdds = new ConcurrentHashMap<>();
    private final Set<UUID> pendingDeletes = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> divorceCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> marryChatToggled = ConcurrentHashMap.newKeySet();

    public CacheDatabase(final @NotNull SimpleMarry plugin, final @NotNull LocalDatabase localDatabase, final @NotNull ConfigManager configManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.localDatabase = Objects.requireNonNull(localDatabase, "localDatabase cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    public void setDivorceCooldown(final @NotNull UUID playerUuid) {
        divorceCooldowns.put(playerUuid, System.currentTimeMillis());
    }

    public boolean hasDivorceCooldown(final @NotNull UUID playerUuid) {
        final Long last = divorceCooldowns.get(playerUuid);
        return last != null && (System.currentTimeMillis() - last) < configManager.getDivorceCooldownMs();
    }

    public long getRemainingDivorceCooldown(final @NotNull UUID playerUuid) {
        final Long last = divorceCooldowns.get(playerUuid);
        if (last == null) return 0;
        final long passed = System.currentTimeMillis() - last;
        final long cooldownMs = configManager.getDivorceCooldownMs();
        return passed >= cooldownMs ? 0 : ((cooldownMs - passed) / 1000L) + 1;
    }

    public void loadAsync() {
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> {
            try {
                final Set<Marriage> marriages = localDatabase.loadAll();
                for (Marriage marriage : marriages) {
                    marriageCache.put(marriage.player1(), marriage.player2());
                    marriageCache.put(marriage.player2(), marriage.player1());
                    orderedMarriages.add(marriage);
                }
                plugin.getLogger().info("Loaded " + marriages.size() + " marriages into cache.");
            } catch (Exception e) {
                plugin.getLogger().severe("Cache load failed: " + e.getMessage());
            }
        });
    }

    public boolean isMarried(final @NotNull UUID playerUuid) {
        return marriageCache.containsKey(playerUuid);
    }

    public @Nullable UUID getPartner(final @NotNull UUID playerUuid) {
        return marriageCache.get(playerUuid);
    }

    public @NotNull List<Marriage> getOrderedMarriages() {
        return Collections.unmodifiableList(new ArrayList<>(orderedMarriages));
    }

    public void marry(final @NotNull UUID player1, final @NotNull UUID player2) {
        marriageCache.put(player1, player2);
        marriageCache.put(player2, player1);

        final Marriage marriage = new Marriage(player1, player2);
        orderedMarriages.add(marriage);

        pendingDeletes.remove(player1);
        pendingDeletes.remove(player2);
        pendingAdds.put(player1, player2);

        checkAutoFlushThreshold();
    }

    public void divorce(final @NotNull UUID playerUuid) {
        final UUID partnerUuid = marriageCache.remove(playerUuid);
        if (partnerUuid != null) {
            marriageCache.remove(partnerUuid);
            orderedMarriages.removeIf(m -> m.player1().equals(playerUuid) || m.player2().equals(playerUuid));

            pendingAdds.remove(playerUuid);
            pendingAdds.remove(partnerUuid);

            pendingDeletes.add(playerUuid);
            pendingDeletes.add(partnerUuid);

            marryChatToggled.remove(playerUuid);
            marryChatToggled.remove(partnerUuid);

            checkAutoFlushThreshold();
        }
    }

    private void checkAutoFlushThreshold() {
        if (pendingAdds.size() + pendingDeletes.size() >= configManager.getBatchThreshold()) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, task -> flush());
        }
    }

    public synchronized void flush() {
        if (pendingAdds.isEmpty() && pendingDeletes.isEmpty()) return;

        final Map<UUID, UUID> addsBatch = new HashMap<>();
        for (Iterator<Map.Entry<UUID, UUID>> it = pendingAdds.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, UUID> entry = it.next();
            addsBatch.put(entry.getKey(), entry.getValue());
            it.remove();
        }

        final Set<UUID> deletesBatch = new HashSet<>();
        for (Iterator<UUID> it = pendingDeletes.iterator(); it.hasNext(); ) {
            UUID uuid = it.next();
            deletesBatch.add(uuid);
            it.remove();
        }

        if (addsBatch.isEmpty() && deletesBatch.isEmpty()) return;

        final Set<Marriage> marriagesToAdd = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : addsBatch.entrySet()) {
            marriagesToAdd.add(new Marriage(entry.getKey(), entry.getValue()));
        }

        try {
            localDatabase.saveBatch(marriagesToAdd, deletesBatch);
        } catch (Exception e) {
            plugin.getLogger().severe("DB batch flush error: " + e.getMessage());
            for (Marriage m : marriagesToAdd) {
                pendingAdds.putIfAbsent(m.player1(), m.player2());
            }
            pendingDeletes.addAll(deletesBatch);
        }
    }

    public boolean toggleMarryChat(final @NotNull UUID playerUuid) {
        if (marryChatToggled.contains(playerUuid)) {
            marryChatToggled.remove(playerUuid);
            return false;
        } else {
            marryChatToggled.add(playerUuid);
            return true;
        }
    }

    public boolean isMarryChatToggled(final @NotNull UUID playerUuid) {
        return marryChatToggled.contains(playerUuid);
    }
}