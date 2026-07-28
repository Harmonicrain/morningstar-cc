package com.eu.habbo.habbohotel.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.leaderboards.BadgeLeaderboardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class HabboBadge implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HabboBadge.class);
    private int id;
    private String code;
    private String persistedCode;
    private int slot;
    private Habbo habbo;
    private boolean needsUpdate;
    private boolean needsInsert;

    public HabboBadge(ResultSet set, Habbo habbo) throws SQLException {
        this.id = set.getInt("id");
        this.code = set.getString("badge_code");
        this.persistedCode = this.code;
        this.slot = set.getInt("slot_id");
        this.habbo = habbo;
        this.needsUpdate = false;
        this.needsInsert = false;
    }

    public HabboBadge(int id, String code, int slot, Habbo habbo) {
        this.id = id;
        this.code = code;
        this.persistedCode = null;
        this.slot = slot;
        this.habbo = habbo;
        this.needsUpdate = false;
        this.needsInsert = true;
    }

    public int getId() {
        return this.id;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getSlot() {
        return this.slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    @Override
    public void run() {
        try {
            if (this.needsInsert) {
                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO users_badges (user_id, slot_id, badge_code) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, this.habbo.getHabboInfo().getId());
                    statement.setInt(2, this.slot);
                    statement.setString(3, this.code);
                    statement.execute();
                    try (ResultSet set = statement.getGeneratedKeys()) {
                        if (set.next()) {
                            this.id = set.getInt(1);
                        }
                    }
                }
                this.needsInsert = false;
                this.persistedCode = this.code;
                BadgeLeaderboardManager.getInstance().badgeOwnershipChanged(this.code);
            } else if (this.needsUpdate) {
                String previousCode = this.persistedCode;
                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE users_badges SET slot_id = ?, badge_code = ? WHERE id = ? AND user_id = ?")) {
                    statement.setInt(1, this.slot);
                    statement.setString(2, this.code);
                    statement.setInt(3, this.id);
                    statement.setInt(4, this.habbo.getHabboInfo().getId());
                    statement.execute();
                }
                this.needsUpdate = false;
                this.persistedCode = this.code;

                if (previousCode != null && !previousCode.equalsIgnoreCase(this.code)) {
                    BadgeLeaderboardManager.getInstance().badgeOwnershipChanged(previousCode);
                    BadgeLeaderboardManager.getInstance().badgeOwnershipChanged(this.code);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    public void needsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }

    public void needsInsert(boolean needsInsert) {
        this.needsInsert = needsInsert;
    }
}
