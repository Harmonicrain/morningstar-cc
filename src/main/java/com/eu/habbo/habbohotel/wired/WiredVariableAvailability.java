package com.eu.habbo.habbohotel.wired;

/** Availability values exposed by July's Global Variable editor. */
public enum WiredVariableAvailability {
    ROOM_ACTIVE(1),
    PERMANENT(10),
    SHARED_PERMANENT(11);

    public final int code;

    WiredVariableAvailability(int code) {
        this.code = code;
    }

    public boolean isPersistent() {
        return this == PERMANENT || this == SHARED_PERMANENT;
    }

    public static WiredVariableAvailability fromCode(int code) {
        for (WiredVariableAvailability availability : values()) {
            if (availability.code == code) {
                return availability;
            }
        }
        return null;
    }
}
