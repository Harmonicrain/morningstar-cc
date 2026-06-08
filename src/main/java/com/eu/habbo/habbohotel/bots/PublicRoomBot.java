package com.eu.habbo.habbohotel.bots;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import gnu.trove.map.hash.THashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PublicRoomBot extends Bot {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicRoomBot.class);
    private static final int MIN_WALK_TIME = 3;
    private static final int MAX_WALK_TIME = 10;
    private static final int MIN_SPEAK_TIME = 20;
    private static final int MAX_SPEAK_TIME = 50;
    private static final double LISTEN_DISTANCE = Math.sqrt(14);

    private static final THashMap<Integer, PublicRoomBotData> DATA = new THashMap<>();

    private int nextWalkTime;
    private int nextSpeechTime;
    private String lastSpeech;

    public PublicRoomBot(ResultSet set) throws SQLException {
        super(set);
        this.resetTimers();
    }

    public PublicRoomBot(Bot bot) {
        super(bot);
        this.resetTimers();
    }

    public static void initialise() {
        synchronized (DATA) {
            DATA.clear();

            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement dataStatement = connection.prepareStatement(
                         "SELECT * FROM bot_public_room_data");
                 ResultSet dataSet = dataStatement.executeQuery()) {
                while (dataSet.next()) {
                    DATA.put(dataSet.getInt("bot_id"), new PublicRoomBotData(dataSet));
                }

                try (PreparedStatement drinkStatement = connection.prepareStatement(
                        "SELECT * FROM bot_public_room_drinks");
                     ResultSet drinkSet = drinkStatement.executeQuery()) {
                    while (drinkSet.next()) {
                        PublicRoomBotData data = DATA.get(drinkSet.getInt("bot_id"));
                        if (data != null) {
                            data.drinks.add(new PublicRoomBotDrink(
                                    drinkSet.getString("drink_name"),
                                    drinkSet.getInt("handitem_id")));
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        }
    }

    public static void dispose() {
        synchronized (DATA) {
            DATA.clear();
        }
    }

    @Override
    public void cycle(boolean allowBotsWalk) {
        if (this.getRoom() == null || this.getRoomUnit() == null) {
            return;
        }

        PublicRoomBotData data = this.getPublicRoomData();
        if (data == null) {
            return;
        }

        int now = Emulator.getIntUnixTimestamp();

        if (allowBotsWalk && this.canWalk() && !this.getRoomUnit().isWalking() && now > this.nextWalkTime) {
            RoomTile destination = data.randomWalkTile(this);
            if (destination != null) {
                this.getRoomUnit().setGoalLocation(destination);
            }
            this.nextWalkTime = now + randomBetween(MIN_WALK_TIME, MAX_WALK_TIME);
        }

        if (!data.ambientLines.isEmpty() && now > this.nextSpeechTime) {
            String line = data.randomAmbientLine(this.lastSpeech);
            if (line != null) {
                this.sayParsed(line, null);
                this.lastSpeech = line;
            }
            this.nextSpeechTime = now + randomBetween(MIN_SPEAK_TIME, MAX_SPEAK_TIME);
        }
    }

    @Override
    public void onUserSay(RoomChatMessage message) {
        if (message == null || message.getHabbo() == null || this.getRoom() == null || this.getRoomUnit() == null) {
            return;
        }

        if (this.getRoomUnit().hasStatus(RoomUnitStatus.MOVE)) {
            return;
        }

        if (message.getHabbo().getRoomUnit() == null) {
            return;
        }

        if (this.getRoomUnit().getCurrentLocation().distance(message.getHabbo().getRoomUnit().getCurrentLocation()) > LISTEN_DISTANCE) {
            return;
        }

        PublicRoomBotData data = this.getPublicRoomData();
        if (data == null) {
            return;
        }

        String incoming = message.getUnfilteredMessage() == null ? "" : message.getUnfilteredMessage();
        PublicRoomBotDrink drink = data.findRequestedDrink(incoming);

        if (drink == null && data.isGenericDrinkRequest(incoming) && !data.drinks.isEmpty()) {
            drink = data.drinks.get(Emulator.getRandom().nextInt(data.drinks.size()));
        }

        if (drink != null) {
            this.getRoom().giveHandItem(message.getHabbo(), drink.handItemId);
            String response = data.randomResponseLine();
            if (response != null) {
                this.sayParsed(response
                        .replace("%lowercaseDrink%", drink.name.toLowerCase(Locale.ROOT))
                        .replace("%drink%", drink.name), message.getHabbo());
            }
            return;
        }

        if (incoming.toLowerCase(Locale.ROOT).contains(this.getName().toLowerCase(Locale.ROOT))) {
            String response = data.randomUnrecognisedLine();
            if (response != null) {
                this.sayParsed(response, message.getHabbo());
            }
        }
    }

    private PublicRoomBotData getPublicRoomData() {
        synchronized (DATA) {
            return DATA.get(this.getId());
        }
    }

    private void sayParsed(String rawMessage, Habbo target) {
        ParsedSpeech speech = ParsedSpeech.parse(rawMessage);
        if (speech.message.isBlank()) {
            return;
        }

        if (speech.type == SpeechType.SHOUT) {
            if (!WiredManager.triggerUserSays(this.getRoom(), this.getRoomUnit(), speech.message)) {
                this.shout(speech.message);
            }
        } else if (speech.type == SpeechType.WHISPER && target != null) {
            this.whisper(speech.message, target);
        } else {
            if (!WiredManager.triggerUserSays(this.getRoom(), this.getRoomUnit(), speech.message)) {
                this.talk(speech.message);
            }
        }
    }

    private void resetTimers() {
        int now = Emulator.getIntUnixTimestamp();
        this.nextWalkTime = now + randomBetween(MIN_WALK_TIME, MAX_WALK_TIME);
        this.nextSpeechTime = now + randomBetween(MIN_SPEAK_TIME, MAX_SPEAK_TIME);
        this.lastSpeech = null;
    }

    private static int randomBetween(int minInclusive, int maxExclusive) {
        if (maxExclusive <= minInclusive) {
            return minInclusive;
        }
        return minInclusive + Emulator.getRandom().nextInt(maxExclusive - minInclusive);
    }

    private static final class PublicRoomBotData {
        private final List<BotTile> walkspace = new ArrayList<>();
        private final List<String> ambientLines;
        private final List<String> responseLines;
        private final List<String> unrecognisedLines;
        private final List<PublicRoomBotDrink> drinks = new ArrayList<>();

        private PublicRoomBotData(ResultSet set) throws SQLException {
            this.parseWalkspace(set.getString("walkspace"));
            this.ambientLines = parseLines(set.getString("ambient_lines"));
            this.responseLines = parseLines(set.getString("response_lines"));
            this.unrecognisedLines = parseLines(set.getString("unrecognised_lines"));
        }

        private void parseWalkspace(String walkspaceData) {
            if (walkspaceData == null || walkspaceData.isBlank()) {
                return;
            }

            for (String tile : walkspaceData.split(" ")) {
                String[] parts = tile.split(",");
                if (parts.length != 2) {
                    continue;
                }

                try {
                    this.walkspace.add(new BotTile(Short.parseShort(parts[0]), Short.parseShort(parts[1])));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        private RoomTile randomWalkTile(PublicRoomBot bot) {
            if (this.walkspace.isEmpty()) {
                return null;
            }

            for (int i = 0; i < this.walkspace.size(); i++) {
                BotTile tile = this.walkspace.get(Emulator.getRandom().nextInt(this.walkspace.size()));
                RoomTile roomTile = bot.getRoom().getLayout().getTile(tile.x, tile.y);
                if (roomTile != null && bot.getRoom().getTileManager().canWalkAt(roomTile)) {
                    return roomTile;
                }
            }

            return null;
        }

        private String randomAmbientLine(String excludedLine) {
            if (this.ambientLines.isEmpty()) {
                return null;
            }

            if (this.ambientLines.size() == 1) {
                return this.ambientLines.get(0);
            }

            // Avoid repeating the last line, but cap retries so duplicate entries can't spin forever.
            String line = this.ambientLines.get(Emulator.getRandom().nextInt(this.ambientLines.size()));
            for (int attempts = 0; attempts < 5 && line.equals(excludedLine); attempts++) {
                line = this.ambientLines.get(Emulator.getRandom().nextInt(this.ambientLines.size()));
            }

            return line;
        }

        private String randomResponseLine() {
            return randomLine(this.responseLines);
        }

        private String randomUnrecognisedLine() {
            return randomLine(this.unrecognisedLines);
        }

        private PublicRoomBotDrink findRequestedDrink(String message) {
            String incoming = message.toLowerCase(Locale.ROOT);
            for (PublicRoomBotDrink drink : this.drinks) {
                if (incoming.contains(drink.name.toLowerCase(Locale.ROOT))) {
                    return drink;
                }
            }
            return null;
        }

        private boolean isGenericDrinkRequest(String message) {
            String incoming = message.toLowerCase(Locale.ROOT);
            return incoming.contains("drink please")
                    || incoming.contains("can i have")
                    || incoming.contains("i'll have");
        }

        private static List<String> parseLines(String lines) {
            List<String> parsed = new ArrayList<>();
            if (lines == null || lines.isBlank()) {
                return parsed;
            }

            for (String line : lines.split("\\|")) {
                if (!line.isBlank()) {
                    parsed.add(line);
                }
            }
            return parsed;
        }

        private static String randomLine(List<String> lines) {
            if (lines.isEmpty()) {
                return null;
            }
            return lines.get(Emulator.getRandom().nextInt(lines.size()));
        }
    }

    private record BotTile(short x, short y) {
    }

    private record PublicRoomBotDrink(String name, int handItemId) {
    }

    private enum SpeechType {
        TALK,
        SHOUT,
        WHISPER
    }

    private record ParsedSpeech(String message, SpeechType type) {
        private static ParsedSpeech parse(String rawMessage) {
            int marker = rawMessage.lastIndexOf('#');
            if (marker == -1 || marker == rawMessage.length() - 1) {
                return new ParsedSpeech(rawMessage, SpeechType.TALK);
            }

            String suffix = rawMessage.substring(marker + 1).trim().toUpperCase(Locale.ROOT);
            if ("SHOUT".equals(suffix)) {
                return new ParsedSpeech(rawMessage.substring(0, marker), SpeechType.SHOUT);
            }

            if ("WHISPER".equals(suffix)) {
                return new ParsedSpeech(rawMessage.substring(0, marker), SpeechType.WHISPER);
            }

            return new ParsedSpeech(rawMessage, SpeechType.TALK);
        }
    }
}
