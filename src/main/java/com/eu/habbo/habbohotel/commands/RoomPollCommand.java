package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

import java.util.Arrays;
import java.util.List;

public class RoomPollCommand extends Command {

    public RoomPollCommand() {
        super("cmd_room_poll", new String[]{"roompoll"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) return true;

        // Usage: :roompoll <duration> <question>;<choice1>;<choice2>;...
        if (params.length < 3) {
            gameClient.getHabbo().whisper("Usage: :roompoll <seconds> <question>;<choice1>;<choice2>;...", RoomChatMessageBubbles.ALERT);
            return true;
        }

        int durationSeconds;
        try {
            durationSeconds = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            gameClient.getHabbo().whisper("Invalid duration. Usage: :roompoll <seconds> <question>;<choice1>;<choice2>;...", RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (durationSeconds <= 0) {
            gameClient.getHabbo().whisper("Duration must be greater than 0.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        // Everything after params[1] is the payload, re-join with spaces then split on semicolons.
        StringBuilder payloadBuilder = new StringBuilder();
        for (int i = 2; i < params.length; i++) {
            if (i > 2) payloadBuilder.append(' ');
            payloadBuilder.append(params[i]);
        }
        String[] parts = payloadBuilder.toString().split(";");

        if (parts.length < 3) {
            gameClient.getHabbo().whisper("You must provide a question and at least 2 choices separated by semicolons.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        String question = parts[0].trim();
        List<String> choices = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length));

        // Why: client uses String.fromCharCode(97+index) for labels; alphabet has 26 letters.
        if (choices.size() > 26) {
            gameClient.getHabbo().whisper("A poll can have at most 26 choices.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        for (String choice : choices) {
            if (choice.trim().isEmpty()) {
                gameClient.getHabbo().whisper("Choice labels must not be empty.", RoomChatMessageBubbles.ALERT);
                return true;
            }
        }

        boolean started = Emulator.getGameEnvironment().getRoomPollManager().start(room, question, choices, durationSeconds * 1000);
        if (!started) {
            gameClient.getHabbo().whisper("A poll is already running in this room.", RoomChatMessageBubbles.ALERT);
        }

        return true;
    }
}
