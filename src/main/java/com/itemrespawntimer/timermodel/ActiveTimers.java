package com.itemrespawntimer.timermodel;

import lombok.Getter;

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


    public void removeExpiredIfMetConfigConditions(){
        //todo
        long nowMillis = Instant.now().toEpochMilli();
        activeTimers.removeIf(timer -> timer.getRespawnAt()<(nowMillis-5000));
    }


    public void removeExpiredSingle(){
        long nowMillis = Instant.now().toEpochMilli();
        if(activeTimers.getFirst().getRespawnAt()<nowMillis) {//todo: make this check a method of RespawnTimer
            activeTimers.removeFirst();
        }
    }


    public void removeExpiredAll(){
        long nowMillis = Instant.now().toEpochMilli();
        activeTimers.removeIf(timer -> timer.getRespawnAt()<nowMillis); //todo: make this check a method of RespawnTimer

    }
}
