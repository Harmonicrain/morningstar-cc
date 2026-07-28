package com.eu.habbo.habbohotel.leaderboards;

import com.eu.habbo.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class BadgeLeaderboardManager {
    public static final int TYPE_TOTAL_BADGES = 0;
    public static final int TYPE_BADGES_BY_RARITY = 1;
    public static final int TYPE_ACHIEVEMENT_SCORE = 2;
    public static final int MIN_RARITY = 1;
    public static final int MAX_RARITY = 6;
    private static final int MAX_PAGE_SIZE = 50;

    private static final Logger LOGGER = LoggerFactory.getLogger(BadgeLeaderboardManager.class);
    private static final BadgeLeaderboardManager INSTANCE = new BadgeLeaderboardManager();

    private final Map<String, CacheData> cache = new ConcurrentHashMap<>();
    private final Set<String> refreshing = ConcurrentHashMap.newKeySet();
    private final Object cacheLock = new Object();
    private final Object aggregateLock = new Object();
    private final AtomicBoolean rebuildingAggregates = new AtomicBoolean(false);
    private final AtomicBoolean processingBadgeChanges = new AtomicBoolean(false);
    private final Set<String> changedBadgeCodes = ConcurrentHashMap.newKeySet();

    private BadgeLeaderboardManager() {
    }

    public static BadgeLeaderboardManager getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return Emulator.getConfig().getBoolean("leaderboards.enabled", true);
    }

    public void clearCache() {
        this.cache.clear();
    }

    public boolean isRebuildingAggregates() {
        return this.rebuildingAggregates.get();
    }

    public boolean rebuildAggregatesAsync(int requestedByUserId) {
        if (!this.rebuildingAggregates.compareAndSet(false, true)) {
            return false;
        }

        Emulator.getThreading().run(() -> {
            long startedAt = System.currentTimeMillis();

            try {
                this.rebuildAggregates();
                this.clearCache();

                long duration = System.currentTimeMillis() - startedAt;
                LOGGER.info("Badge leaderboard aggregates rebuilt in {} ms", duration);
                this.notifyRequester(
                        requestedByUserId,
                        Emulator.getTexts().getValue(
                                "commands.success.cmd_update_badge_leaderboards.completed")
                                .replace("%duration%", Long.toString(duration)));
            } catch (Exception exception) {
                LOGGER.error("Failed to rebuild badge leaderboard aggregates", exception);
                this.notifyRequester(
                        requestedByUserId,
                        Emulator.getTexts().getValue(
                                "commands.error.cmd_update_badge_leaderboards"));
            } finally {
                this.rebuildingAggregates.set(false);
            }
        });

        return true;
    }

    public void badgeOwnershipChanged(String badgeCode) {
        if (badgeCode == null || badgeCode.isBlank()) {
            return;
        }

        this.changedBadgeCodes.add(badgeCode);
        this.scheduleBadgeChanges();
    }

    private void scheduleBadgeChanges() {
        if (!this.processingBadgeChanges.compareAndSet(false, true)) {
            return;
        }

        Emulator.getThreading().run(() -> {
            try {
                while (!this.changedBadgeCodes.isEmpty()) {
                    Set<String> batch = ConcurrentHashMap.newKeySet();
                    batch.addAll(this.changedBadgeCodes);
                    this.changedBadgeCodes.removeAll(batch);

                    synchronized (this.aggregateLock) {
                        for (String badgeCode : batch) {
                            try {
                                this.refreshBadgeAggregate(badgeCode);
                            } catch (SQLException exception) {
                                LOGGER.error(
                                        "Failed to update badge leaderboard aggregates for badge {}",
                                        badgeCode,
                                        exception);
                            }
                        }
                    }

                    this.clearCache();
                }
            } finally {
                this.processingBadgeChanges.set(false);

                if (!this.changedBadgeCodes.isEmpty()) {
                    this.scheduleBadgeChanges();
                }
            }
        });
    }

    private void notifyRequester(int userId, String message) {
        var habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);

        if (habbo != null && habbo.getClient() != null) {
            habbo.whisper(message);
        }
    }

    private void rebuildAggregates() throws SQLException {
        int maxUserRank = Math.max(1, Emulator.getConfig().getInt("leaderboards.rank.max", 2));

        synchronized (this.aggregateLock) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement dropBuildTables = connection.prepareStatement(
                     "DROP TABLE IF EXISTS badge_owner_counts_build, user_badge_rarity_counts_build, " +
                             "badge_owner_counts_old, user_badge_rarity_counts_old");
             PreparedStatement createOwnerBuild = connection.prepareStatement(
                     "CREATE TABLE badge_owner_counts_build LIKE badge_owner_counts");
             PreparedStatement createUserBuild = connection.prepareStatement(
                     "CREATE TABLE user_badge_rarity_counts_build LIKE user_badge_rarity_counts");
             PreparedStatement populateOwners = connection.prepareStatement(
                     "INSERT INTO badge_owner_counts_build (badge_code, owner_count, rarity) " +
                             "SELECT badge_code, COUNT(DISTINCT user_id), CASE " +
                             "WHEN COUNT(DISTINCT user_id) BETWEEN 51 AND 200 THEN 1 " +
                             "WHEN COUNT(DISTINCT user_id) BETWEEN 11 AND 50 THEN 2 " +
                             "WHEN COUNT(DISTINCT user_id) BETWEEN 6 AND 10 THEN 3 " +
                             "WHEN COUNT(DISTINCT user_id) BETWEEN 4 AND 5 THEN 4 " +
                             "WHEN COUNT(DISTINCT user_id) BETWEEN 2 AND 3 THEN 5 " +
                             "WHEN COUNT(DISTINCT user_id) = 1 THEN 6 ELSE 0 END " +
                             "FROM users_badges ub " +
                             "JOIN users u ON u.id = ub.user_id " +
                             "WHERE u.rank <= ? " +
                             "GROUP BY badge_code");
             PreparedStatement populateUsers = connection.prepareStatement(
                     "INSERT INTO user_badge_rarity_counts_build (user_id, rarity, badge_count) " +
                             "SELECT ub.user_id, owners.rarity, COUNT(DISTINCT ub.badge_code) " +
                             "FROM users_badges ub " +
                             "JOIN badge_owner_counts_build owners ON owners.badge_code = ub.badge_code " +
                             "JOIN users u ON u.id = ub.user_id " +
                             "WHERE u.rank <= ? " +
                             "GROUP BY ub.user_id, owners.rarity");
             PreparedStatement swapTables = connection.prepareStatement(
                     "RENAME TABLE " +
                             "badge_owner_counts TO badge_owner_counts_old, " +
                             "badge_owner_counts_build TO badge_owner_counts, " +
                             "user_badge_rarity_counts TO user_badge_rarity_counts_old, " +
                             "user_badge_rarity_counts_build TO user_badge_rarity_counts");
             PreparedStatement dropOldTables = connection.prepareStatement(
                     "DROP TABLE badge_owner_counts_old, user_badge_rarity_counts_old")) {
            dropBuildTables.executeUpdate();
            createOwnerBuild.executeUpdate();
            createUserBuild.executeUpdate();
            populateOwners.setInt(1, maxUserRank);
            populateOwners.executeUpdate();
            populateUsers.setInt(1, maxUserRank);
            populateUsers.executeUpdate();
            swapTables.executeUpdate();

            try {
                dropOldTables.executeUpdate();
            } catch (SQLException exception) {
                // The new aggregate tables are already active. Leftover backup tables
                // are harmless and will be removed before the next rebuild.
                LOGGER.warn("Badge leaderboard aggregates were swapped, but old tables could not be dropped", exception);
            }
            }
        }
    }

    private void refreshBadgeAggregate(String badgeCode) throws SQLException {
        int maxUserRank = Math.max(1, Emulator.getConfig().getInt("leaderboards.rank.max", 2));

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try {
                int oldRarity = 0;

                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT rarity FROM badge_owner_counts WHERE badge_code = ?")) {
                    statement.setString(1, badgeCode);

                    try (ResultSet set = statement.executeQuery()) {
                        if (set.next()) {
                            oldRarity = set.getInt("rarity");
                        }
                    }
                }

                int ownerCount;

                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(DISTINCT ub.user_id) AS owner_count " +
                                "FROM users_badges ub JOIN users u ON u.id = ub.user_id " +
                                "WHERE ub.badge_code = ? AND u.rank <= ?")) {
                    statement.setString(1, badgeCode);
                    statement.setInt(2, maxUserRank);

                    try (ResultSet set = statement.executeQuery()) {
                        set.next();
                        ownerCount = set.getInt("owner_count");
                    }
                }

                int newRarity = rarityForOwnerCount(ownerCount);

                if (ownerCount == 0) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM badge_owner_counts WHERE badge_code = ?")) {
                        statement.setString(1, badgeCode);
                        statement.executeUpdate();
                    }
                } else {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO badge_owner_counts (badge_code, owner_count, rarity) VALUES (?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE owner_count = VALUES(owner_count), rarity = VALUES(rarity)")) {
                        statement.setString(1, badgeCode);
                        statement.setInt(2, ownerCount);
                        statement.setInt(3, newRarity);
                        statement.executeUpdate();
                    }
                }

                this.rebuildRarityCounts(connection, oldRarity, maxUserRank);

                if (newRarity != oldRarity) {
                    this.rebuildRarityCounts(connection, newRarity, maxUserRank);
                }

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void rebuildRarityCounts(Connection connection, int rarity, int maxUserRank) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM user_badge_rarity_counts WHERE rarity = ?");
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO user_badge_rarity_counts (user_id, rarity, badge_count) " +
                             "SELECT ub.user_id, owners.rarity, COUNT(DISTINCT ub.badge_code) " +
                             "FROM users_badges ub " +
                             "JOIN badge_owner_counts owners ON owners.badge_code = ub.badge_code " +
                             "JOIN users u ON u.id = ub.user_id " +
                             "WHERE owners.rarity = ? AND u.rank <= ? " +
                             "GROUP BY ub.user_id, owners.rarity")) {
            delete.setInt(1, rarity);
            delete.executeUpdate();
            insert.setInt(1, rarity);
            insert.setInt(2, maxUserRank);
            insert.executeUpdate();
        }
    }

    private static int rarityForOwnerCount(int ownerCount) {
        if (ownerCount >= 51 && ownerCount <= 200) return 1;
        if (ownerCount >= 11 && ownerCount <= 50) return 2;
        if (ownerCount >= 6 && ownerCount <= 10) return 3;
        if (ownerCount >= 4 && ownerCount <= 5) return 4;
        if (ownerCount >= 2 && ownerCount <= 3) return 5;
        if (ownerCount == 1) return 6;
        return 0;
    }

    public BadgeLeaderboardPage getPage(int type, int rarity, int chunk, int size, int userId) throws Exception {
        if (!this.isEnabled()) {
            this.clearCache();
            return new BadgeLeaderboardPage(0, Collections.emptyList(), null);
        }

        if (type != TYPE_TOTAL_BADGES && type != TYPE_BADGES_BY_RARITY && type != TYPE_ACHIEVEMENT_SCORE) {
            type = TYPE_TOTAL_BADGES;
        }

        if (type == TYPE_BADGES_BY_RARITY) {
            rarity = Math.max(MIN_RARITY, Math.min(MAX_RARITY, rarity));
        } else {
            rarity = -1;
        }

        chunk = Math.max(0, chunk);
        size = Math.max(1, Math.min(MAX_PAGE_SIZE, size));

        int maxUserRank = Math.max(1, Emulator.getConfig().getInt("leaderboards.rank.max", 2));
        int maxEntries = Math.max(1, Emulator.getConfig().getInt("leaderboards.entries.max", 500));
        long cacheLifetime = Math.max(1, Emulator.getConfig().getInt("leaderboards.cache.seconds", 120)) * 1000L;
        CacheData cached = this.getCache(type, rarity, maxUserRank, maxEntries, cacheLifetime);
        int offset = (int) Math.min((long) chunk * size, cached.entries.size());
        int end = Math.min(offset + size, cached.entries.size());

        return new BadgeLeaderboardPage(
                cached.entries.size(),
                new ArrayList<>(cached.entries.subList(offset, end)),
                cached.entriesByUserId.get(userId));
    }

    private CacheData getCache(int type, int rarity, int maxUserRank, int maxEntries, long cacheLifetime) throws Exception {
        String key = cacheKey(type, rarity, maxUserRank, maxEntries);
        long now = System.currentTimeMillis();
        CacheData cached = this.cache.get(key);

        if (cached != null) {
            if (now - cached.createdAt >= cacheLifetime) {
                this.scheduleRefresh(type, rarity, maxUserRank, maxEntries);
            }

            return cached;
        }

        synchronized (this.cacheLock) {
            cached = this.cache.get(key);

            if (cached != null) {
                return cached;
            }

            if (type == TYPE_BADGES_BY_RARITY) {
                this.refreshAllRarityCaches(maxUserRank, maxEntries);
            } else {
                this.cache.put(key, this.loadCache(type, maxUserRank, maxEntries));
            }

            CacheData refreshed = this.cache.get(key);

            return refreshed != null
                    ? refreshed
                    : createCacheData(now, Collections.emptyList(), Collections.emptyMap());
        }
    }

    private void scheduleRefresh(int type, int rarity, int maxUserRank, int maxEntries) {
        String refreshKey = type == TYPE_BADGES_BY_RARITY
                ? "rarities:" + maxUserRank + ":" + maxEntries
                : cacheKey(type, rarity, maxUserRank, maxEntries);

        if (!this.refreshing.add(refreshKey)) {
            return;
        }

        Emulator.getThreading().run(() -> {
            try {
                if (!this.isEnabled()) {
                    return;
                }

                if (type == TYPE_BADGES_BY_RARITY) {
                    this.refreshAllRarityCaches(maxUserRank, maxEntries);
                } else {
                    CacheData refreshed = this.loadCache(type, maxUserRank, maxEntries);

                    if (this.isEnabled()) {
                        this.cache.put(cacheKey(type, rarity, maxUserRank, maxEntries), refreshed);
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to refresh badge leaderboard cache {}", refreshKey, exception);
            } finally {
                this.refreshing.remove(refreshKey);
            }
        });
    }

    private CacheData loadCache(int type, int maxUserRank, int maxEntries) throws Exception {
        Query query = Query.forType(type);
        List<BadgeLeaderboardEntry> entries = new ArrayList<>();
        Map<Integer, BadgeLeaderboardEntry> entriesByUserId = new HashMap<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(query.entriesSql)) {
            statement.setInt(1, maxUserRank);
            statement.setInt(2, maxEntries);

            try (ResultSet set = statement.executeQuery()) {
                int rank = 1;

                while (set.next()) {
                    BadgeLeaderboardEntry entry = createEntry(set, rank++);

                    entries.add(entry);
                    entriesByUserId.put(entry.getUserId(), entry);
                }
            }
        }

        return createCacheData(System.currentTimeMillis(), entries, entriesByUserId);
    }

    private void refreshAllRarityCaches(int maxUserRank, int maxEntries) throws Exception {
        Map<Integer, List<BadgeLeaderboardEntry>> entriesByRarity = new HashMap<>();
        Map<Integer, Map<Integer, BadgeLeaderboardEntry>> usersByRarity = new HashMap<>();

        for (int rarity = MIN_RARITY; rarity <= MAX_RARITY; rarity++) {
            entriesByRarity.put(rarity, new ArrayList<>());
            usersByRarity.put(rarity, new HashMap<>());
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(Query.ALL_RARITIES_SQL)) {
            statement.setInt(1, maxUserRank);
            statement.setInt(2, maxEntries);

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    int rarity = set.getInt("rarity");
                    List<BadgeLeaderboardEntry> entries = entriesByRarity.get(rarity);

                    if (entries == null) {
                        continue;
                    }

                    BadgeLeaderboardEntry entry = createEntry(set, entries.size() + 1);

                    entries.add(entry);
                    usersByRarity.get(rarity).put(entry.getUserId(), entry);
                }
            }
        }

        if (!this.isEnabled()) {
            return;
        }

        long createdAt = System.currentTimeMillis();

        for (int rarity = MIN_RARITY; rarity <= MAX_RARITY; rarity++) {
            this.cache.put(
                    cacheKey(TYPE_BADGES_BY_RARITY, rarity, maxUserRank, maxEntries),
                    createCacheData(createdAt, entriesByRarity.get(rarity), usersByRarity.get(rarity)));
        }
    }

    private static BadgeLeaderboardEntry createEntry(ResultSet set, int rank) throws SQLException {
        return new BadgeLeaderboardEntry(
                set.getInt("id"),
                set.getString("username"),
                set.getString("look"),
                rank,
                set.getInt("score"));
    }

    private static CacheData createCacheData(long createdAt, List<BadgeLeaderboardEntry> entries, Map<Integer, BadgeLeaderboardEntry> entriesByUserId) {
        return new CacheData(
                createdAt,
                Collections.unmodifiableList(new ArrayList<>(entries)),
                Collections.unmodifiableMap(new HashMap<>(entriesByUserId)));
    }

    private static String cacheKey(int type, int rarity, int maxUserRank, int maxEntries) {
        int normalizedRarity = type == TYPE_BADGES_BY_RARITY ? rarity : 0;

        return type + ":" + normalizedRarity + ":" + maxUserRank + ":" + maxEntries;
    }

    private static final class Query {
        private static final String ALL_RARITIES_SQL =
                "SELECT id, username, look, rarity, score FROM (" +
                    "SELECT u.id, u.username, u.look, counts.rarity, counts.badge_count AS score, " +
                        "ROW_NUMBER() OVER (PARTITION BY counts.rarity ORDER BY counts.badge_count DESC, u.id ASC) AS row_number " +
                    "FROM user_badge_rarity_counts counts " +
                    "JOIN users u ON u.id = counts.user_id " +
                    "WHERE counts.rarity BETWEEN 1 AND 6 AND u.rank <= ?" +
                ") leaderboard " +
                "WHERE row_number <= ? " +
                "ORDER BY rarity ASC, score DESC, id ASC";

        private final String entriesSql;

        private Query(String entriesSql) {
            this.entriesSql = entriesSql;
        }

        private static Query forType(int type) {
            if (type == TYPE_ACHIEVEMENT_SCORE) {
                return new Query(
                        "SELECT u.id, u.username, u.look, us.achievement_score AS score FROM users_settings us JOIN users u ON u.id = us.user_id WHERE us.achievement_score > 0 AND u.rank <= ? ORDER BY score DESC, u.id ASC LIMIT ?");
            }

            String scores = "SELECT user_id, SUM(badge_count) AS score FROM user_badge_rarity_counts GROUP BY user_id";
            return new Query(
                    "SELECT u.id, u.username, u.look, ranked.score FROM (" + scores + ") ranked JOIN users u ON u.id = ranked.user_id WHERE u.rank <= ? ORDER BY ranked.score DESC, u.id ASC LIMIT ?");
        }
    }

    private static final class CacheData {
        private final long createdAt;
        private final List<BadgeLeaderboardEntry> entries;
        private final Map<Integer, BadgeLeaderboardEntry> entriesByUserId;

        private CacheData(long createdAt, List<BadgeLeaderboardEntry> entries, Map<Integer, BadgeLeaderboardEntry> entriesByUserId) {
            this.createdAt = createdAt;
            this.entries = entries;
            this.entriesByUserId = entriesByUserId;
        }
    }
}
