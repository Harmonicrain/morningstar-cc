package com.eu.habbo.habbohotel.communitygoals;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.quests.CommunityGoalEarnedPrizesComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class CommunityGoalManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommunityGoalManager.class);
    private static final long MIN_REWARD_DELAY_MS = 1000L;
    private static final long NO_GOAL_REWARD_DELAY_MS = 60000L;

    private ScheduledFuture<?> rewardTask;
    private volatile boolean disposed;

    public CommunityGoalManager() {
        long millis = System.currentTimeMillis();

        LOGGER.info("Community Goal Manager -> Loaded! ({} MS)", System.currentTimeMillis() - millis);
    }

    public void startRewardScheduler() {
        this.processExpiredGoalRewards();
        this.scheduleNextRewardRun();
    }

    private void scheduleNextRewardRun() {
        if (this.disposed || Emulator.getThreading() == null) {
            return;
        }

        long delay = this.getDelayUntilNextGoalEnd();

        this.rewardTask = Emulator.getThreading().run(() -> {
            if (this.disposed) {
                return;
            }

            try {
                this.processExpiredGoalRewards();
            } finally {
                this.scheduleNextRewardRun();
            }
        }, delay);
    }

    private long getDelayUntilNextGoalEnd() {
        int now = Emulator.getIntUnixTimestamp();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT MIN(start_timestamp + duration_seconds) AS ends_at FROM community_goals WHERE enabled = 1 AND start_timestamp > 0 AND start_timestamp + duration_seconds > ?")) {
            statement.setInt(1, now);

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    int endsAt = set.getInt("ends_at");

                    if (endsAt > 0) {
                        return Math.max(MIN_REWARD_DELAY_MS, (long) (endsAt - now) * 1000L);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return NO_GOAL_REWARD_DELAY_MS;
    }

    public boolean registerVote(int userId, int vote) {
        if (!isCommunityGoalsEnabled()) {
            return false;
        }

        if (vote < 1 || vote > 2) {
            return false;
        }

        CommunityGoal goal = this.getAcceptingCommunityGoal();
        if (goal == null) {
            return false;
        }

        if (!goal.isVersus()) {
            return false;
        }

        int contributionScore = vote == 1 ? -1 : 1;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement insertStatement = connection.prepareStatement("INSERT IGNORE INTO users_community_goals (user_id, goal_code, contribution_score) VALUES (?, ?, ?)")) {
                insertStatement.setInt(1, userId);
                insertStatement.setString(2, goal.code);
                insertStatement.setInt(3, contributionScore);

                if (insertStatement.executeUpdate() == 0) {
                    connection.rollback();
                    return false;
                }
            }

            try (PreparedStatement updateStatement = connection.prepareStatement("UPDATE community_goals SET total_score = total_score + ? WHERE code = ? LIMIT 1")) {
                updateStatement.setInt(1, contributionScore);
                updateStatement.setString(2, goal.code);
                updateStatement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return false;
    }

    public boolean addContribution(int userId, int contributionScore) {
        if (!isCommunityGoalsEnabled()) {
            return false;
        }

        CommunityGoal goal = this.getAcceptingCommunityGoal();
        if (goal == null) {
            return false;
        }

        return this.addContribution(userId, goal.code, contributionScore);
    }

    public boolean addContribution(int userId, String goalCode, int contributionScore) {
        if (!isCommunityGoalsEnabled()) {
            return false;
        }

        if (userId <= 0 || goalCode == null || goalCode.isEmpty() || contributionScore <= 0) {
            return false;
        }

        CommunityGoal goal = this.getCommunityGoalByCode(goalCode);
        if (goal == null || !goal.isAcceptingContributions()) {
            return false;
        }

        if (goal.isVersus()) {
            return false;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO users_community_goals (user_id, goal_code, contribution_score) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE contribution_score = contribution_score + VALUES(contribution_score)")) {
                insertStatement.setInt(1, userId);
                insertStatement.setString(2, goalCode);
                insertStatement.setInt(3, contributionScore);
                insertStatement.executeUpdate();
            }

            try (PreparedStatement updateStatement = connection.prepareStatement("UPDATE community_goals SET total_score = total_score + ? WHERE code = ? AND enabled = 1 LIMIT 1")) {
                updateStatement.setInt(1, contributionScore);
                updateStatement.setString(2, goalCode);

                if (updateStatement.executeUpdate() == 0) {
                    connection.rollback();
                    return false;
                }
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return false;
    }

    public int addTriggerContribution(int userId, String triggerType, int amount) {
        return this.addTriggerContribution(userId, triggerType, amount, 0, 0, new ArrayList<>(), -1);
    }

    public int addTriggerContribution(int userId, CommunityGoalTriggerType triggerType, int amount) {
        return triggerType == null ? 0 : this.addTriggerContribution(userId, triggerType.getCode(), amount);
    }

    public int addTriggerContribution(int userId, CommunityGoalTriggerType triggerType, int amount, int catalogPageId, int catalogItemId, List<Integer> baseItemIds, int currencyType) {
        return triggerType == null ? 0 : this.addTriggerContribution(userId, triggerType.getCode(), amount, catalogPageId, catalogItemId, baseItemIds, currencyType);
    }

    public int addTriggerContribution(int userId, String triggerType, int amount, int catalogPageId, int catalogItemId, List<Integer> baseItemIds, int currencyType) {
        if (!isCommunityGoalsEnabled() || userId <= 0 || triggerType == null || triggerType.isEmpty() || amount <= 0) {
            return 0;
        }

        int updated = 0;
        String query = "SELECT * FROM community_goal_triggers WHERE enabled = 1 AND trigger_type = ?";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, triggerType);

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    CommunityGoalTrigger trigger = new CommunityGoalTrigger(set);

                    if (!trigger.matches(amount, catalogPageId, catalogItemId, baseItemIds, currencyType)) {
                        continue;
                    }

                    int contribution = trigger.getContribution(amount);
                    if (contribution > 0 && this.addContribution(userId, trigger.goalCode, contribution)) {
                        updated++;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return updated;
    }

    public boolean hasVote(int userId) {
        if (!isCommunityGoalsEnabled()) {
            return false;
        }

        CommunityGoal goal = this.getVisibleCommunityGoal();
        return goal != null && this.getUserContribution(userId, goal.code) != 0;
    }

    public boolean shouldHideVoteButton(int userId) {
        if (!isCommunityGoalsEnabled()) {
            return true;
        }

        CommunityGoal goal = this.getVisibleCommunityGoal();
        return goal == null || !goal.isAcceptingContributions() || this.getUserContribution(userId, goal.code) != 0;
    }

    public int getVoteCount() {
        if (!isCommunityGoalsEnabled()) {
            return 0;
        }

        CommunityGoal goal = this.getVisibleCommunityGoal();
        return goal == null ? 0 : Math.abs(goal.totalScore);
    }

    public CommunityGoalProgress getProgress(int userId) {
        if (!isCommunityGoalsEnabled()) {
            return CommunityGoalProgress.empty();
        }

        CommunityGoal goal = this.getVisibleCommunityGoal();
        if (goal == null) {
            return CommunityGoalProgress.empty();
        }

        List<CommunityGoalLevel> levels = this.getCommunityGoalLevels(goal.code);
        CommunityGoalLevelProgress levelProgress = CommunityGoalLevelProgress.calculate(goal.totalScore, levels);
        int contributionScore = Math.abs(this.getUserContribution(userId, goal.code));

        return new CommunityGoalProgress(
                false,
                contributionScore,
                0,
                goal.totalScore,
                levelProgress.level,
                levelProgress.scoreRemaining,
                levelProgress.percent,
                goal.code,
                goal.getTimeLeft(),
                this.getRankData(levels));
    }

    public int getConcurrentUsersGoalTarget() {
        if (!isCommunityGoalsEnabled()) {
            return 0;
        }

        CommunityGoal goal = this.getAcceptingCommunityGoalByTrigger(CommunityGoalTriggerType.CONCURRENT_USERS);
        return goal == null ? 0 : this.getNextPositiveLevelThreshold(goal);
    }

    public List<CommunityGoalEarnedPrize> getEarnedPrizes(int userId) {
        List<CommunityGoalEarnedPrize> prizes = new ArrayList<>();

        if (!isCommunityGoalsEnabled() || userId <= 0) {
            return prizes;
        }

        CommunityGoal goal = this.getVisibleCommunityGoal();
        if (goal == null || goal.isAcceptingContributions()) {
            return prizes;
        }

        int contributionScore = this.getUserContribution(userId, goal.code);

        if (Math.abs(contributionScore) <= 0) {
            return prizes;
        }

        CommunityGoalLevelProgress levelProgress = CommunityGoalLevelProgress.calculate(goal.totalScore, this.getCommunityGoalLevels(goal.code));

        String query = "SELECT cgr.* " +
                "FROM community_goal_rewards cgr " +
                "LEFT JOIN users_community_goal_rewards claimed ON claimed.user_id = ? AND claimed.goal_code = ? AND claimed.prize_id = cgr.id " +
                "WHERE cgr.goal_code = ? AND cgr.enabled = 1 AND cgr.reward_timing = 'on_finish' AND cgr.required_level <= ? AND claimed.user_id IS NULL " +
                "ORDER BY cgr.required_level ASC, cgr.id ASC";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setString(2, goal.code);
            statement.setString(3, goal.code);
            statement.setInt(4, levelProgress.level);

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    if (!this.isRewardEligible(goal, contributionScore, set)) {
                        continue;
                    }

                    prizes.add(this.createEarnedPrize(set));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return prizes;
    }

    private boolean isBadgePrizeType(String prizeType) {
        return prizeType != null && "badge".equalsIgnoreCase(prizeType.trim());
    }

    public List<CommunityGoalEarnedPrize> claimAndDeliverVoteRewards(int userId) {
        List<CommunityGoalEarnedPrize> prizes = new ArrayList<>();

        if (!isCommunityGoalsEnabled() || userId <= 0) {
            return prizes;
        }

        CommunityGoal goal = this.getAcceptingCommunityGoal();
        if (goal == null) {
            return prizes;
        }

        int contributionScore = this.getUserContribution(userId, goal.code);
        if (Math.abs(contributionScore) <= 0) {
            return prizes;
        }

        CommunityGoalLevelProgress levelProgress = CommunityGoalLevelProgress.calculate(goal.totalScore, this.getCommunityGoalLevels(goal.code));
        String query = "SELECT cgr.* " +
                "FROM community_goal_rewards cgr " +
                "LEFT JOIN users_community_goal_rewards claimed ON claimed.user_id = ? AND claimed.goal_code = ? AND claimed.prize_id = cgr.id " +
                "WHERE cgr.goal_code = ? AND cgr.enabled = 1 AND cgr.reward_timing = 'on_vote' AND cgr.required_level <= ? AND claimed.user_id IS NULL " +
                "ORDER BY cgr.required_level ASC, cgr.id ASC";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setString(2, goal.code);
            statement.setString(3, goal.code);
            statement.setInt(4, levelProgress.level);

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    if (!this.isRewardEligible(goal, contributionScore, set)) {
                        continue;
                    }

                    CommunityGoalEarnedPrize prize = this.createEarnedPrize(set);
                    if (this.claimAndDeliverEarnedPrize(userId, goal.code, prize)) {
                        prizes.add(prize);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return prizes;
    }

    public boolean claimAndDeliverEarnedPrize(int userId, CommunityGoalEarnedPrize prize) {
        CommunityGoal goal = this.getVisibleCommunityGoal();

        if (goal == null || goal.isAcceptingContributions()) {
            return false;
        }

        return this.claimAndDeliverEarnedPrize(userId, goal.code, prize);
    }

    private boolean claimAndDeliverEarnedPrize(int userId, String goalCode, CommunityGoalEarnedPrize prize) {
        if (!this.canDeliverReward(userId, prize)) {
            return false;
        }

        if (!this.claimEarnedPrize(userId, goalCode, prize)) {
            return false;
        }

        return this.deliverReward(userId, prize);
    }

    private boolean claimEarnedPrize(int userId, String goalCode, CommunityGoalEarnedPrize prize) {
        if (!isCommunityGoalsEnabled() || userId <= 0 || goalCode == null || goalCode.isEmpty() || prize == null || prize.prizeId <= 0) {
            return false;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT IGNORE INTO users_community_goal_rewards (user_id, goal_code, prize_id, claimed_at) VALUES (?, ?, ?, ?)")) {
            statement.setInt(1, userId);
            statement.setString(2, goalCode);
            statement.setInt(3, prize.prizeId);
            statement.setInt(4, Emulator.getIntUnixTimestamp());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return false;
    }

    public synchronized void processExpiredGoalRewards() {
        if (!isCommunityGoalsEnabled()) {
            return;
        }

        int processed = 0;
        int deactivated = 0;
        Map<Integer, List<CommunityGoalEarnedPrize>> onlineNotifications = new HashMap<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM community_goals WHERE enabled = 1 AND start_timestamp > 0 AND start_timestamp + duration_seconds <= ? ORDER BY start_timestamp ASC")) {
            statement.setInt(1, Emulator.getIntUnixTimestamp());

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    CommunityGoal goal = new CommunityGoal(set);
                    int goalProcessed = this.processExpiredGoalRewards(goal, onlineNotifications);

                    if (goalProcessed < 0) {
                        continue;
                    }

                    processed += goalProcessed;

                    if (this.deactivateExpiredGoal(goal.code)) {
                        deactivated++;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        for (Map.Entry<Integer, List<CommunityGoalEarnedPrize>> entry : onlineNotifications.entrySet()) {
            Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(entry.getKey());

            if (habbo != null && habbo.getClient() != null && !entry.getValue().isEmpty()) {
                habbo.getClient().sendResponse(new CommunityGoalEarnedPrizesComposer(entry.getValue()));
            }
        }

        if (processed > 0) {
            LOGGER.info("Community Goal Manager -> Delivered {} expired goal rewards.", processed);
        }

        if (deactivated > 0) {
            LOGGER.info("Community Goal Manager -> Deactivated {} expired community goals.", deactivated);
        }
    }

    private int processExpiredGoalRewards(CommunityGoal goal, Map<Integer, List<CommunityGoalEarnedPrize>> onlineNotifications) {
        int processed = 0;
        CommunityGoalLevelProgress levelProgress = CommunityGoalLevelProgress.calculate(goal.totalScore, this.getCommunityGoalLevels(goal.code));

        String query = "SELECT ucg.user_id, ucg.contribution_score, cgr.* " +
                "FROM users_community_goals ucg " +
                "INNER JOIN community_goal_rewards cgr ON cgr.goal_code = ? AND cgr.enabled = 1 AND cgr.reward_timing = 'on_finish' AND cgr.required_level <= ? " +
                "LEFT JOIN users_community_goal_rewards claimed ON claimed.user_id = ucg.user_id AND claimed.goal_code = ? AND claimed.prize_id = cgr.id " +
                "WHERE ucg.goal_code = ? AND ucg.contribution_score != 0 AND claimed.user_id IS NULL " +
                "ORDER BY ucg.user_id ASC, cgr.required_level ASC, cgr.id ASC";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, goal.code);
            statement.setInt(2, levelProgress.level);
            statement.setString(3, goal.code);
            statement.setString(4, goal.code);

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    int userId = set.getInt("user_id");

                    if (!this.isRewardEligible(goal, set.getInt("contribution_score"), set)) {
                        continue;
                    }

                    CommunityGoalEarnedPrize prize = this.createEarnedPrize(set);

                    if (!this.claimAndDeliverEarnedPrize(userId, goal.code, prize)) {
                        continue;
                    }

                    Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);

                    if (habbo != null && habbo.getClient() != null) {
                        onlineNotifications.computeIfAbsent(userId, ignored -> new ArrayList<>()).add(prize);
                    }

                    processed++;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
            return -1;
        }

        return processed;
    }

    private boolean deactivateExpiredGoal(String goalCode) {
        if (goalCode == null || goalCode.isEmpty()) {
            return false;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE community_goals SET enabled = 0 WHERE code = ? AND enabled = 1 AND start_timestamp > 0 AND start_timestamp + duration_seconds <= ? LIMIT 1")) {
            statement.setString(1, goalCode);
            statement.setInt(2, Emulator.getIntUnixTimestamp());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return false;
    }

    private boolean isRewardEligible(CommunityGoal goal, int contributionScore, ResultSet rewardSet) throws SQLException {
        if (goal == null || rewardSet == null) {
            return false;
        }

        int absoluteContribution = Math.abs(contributionScore);

        if (absoluteContribution <= 0) {
            return false;
        }

        int minimumContribution = rewardSet.getInt("minimum_contribution");

        if (minimumContribution > 0 && absoluteContribution < minimumContribution) {
            return false;
        }

        int voteOption = this.getVoteOption(contributionScore);
        boolean voteReward = "on_vote".equalsIgnoreCase(rewardSet.getString("reward_timing"));

        if (goal.isVersusWinner() && !voteReward) {
            if (goal.resultOption <= 0 || voteOption != goal.resultOption) {
                return false;
            }
        } else if (goal.isVersusAll()) {
            if (voteOption <= 0) {
                return false;
            }
        }

        int requiredVoteOption = rewardSet.getInt("required_vote_option");

        if (requiredVoteOption > 0) {
            if (!goal.isVersus() || voteOption != requiredVoteOption) {
                return false;
            }
        }

        int requiredResultOption = rewardSet.getInt("required_result_option");

        if (requiredResultOption > 0) {
            if (!goal.isVersus() || goal.resultOption != requiredResultOption) {
                return false;
            }
        }

        return true;
    }

    private int getVoteOption(int contributionScore) {
        if (contributionScore < 0) {
            return 1;
        }

        if (contributionScore > 0) {
            return 2;
        }

        return 0;
    }

    private CommunityGoalEarnedPrize createEarnedPrize(ResultSet set) throws SQLException {
        String prizeType = set.getString("prize_type");

        return new CommunityGoalEarnedPrize(
                set.getInt("id"),
                prizeType,
                1,
                set.getString("reward_value"),
                this.isBadgePrizeType(prizeType),
                set.getString("localized_name"));
    }

    private boolean canDeliverReward(int userId, CommunityGoalEarnedPrize prize) {
        if (userId <= 0 || prize == null || prize.prizeType == null) {
            return false;
        }

        String prizeType = prize.prizeType.trim().toLowerCase();

        if ("badge".equals(prizeType)) {
            return prize.badgeCode != null && !prize.badgeCode.isEmpty();
        }

        if ("item".equals(prizeType)) {
            return this.getRewardItem(prize) != null;
        }

        if ("credits".equals(prizeType)) {
            return this.parsePositiveInt(prize.badgeCode) > 0;
        }

        if ("points".equals(prizeType)) {
            return this.parsePointReward(prize.badgeCode) != null;
        }

        return false;
    }

    private boolean deliverReward(int userId, CommunityGoalEarnedPrize prize) {
        String prizeType = prize.prizeType.trim().toLowerCase();
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);

        if ("badge".equals(prizeType)) {
            if (habbo != null) {
                habbo.addBadge(prize.badgeCode);
                return true;
            }

            return this.giveOfflineBadge(userId, prize.badgeCode);
        }

        if ("item".equals(prizeType)) {
            Item item = this.getRewardItem(prize);

            if (item == null) {
                return false;
            }

            HabboItem habboItem = Emulator.getGameEnvironment().getItemManager().createItem(userId, item, 0, 0, "");

            if (habboItem == null) {
                return false;
            }

            if (habbo != null) {
                habbo.addFurniture(habboItem);
            }

            return true;
        }

        if ("credits".equals(prizeType)) {
            Emulator.getGameEnvironment().getHabboManager().giveCredits(userId, this.parsePositiveInt(prize.badgeCode));
            return true;
        }

        if ("points".equals(prizeType)) {
            int[] pointReward = this.parsePointReward(prize.badgeCode);

            if (pointReward == null) {
                return false;
            }

            if (habbo != null) {
                habbo.givePoints(pointReward[0], pointReward[1]);
                return true;
            }

            return this.giveOfflinePoints(userId, pointReward[0], pointReward[1]);
        }

        return false;
    }

    private boolean giveOfflineBadge(int userId, String badgeCode) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement existsStatement = connection.prepareStatement("SELECT id FROM users_badges WHERE user_id = ? AND badge_code = ? LIMIT 1")) {
            existsStatement.setInt(1, userId);
            existsStatement.setString(2, badgeCode);

            try (ResultSet set = existsStatement.executeQuery()) {
                if (set.next()) {
                    return true;
                }
            }

            try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO users_badges (user_id, slot_id, badge_code) VALUES (?, 0, ?)")) {
                insertStatement.setInt(1, userId);
                insertStatement.setString(2, badgeCode);
                insertStatement.execute();
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return false;
    }

    private boolean giveOfflinePoints(int userId, int type, int amount) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO users_currency (user_id, type, amount) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE amount = amount + ?")) {
            statement.setInt(1, userId);
            statement.setInt(2, type);
            statement.setInt(3, amount);
            statement.setInt(4, amount);
            statement.execute();
            return true;
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return false;
    }

    private Item getRewardItem(CommunityGoalEarnedPrize prize) {
        int baseItemId = this.parsePositiveInt(prize.badgeCode);

        if (baseItemId <= 0) {
            return null;
        }

        return Emulator.getGameEnvironment().getItemManager().getItem(baseItemId);
    }

    private int[] parsePointReward(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String[] parts = value.split(",", 2);

        if (parts.length == 1) {
            int amount = this.parsePositiveInt(parts[0]);
            return amount > 0 ? new int[]{0, amount} : null;
        }

        int type = this.parseNonNegativeInt(parts[0]);
        int amount = this.parsePositiveInt(parts[1]);

        return type >= 0 && amount > 0 ? new int[]{type, amount} : null;
    }

    private int parsePositiveInt(String value) {
        int parsed = this.parseNonNegativeInt(value);
        return parsed > 0 ? parsed : 0;
    }

    private int parseNonNegativeInt(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 ? parsed : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean isCommunityGoalsEnabled() {
        return Emulator.getConfig().getBoolean("hotelview.community.goal.enabled", true);
    }

    private CommunityGoal getVisibleCommunityGoal() {
        String configuredCode = Emulator.getConfig().getValue("hotelview.community.goal.code", "");

        if (!configuredCode.isEmpty()) {
            return this.getCommunityGoalByCode(configuredCode);
        }

        CommunityGoal activeGoal = this.getAcceptingCommunityGoal();
        if (activeGoal != null) {
            return activeGoal;
        }

        String query = "SELECT * FROM community_goals WHERE enabled = 1 AND (start_timestamp = 0 OR start_timestamp <= ?) ORDER BY start_timestamp DESC LIMIT 1";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, Emulator.getIntUnixTimestamp());

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return new CommunityGoal(set);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return null;
    }

    private CommunityGoal getAcceptingCommunityGoal() {
        String configuredCode = Emulator.getConfig().getValue("hotelview.community.goal.code", "");

        if (!configuredCode.isEmpty()) {
            CommunityGoal goal = this.getCommunityGoalByCode(configuredCode);
            return goal != null && goal.isAcceptingContributions() ? goal : null;
        }

        String query = "SELECT * FROM community_goals WHERE enabled = 1 AND (start_timestamp = 0 OR (start_timestamp <= ? AND start_timestamp + duration_seconds > ?)) ORDER BY start_timestamp DESC LIMIT 1";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            int now = Emulator.getIntUnixTimestamp();
            statement.setInt(1, now);
            statement.setInt(2, now);

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return new CommunityGoal(set);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return null;
    }

    private CommunityGoal getCommunityGoalByCode(String code) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM community_goals WHERE enabled = 1 AND code = ? LIMIT 1")) {
            statement.setString(1, code);

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return new CommunityGoal(set);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return null;
    }

    private CommunityGoal getAcceptingCommunityGoalByTrigger(CommunityGoalTriggerType triggerType) {
        if (triggerType == null) {
            return null;
        }

        String query = "SELECT cg.* " +
                "FROM community_goals cg " +
                "INNER JOIN community_goal_triggers cgt ON cgt.goal_code = cg.code " +
                "WHERE cg.enabled = 1 AND cgt.enabled = 1 AND cgt.trigger_type = ? " +
                "AND (cg.start_timestamp = 0 OR (cg.start_timestamp <= ? AND cg.start_timestamp + cg.duration_seconds > ?)) " +
                "ORDER BY cg.start_timestamp DESC LIMIT 1";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            int now = Emulator.getIntUnixTimestamp();
            statement.setString(1, triggerType.getCode());
            statement.setInt(2, now);
            statement.setInt(3, now);

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return new CommunityGoal(set);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return null;
    }

    private List<CommunityGoalLevel> getCommunityGoalLevels(String goalCode) {
        List<CommunityGoalLevel> levels = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM community_goal_levels WHERE goal_code = ? ORDER BY score_threshold ASC")) {
            statement.setString(1, goalCode);

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    levels.add(new CommunityGoalLevel(set));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return levels;
    }

    private int getNextPositiveLevelThreshold(CommunityGoal goal) {
        if (goal == null) {
            return 0;
        }

        List<CommunityGoalLevel> levels = this.getCommunityGoalLevels(goal.code);
        if (levels.isEmpty()) {
            return 0;
        }

        levels.sort(Comparator.comparingInt(level -> level.scoreThreshold));

        int highest = 0;

        for (CommunityGoalLevel level : levels) {
            if (level.scoreThreshold <= 0) {
                continue;
            }

            highest = Math.max(highest, level.scoreThreshold);

            if (level.scoreThreshold > goal.totalScore) {
                return level.scoreThreshold;
            }
        }

        return highest;
    }

    private int getUserContribution(int userId, String goalCode) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT contribution_score FROM users_community_goals WHERE user_id = ? AND goal_code = ? LIMIT 1")) {
            statement.setInt(1, userId);
            statement.setString(2, goalCode);

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return set.getInt("contribution_score");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return 0;
    }

    private int[] getRankData(List<CommunityGoalLevel> levels) {
        if (levels == null || levels.isEmpty()) {
            return new int[0];
        }

        levels.sort(Comparator.comparingInt(level -> level.level));

        int[] rankData = new int[levels.size()];

        for (int i = 0; i < levels.size(); i++) {
            rankData[i] = levels.get(i).scoreThreshold;
        }

        return rankData;
    }

    public void dispose() {
        this.disposed = true;

        if (this.rewardTask != null) {
            this.rewardTask.cancel(false);
        }

        LOGGER.info("Community Goal Manager -> Disposed!");
    }

}
