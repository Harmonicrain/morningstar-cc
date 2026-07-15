package com.eu.habbo.habbohotel.communitygoals;

import java.util.Comparator;
import java.util.List;

class CommunityGoalLevelProgress {
    final int level;
    final int scoreRemaining;
    final int percent;

    private CommunityGoalLevelProgress(int level, int scoreRemaining, int percent) {
        this.level = level;
        this.scoreRemaining = scoreRemaining;
        this.percent = percent;
    }

    static CommunityGoalLevelProgress calculate(int totalScore, List<CommunityGoalLevel> levels) {
        if (levels.isEmpty()) {
            return new CommunityGoalLevelProgress(0, 0, 0);
        }

        levels.sort(Comparator.comparingInt(level -> level.scoreThreshold));

        if (totalScore < 0) {
            return calculateNegativeProgress(totalScore, levels);
        }

        return calculatePositiveProgress(totalScore, levels);
    }

    private static CommunityGoalLevelProgress calculatePositiveProgress(int totalScore, List<CommunityGoalLevel> levels) {
        CommunityGoalLevel current = levels.get(0);
        CommunityGoalLevel next = null;

        for (CommunityGoalLevel level : levels) {
            if (level.scoreThreshold <= totalScore) {
                current = level;
                next = null;
            } else {
                next = level;
                break;
            }
        }

        if (next == null) {
            return new CommunityGoalLevelProgress(current.level, 0, 100);
        }

        int distance = Math.max(next.scoreThreshold - current.scoreThreshold, 1);
        int progress = Math.max(totalScore - current.scoreThreshold, 0);
        return new CommunityGoalLevelProgress(current.level, next.scoreThreshold - totalScore, Math.min((progress * 100) / distance, 100));
    }

    private static CommunityGoalLevelProgress calculateNegativeProgress(int totalScore, List<CommunityGoalLevel> levels) {
        CommunityGoalLevel current = levels.get(levels.size() - 1);
        CommunityGoalLevel next = null;

        for (int i = levels.size() - 1; i >= 0; i--) {
            CommunityGoalLevel level = levels.get(i);
            if (level.scoreThreshold >= totalScore) {
                current = level;
                next = null;
            } else {
                next = level;
                break;
            }
        }

        if (next == null) {
            return new CommunityGoalLevelProgress(current.level, 0, 100);
        }

        int distance = Math.max(current.scoreThreshold - next.scoreThreshold, 1);
        int progress = Math.max(current.scoreThreshold - totalScore, 0);
        return new CommunityGoalLevelProgress(current.level, next.scoreThreshold - totalScore, Math.min((progress * 100) / distance, 100));
    }
}
