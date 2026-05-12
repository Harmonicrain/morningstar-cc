package com.eu.habbo.habbohotel.polls.infobus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class RoomPoll {

    private final int roomId;
    private final String question;
    private final List<String> choices;
    private final int durationMs;
    private final long startedAt;

    // userId -> 1-based choice index
    private final Map<Integer, Integer> votesByUserId = new HashMap<>();

    // Handle kept so onRoomUnload can cancel the scheduled finish.
    private volatile ScheduledFuture<?> finishTask;

    public RoomPoll(int roomId, String question, List<String> choices, int durationMs) {
        this.roomId = roomId;
        this.question = question;
        this.choices = Collections.unmodifiableList(choices);
        this.durationMs = durationMs;
        this.startedAt = System.currentTimeMillis();
    }

    /**
     * Record a vote. Returns false if the index is out of range or the user already voted.
     */
    public synchronized boolean vote(int userId, int choiceIndex) {
        if (choiceIndex < 1 || choiceIndex > choices.size()) return false;
        if (votesByUserId.containsKey(userId)) return false;
        votesByUserId.put(userId, choiceIndex);
        return true;
    }

    /**
     * Returns per-choice vote counts (0-based index into returned array maps to choices).
     * Last element [choices.size()] is total votes.
     */
    public synchronized int[] tally() {
        int[] counts = new int[choices.size() + 1];
        for (int choice : votesByUserId.values()) {
            counts[choice - 1]++;
            counts[choices.size()]++;
        }
        return counts;
    }

    public int getRoomId() { return roomId; }
    public String getQuestion() { return question; }
    public List<String> getChoices() { return choices; }
    public int getDurationMs() { return durationMs; }
    public long getStartedAt() { return startedAt; }

    public void setFinishTask(ScheduledFuture<?> task) { this.finishTask = task; }

    public void cancelFinishTask() {
        if (finishTask != null) {
            finishTask.cancel(false);
            finishTask = null;
        }
    }
}
