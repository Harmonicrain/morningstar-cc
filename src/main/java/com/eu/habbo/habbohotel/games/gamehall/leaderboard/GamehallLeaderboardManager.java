package com.eu.habbo.habbohotel.games.gamehall.leaderboard;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.games.gamehall.GamehallGameType;
import com.eu.habbo.habbohotel.users.Habbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class GamehallLeaderboardManager {
    public static final String ALL_GAMES = "ALL";

    private static final Logger LOGGER = LoggerFactory.getLogger(GamehallLeaderboardManager.class);
    private static final int POINTS_PER_WIN = 1;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private static final String ORDER_BY =
            "s.points DESC, s.wins DESC, s.losses ASC, s.last_result_at DESC, LOWER(u.username) ASC, s.user_id ASC";

    private final Map<CacheKey, GamehallLeaderboardData> cache = new ConcurrentHashMap<>();

    public void dispose() {
        this.cache.clear();
    }

    public void recordWin(GamehallGameType gameType, int roomId, int stationId, Habbo winner, Habbo loser) {
        if (gameType == null || !isValidHabbo(winner) || !isValidHabbo(loser)) {
            return;
        }

        int winnerId = winner.getHabboInfo().getId();
        int loserId = loser.getHabboInfo().getId();
        if (winnerId == loserId) {
            return;
        }

        long now = System.currentTimeMillis() / 1000L;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                insertResult(connection, gameType.name(), roomId, stationId, winnerId, loserId, now);
                updateStats(connection, gameType.name(), winnerId, true, now);
                updateStats(connection, gameType.name(), loserId, false, now);
                updateStats(connection, ALL_GAMES, winnerId, true, now);
                updateStats(connection, ALL_GAMES, loserId, false, now);

                connection.commit();
                connection.setAutoCommit(autoCommit);
                invalidate(gameType.name());
                invalidate(ALL_GAMES);
            } catch (SQLException e) {
                rollback(connection);
                connection.setAutoCommit(autoCommit);
                LOGGER.error("Caught SQL exception while recording Games Hall result", e);
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception while recording Games Hall result", e);
        }
    }

    public GamehallLeaderboardData getLeaderboard(String requestedGameType, String requestedPeriod, int offset, int limit, Habbo habbo) {
        String gameType = normalizeGameType(requestedGameType);
        GamehallLeaderboardPeriod period = GamehallLeaderboardPeriod.fromString(requestedPeriod);
        int safeOffset = Math.max(0, offset);
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        long periodStart = period.getCurrentPeriodStart();
        CacheKey cacheKey = new CacheKey(gameType, period, periodStart, safeOffset, safeLimit);

        GamehallLeaderboardData cached = this.cache.get(cacheKey);
        if (cached != null) {
            return withOwnRow(cached, gameType, period, safeOffset, safeLimit, periodStart, habbo);
        }

        GamehallLeaderboardData loaded = loadLeaderboard(gameType, period, safeOffset, safeLimit, periodStart, null);
        this.cache.put(cacheKey, loaded);
        return withOwnRow(loaded, gameType, period, safeOffset, safeLimit, periodStart, habbo);
    }

    private GamehallLeaderboardData withOwnRow(GamehallLeaderboardData data, String gameType, GamehallLeaderboardPeriod period,
                                               int offset, int limit, long periodStart, Habbo habbo) {
        GamehallLeaderboardRow ownRow = loadOwnRow(gameType, period, periodStart, habbo);
        return new GamehallLeaderboardData(gameType, period, offset, limit, data.getTotalRows(), data.getRows(), ownRow);
    }

    private GamehallLeaderboardData loadLeaderboard(String gameType, GamehallLeaderboardPeriod period, int offset,
                                                    int limit, long periodStart, Habbo habbo) {
        List<GamehallLeaderboardRow> rows = new ArrayList<>();
        int totalRows = 0;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            totalRows = countRows(connection, gameType, period, periodStart);

            String sql = "SELECT s.user_id, u.username, u.look, u.gender, s.points, s.wins, s.losses, s.draws, s.games_played " +
                    "FROM gamehall_leaderboard_stats s " +
                    "INNER JOIN users u ON u.id = s.user_id " +
                    "WHERE s.game_type = ? AND s.period_type = ? AND s.period_start = ? AND s.points > 0 " +
                    "ORDER BY " + ORDER_BY + " LIMIT ? OFFSET ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, gameType);
                statement.setString(2, period.name());
                statement.setLong(3, periodStart);
                statement.setInt(4, limit);
                statement.setInt(5, offset);

                try (ResultSet set = statement.executeQuery()) {
                    int rank = offset + 1;
                    while (set.next()) {
                        rows.add(rowFromSet(set, rank++, false));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception while loading Games Hall leaderboard", e);
        }

        return new GamehallLeaderboardData(gameType, period, offset, limit, totalRows, rows, null);
    }

    private GamehallLeaderboardRow loadOwnRow(String gameType, GamehallLeaderboardPeriod period, long periodStart, Habbo habbo) {
        if (!isValidHabbo(habbo)) {
            return null;
        }

        int userId = habbo.getHabboInfo().getId();
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            String sql = "SELECT s.user_id, u.username, u.look, u.gender, s.points, s.wins, s.losses, s.draws, s.games_played " +
                    "FROM gamehall_leaderboard_stats s " +
                    "INNER JOIN users u ON u.id = s.user_id " +
                    "WHERE s.game_type = ? AND s.period_type = ? AND s.period_start = ? AND s.user_id = ? AND s.points > 0 LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, gameType);
                statement.setString(2, period.name());
                statement.setLong(3, periodStart);
                statement.setInt(4, userId);

                try (ResultSet set = statement.executeQuery()) {
                    if (set.next()) {
                        return rowFromSet(set, getRank(connection, gameType, period, periodStart, set), true);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception while loading Games Hall own leaderboard row", e);
        }

        return null;
    }

    private int countRows(Connection connection, String gameType, GamehallLeaderboardPeriod period, long periodStart) throws SQLException {
        String sql = "SELECT COUNT(*) FROM gamehall_leaderboard_stats WHERE game_type = ? AND period_type = ? AND period_start = ? AND points > 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, gameType);
            statement.setString(2, period.name());
            statement.setLong(3, periodStart);
            try (ResultSet set = statement.executeQuery()) {
                return set.next() ? set.getInt(1) : 0;
            }
        }
    }

    private int getRank(Connection connection, String gameType, GamehallLeaderboardPeriod period, long periodStart, ResultSet row) throws SQLException {
        String sql = "SELECT COUNT(*) + 1 " +
                "FROM gamehall_leaderboard_stats s " +
                "INNER JOIN users u ON u.id = s.user_id " +
                "WHERE s.game_type = ? AND s.period_type = ? AND s.period_start = ? AND (" +
                "s.points > ? OR " +
                "(s.points = ? AND s.wins > ?) OR " +
                "(s.points = ? AND s.wins = ? AND s.losses < ?) OR " +
                "(s.points = ? AND s.wins = ? AND s.losses = ? AND s.last_result_at > ?) OR " +
                "(s.points = ? AND s.wins = ? AND s.losses = ? AND s.last_result_at = ? AND LOWER(u.username) < LOWER(?)) OR " +
                "(s.points = ? AND s.wins = ? AND s.losses = ? AND s.last_result_at = ? AND LOWER(u.username) = LOWER(?) AND s.user_id < ?)" +
                ")";

        int points = row.getInt("points");
        int wins = row.getInt("wins");
        int losses = row.getInt("losses");
        long lastResultAt = getLastResultAt(connection, gameType, period, periodStart, row.getInt("user_id"));
        String username = row.getString("username");
        int userId = row.getInt("user_id");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setString(index++, gameType);
            statement.setString(index++, period.name());
            statement.setLong(index++, periodStart);
            statement.setInt(index++, points);
            statement.setInt(index++, points);
            statement.setInt(index++, wins);
            statement.setInt(index++, points);
            statement.setInt(index++, wins);
            statement.setInt(index++, losses);
            statement.setInt(index++, points);
            statement.setInt(index++, wins);
            statement.setInt(index++, losses);
            statement.setLong(index++, lastResultAt);
            statement.setInt(index++, points);
            statement.setInt(index++, wins);
            statement.setInt(index++, losses);
            statement.setLong(index++, lastResultAt);
            statement.setString(index++, username);
            statement.setInt(index++, points);
            statement.setInt(index++, wins);
            statement.setInt(index++, losses);
            statement.setLong(index++, lastResultAt);
            statement.setString(index++, username);
            statement.setInt(index, userId);

            try (ResultSet set = statement.executeQuery()) {
                return set.next() ? set.getInt(1) : 0;
            }
        }
    }

    private long getLastResultAt(Connection connection, String gameType, GamehallLeaderboardPeriod period, long periodStart, int userId) throws SQLException {
        String sql = "SELECT last_result_at FROM gamehall_leaderboard_stats WHERE game_type = ? AND period_type = ? AND period_start = ? AND user_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, gameType);
            statement.setString(2, period.name());
            statement.setLong(3, periodStart);
            statement.setInt(4, userId);
            try (ResultSet set = statement.executeQuery()) {
                return set.next() ? set.getLong("last_result_at") : 0L;
            }
        }
    }

    private GamehallLeaderboardRow rowFromSet(ResultSet set, int rank, boolean own) throws SQLException {
        return new GamehallLeaderboardRow(
                set.getInt("user_id"),
                rank,
                set.getString("username"),
                set.getString("look"),
                set.getString("gender"),
                set.getInt("points"),
                set.getInt("wins"),
                set.getInt("losses"),
                set.getInt("draws"),
                set.getInt("games_played"),
                own);
    }

    private void insertResult(Connection connection, String gameType, int roomId, int stationId, int winnerId, int loserId, long now) throws SQLException {
        String sql = "INSERT INTO gamehall_match_results " +
                "(game_type, room_id, station_id, player_one_user_id, player_two_user_id, winner_user_id, loser_user_id, result_type, ended_at, duration_seconds, winner_score, loser_score) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'WIN', ?, 0, ?, 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS)) {
            statement.setString(1, gameType);
            statement.setInt(2, roomId);
            statement.setInt(3, stationId);
            statement.setInt(4, winnerId);
            statement.setInt(5, loserId);
            statement.setInt(6, winnerId);
            statement.setInt(7, loserId);
            statement.setLong(8, now);
            statement.setInt(9, POINTS_PER_WIN);
            statement.executeUpdate();
        }
    }

    private void updateStats(Connection connection, String gameType, int userId, boolean win, long now) throws SQLException {
        for (GamehallLeaderboardPeriod period : GamehallLeaderboardPeriod.values()) {
            updateStatsForPeriod(connection, gameType, period, period.getCurrentPeriodStart(), userId, win, now);
        }
    }

    private void updateStatsForPeriod(Connection connection, String gameType, GamehallLeaderboardPeriod period, long periodStart,
                                      int userId, boolean win, long now) throws SQLException {
        if (!win) {
            updateLossStatsForPeriod(connection, gameType, period, periodStart, userId, now);
            return;
        }

        int points = POINTS_PER_WIN;
        int wins = 1;
        int losses = 0;
        int currentStreak = 1;
        int bestStreak = 1;

        String sql = "INSERT INTO gamehall_leaderboard_stats " +
                "(game_type, period_type, period_start, user_id, points, wins, losses, draws, games_played, current_streak, best_streak, last_result_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 1, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "points = points + VALUES(points), " +
                "wins = wins + VALUES(wins), " +
                "losses = losses + VALUES(losses), " +
                "games_played = games_played + 1, " +
                "best_streak = IF(VALUES(wins) > 0, GREATEST(best_streak, current_streak + 1), best_streak), " +
                "current_streak = IF(VALUES(wins) > 0, current_streak + 1, 0), " +
                "last_result_at = GREATEST(last_result_at, VALUES(last_result_at))";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, gameType);
            statement.setString(2, period.name());
            statement.setLong(3, periodStart);
            statement.setInt(4, userId);
            statement.setInt(5, points);
            statement.setInt(6, wins);
            statement.setInt(7, losses);
            statement.setInt(8, currentStreak);
            statement.setInt(9, bestStreak);
            statement.setLong(10, now);
            statement.executeUpdate();
        }
    }

    private void updateLossStatsForPeriod(Connection connection, String gameType, GamehallLeaderboardPeriod period, long periodStart,
                                          int userId, long now) throws SQLException {
        String sql = "UPDATE gamehall_leaderboard_stats " +
                "SET losses = losses + 1, " +
                "games_played = games_played + 1, " +
                "current_streak = 0, " +
                "last_result_at = GREATEST(last_result_at, ?) " +
                "WHERE game_type = ? AND period_type = ? AND period_start = ? AND user_id = ? AND points > 0";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now);
            statement.setString(2, gameType);
            statement.setString(3, period.name());
            statement.setLong(4, periodStart);
            statement.setInt(5, userId);
            statement.executeUpdate();
        }
    }

    private void invalidate(String gameType) {
        if (gameType == null) {
            return;
        }

        for (Iterator<CacheKey> iterator = this.cache.keySet().iterator(); iterator.hasNext();) {
            CacheKey key = iterator.next();
            if (gameType.equals(key.gameType)) {
                iterator.remove();
            }
        }
    }

    private String normalizeGameType(String gameType) {
        if (gameType == null || gameType.trim().isEmpty()) {
            return ALL_GAMES;
        }

        String normalized = gameType.trim().toUpperCase(Locale.ROOT);
        if (ALL_GAMES.equals(normalized)) {
            return ALL_GAMES;
        }

        try {
            return GamehallGameType.valueOf(normalized).name();
        } catch (IllegalArgumentException e) {
            return ALL_GAMES;
        }
    }

    private static boolean isValidHabbo(Habbo habbo) {
        return habbo != null && habbo.getHabboInfo() != null;
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private static final class CacheKey {
        private final String gameType;
        private final GamehallLeaderboardPeriod period;
        private final long periodStart;
        private final int offset;
        private final int limit;

        private CacheKey(String gameType, GamehallLeaderboardPeriod period, long periodStart, int offset, int limit) {
            this.gameType = gameType;
            this.period = period;
            this.periodStart = periodStart;
            this.offset = offset;
            this.limit = limit;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            return this.periodStart == other.periodStart
                    && this.offset == other.offset
                    && this.limit == other.limit
                    && Objects.equals(this.gameType, other.gameType)
                    && this.period == other.period;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.gameType, this.period, this.periodStart, this.offset, this.limit);
        }
    }
}
