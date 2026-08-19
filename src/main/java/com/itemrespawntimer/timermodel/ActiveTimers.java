package com.itemrespawntimer.timermodel;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;

@Singleton
public class ActiveTimers {
    //region head
    @Inject
    private Client client;


    @Inject
    private ItemRespawnTimerConfig config;
    //endregion


    /**
     * The collection of active 'RespawnTimer's.
     * Ordered by respawn time (ties broken by worldId then by worldPoint).
     * No more than 1 timer for the same worldId and worldPoint.
     */
    @Getter
    public final LinkedList<RespawnTimer> activeTimers = new LinkedList<>();


    //region add(RespawnTimer newTimer)
    public void add(RespawnTimer newTimer){
        removeIfSameWorldAndLocationAs(newTimer);
        // Comparator won't find a match because we already removed if there is the same location.
        // If not found, binarySearch returns (-(insertion point) - 1)
        int insertionIndex = -Collections.binarySearch(activeTimers,newTimer,ordering) -1;
        activeTimers.add(insertionIndex, newTimer);

    }


    private void removeIfSameWorldAndLocationAs(RespawnTimer newTimer)
    {
        activeTimers.removeIf(t-> (
                (newTimer.getWorldId()==t.getWorldId())
                        && (newTimer.getWorldPoint().equals(t.getWorldPoint()))
        ));
    }


    private final Comparator<RespawnTimer> ordering =  Comparator
            .comparingLong(RespawnTimer::getRespawnAt)
            .thenComparingInt(RespawnTimer::getWorldId)
            .thenComparingInt(t->t.getWorldPoint().getPlane())
            .thenComparingInt(t->t.getWorldPoint().getY())
            .thenComparingInt(t->t.getWorldPoint().getX());
    //endregion


    //region clear
    public void clear(){
        activeTimers.clear();
    }


    private void clearDeletedTimers(){
        activeTimers.removeIf(RespawnTimer::isDeleted);
    }
    //endregion


    //region removeTick
    /**
     * Sets the deleted state for timers matching the config auto-removal conditions. Then calls {@link #clearDeletedTimers}.
     * @see RespawnTimer#delete
     */
    public void removeTick(){
        activeTimers.stream()
                .filter(timer -> toAutomaticallyDelete(
                        timer,
                        config.removeTimersEvents(),
                        config.removeTimersCustom1()*1000L,
                        config.removeTimersCustom2()*1000L,
                        Instant.now().toEpochMilli(),
                        client.getWorld(),
                        client.getLocalPlayer().getWorldLocation()
                ))
                .forEach(RespawnTimer::delete);

        clearDeletedTimers();
    }

    private boolean toAutomaticallyDelete(
            RespawnTimer timer,
            Set<RemoveExpiredTimerEvent> selectedRemoveTimerEvents,
            long custom1Millis,
            long custom2Millis,
            long nowMillis,
            int currentWorldId,
            WorldPoint playerPoint
    ){

        /*
        todo
          - removal method should take the current time, the current world, and the current location, and current config options for automatic removal then pass to each timer.
            - timer should know from the config and the passed variables whether it meets a condition for removal
         */
        return selectedRemoveTimerEvents.stream()
                .anyMatch(selectedRemoveTimerEvent -> {
                    switch(selectedRemoveTimerEvent){
                        case CAN_SEE_LOCATION:
                            return timer.isExpired()
                                    && (timer.getWorldId()==currentWorldId)
                                    && timer.isSpawnLocationWithinViewDistance(playerPoint);

                        case  SAME_WORLD:
                            return timer.isExpired()
                                    && (timer.getWorldId()==currentWorldId);

                        case T_MINUS_ZERO:
                            return timer.isExpired();

                        case T_PLUS_60_SECONDS:
                            return nowMillis >= (timer.getRespawnAt()        + 60_000L      );

                        case T_PLUS_CUSTOM:
                            return nowMillis >= (timer.getRespawnAt()        + custom1Millis);

                        case TWICE_RESPAWN_TIME:
                            return nowMillis >= (timer.getTwiceRespawnTime()                );

                        case TWO_T_PLUS_CUSTOM:
                            return nowMillis >= (timer.getTwiceRespawnTime() + custom2Millis);

                        default:
                            return false;
                    }
                });
    }
    //endregion


    /**
     * Sets the deleted state for a single expired timer.
     * @see RespawnTimer#delete
     */
    public void deleteExpiredSingle(){
        activeTimers.stream()
                .findFirst()
                .filter(RespawnTimer::isExpired)
                .ifPresent(RespawnTimer::delete);
    }


    /**
     * Sets the deleted state for all expired timers.
     * @see RespawnTimer#delete
     */
    public void deleteExpiredAll(){
        activeTimers.stream()
                .filter(RespawnTimer::isExpired)
                .forEach(RespawnTimer::delete);
    }

}
