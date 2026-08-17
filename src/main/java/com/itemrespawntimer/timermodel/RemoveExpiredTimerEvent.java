package com.itemrespawntimer.timermodel;

public enum RemoveExpiredTimerEvent {
    CAN_SEE_LOCATION("you can see the location"), //todo: implement this option
    SAME_WORLD("you're in the same world"), //todo: implement this option
    T_MINUS_ZERO("at T-0"), //todo: implement this option
    T_PLUS_60_SECONDS("at T + 60 seconds"), //todo: implement this option
    T_PLUS_CUSTOM("at T + custom offset 1"), //todo: implement this option
    TWICE_RESPAWN_TIME("at 2T (twice the respawn time)"), //todo: implement this option
    TWO_T_PLUS_CUSTOM("at 2T + custom offset 2"); //todo: implement this option

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
