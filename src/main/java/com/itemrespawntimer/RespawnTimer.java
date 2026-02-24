package com.itemrespawntimer;

import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RespawnTimer
{
    @Getter
    private final long respawnAt;

    @Getter
    private final int totalSeconds;

    /**
     * Future that will be completed when the respawnAt time is reached
     */
    @Getter
    private final CompletableFuture<Void> finished;



    public RespawnTimer(int worldPopulation,StaticSpawn spawn,long nowMillis)
    {
        int respawnDelayTicks = (int)Math.floor(spawn.getBaseRespawnTicks() * ((4000D-worldPopulation)/4000));
        int respawnDelaySeconds = (int)(respawnDelayTicks*0.6);

        this.respawnAt = nowMillis + respawnDelaySeconds * 1000L;
        this.totalSeconds = respawnDelaySeconds;

        this.finished = CompletableFuture.runAsync(
            ()->{},
            CompletableFuture.delayedExecutor(respawnDelaySeconds, TimeUnit.SECONDS)
        );
    }


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
     *
     * @param nowMillis the result of Instant.now().toEpochMilli();
     * @return whether the timer is for a point in the future
     */
    public Boolean isInFuture(long nowMillis){
        return (this.respawnAt > nowMillis);
    }
}