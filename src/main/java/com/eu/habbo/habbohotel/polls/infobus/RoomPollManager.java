package com.eu.habbo.habbohotel.polls.infobus;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.polls.infobus.RoomPollResultMessageComposer;
import com.eu.habbo.messages.outgoing.polls.infobus.StartRoomPollMessageComposer;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class RoomPollManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomPollManager.class);

    private final TIntObjectHashMap<RoomPoll> activePolls = new TIntObjectHashMap<>();

    /**
     * Start a poll in a room. Returns false if a poll is already running in that room.
     */
    public synchronized boolean start(Room room, String question, List<String> choices, int durationMs) {
        if (activePolls.containsKey(room.getId())) return false;

        RoomPoll poll = new RoomPoll(room.getId(), question, choices, durationMs);
        activePolls.put(room.getId(), poll);

        room.sendComposer(new StartRoomPollMessageComposer(poll).compose());

        int roomId = room.getId();
        ScheduledFuture<?> task = Emulator.getThreading().run(() -> finish(roomId), durationMs);
        poll.setFinishTask(task);

        return true;
    }

    /**
     * Record a vote from a player. Silently drops invalid or duplicate votes.
     */
    public synchronized void vote(Room room, Habbo voter, int choiceIndex) {
        RoomPoll poll = activePolls.get(room.getId());
        if (poll == null) return;
        poll.vote(voter.getHabboInfo().getId(), choiceIndex);
    }

    /**
     * End the poll, broadcast results, and clean up. Idempotent.
     */
    public void finish(int roomId) {
        RoomPoll poll;
        synchronized (this) {
            poll = activePolls.remove(roomId);
        }
        if (poll == null) return;

        // Room may already be unloaded — getRoomById returns null in that case and broadcast is a safe no-op.
        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);
        if (room != null) {
            room.sendComposer(new RoomPollResultMessageComposer(poll).compose());
        }
    }

    /**
     * Called from Room.dispose() to cancel any scheduled task and drop the entry.
     */
    public synchronized void onRoomUnload(Room room) {
        RoomPoll poll = activePolls.remove(room.getId());
        if (poll != null) {
            poll.cancelFinishTask();
        }
    }

    public synchronized boolean hasActivePoll(int roomId) {
        return activePolls.containsKey(roomId);
    }
}
