package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.guilds.forums.UnreadForumsCountMessageComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GetUnreadForumsCountMessageEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetUnreadForumsCountMessageEvent.class);

    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new UnreadForumsCountMessageComposer(this.getUnreadForumsCount()));
    }

    private int getUnreadForumsCount() {
        if (this.client == null || this.client.getHabbo() == null) {
            return 0;
        }

        int userId = this.client.getHabbo().getHabboInfo().getId();
        boolean isStaff = this.client.getHabbo().hasPermission(Permission.ACC_MODTOOL_TICKET_Q);

        String query =
                "SELECT COUNT(*) AS unread_forums FROM (" +
                        "SELECT g.id " +
                        "FROM guilds g " +
                        "INNER JOIN guilds_forums_threads t ON t.guild_id = g.id " +
                        "INNER JOIN guilds_forums_comments c ON c.thread_id = t.id " +
                        "LEFT JOIN (" +
                        "SELECT guild_id, MAX(timestamp) AS last_seen_at " +
                        "FROM guild_forum_views " +
                        "WHERE user_id = ? " +
                        "GROUP BY guild_id" +
                        ") v ON v.guild_id = g.id " +
                        "LEFT JOIN guilds_members gm ON gm.guild_id = g.id AND gm.user_id = ? AND gm.level_id IN (0, 1, 2) " +
                        "WHERE g.forum = '1' " +
                        "AND c.created_at > COALESCE(v.last_seen_at, 0) " +
                        "AND (? = 1 OR g.read_forum = 'EVERYONE' OR gm.user_id IS NOT NULL OR g.user_id = ?) " +
                        "GROUP BY g.id" +
                        ") unread";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, isStaff ? 1 : 0);
            statement.setInt(4, userId);

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return set.getInt("unread_forums");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return 0;
    }
}
