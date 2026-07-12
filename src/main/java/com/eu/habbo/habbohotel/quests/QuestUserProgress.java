package com.eu.habbo.habbohotel.quests;

import java.sql.ResultSet;
import java.sql.SQLException;

public class QuestUserProgress {
    private final int userId;
    private final int questId;

    private boolean accepted;
    private int completedSteps;
    private int completedAt;
    private int claimedAt;

    private boolean needsInsert;
    private boolean needsUpdate;

    public QuestUserProgress(int userId, int questId) {
        this.userId = userId;
        this.questId = questId;
        this.accepted = false;
        this.completedSteps = 0;
        this.completedAt = 0;
        this.claimedAt = 0;
        this.needsInsert = true;
        this.needsUpdate = true;
    }

    public QuestUserProgress(ResultSet set) throws SQLException {
        this.userId = set.getInt("user_id");
        this.questId = set.getInt("quest_id");
        this.accepted = set.getBoolean("accepted");
        this.completedSteps = set.getInt("completed_steps");
        this.completedAt = set.getInt("completed_at");
        this.claimedAt = set.getInt("claimed_at");
        this.needsInsert = false;
        this.needsUpdate = false;
    }

    public boolean isCompleted(Quest quest) {
        return this.completedSteps >= quest.getTotalSteps();
    }

    public void markForUpdate() {
        if (!this.needsInsert) {
            this.needsUpdate = true;
        }
    }

    public int getUserId() {
        return userId;
    }

    public int getQuestId() {
        return questId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        if (this.accepted == accepted) return;

        this.accepted = accepted;
        this.markForUpdate();
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(int completedSteps) {
        if (this.completedSteps == completedSteps) return;

        this.completedSteps = completedSteps;
        this.markForUpdate();
    }

    public int getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(int completedAt) {
        if (this.completedAt == completedAt) return;

        this.completedAt = completedAt;
        this.markForUpdate();
    }

    public int getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(int claimedAt) {
        if (this.claimedAt == claimedAt) return;

        this.claimedAt = claimedAt;
        this.markForUpdate();
    }

    public boolean needsInsert() {
        return needsInsert;
    }

    public void needsInsert(boolean needsInsert) {
        this.needsInsert = needsInsert;
    }

    public boolean needsUpdate() {
        return needsUpdate;
    }

    public void needsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }
}