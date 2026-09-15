package com.itemrespawntimer.timermodel;

import com.itemrespawntimer.staticspawnservice.StaticSpawn;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.time.Instant;

@Slf4j
public class RespawnTimer
{

    //region final values
    /**
     * item and location information
     */
    @Getter
    private final StaticSpawn spawn;


    /**
     * the world that the item will respawn in
     */
    @Getter
    private final int worldId;


    /**
     * when the timer started, as a millisecond timestamp
     */
    @Getter
    private final long start;


    /**
     * when the item will respawn, as a millisecond timestamp
     */
    @Getter
    private final long respawnAt;


    /**
     * the time after twice the respawn delay, as a millisecond timestamp
     */
    @Getter
    private final long twiceRespawnTime;


    /**
     * the total time that the timer is counting out of
     */
    @Getter
    private final int totalSeconds;


    /**
     * the total time that the timer is counting out of
     */
    @Getter
    private final int totalMillis;
    //endregion


    //region deleted
    /**
     * Whether the timer is to be discarded
     */
    @Getter
    private boolean deleted;


    /**
     * The timer is to be discarded.
     */
    public void delete(){
        deleted = true;
    }
    //endregion


    public RespawnTimer(
            StaticSpawn spawn,
            int worldId,
            int worldPopulation
    )
    {
        this.spawn = spawn;
        this.worldId = worldId;
        this.start = Instant.now().toEpochMilli();

        int respawnDelayTicks = (int)Math.floor(this.spawn.getBaseRespawnTicks() * ((4000D-worldPopulation)/4000));
        this.respawnAt =        start + (    respawnDelayTicks * 600L);
        this.twiceRespawnTime = start + (2 * respawnDelayTicks * 600L);

        int respawnDelaySeconds = (int)(respawnDelayTicks*0.6);
        this.totalSeconds = Math.max(respawnDelaySeconds, 1); // fallback if somehow respawnDelaySeconds is 0, to avoid any divide by zeros


        int respawnDelayMillis = (respawnDelayTicks*600);
        this.totalMillis = Math.max(respawnDelayMillis, 1); // fallback if somehow respawnDelayMillis is 0, to avoid any divide by zeros


    }


    //region getters
    /**
     * the location in the world where the spawn is
     */
    public WorldPoint getWorldPoint(){
        return spawn.getWorldPoint();
    }

    /**
     * Check if current time exceeds {@link #}respawnAt}.
     * @return Whether time T-0 has passed.
     */
    public boolean isExpired(){
        long nowMillis = Instant.now().toEpochMilli();
        return nowMillis > respawnAt;
    }


    /**
     * The proportion of progress out of totalSeconds, for drawing the dial.
     * @return number from 0.0 to 1.0
     */
    public double getProgress()
    {
        long nowMillis = Instant.now().toEpochMilli();
        long elapsedMillis = nowMillis - start;
        double progress = elapsedMillis / (totalSeconds * 1000.0);

        return Math.min(Math.max( // clamped progress between 0.0 and 1.0. Math.clamp is not available for java 11
                progress,
                0.0), 1.0);
    }
    //endregion

}