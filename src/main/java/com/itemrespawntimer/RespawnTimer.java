package com.itemrespawntimer;

import lombok.Getter;

public class RespawnTimer
{
    @Getter
    private final long respawnAt;

    @Getter
    private final int totalSeconds;


    public RespawnTimer(long respawnAt, int totalSeconds)
    {
        this.respawnAt = respawnAt;
        this.totalSeconds = totalSeconds;
    }


    public double getProgress(long nowMillis)
    {
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
}