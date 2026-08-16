package com.itemrespawntimer.timermodel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

//todo: move responsibility of ordering into the clasess for the side panel,
// model shouldn't need to maintain the order
public class OrderedTimerCollection {

    // a list of timers kept sorted so that the most soon timer is first
    List<RespawnTimer> timers;


    OrderedTimerCollection(){
        timers = new ArrayList<>();

    }


    public void add(RespawnTimer t){
        timers.add(t);
        timers.sort(Comparator.comparingLong(RespawnTimer::getRespawnAt));
    }

    public void remove(RespawnTimer timer){
        timers.remove(timer);
    }


    public boolean isEmpty(){
        return timers.isEmpty();
    }


    /**
     * @param nowMillis the result of Instant.now().toEpochMilli();
     * @return seconds remaining of most soon timer, or else -1 if no timers
     */
    public int getSecondsRemaining(long nowMillis){
        return timers.stream()
                .findFirst()
                .map(t->t.getSecondsRemaining(nowMillis))
                .orElse(-1);
    }

    /**
     *
     * @return the respawnAt of the earliest timer, or else -1 if no timers
     */
    public long getRespawnAt(){
        return timers.stream()
                .findFirst()
                .map(t->t.getRespawnAt())
                .orElse(-1L); //todo remove duplication of sorting code
    }

}
