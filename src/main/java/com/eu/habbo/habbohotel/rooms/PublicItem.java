package com.eu.habbo.habbohotel.rooms;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PublicItem {
    private final String id;
    private final String sprite;
    private final int x;
    private final int y;
    private final int z;
    private final int rotation;
    private final int length;
    private final int width;
    private final boolean hasDimensions;

    private final String behaviour;
    private final double topHeight;

    // Derived collision flags parsed from the behaviour CSV.
    private final boolean hasBehaviour;
    private final boolean allowWalk;
    private final boolean allowSit;
    private final boolean allowLay;
    private final boolean allowStack;

    public PublicItem(ResultSet set) throws SQLException {
        this.id = Integer.toString(set.getInt("id"), 36);
        this.sprite = set.getString("sprite");
        this.x = set.getInt("x");
        this.y = set.getInt("y");
        this.z = set.getInt("z");
        this.rotation = set.getInt("rotation");
        this.length = set.getInt("length");
        this.width = set.getInt("width");
        this.hasDimensions = set.getBoolean("has_dimensions");

        String behaviourValue = "";
        double topHeightValue = 1.0;
        try {
            behaviourValue = set.getString("behaviour");
            if (behaviourValue == null) {
                behaviourValue = "";
            }
        } catch (SQLException ignored) {
            // column may not exist on un-migrated databases; treat as visual-only
        }
        try {
            topHeightValue = set.getDouble("top_height");
        } catch (SQLException ignored) {
        }
        this.behaviour = behaviourValue.trim();
        this.topHeight = topHeightValue;

        // Parse the CSV behaviour tokens into collision flags.
        boolean collides = false;
        boolean walk = false;
        boolean sit = false;
        boolean lay = false;
        boolean stack = false;

        for (String raw : this.behaviour.split(",")) {
            String token = raw.trim().toLowerCase();
            switch (token) {
                case "solid":
                case "solid_single_tile":
                    collides = true;
                    walk = false;
                    break;
                case "can_sit_on_top":
                    collides = true;
                    sit = true;
                    walk = true;
                    break;
                case "can_lay_on_top":
                    collides = true;
                    lay = true;
                    walk = true;
                    break;
                case "can_stand_on_top":
                    collides = true;
                    walk = true;
                    break;
                case "can_stack_on_top":
                    collides = true;
                    stack = true;
                    break;
                default:
                    // 'invisible' and unknown tokens have no server-side collision effect
                    break;
            }
        }

        this.hasBehaviour = collides;
        this.allowWalk = walk;
        this.allowSit = sit;
        this.allowLay = lay;
        this.allowStack = stack;
    }

    public String getId() {
        return this.id;
    }

    public String getSprite() {
        return this.sprite;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getRotation() {
        return this.rotation;
    }

    public int getLength() {
        return this.length;
    }

    public int getWidth() {
        return this.width;
    }

    public boolean hasDimensions() {
        return this.hasDimensions;
    }

    public String getBehaviour() {
        return this.behaviour;
    }

    public double getTopHeight() {
        return this.topHeight;
    }

    public boolean hasBehaviour() {
        return this.hasBehaviour;
    }

    public boolean allowWalk() {
        return this.allowWalk;
    }

    public boolean allowSit() {
        return this.allowSit;
    }

    public boolean allowLay() {
        return this.allowLay;
    }

    public boolean allowStack() {
        return this.allowStack;
    }
}
