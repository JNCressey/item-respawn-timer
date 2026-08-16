package com.itemrespawntimer.timermodel;

public enum RemoveExpiredTimerEvent {
    CAN_SEE_LOCATION("you can see the location"), //todo: implement this option
    SAME_WORLD("you're in the same world"), //todo: implement this option
    TWICE_RESPAWN_TIME("after twice the respawn time"), //todo: implement this option
    IMMEDIATELY("at T-0"), //todo: implement this option
    T_PLUS_60_SECONDS("at T+60 seconds"), //todo: implement this option
    T_PLUS_X("at T+X (configured below)"); //todo: implement this option

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
