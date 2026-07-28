package pl.caltonek.simpleMarry.database;

import pl.caltonek.simpleMarry.SimpleMarry;
import pl.caltonek.simpleMarry.config.ConfigManager;
import pl.caltonek.simpleMarry.model.Marriage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CacheDatabase {

    private final SimpleMarry plugin;
    private final LocalDatabase localDatabase;
    private final ConfigManager configManager;

    private final Map<UUID, UUID> marriageCache = new ConcurrentHashMap<>();
    private final List<Marriage> orderedMarriages = new CopyOnWriteArrayList<>();
    private final Map<UUID, UUID> pendingAdds = new ConcurrentHashMap<>();
    private final Set<UUID> pendingDeletes = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> divorceCooldowns = new ConcurrentHashMap<>();

    public CacheDatabase(SimpleMarry plugin, LocalDatabase localDatabase, ConfigManager configManager) {
        this.plugin = plugin;
        this.localDatabase = localDatabase;
        this.configManager = configManager;
    }

    public void setDivorceCooldown(UUID playerUuid) {
        divorceCooldowns.put(playerUuid, System.currentTimeMillis());
    }

    public boolean hasDivorceCooldown(UUID playerUuid) {
        Long last = divorceCooldowns.get(playerUuid);
        return last != null && (System.currentTimeMillis() - last) < configManager.getDivorceCooldownMs();
    }

    public long getRemainingDivorceCooldown(UUID playerUuid) {
        Long last = divorceCooldowns.get(playerUuid);
        if (last == null) return 0;
        long passed = System.currentTimeMillis() - last;
        long cooldownMs = configManager.getDivorceCooldownMs();
        return passed >= cooldownMs ? 0 : ((cooldownMs - passed) / 1000L) + 1;
    }

    public void loadAsync() {
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> {
            long start = System.currentTimeMillis();
            try {
                Set<Marriage> marriages = localDatabase.loadAll();
                for (Marriage marriage : marriages) {
                    marriageCache.put(marriage.player1(), marriage.player2());
                    marriageCache.put(marriage.player2(), marriage.player1());
                    orderedMarriages.add(marriage);
                }
                plugin.getLogger().info("Loaded " + marriages.size() + " marriages in " + (System.currentTimeMillis() - start) + "ms.");
            } catch (Exception e) {
                plugin.getLogger().severe("Cache load failed: " + e.getMessage());
            }
        });
    }

    public boolean isMarried(UUID playerUuid) {
        return marriageCache.containsKey(playerUuid);
    }

    public UUID getPartner(UUID playerUuid) {
        return marriageCache.get(playerUuid);
    }

    public List<Marriage> getOrderedMarriages() {
        return Collections.unmodifiableList(new ArrayList<>(orderedMarriages));
    }

    public void marry(UUID player1, UUID player2) {
        marriageCache.put(player1, player2);
        marriageCache.put(player2, player1);

        Marriage marriage = new Marriage(player1, player2);
        orderedMarriages.add(marriage);

        pendingDeletes.remove(player1);
        pendingDeletes.remove(player2);

        pendingAdds.put(player1, player2);
    }

    public void divorce(UUID playerUuid) {
        UUID partnerUuid = marriageCache.remove(playerUuid);
        if (partnerUuid != null) {
            marriageCache.remove(partnerUuid);

            orderedMarriages.removeIf(m -> m.player1().equals(playerUuid) || m.player2().equals(playerUuid));

            pendingAdds.remove(playerUuid);
            pendingAdds.remove(partnerUuid);

            pendingDeletes.add(playerUuid);
            pendingDeletes.add(partnerUuid);
        }
    }

    public synchronized void flush() {
        if (pendingAdds.isEmpty() && pendingDeletes.isEmpty()) return;

        Set<Marriage> addsCopy = new HashSet<>(pendingAdds.size());
        for (Map.Entry<UUID, UUID> entry : pendingAdds.entrySet()) {
            addsCopy.add(new Marriage(entry.getKey(), entry.getValue()));
        }
        Set<UUID> deletesCopy = new HashSet<>(pendingDeletes);

        try {
            localDatabase.saveBatch(addsCopy, deletesCopy);

            for (Marriage m : addsCopy) {
                pendingAdds.remove(m.player1());
            }
            pendingDeletes.removeAll(deletesCopy);
        } catch (Exception e) {
            plugin.getLogger().severe("DB flush error: " + e.getMessage());
        }
    }
}