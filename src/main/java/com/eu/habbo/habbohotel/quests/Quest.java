package com.eu.habbo.habbohotel.quests;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ISerialize;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Quest implements ISerialize {
    private final int id;
    private final int campaignId;
    private final String campaignCode;
    private final int activityPointType;
    private final QuestType triggerType;
    private final String targetType;
    private final String targetValue;
    private final String imageVersion;
    private final int rewardCurrencyAmount;
    private final String localizationCode;
    private final int totalSteps;
    private final int sortOrder;
    private final String catalogPageName;
    private final String chainCode;
    private final boolean easy;

    private final List<QuestCondition> conditions = new ArrayList<>();

    public Quest(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.campaignId = set.getInt("campaign_id");
        this.campaignCode = set.getString("campaign_code");
        this.activityPointType = set.getInt("activity_point_type");
        this.triggerType = QuestType.fromString(set.getString("trigger_type"));
        this.targetType = set.getString("target_type");
        this.targetValue = set.getString("target_value");
        this.imageVersion = set.getString("image_version");
        this.rewardCurrencyAmount = set.getInt("reward_currency_amount");
        this.localizationCode = set.getString("localization_code");
        this.totalSteps = set.getInt("total_steps");
        this.sortOrder = set.getInt("sort_order");
        this.catalogPageName = set.getString("catalog_page_name");
        this.chainCode = set.getString("chain_code");
        this.easy = set.getBoolean("easy");
    }

    public void serialize(ServerMessage message, QuestUserProgress progress, int completedQuestsInCampaign, int questCountInCampaign) {
        message.appendString(this.campaignCode);
        message.appendInt(completedQuestsInCampaign);
        message.appendInt(questCountInCampaign);
        message.appendInt(this.activityPointType);
        message.appendInt(this.id);
        message.appendBoolean(progress != null && progress.isAccepted());
        message.appendString(this.triggerType.name());
        message.appendString(this.imageVersion);
        message.appendInt(this.rewardCurrencyAmount);
        message.appendString(this.localizationCode);
        message.appendInt(progress != null ? progress.getCompletedSteps() : 0);
        message.appendInt(this.totalSteps);
        message.appendInt(this.sortOrder);
        message.appendString(this.catalogPageName);
        message.appendString(this.chainCode);
        message.appendBoolean(this.easy);
    }

    @Override
    public void serialize(ServerMessage message) {
        this.serialize(message, null, 0, 1);
    }

    public boolean isCompleted(QuestUserProgress progress) {
        return progress != null && progress.getCompletedSteps() >= this.totalSteps;
    }

    public int getId() {
        return this.id;
    }

    public int getCampaignId() {
        return this.campaignId;
    }

    public String getCampaignCode() {
        return this.campaignCode;
    }

    public int getActivityPointType() {
        return this.activityPointType;
    }

    public QuestType getTriggerType() {
        return this.triggerType;
    }

    public String getTargetType() {
        return this.targetType;
    }

    public String getTargetValue() {
        return this.targetValue;
    }

    public String getImageVersion() {
        return this.imageVersion;
    }

    public int getRewardCurrencyAmount() {
        return this.rewardCurrencyAmount;
    }

    public String getLocalizationCode() {
        return this.localizationCode;
    }

    public int getTotalSteps() {
        return this.totalSteps;
    }

    public int getSortOrder() {
        return this.sortOrder;
    }

    public String getCatalogPageName() {
        return this.catalogPageName;
    }

    public String getChainCode() {
        return this.chainCode;
    }

    public boolean isEasy() {
        return this.easy;
    }

    public void addCondition(QuestCondition condition) {
        if (condition == null) {
            return;
        }

        this.conditions.add(condition);
    }

    public List<QuestCondition> getConditions() {
        return this.conditions;
    }

    public boolean matchesConditions(Habbo habbo) {
        for (QuestCondition condition : this.conditions) {
            if (!condition.matches(habbo)) {
                return false;
            }
        }

        return true;
    }
}