package com.itemrespawntimer;

import lombok.Getter;

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


    //#region state summary values
    @Getter
    private long respawnAt; // the time when the item will respawn

    @Getter
    private int totalSeconds; // the total time that the timer is counting out of

    @Getter
    private boolean didObservePickup; // whether this timer is based on direct observation of the item being picked up

    @Getter
    private boolean isHiddenFromWorldPanel; // whether this timer should be omitted from the side-panel
    //todo: automatically set hidden when value is below configed value
    //todo: add method for setting hidden

    @Getter
    private boolean isFinished; // whether the target time has been exceeded
    //#endregion

    //#region observation collections
    private final List<Long> observedPickups = new ArrayList<>();
    private final List<Long> observedEnteredAreaNoItem = new ArrayList<>();
    private final List<Long> observedLeftAreaNoItem = new ArrayList<>();

    //#endregion


    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);


    //#region initialisation
    public RespawnTimer(StaticSpawn spawn){
        this.spawn = spawn;
    }

    public RespawnTimer(int worldPopulation,StaticSpawn spawn,long nowMillis)
    {
        //this.spawn = spawn;
        this(spawn);
        submitObservationPickup(worldPopulation,nowMillis);
    }

    /**
     * reset the values to as if freshly constructed
     */
    private void reset(){
        respawnAt = 0;
        totalSeconds = 0;
        didObservePickup = false;
        isFinished = false;
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
        isFinished = false;
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
     * based on the observations, calculate the summary values for the timer and submit a "finished" property change event if appropriate
     */
    private void processObservations(){
        long nowMillis = Instant.now().toEpochMilli();
        //todo only set finished if appropriate
        //todo process other predicted values based on observations
        setFinished();
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
     * set the value for `isFinished` to true and fire a corresponding property change event
     */
    private void setFinished(){
        boolean newValue = true;
        boolean oldValue = isFinished;
        isFinished = newValue;
        pcs.firePropertyChange("isFinished",oldValue,newValue);
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
     *
     * @param nowMillis the result of Instant.now().toEpochMilli();
     * @return whether the timer is for a point in the future
     */
    public Boolean isInFuture(long nowMillis){
        return (this.respawnAt > nowMillis);
    }
    //#endregion
}