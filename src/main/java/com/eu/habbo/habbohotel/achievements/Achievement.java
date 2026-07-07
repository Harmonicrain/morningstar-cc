package com.eu.habbo.habbohotel.achievements;

import gnu.trove.map.hash.THashMap;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Achievement {

    public final int id;

    public final String name;

    public final THashMap<Integer, AchievementLevel> levels;

    private final int categoryId;

    private AchievementCategory category;

    private final boolean badgeHasLevel;

    private int orderNum = 0;

    public Achievement(ResultSet set) throws SQLException {
        this.levels = new THashMap<>();

        this.id = set.getInt("id");
        this.name = set.getString("name");
        this.categoryId = set.getInt("category_id");
        this.badgeHasLevel = set.getInt("badge_has_level") == 1;

        this.addLevel(new AchievementLevel(set));
    }

    public void addLevel(AchievementLevel level) {
        synchronized (this.levels) {
            this.levels.put(level.level, level);

            if (level.level == 1) {
                this.orderNum = level.orderNum;
            }
        }
    }

    public AchievementLevel getLevelForProgress(int progress) {
        AchievementLevel l = null;
        if (progress > 0) {
            for (AchievementLevel level : this.levels.values()) {
                if (progress >= level.progress) {
                    if (l != null) {
                        if (l.level > level.level) {
                            continue;
                        }
                    }

                    l = level;
                }
            }
        }
        return l;
    }

    public AchievementLevel getNextLevel(int currentLevel) {
        for (AchievementLevel level : this.levels.values()) {
            if (level.level == (currentLevel + 1))
                return level;
        }

        return null;
    }

    public AchievementLevel firstLevel() {
        return this.levels.get(1);
    }

    public void clearLevels() {
        this.levels.clear();
    }

    public String getBadgeCode(AchievementLevel level) {
        return this.getBadgeCode(level != null ? level.level : 1);
    }

    public String getBadgeCode(int level) {
        if (!this.badgeHasLevel) {
            return "ACH_" + this.name;
        }
        return "ACH_" + this.name + level;
    }

    public int getOrderNum() {
        return this.orderNum;
    }

    public int getCategoryId() {
        return this.categoryId;
    }

    public AchievementCategory getCategory() {
        return this.category;
    }

    public void setCategory(AchievementCategory category) {
        this.category = category;
    }
}
