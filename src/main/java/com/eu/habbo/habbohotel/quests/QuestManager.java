package com.eu.habbo.habbohotel.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.quests.QuestCancelledMessageComposer;
import com.eu.habbo.messages.outgoing.quests.QuestCompletedMessageComposer;
import com.eu.habbo.messages.outgoing.quests.QuestMessageComposer;
import com.eu.habbo.messages.outgoing.quests.SeasonalQuestsComposer;
import com.eu.habbo.plugin.Event;
import com.eu.habbo.plugin.events.users.quests.UserQuestAcceptedEvent;
import com.eu.habbo.plugin.events.users.quests.UserQuestCancelledEvent;
import com.eu.habbo.plugin.events.users.quests.UserQuestCompletedEvent;
import com.eu.habbo.plugin.events.users.quests.UserQuestProgressEvent;

import gnu.trove.map.hash.THashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class QuestManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestManager.class);

    private final THashMap<Integer, Quest> quests;

    public QuestManager() {
        this.quests = new THashMap<>();
    }

    public void reload() {
        long millis = System.currentTimeMillis();

        synchronized (this.quests) {
            this.quests.clear();

            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                try (Statement statement = connection.createStatement();
                        ResultSet set = statement.executeQuery(
                                "SELECT * FROM quests " +
                                        "WHERE enabled = 1 " +
                                        "ORDER BY campaign_code ASC, sort_order ASC, id ASC")) {

                    while (set.next()) {
                        Quest quest = new Quest(set);

                        if (quest.getTriggerType() == null) {
                            LOGGER.warn("Skipping quest {} because trigger_type is invalid: {}", quest.getId(),
                                    set.getString("trigger_type"));
                            continue;
                        }

                        this.quests.put(quest.getId(), quest);
                    }
                }

                try (Statement statement = connection.createStatement();
                        ResultSet set = statement.executeQuery("SELECT * FROM quests_conditions")) {

                    while (set.next()) {
                        QuestCondition condition = new QuestCondition(set);

                        if (condition.getConditionType() == null) {
                            continue;
                        }

                        Quest quest = this.quests.get(condition.getQuestId());

                        if (quest == null) {
                            continue;
                        }

                        quest.addCondition(condition);
                    }
                }

            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
                LOGGER.error("Quest Manager -> Failed to load!");
                return;
            } catch (Exception e) {
                LOGGER.error("Caught exception", e);
                LOGGER.error("Quest Manager -> Failed to load!");
                return;
            }
        }

        LOGGER.info("Quest Manager -> Loaded! ({} MS)", System.currentTimeMillis() - millis);
    }

    public static void saveQuests(Habbo habbo) {
        if (habbo == null)
            return;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                    PreparedStatement insertStatement = connection.prepareStatement(
                            "INSERT INTO users_quests " +
                                    "(user_id, quest_id, accepted, completed_steps, completed_at, claimed_at) " +
                                    "VALUES (?, ?, ?, ?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE " +
                                    "accepted = VALUES(accepted), " +
                                    "completed_steps = VALUES(completed_steps), " +
                                    "completed_at = VALUES(completed_at), " +
                                    "claimed_at = VALUES(claimed_at)");
                    PreparedStatement updateStatement = connection.prepareStatement(
                            "UPDATE users_quests " +
                                    "SET accepted = ?, completed_steps = ?, completed_at = ?, claimed_at = ? " +
                                    "WHERE user_id = ? AND quest_id = ? " +
                                    "LIMIT 1")) {

                for (QuestUserProgress progress : habbo.getHabboStats().getQuestProgress().values()) {
                    if (progress.needsInsert()) {
                        insertStatement.setInt(1, progress.getUserId());
                        insertStatement.setInt(2, progress.getQuestId());
                        insertStatement.setBoolean(3, progress.isAccepted());
                        insertStatement.setInt(4, progress.getCompletedSteps());
                        insertStatement.setInt(5, progress.getCompletedAt());
                        insertStatement.setInt(6, progress.getClaimedAt());
                        insertStatement.addBatch();

                        progress.needsInsert(false);
                        progress.needsUpdate(false);
                    } else if (progress.needsUpdate()) {
                        updateStatement.setBoolean(1, progress.isAccepted());
                        updateStatement.setInt(2, progress.getCompletedSteps());
                        updateStatement.setInt(3, progress.getCompletedAt());
                        updateStatement.setInt(4, progress.getClaimedAt());
                        updateStatement.setInt(5, progress.getUserId());
                        updateStatement.setInt(6, progress.getQuestId());
                        updateStatement.addBatch();

                        progress.needsUpdate(false);
                    }
                }

                insertStatement.executeBatch();
                updateStatement.executeBatch();
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        }
    }

    public static boolean handleTrigger(Habbo habbo, QuestType triggerType) {
        return handleTrigger(habbo, triggerType, "", "", 1);
    }

    public static boolean handleTrigger(Habbo habbo, QuestType triggerType, int amount) {
        return handleTrigger(habbo, triggerType, "", "", amount);
    }

    public static boolean handleTrigger(Habbo habbo, QuestType triggerType, String targetType, String targetValue) {
        return handleTrigger(habbo, triggerType, targetType, targetValue, 1);
    }

    public static boolean handleTrigger(Habbo habbo, QuestType triggerType, String targetType, String targetValue, int amount) {
        if (!isQuestSystemEnabled())
            return false;

        if (habbo == null || !habbo.isOnline())
            return false;

        if (triggerType == null)
            return false;

        if (amount <= 0)
            return false;

        Quest quest = getActiveQuest(habbo);

        if (quest == null)
            return false;

        if (quest.getTriggerType() != triggerType)
            return false;

        if (quest.getTargetType() != null && !quest.getTargetType().isEmpty()) {

            if (targetType == null || targetValue == null)
                return false;

            if (!quest.getTargetType().equalsIgnoreCase(targetType))
                return false;

            if (quest.getTargetValue() == null)
                return false;

            if (quest.getTargetType().endsWith("_min")) {
                try {
                    int currentValue = Integer.parseInt(targetValue);
                    int requiredValue = Integer.parseInt(quest.getTargetValue());

                    if (currentValue < requiredValue)
                        return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            } else {
                if (!targetValueMatches(quest.getTargetValue(), targetValue))
                    return false;
            }
        }

        if (!quest.matchesConditions(habbo))
            return false;

        return progressQuest(habbo, quest, amount);
    }

    private static boolean targetValueMatches(String questTargetValue, String targetValue) {
        if (questTargetValue == null || targetValue == null)
            return false;

        String trimmedTargetValue = targetValue.trim();

        for (String acceptedValue : questTargetValue.split(";")) {
            if (acceptedValue.trim().equalsIgnoreCase(trimmedTargetValue))
                return true;
        }

        return false;
    }

    public static Quest getActiveQuest(Habbo habbo) {
        if (!isQuestSystemEnabled())
            return null;

        if (habbo == null)
            return null;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            for (QuestUserProgress progress : habbo.getHabboStats().getQuestProgress().values()) {
                if (!progress.isAccepted())
                    continue;

                Quest quest = Emulator.getGameEnvironment()
                        .getQuestManager()
                        .getQuest(progress.getQuestId());

                if (quest == null)
                    continue;

                if (Emulator.getGameEnvironment().getQuestManager().isSeasonalQuest(quest) && !Emulator.getGameEnvironment().getQuestManager().isCurrentSeasonalQuest(quest))
                    continue;

                if (progress.isCompleted(quest))
                    continue;

                return quest;
            }
        }

        return null;
    }

    public static Quest getNextQuest(Habbo habbo) {
        if (!isQuestSystemEnabled())
            return null;

        if (habbo == null)
            return null;

        QuestManager questManager = Emulator.getGameEnvironment().getQuestManager();

        List<Quest> visibleQuests = questManager.getVisibleQuests(habbo);

        for (Quest quest : visibleQuests) {
            if (quest == null)
                continue;

            QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

            if (progress != null && progress.isCompleted(quest))
                continue;

            return quest;
        }

        return null;
    }

    public static Quest activateNextQuest(Habbo habbo) {
        if (habbo == null)
            return null;

        Quest quest = getNextQuest(habbo);

        if (quest == null)
            return null;

        return acceptQuest(habbo, quest) ? quest : null;
    }

    public static Quest activateNextQuestAfterLastCompletedQuest(Habbo habbo) {
        if (!isQuestSystemEnabled())
            return null;

        if (habbo == null)
            return null;

        QuestManager questManager = Emulator.getGameEnvironment().getQuestManager();
        Quest lastCompletedQuest = questManager.getLastCompletedRegularQuest(habbo);

        if (lastCompletedQuest == null)
            return activateNextQuest(habbo);

        Quest quest = questManager.getNextVisibleUncompletedQuestForCampaign(habbo, lastCompletedQuest.getCampaignCode());

        if (quest == null)
            return null;

        return acceptQuest(habbo, quest) ? quest : null;
    }

    public static boolean progressQuest(Habbo habbo, QuestType type) {
        return handleTrigger(habbo, type);
    }

    public static boolean progressQuest(Habbo habbo, QuestType type, int amount) {
        return handleTrigger(habbo, type, amount);
    }

    public static boolean progressQuest(int habboId, Quest quest) {
        return progressQuest(habboId, quest, 1);
    }

    public static boolean progressQuest(int habboId, int questId) {
        return progressQuest(habboId, questId, 1);
    }

    public static boolean progressQuest(int habboId, int questId, int amount) {
        Quest quest = Emulator.getGameEnvironment().getQuestManager().getQuest(questId);

        return progressQuest(habboId, quest, amount);
    }

    public static boolean progressQuest(int habboId, Quest quest, int amount) {
        if (!isQuestSystemEnabled())
            return false;

        if (habboId <= 0)
            return false;

        if (quest == null)
            return false;

        if (amount <= 0)
            return false;

        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(habboId);

        if (habbo != null && habbo.isOnline()) {
            return progressQuest(habbo, quest, amount);
        }

        if (!isOfflineProgressQueueEnabled())
            return false;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users_quests_queue (user_id, quest_id, amount) VALUES (?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE amount = amount + ?")) {
            statement.setInt(1, habboId);
            statement.setInt(2, quest.getId());
            statement.setInt(3, amount);
            statement.setInt(4, amount);
            statement.execute();
            return true;
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return false;
    }

    public static boolean progressQuest(Habbo habbo, int questId) {
        return progressQuest(habbo, questId, 1);
    }

    public static boolean progressQuest(Habbo habbo, int questId, int amount) {
        Quest quest = Emulator.getGameEnvironment().getQuestManager().getQuest(questId);

        return progressQuest(habbo, quest, amount);
    }

    public static boolean progressQuest(Habbo habbo, Quest quest) {
        return progressQuest(habbo, quest, 1);
    }

    public static boolean progressQuest(Habbo habbo, Quest quest, int amount) {
        if (!isQuestSystemEnabled())
            return false;

        if (habbo == null || !habbo.isOnline())
            return false;

        if (quest == null)
            return false;

        if (amount <= 0)
            return false;

        if (Emulator.getGameEnvironment().getQuestManager().isSeasonalQuest(quest) && !Emulator.getGameEnvironment().getQuestManager().isCurrentSeasonalQuest(quest))
            return false;

        QuestUserProgress progress;
        boolean completedNow = false;
        boolean shouldGiveReward = false;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            progress = habbo.getHabboStats().getQuestProgress(quest);

            if (progress == null) {
                progress = new QuestUserProgress(habbo.getHabboInfo().getId(), quest.getId());
                progress.setAccepted(true);

                habbo.getHabboStats().setQuestProgress(progress);
            }

            if (!progress.isAccepted())
                return false;

            if (progress.isCompleted(quest))
                return false;

            int oldSteps = progress.getCompletedSteps();
            int newSteps = Math.min(quest.getTotalSteps(), oldSteps + amount);

            if (newSteps == oldSteps)
                return false;

            if (Emulator.getPluginManager().isRegistered(UserQuestProgressEvent.class, true)) {
                Event userQuestProgressEvent = new UserQuestProgressEvent(habbo, quest, amount, oldSteps, newSteps);
                Emulator.getPluginManager().fireEvent(userQuestProgressEvent);

                if (userQuestProgressEvent.isCancelled())
                    return false;
            }

            progress.setCompletedSteps(newSteps);

            if (newSteps >= quest.getTotalSteps() && progress.getCompletedAt() <= 0) {
                progress.setCompletedAt((int) (System.currentTimeMillis() / 1000));
                progress.setAccepted(false);
                completedNow = true;
                shouldGiveReward = quest.getRewardCurrencyAmount() > 0;
            }
        }

        if (completedNow) {
            if (Emulator.getPluginManager().isRegistered(UserQuestCompletedEvent.class, true)) {
                Event userQuestCompletedEvent = new UserQuestCompletedEvent(habbo, quest, quest.getRewardCurrencyAmount(), quest.getActivityPointType());
                Emulator.getPluginManager().fireEvent(userQuestCompletedEvent);

                if (userQuestCompletedEvent.isCancelled())
                    return true;
            }

            if (shouldGiveReward) {
                habbo.givePoints(quest.getActivityPointType(), quest.getRewardCurrencyAmount());
            }

            if (Emulator.getGameEnvironment().getQuestManager().isSeasonalQuest(quest)) {
                habbo.getClient().sendResponse(new QuestCancelledMessageComposer(false));
                habbo.getClient().sendResponse(new SeasonalQuestsComposer(habbo,
                        Emulator.getGameEnvironment().getQuestManager().getSeasonalQuests(habbo)));
                return true;
            }

            habbo.getClient().sendResponse(new QuestCompletedMessageComposer(habbo, quest, true));

            Quest nextQuest = Emulator.getGameEnvironment().getQuestManager().getVisibleQuestForCampaign(habbo, quest.getCampaignCode());

            if (nextQuest != null && nextQuest.getId() != quest.getId()) {
                habbo.getClient().sendResponse(new QuestMessageComposer(habbo, nextQuest));
            }
        } else {
            habbo.getClient().sendResponse(new QuestMessageComposer(habbo, quest));
        }

        return true;
    }

    public static void drainQuestProgressQueue(Habbo habbo) {
        if (!isQuestSystemEnabled() || !isOfflineProgressQueueEnabled())
            return;

        if (habbo == null || !habbo.isOnline())
            return;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement queueStatement = connection.prepareStatement("SELECT * FROM users_quests_queue WHERE user_id = ?")) {
            queueStatement.setInt(1, habbo.getHabboInfo().getId());

            try (ResultSet set = queueStatement.executeQuery()) {
                while (set.next()) {
                    Quest quest = Emulator.getGameEnvironment().getQuestManager().getQuest(set.getInt("quest_id"));
                    QuestManager.progressQuest(habbo, quest, set.getInt("amount"));
                }
            }

            try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM users_quests_queue WHERE user_id = ?")) {
                deleteStatement.setInt(1, habbo.getHabboInfo().getId());
                deleteStatement.execute();
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    public static boolean acceptQuest(Habbo habbo, Quest quest) {
        if (!isQuestSystemEnabled())
            return false;

        if (habbo == null || !habbo.isOnline())
            return false;

        if (quest == null)
            return false;

        if (Emulator.getGameEnvironment().getQuestManager().isSeasonalQuest(quest) && !Emulator.getGameEnvironment().getQuestManager().isCurrentSeasonalQuest(quest))
            return false;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

            if (progress != null && progress.isCompleted(quest))
                return false;

            if (Emulator.getPluginManager().isRegistered(UserQuestAcceptedEvent.class, true)) {
                Event userQuestAcceptedEvent = new UserQuestAcceptedEvent(habbo, quest);
                Emulator.getPluginManager().fireEvent(userQuestAcceptedEvent);

                if (userQuestAcceptedEvent.isCancelled())
                    return false;
            }

            for (QuestUserProgress activeProgress : habbo.getHabboStats().getQuestProgress().values()) {
                if (activeProgress.isAccepted() && activeProgress.getQuestId() != quest.getId()) {
                    activeProgress.setAccepted(false);
                }
            }

            if (progress == null) {
                progress = new QuestUserProgress(habbo.getHabboInfo().getId(), quest.getId());
                habbo.getHabboStats().setQuestProgress(progress);
            }

            progress.setAccepted(true);
        }

        return true;
    }

    public static boolean forceAcceptQuest(Habbo habbo, Quest quest) {
        if (!isQuestSystemEnabled())
            return false;

        if (habbo == null || !habbo.isOnline())
            return false;

        if (quest == null)
            return false;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            if (Emulator.getPluginManager().isRegistered(UserQuestAcceptedEvent.class, true)) {
                Event userQuestAcceptedEvent = new UserQuestAcceptedEvent(habbo, quest);
                Emulator.getPluginManager().fireEvent(userQuestAcceptedEvent);

                if (userQuestAcceptedEvent.isCancelled())
                    return false;
            }

            for (QuestUserProgress activeProgress : habbo.getHabboStats().getQuestProgress().values()) {
                if (activeProgress.isAccepted() && activeProgress.getQuestId() != quest.getId()) {
                    activeProgress.setAccepted(false);
                }
            }

            QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

            if (progress == null) {
                progress = new QuestUserProgress(habbo.getHabboInfo().getId(), quest.getId());
                habbo.getHabboStats().setQuestProgress(progress);
            }

            progress.setCompletedSteps(0);
            progress.setCompletedAt(0);
            progress.setClaimedAt(0);
            progress.setAccepted(true);
        }

        return true;
    }

    public static Quest rejectActiveQuest(Habbo habbo) {
        if (!isQuestSystemEnabled())
            return null;

        if (habbo == null || !habbo.isOnline())
            return null;

        Quest rejectedQuest = null;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            for (QuestUserProgress progress : habbo.getHabboStats().getQuestProgress().values()) {
                if (!progress.isAccepted())
                    continue;

                Quest quest = Emulator.getGameEnvironment()
                        .getQuestManager()
                        .getQuest(progress.getQuestId());

                if (quest == null)
                    continue;

                if (Emulator.getPluginManager().isRegistered(UserQuestCancelledEvent.class, true)) {
                    Event userQuestCancelledEvent = new UserQuestCancelledEvent(habbo, quest);
                    Emulator.getPluginManager().fireEvent(userQuestCancelledEvent);

                    if (userQuestCancelledEvent.isCancelled())
                        return null;
                }

                progress.setAccepted(false);
                rejectedQuest = quest;

                break;
            }
        }

        return rejectedQuest;
    }

    public static void claimQuest(Habbo habbo, Quest quest) {
        if (habbo == null || !habbo.isOnline())
            return;

        if (quest == null)
            return;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

            if (progress == null)
                return;

            if (!progress.isCompleted(quest))
                return;

            if (progress.getClaimedAt() > 0)
                return;

            progress.setClaimedAt((int) (System.currentTimeMillis() / 1000));
        }
    }

    public Quest getDailyQuest(Habbo habbo, boolean easy, int index) {
        if (!isQuestSystemEnabled() || !isDailyQuestSystemEnabled())
            return null;

        if (habbo == null)
            return null;

        if (index < 0)
            return null;

        Quest activeDailyQuest = this.getActiveDailyQuest(habbo);
        if (activeDailyQuest != null)
            return activeDailyQuest;

        List<Quest> dailyQuests = this.getAvailableDailyQuests(habbo, easy);

        if (index >= dailyQuests.size())
            return null;

        return dailyQuests.get(index);
    }

    private Quest getActiveDailyQuest(Habbo habbo) {
        Quest activeQuest = QuestManager.getActiveQuest(habbo);

        if (activeQuest == null)
            return null;

        if (!this.isDailyQuest(activeQuest))
            return null;

        return activeQuest;
    }

    public int getDailyQuestCount(Habbo habbo, boolean easy) {
        if (!isQuestSystemEnabled() || !isDailyQuestSystemEnabled())
            return 0;

        return this.getAvailableDailyQuests(habbo, easy).size();
    }

    private List<Quest> getAvailableDailyQuests(Habbo habbo, boolean easy) {
        List<Quest> dailyQuests = new ArrayList<>();

        if (habbo == null)
            return dailyQuests;

        synchronized (this.quests) {
            for (Quest quest : this.quests.values()) {
                if (!quest.isDaily())
                    continue;

                if (quest.isEasy() != easy)
                    continue;

                if (!quest.matchesConditions(habbo))
                    continue;

                QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

                if (progress != null && progress.isCompleted(quest))
                    continue;

                dailyQuests.add(quest);
            }
        }

        dailyQuests.sort(Comparator.comparingInt(Quest::getSortOrder).thenComparingInt(Quest::getId));

        return dailyQuests;
    }

    public Quest rejectDailyQuest(Habbo habbo) {
        if (!isQuestSystemEnabled() || !isDailyQuestSystemEnabled())
            return null;

        if (habbo == null || !habbo.isOnline())
            return null;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            for (QuestUserProgress progress : habbo.getHabboStats().getQuestProgress().values()) {
                if (!progress.isAccepted())
                    continue;

                Quest quest = this.getQuest(progress.getQuestId());

                if (quest == null)
                    continue;

                if (!quest.isDaily())
                    continue;

                if (Emulator.getPluginManager().isRegistered(UserQuestCancelledEvent.class, true)) {
                    Event userQuestCancelledEvent = new UserQuestCancelledEvent(habbo, quest);
                    Emulator.getPluginManager().fireEvent(userQuestCancelledEvent);

                    if (userQuestCancelledEvent.isCancelled())
                        return null;
                }

                progress.setAccepted(false);
                return quest;
            }
        }

        return null;
    }

    public boolean isDailyQuest(Quest quest) {
        if (quest == null)
            return false;

        return quest.isDaily();
    }

    public List<Quest> getSeasonalQuests(Habbo habbo) {
        List<Quest> seasonalQuests = new ArrayList<>();

        if (!isQuestSystemEnabled() || !isSeasonalQuestSystemEnabled())
            return seasonalQuests;

        int currentDay = this.getCurrentSeasonalQuestDay();

        synchronized (this.quests) {
            for (Quest quest : this.quests.values()) {
                if (!this.isSeasonalQuest(quest))
                    continue;

                if (quest.getSortOrder() > currentDay)
                    continue;

                QuestUserProgress progress = habbo != null ? habbo.getHabboStats().getQuestProgress(quest) : null;

                if (quest.getSortOrder() < currentDay && (progress == null || !progress.isCompleted(quest)))
                    continue;

                seasonalQuests.add(quest);
            }
        }

        seasonalQuests.sort(Comparator.comparingInt(Quest::getSortOrder).thenComparingInt(Quest::getId));

        return seasonalQuests;
    }

    public boolean isCurrentSeasonalQuest(Quest quest) {
        if (quest == null)
            return false;

        if (!this.isSeasonalQuest(quest))
            return true;

        if (!isSeasonalQuestSystemEnabled())
            return false;

        return quest.getSortOrder() == this.getCurrentSeasonalQuestDay();
    }

    public boolean isSeasonalQuest(Quest quest) {
        if (quest == null || !quest.isSeasonal())
            return false;

        String campaignPrefix = this.getSeasonalCampaignPrefix();

        return campaignPrefix.isEmpty() || (quest.getCampaignCode() != null && quest.getCampaignCode().startsWith(campaignPrefix));
    }

    public String getSeasonalCampaignPrefix() {
        return Emulator.getConfig().getValue("quests.seasonal.campaign_prefix", "").trim();
    }

    public int getCurrentSeasonalQuestDay() {
        LocalDate startDate = this.getSeasonalQuestStartDate();
        LocalDate today = Instant.ofEpochSecond(Emulator.getIntUnixTimestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        long elapsedDays = ChronoUnit.DAYS.between(startDate, today);

        if (elapsedDays < 0)
            return 1;

        return (int) elapsedDays + 1;
    }

    private LocalDate getSeasonalQuestStartDate() {
        String start = Emulator.getConfig().getValue("quests.seasonal.start", "").trim();

        if (start.isEmpty())
            return LocalDate.now(ZoneId.systemDefault());

        try {
            return LocalDate.parse(start);
        } catch (DateTimeParseException ignored) {
        }

        LOGGER.warn("Invalid {} value: {}. Expected yyyy-MM-dd.", "quests.seasonal.start", start);

        return LocalDate.now(ZoneId.systemDefault());
    }

    public List<Quest> getVisibleQuests(Habbo habbo) {
        List<Quest> result = new ArrayList<>();

        if (!isQuestSystemEnabled())
            return result;

        if (habbo == null)
            return result;

        synchronized (this.quests) {
            THashMap<String, List<Quest>> questsByCampaign = new THashMap<>();

            for (Quest quest : this.getQuests()) {
                if (quest.isSeasonal() || quest.isDaily())
                    continue;

                if (!questsByCampaign.containsKey(quest.getCampaignCode())) {
                    questsByCampaign.put(quest.getCampaignCode(), new ArrayList<>());
                }

                questsByCampaign.get(quest.getCampaignCode()).add(quest);
            }

            for (List<Quest> campaignQuests : questsByCampaign.values()) {
                Quest visibleQuest = this.getVisibleQuestFromList(habbo, campaignQuests);

                if (visibleQuest != null) {
                    result.add(visibleQuest);
                }
            }
        }

        result.sort(Comparator.comparingInt(Quest::getSortOrder).thenComparingInt(Quest::getId));

        return result;
    }

    public Quest getVisibleQuestForCampaign(Habbo habbo, String campaignCode) {
        if (habbo == null)
            return null;

        List<Quest> campaignQuests = this.getQuestsByCampaign(campaignCode);

        return this.getVisibleQuestFromList(habbo, campaignQuests);
    }

    private Quest getNextVisibleUncompletedQuestForCampaign(Habbo habbo, String campaignCode) {
        Quest quest = this.getVisibleQuestForCampaign(habbo, campaignCode);

        if (quest == null)
            return null;

        QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

        if (progress != null && progress.isCompleted(quest))
            return null;

        return quest;
    }

    private Quest getLastCompletedRegularQuest(Habbo habbo) {
        if (habbo == null)
            return null;

        Quest lastQuest = null;
        int lastCompletedAt = -1;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            for (QuestUserProgress progress : habbo.getHabboStats().getQuestProgress().values()) {
                Quest quest = this.getQuest(progress.getQuestId());

                if (quest == null)
                    continue;

                if (quest.isDaily() || quest.isSeasonal())
                    continue;

                if (!progress.isCompleted(quest))
                    continue;

                if (progress.getCompletedAt() > lastCompletedAt) {
                    lastQuest = quest;
                    lastCompletedAt = progress.getCompletedAt();
                    continue;
                }

                if (progress.getCompletedAt() == lastCompletedAt && lastQuest != null && quest.getId() > lastQuest.getId()) {
                    lastQuest = quest;
                }
            }
        }

        return lastQuest;
    }

    private Quest getVisibleQuestFromList(Habbo habbo, List<Quest> campaignQuests) {
        if (habbo == null || campaignQuests == null || campaignQuests.isEmpty())
            return null;

        campaignQuests.sort(Comparator.comparingInt(Quest::getSortOrder).thenComparingInt(Quest::getId));

        Quest activeQuest = null;
        Quest nextQuest = null;
        Quest lastQuest = null;

        for (Quest quest : campaignQuests) {
            lastQuest = quest;

            QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

            if (progress != null && progress.isAccepted() && !progress.isCompleted(quest)) {
                activeQuest = quest;
                break;
            }

            if (nextQuest == null && (progress == null || !progress.isCompleted(quest))) {
                nextQuest = quest;
            }
        }

        if (activeQuest != null)
            return activeQuest;

        if (nextQuest != null)
            return nextQuest;

        return lastQuest;
    }

    public boolean hasCompletedQuest(Habbo habbo, Quest quest) {
        if (habbo == null || quest == null)
            return false;

        QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

        return progress != null && progress.isCompleted(quest);
    }

    public int getCompletedQuestsInCampaign(Habbo habbo, String campaignCode) {
        if (habbo == null)
            return 0;

        int completed = 0;

        synchronized (this.quests) {
            for (Quest quest : this.quests.values()) {
                if (!campaignMatches(quest, campaignCode))
                    continue;

                QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

                if (progress != null && progress.isCompleted(quest)) {
                    completed++;
                }
            }
        }

        return completed;
    }

    public int getQuestCountInCampaign(String campaignCode) {
        int count = 0;

        synchronized (this.quests) {
            for (Quest quest : this.quests.values()) {
                if (campaignMatches(quest, campaignCode)) {
                    count++;
                }
            }
        }

        return count;
    }

    public Quest getQuest(int id) {
        synchronized (this.quests) {
            return this.quests.get(id);
        }
    }

    public List<Quest> getQuests() {
        synchronized (this.quests) {
            List<Quest> list = new ArrayList<>(this.quests.values());

            list.sort(Comparator.comparingInt(Quest::getSortOrder).thenComparingInt(Quest::getId));

            return list;
        }
    }

    public List<Quest> getQuestsByCampaign(String campaignCode) {
        List<Quest> list = new ArrayList<>();

        synchronized (this.quests) {
            for (Quest quest : this.quests.values()) {
                if (campaignMatches(quest, campaignCode)) {
                    list.add(quest);
                }
            }
        }

        list.sort(Comparator.comparingInt(Quest::getSortOrder).thenComparingInt(Quest::getId));

        return list;
    }

    private static boolean campaignMatches(Quest quest, String campaignCode) {
        if (quest == null)
            return false;

        if (quest.getCampaignCode() == null)
            return campaignCode == null;

        return quest.getCampaignCode().equalsIgnoreCase(campaignCode);
    }

    public List<Quest> getQuestsByType(QuestType type) {
        if (type == null)
            return Collections.emptyList();

        List<Quest> list = new ArrayList<>();

        synchronized (this.quests) {
            for (Quest quest : this.quests.values()) {
                if (quest.getTriggerType() == type) {
                    list.add(quest);
                }
            }
        }

        list.sort(Comparator.comparingInt(Quest::getSortOrder).thenComparingInt(Quest::getId));

        return list;
    }

    public THashMap<Integer, Quest> getQuestMap() {
        return this.quests;
    }

    public static boolean isQuestSystemEnabled() {
        return Emulator.getConfig().getBoolean("quests.enabled", false);
    }

    public static boolean isDailyQuestSystemEnabled() {
        return Emulator.getConfig().getBoolean("quests.daily.enabled", false);
    }

    public static boolean isSeasonalQuestSystemEnabled() {
        return Emulator.getConfig().getBoolean("quests.seasonal.enabled", false);
    }

    public static boolean isOfflineProgressQueueEnabled() {
        return Emulator.getConfig().getBoolean("quests.progress.offline_queue", false);
    }
}
