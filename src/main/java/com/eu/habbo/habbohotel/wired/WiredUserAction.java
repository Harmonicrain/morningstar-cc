package com.eu.habbo.habbohotel.wired;

/**
 * Official May 2026 Wired user action codes used by trigger 16 and the
 * matching action condition/selector family.
 */
public final class WiredUserAction {
    public static final int WAVE = 0;
    public static final int BLOW = 1;
    public static final int LAUGH = 2;
    public static final int RESPECT = 3;
    public static final int AWAKE = 4;
    public static final int SLEEP = 5;
    public static final int SIT = 6;
    public static final int STAND = 7;
    public static final int LAY = 8;
    public static final int SIGN = 10;
    public static final int DANCE = 11;
    public static final int SIXTY_SEVEN = 67;

    private WiredUserAction() {
    }

    public static int fromAvatarExpression(int action) {
        switch (action) {
            case 0:
                return AWAKE;
            case 1:
                return WAVE;
            case 2:
                return BLOW;
            case 3:
                return LAUGH;
            case 5:
                return SLEEP;
            case 7:
                return RESPECT;
            case 67:
                return SIXTY_SEVEN;
            default:
                return action;
        }
    }

    public static String signExtra(int signId) {
        return String.valueOf(signId);
    }

    public static String danceExtra(int danceId) {
        return "dance " + danceId;
    }
}
