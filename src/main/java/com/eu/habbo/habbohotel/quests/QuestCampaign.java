package com.eu.habbo.habbohotel.quests;

import java.sql.ResultSet;
import java.sql.SQLException;

public class QuestCampaign {
    private final int id;
    private final String code;
    private final boolean enabled;

    public QuestCampaign(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.code = set.getString("code");
        this.enabled = set.getBoolean("enabled");
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public boolean isEnabled() {
        return enabled;
    }
}