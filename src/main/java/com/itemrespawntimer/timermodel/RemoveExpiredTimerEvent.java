package com.itemrespawntimer.timermodel;

public enum RemoveExpiredTimerEvent {
    CAN_SEE_LOCATION("you can see the location"),
    SAME_WORLD("you're in the same world"),
    T_MINUS_ZERO("at T-0"),
    T_PLUS_60_SECONDS("at T + 60 seconds"),
    T_PLUS_CUSTOM("at T + custom offset 1"),
    TWICE_RESPAWN_TIME("at 2T (twice the respawn time)"),
    TWO_T_PLUS_CUSTOM("at 2T + custom offset 2");

    private final String displayName;

    RemoveExpiredTimerEvent(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
