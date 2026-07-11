package com.eu.habbo.habbohotel.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.quests.QuestCompletedMessageComposer;
import com.eu.habbo.messages.outgoing.quests.QuestMessageComposer;
import com.eu.habbo.messages.outgoing.quests.QuestsMessageComposer;

import gnu.trove.map.hash.THashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class QuestManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestManager.class);

    private final THashMap<Integer, QuestCampaign> campaigns;
    private final THashMap<String, QuestCampaign> campaignsByCode;
    private final THashMap<Integer, Quest> quests;

    public QuestManager() {
        this.campaigns = new THashMap<>();
        this.campaignsByCode = new THashMap<>();
        this.quests = new THashMap<>();
    }

    public void reload() {
        long millis = System.currentTimeMillis();

        synchronized (this.quests) {
            this.campaigns.clear();
            this.campaignsByCode.clear();
            this.quests.clear();

            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                try (Statement statement = connection.createStatement();
                        ResultSet set = statement.executeQuery("SELECT * FROM quests_campaigns WHERE enabled = 1")) {

                    while (set.next()) {
                        QuestCampaign campaign = new QuestCampaign(set);

                        this.campaigns.put(campaign.getId(), campaign);
                        this.campaignsByCode.put(campaign.getCode(), campaign);
                    }
                }

                try (Statement statement = connection.createStatement();
                        ResultSet set = statement.executeQuery(
                                "SELECT q.*, qc.code AS campaign_code " +
                                        "FROM quests q " +
                                        "INNER JOIN quests_campaigns qc ON qc.id = q.campaign_id " +
                                        "WHERE q.enabled = 1 " +
                                        "AND qc.enabled = 1 " +
                                        "ORDER BY qc.id ASC, q.sort_order ASC, q.id ASC")) {

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

    public static boolean handleTrigger(Habbo habbo, QuestType triggerType, String targetType, String targetValue,
            int amount) {
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
                if (!quest.getTargetValue().equalsIgnoreCase(targetValue))
                    return false;
            }
        }

        if (!quest.matchesConditions(habbo))
            return false;

        return progressQuest(habbo, quest, amount);
    }

    public static Quest getActiveQuest(Habbo habbo) {
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

                if (progress.isCompleted(quest))
                    continue;

                return quest;
            }
        }

        return null;
    }

    public static Quest getNextQuest(Habbo habbo) {
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

        acceptQuest(habbo, quest);

        return quest;
    }

    public static boolean progressQuest(Habbo habbo, QuestType type) {
        return handleTrigger(habbo, type);
    }

    public static boolean progressQuest(Habbo habbo, QuestType type, int amount) {
        return handleTrigger(habbo, type, amount);
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
        if (habbo == null || !habbo.isOnline())
            return false;

        if (quest == null)
            return false;

        if (amount <= 0)
            return false;

        QuestUserProgress progress;
        boolean completedNow = false;

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

            progress.setCompletedSteps(newSteps);

            if (newSteps >= quest.getTotalSteps() && progress.getCompletedAt() <= 0) {
                progress.setCompletedAt((int) (System.currentTimeMillis() / 1000));
                progress.setAccepted(false);
                completedNow = true;

                if (quest.getRewardCurrencyAmount() > 0) {
                    habbo.givePoints(quest.getActivityPointType(), quest.getRewardCurrencyAmount());
                }
            }
        }

        if (completedNow) {
            habbo.getClient().sendResponse(new QuestCompletedMessageComposer(habbo, quest, true));

            Quest nextQuest = Emulator.getGameEnvironment().getQuestManager().getVisibleQuestForCampaign(habbo,
                    quest.getCampaignId());

            List<Quest> quests = Emulator.getGameEnvironment().getQuestManager().getVisibleQuests(habbo);
            habbo.getClient().sendResponse(new QuestsMessageComposer(habbo, quests, true));

            if (nextQuest != null && nextQuest.getId() != quest.getId()) {
                habbo.getClient().sendResponse(new QuestMessageComposer(habbo, nextQuest));
            }
        } else {
            habbo.getClient().sendResponse(new QuestMessageComposer(habbo, quest));
        }

        saveQuests(habbo);

        return true;
    }

    public static void acceptQuest(Habbo habbo, Quest quest) {
        if (habbo == null || !habbo.isOnline())
            return;

        if (quest == null)
            return;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

            if (progress != null && progress.isCompleted(quest))
                return;

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

        saveQuests(habbo);
    }

    public static Quest rejectActiveQuest(Habbo habbo) {
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

                progress.setAccepted(false);
                rejectedQuest = quest;

                break;
            }
        }

        if (rejectedQuest != null) {
            saveQuests(habbo);
        }

        return rejectedQuest;
    }

    public static void claimQuest(Habbo habbo, Quest quest) {
        if (habbo == null || !habbo.isOnline())
            return;

        if (quest == null)
            return;

        boolean changed = false;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

            if (progress == null)
                return;

            if (!progress.isCompleted(quest))
                return;

            if (progress.getClaimedAt() > 0)
                return;

            progress.setClaimedAt((int) (System.currentTimeMillis() / 1000));
            changed = true;
        }

        if (changed) {
            saveQuests(habbo);
        }
    }

    public List<Quest> getVisibleQuests(Habbo habbo) {
        List<Quest> result = new ArrayList<>();

        if (habbo == null)
            return result;

        synchronized (this.quests) {
            THashMap<Integer, List<Quest>> questsByCampaign = new THashMap<>();

            for (Quest quest : this.getQuests()) {
                if (!questsByCampaign.containsKey(quest.getCampaignId())) {
                    questsByCampaign.put(quest.getCampaignId(), new ArrayList<>());
                }

                questsByCampaign.get(quest.getCampaignId()).add(quest);
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

    public Quest getVisibleQuestForCampaign(Habbo habbo, int campaignId) {
        if (habbo == null)
            return null;

        List<Quest> campaignQuests = this.getQuestsByCampaign(campaignId);

        return this.getVisibleQuestFromList(habbo, campaignQuests);
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

    public int getCompletedQuestsInCampaign(Habbo habbo, int campaignId) {
        if (habbo == null)
            return 0;

        int completed = 0;

        synchronized (this.quests) {
            for (Quest quest : this.quests.values()) {
                if (quest.getCampaignId() != campaignId)
                    continue;

                QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

                if (progress != null && progress.isCompleted(quest)) {
                    completed++;
                }
            }
        }

        return completed;
    }

    public int getQuestCountInCampaign(int campaignId) {
        int count = 0;

        synchronized (this.quests) {
            for (Quest quest : this.quests.values()) {
                if (quest.getCampaignId() == campaignId) {
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

    public QuestCampaign getCampaign(int id) {
        synchronized (this.campaigns) {
            return this.campaigns.get(id);
        }
    }

    public QuestCampaign getCampaign(String code) {
        synchronized (this.campaignsByCode) {
            return this.campaignsByCode.get(code);
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
        QuestCampaign campaign = this.getCampaign(campaignCode);

        if (campaign == null)
            return Collections.emptyList();

        return this.getQuestsByCampaign(campaign.getId());
    }

    public List<Quest> getQuestsByCampaign(int campaignId) {
        List<Quest> list = new ArrayList<>();

        synchronized (this.quests) {
            for (Quest quest : this.quests.values()) {
                if (quest.getCampaignId() == campaignId) {
                    list.add(quest);
                }
            }
        }

        list.sort(Comparator.comparingInt(Quest::getSortOrder).thenComparingInt(Quest::getId));

        return list;
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

    public THashMap<Integer, QuestCampaign> getCampaigns() {
        return this.campaigns;
    }

    public THashMap<Integer, Quest> getQuestMap() {
        return this.quests;
    }
}