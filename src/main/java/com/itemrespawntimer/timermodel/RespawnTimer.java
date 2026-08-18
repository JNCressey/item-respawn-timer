package com.itemrespawntimer.timermodel;

import com.itemrespawntimer.staticspawndata.StaticSpawn;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class RespawnTimer
{

    @Getter
    private final StaticSpawn spawn; // item and location information

    @Getter
    int worldId;

    @Getter
    WorldPoint worldPoint;

    //#region state summary values
    @Getter
    private long respawnAt; // the time when the item will respawn, as a millisecond timestamp

    @Getter
    private int totalSeconds; // the total time that the timer is counting out of

    @Getter
    private boolean didObservePickup; // whether this timer is based on direct observation of the item being picked up

    @Getter
    private boolean isHiddenFromWorldPanel; // whether this timer should be omitted from the side-panel
    //todo: automatically set hidden when value is below configed value
    //todo: add method for setting hidden

    @Getter
    private boolean isExpired; // whether the timer is no longer needed
    //#endregion

    //#region observation collections
    private final List<Long> observedPickups = new ArrayList<>();
    private final List<Long> observedEnteredAreaNoItem = new ArrayList<>();
    private final List<Long> observedLeftAreaNoItem = new ArrayList<>();

    //#endregion


    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);


    //#region initialisation
    public RespawnTimer(
            StaticSpawn spawn,
            int worldId,
            WorldPoint worldPoint
    ){
        this.spawn = spawn;
        this.worldId = worldId;
        this.worldPoint = worldPoint;
    }

    public RespawnTimer(
            StaticSpawn spawn,
            int worldId,
            WorldPoint worldPoint,
            int worldPopulation,
            long nowMillis
    )
    {
        this(spawn, worldId, worldPoint);
        submitObservationPickup(worldPopulation,nowMillis);
    }

    /**
     * reset the values to as if freshly constructed
     */
    private void reset(){
        respawnAt = 0;
        totalSeconds = 0;
        didObservePickup = false;
        isExpired = false;
        observedPickups.clear();
        observedEnteredAreaNoItem.clear();
        observedLeftAreaNoItem.clear();
    }
    //#endregion



    //#region submit observations
    /**
     * when the item is observed at the moment it is taken, submit the details with this method
     * @param worldPopulation The current population of the world
     * @param nowMillis the result of Instant.now().toEpochMilli();
     */
    public void submitObservationPickup(int worldPopulation, long nowMillis){
        observedPickups.add(nowMillis);
        didObservePickup = true;
        isExpired = false;
        int respawnDelayTicks = (int)Math.floor(this.spawn.getBaseRespawnTicks() * ((4000D-worldPopulation)/4000));
        int respawnDelaySeconds = (int)(respawnDelayTicks*0.6);

        this.respawnAt = nowMillis + respawnDelaySeconds * 1000L;
        this.totalSeconds = respawnDelaySeconds;

        CompletableFuture.runAsync(
                this::processObservations,
                CompletableFuture.delayedExecutor(respawnDelaySeconds, TimeUnit.SECONDS)
        );

    }

    /**
     * when the item is observed as being present, submit this observation
     * @param nowMillis the result of Instant.now().toEpochMilli();
     */
    public void submitObservationItemPresent(long nowMillis){
        reset();
        processObservations();//todo is this call needed?
    }

    /**
     * when the area is entered and the item is not present, submit this observation
     * @param nowMillis the result of Instant.now().toEpochMilli();
     */
    public void submitObservationEnteredAreaNoItem(long nowMillis){
        observedEnteredAreaNoItem.add(nowMillis);
        processObservations();
    }

    /**
     * when the area is left and the item is not present, submit this observation
     * @param nowMillis the result of Instant.now().toEpochMilli();
     */
    public void submitObservationLeftAreaNoItem(long nowMillis){
        observedLeftAreaNoItem.add(nowMillis);
        processObservations();
    }
    //#endregion

    /**
     * based on the observations, calculate the summary values for the timer and submit a "expired" property change event if appropriate
     */
    private void processObservations(){
        long nowMillis = Instant.now().toEpochMilli();
        //todo only set expired if appropriate
        //todo process other predicted values based on observations
        if(nowMillis>= respawnAt){ //todo should be when timer is no longer needed
            setExpired();
        }
        //todo should schedule a followup processing at the next time it will be needed
    }



    //#region property change support
    // Add a listener
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    // Remove a listener
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * set the value for `isExpired` to true and fire a corresponding property change event
     */
    private void setExpired(){
        boolean newValue = true;
        boolean oldValue = isExpired;
        isExpired = newValue;
        pcs.firePropertyChange("isExpired",oldValue,newValue);
    }
    //#endregion



    //#region getters
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
     * Get the countdown to the respawn time, formatted as a T-minus type countdown.
     * Negative for before respawn time, positive for after respawn time.
     * Either T-# for a number of seconds, or like T-#:## if minutes are needed.
     * @return the countdown
    */
    public String getTMinusCountdown(long nowMillis)
    {
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


    /**
     *
     * @param nowMillis the result of Instant.now().toEpochMilli();
     * @return whether the timer is for a point in the future
     */
    public Boolean isInFuture(long nowMillis){
        return (this.respawnAt > nowMillis);
    }
    //#endregion
}