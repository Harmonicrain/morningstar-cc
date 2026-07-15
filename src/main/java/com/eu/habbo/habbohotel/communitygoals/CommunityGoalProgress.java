package com.eu.habbo.habbohotel.communitygoals;

public class CommunityGoalProgress {
    private final boolean achieved;
    private final int personalContributionScore;
    private final int personalRank;
    private final int totalAmount;
    private final int communityHighestAchievedLevel;
    private final int scoreRemainingUntilNextLevel;
    private final int percentCompletionTowardsNextLevel;
    private final String competitionName;
    private final int timeLeft;
    private final int[] rankData;

    public CommunityGoalProgress(boolean achieved, int personalContributionScore, int personalRank, int totalAmount, int communityHighestAchievedLevel, int scoreRemainingUntilNextLevel, int percentCompletionTowardsNextLevel, String competitionName, int timeLeft, int[] rankData) {
        this.achieved = achieved;
        this.personalContributionScore = personalContributionScore;
        this.personalRank = personalRank;
        this.totalAmount = totalAmount;
        this.communityHighestAchievedLevel = communityHighestAchievedLevel;
        this.scoreRemainingUntilNextLevel = scoreRemainingUntilNextLevel;
        this.percentCompletionTowardsNextLevel = percentCompletionTowardsNextLevel;
        this.competitionName = competitionName;
        this.timeLeft = timeLeft;
        this.rankData = rankData;
    }

    static CommunityGoalProgress empty() {
        return new CommunityGoalProgress(false, 0, 0, 0, 0, 0, 0, "", 0, new int[0]);
    }

    public boolean isAchieved() {
        return achieved;
    }

    public int getPersonalContributionScore() {
        return personalContributionScore;
    }

    public int getPersonalRank() {
        return personalRank;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public int getCommunityHighestAchievedLevel() {
        return communityHighestAchievedLevel;
    }

    public int getScoreRemainingUntilNextLevel() {
        return scoreRemainingUntilNextLevel;
    }

    public int getPercentCompletionTowardsNextLevel() {
        return percentCompletionTowardsNextLevel;
    }

    public String getCompetitionName() {
        return competitionName;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public int[] getRankData() {
        return rankData;
    }
}
