package com.itemrespawntimer.timermodel;

import com.itemrespawntimer.staticspawnservice.StaticSpawn;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.Instant;

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
        pcs.firePropertyChange("deleted",false,true);
    }

    //endregion


    //region property change listener support
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);


    // Add a listener
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }


    // Remove a listener
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
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
        this.respawnAt =        startMillis + (    respawnDelayTicks * 600L); //todo: figure out why red stone ball appears to always spawn 1 tick later than this prediction in both populated and sparse worlds, pot and bowl appear 2 ticks late on populated world but spot on on sparse worlds. sapphire on populated world seemed 1 tick early
        this.twiceRespawnTime = startMillis + (2 * respawnDelayTicks * 600L);

        int respawnDelaySeconds = (int)(respawnDelayTicks*0.6);
        this.totalSeconds = Math.max(respawnDelaySeconds, 1); // fallback if somehow respawnDelaySeconds is 0, to avoid any divide by zeros
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


    /**
     *
     * @return The time remaining in seconds.
     */
    public int getSecondsRemaining()
    {
        long nowMillis = Instant.now().toEpochMilli();
        long diff = respawnAt - nowMillis;
        if (diff <= 0)
        {
            return 0;
        }
        return (int) Math.ceil(diff / 1000.0);
    }


    /**
     * Get the countdown to the respawn time, formatted as a T-minus type countdown.
     * Negative for before respawn time, positive for after respawn time.
     * Either T-# for a number of seconds, or like T-#:## if minutes are needed.
     * @return the countdown
    */
    public String getTMinusCountdown(long nowMillis)
    {
        //todo: long nowMillis = Instant.now().toEpochMilli();
        long countdownSeconds = (nowMillis/1000) - (respawnAt/1000);

        char sign = (countdownSeconds <= 0)? '-' : '+';
        long minutesPart = Math.abs(countdownSeconds) / 60;
        long secondsPart = Math.abs(countdownSeconds) % 60;

        if (totalSeconds>=60 || minutesPart!=0){ // with a minute part
            return String.format("T%c%d:%02d", sign, minutesPart, secondsPart);
        } else { // without a minute part
            return String.format("T%c%d", sign, secondsPart);
        }
    }
    //endregion

}