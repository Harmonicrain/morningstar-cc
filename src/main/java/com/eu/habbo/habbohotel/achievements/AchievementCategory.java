package com.eu.habbo.habbohotel.achievements;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AchievementCategory {
    private final int id;
    private final String name;
    private final int orderNum;
    private final boolean visible;

    public AchievementCategory(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.name = set.getString("name");
        this.orderNum = set.getInt("order_num");
        this.visible = set.getInt("visible") == 1;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getOrderNum() {
        return this.orderNum;
    }

    public boolean isVisible() {
        return this.visible;
    }
}