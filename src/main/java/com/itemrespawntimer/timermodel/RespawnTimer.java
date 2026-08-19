package com.itemrespawntimer.timermodel;

import com.itemrespawntimer.staticspawndata.StaticSpawn;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.Instant;

public class RespawnTimer
{

    //region final values
    @Getter
    private final StaticSpawn spawn; // item and location information


    @Getter
    private final int worldId; // the world that the item will respawn in


    //todo put world point inside staticspawn and use a getter here getWorldPoint(){return spawn.getWorldPoint()}
    @Getter
    private final WorldPoint worldPoint; // the location in the world where the spawn is


    @Getter
    private final long start; // when the timer started, as a millisecond timestamp


    @Getter
    private final long respawnAt; // when the item will respawn, as a millisecond timestamp


    @Getter
    private final long twiceRespawnTime; // the time after twice the respawn delay, as a millisecond timestamp


    @Getter
    private final int totalSeconds; // the total time that the timer is counting out of
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
            WorldPoint worldPoint,
            int worldPopulation,
            long startMillis
    )
    {
        this.spawn = spawn;
        this.worldId = worldId;
        this.worldPoint = worldPoint;
        this.start = startMillis;

        int respawnDelayTicks = (int)Math.floor(this.spawn.getBaseRespawnTicks() * ((4000D-worldPopulation)/4000));
        int respawnDelaySeconds = (int)(respawnDelayTicks*0.6);

        this.totalSeconds = Math.max(respawnDelaySeconds, 1); // fallback if somehow respawnDelaySeconds is 0, to avoid any divide by zeros
        this.respawnAt =        startMillis + (    respawnDelaySeconds * 1000L);
        this.twiceRespawnTime = startMillis + (2 * respawnDelaySeconds * 1000L);
    }


    //region getters
    /**
     * Check if current time exceeds {@link #}respawnAt}.
     * @return Whether time T-0 has passed.
     */
    public boolean isExpired(){
        long nowMillis = Instant.now().toEpochMilli();
        return nowMillis > respawnAt;
    }


    //region getProgress
    /**
     * The proportion of progress out of totalSeconds, for drawing the dial.
     * @return number from 0.0 to 1.0
     */
    public double getProgress()
    {
        long nowMillis = Instant.now().toEpochMilli();
        long elapsed = nowMillis - start;
        return clamp(
                elapsed / (totalSeconds * 1000.0),
                0.0,1.0);
    }


    // Math.clamp is not available for java 11
    private static double clamp(double value, double min, double max){
        return Math.min(Math.max(value, min), max);
    }
    //endregion getProgress


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