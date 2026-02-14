package com.itemrespawntimer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrderedTimerCollection {

    // a list of timers kept sorted so that the most soon timer is first
    List<RespawnTimer> timers;


    OrderedTimerCollection(){
        timers = new ArrayList<>();

    }


    void add(RespawnTimer t){
        timers.add(t);
        timers.sort(Comparator.comparingLong(RespawnTimer::getRespawnAt));
    }


    /**
     * @param nowMillis the result of Instant.now().toEpochMilli();
     * remove any timers that are in the past
     */
    void removeIfPast(long nowMillis){
        //todo optimise by finding first future then dropping the number of elements from the top of the list
        timers.removeIf(t->!t.isInFuture(nowMillis));
    }


    boolean isEmpty(){
        return timers.isEmpty();
    }


    /**
     * @param nowMillis the result of Instant.now().toEpochMilli();
     * @return seconds remaining of most soon timer, or else -1 if no timers
     */
    int getSecondsRemaining(long nowMillis){
        return timers.stream()
                .findFirst()
                .map(t->t.getSecondsRemaining(nowMillis))
                .orElse(-1);
    }

}
