package com.itemrespawntimer.timermodel;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;

@Singleton
public class ActiveTimers {

    /**
     * The collection of active 'RespawnTimer's.
     * Ordered by respawn time (ties broken by worldId then by worldPoint).
     * No more than 1 timer for the same worldId and worldPoint.
     */
    @Getter
    public final LinkedList<RespawnTimer> activeTimers = new LinkedList<>();

    @Inject
    private ItemRespawnTimerConfig config;


    private final Comparator<RespawnTimer> ordering =  Comparator
            .comparingLong(RespawnTimer::getRespawnAt)
            .thenComparingInt(RespawnTimer::getWorldId)
            .thenComparingInt(t->t.getWorldPoint().getPlane())
            .thenComparingInt(t->t.getWorldPoint().getY())
            .thenComparingInt(t->t.getWorldPoint().getX());


    public void add(RespawnTimer newTimer){
        removeIfSameWorldAndLocationAs(newTimer);
        /*IntStream.range(0,activeTimers.size())
                .filter(i->ordering.compare(activeTimers.get(i),newTimer)<0)
                .findFirst()
                .ifPresentOrElse(
                        insertionIndex->activeTimers.add(insertionIndex, newTimer),
                        ()->activeTimers.add(newTimer)
                );*/
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


    public void clear(){
        activeTimers.clear();
    }

    private void clearDeletedTimers(){
        activeTimers.removeIf(RespawnTimer::isDeleted);
    }


    public void removeTick(){
        /*
        todo
          - removal method should take the current time, the current world, and the current location, and current config options for automatic removal then pass to each timer.
            - timer should know from the config and the passed variables whether it meets a condition for removal
         */
        Set<RemoveExpiredTimerEvent> removeTimersEvents = config.removeTimersEvents();
        if(removeTimersEvents.contains(RemoveExpiredTimerEvent.T_MINUS_ZERO)) {
            activeTimers.stream()
                    .filter(RespawnTimer::isExpired)
                    .forEach(RespawnTimer::delete);
        }
        clearDeletedTimers();
    }


    public void removeExpiredSingle(){
        activeTimers.stream()
                .findFirst()
                .filter(RespawnTimer::isExpired)
                .ifPresent(RespawnTimer::delete);
    }


    public void removeExpiredAll(){
        activeTimers.stream()
                .filter(RespawnTimer::isExpired)
                .forEach(RespawnTimer::delete);
    }
}
