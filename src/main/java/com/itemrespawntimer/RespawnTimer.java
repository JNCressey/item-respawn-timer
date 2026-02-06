package com.itemrespawntimer;

import lombok.Getter;

import java.time.Instant;

public class RespawnTimer
{
    @Getter
    private final long respawnAt;

    @Getter
    private final int totalSeconds;


    //#region constructors
    public RespawnTimer(long respawnAt, int totalSeconds)
    {
        this.respawnAt = respawnAt;
        this.totalSeconds = totalSeconds;
    }

    //constructor for just an end time
    public RespawnTimer(long respawnAt){
        this.respawnAt = respawnAt;
        this.totalSeconds = -1;
    }
    //#endregion


    public double getProgress(long nowMillis)
    {
        int totalSeconds = this.totalSeconds > 0 ? this.totalSeconds : 60; // default for handling when no total provided
        long start = respawnAt - totalSeconds * 1000L;
        long elapsed = nowMillis - start;
        if (elapsed <= 0)
        {
            return 0.0;
        }
        if (elapsed >= totalSeconds * 1000L)
        {
            return 1.0;
        }
        return (double) elapsed / (totalSeconds * 1000L);
    }


    public int getSecondsRemaining(long nowMillis)
    {
        long diff = respawnAt - nowMillis;
        if (diff <= 0)
        {
            return 0;
        }
        return (int) Math.ceil(diff / 1000.0);
    }

    /**
     * compare with another timer for finding which is respawning sooner
     * @param otherTimer the timer to compare to
     * @return whether this timer is for a sooner time than the other timer
     */
    public Boolean isSoonerThan(RespawnTimer otherTimer){
        return (this.respawnAt < otherTimer.respawnAt);
    }

    public Boolean isInFuture(){
        long now = Instant.now().toEpochMilli();
        return (this.respawnAt > now);
    }
}