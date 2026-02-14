package com.itemrespawntimer;

import java.time.Instant;//todo remove import
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
        //todo optimise by making this called explicitly rather than be called on every getSecondsRemaining and every respawnAt
        timers.removeIf(t->!t.isInFuture(nowMillis));
    }

    /**
     * remove any timers that are in the past
     */
    void removeIfPast(){ //todo remove no arg version
        long nowMillis = Instant.now().toEpochMilli();
        //todo optimise by finding first future then dropping the number of elements from the top of the list
        //todo optimise by making this called explicitly rather than be called on every getSecondsRemaining and every respawnAt
        timers.removeIf(t->!t.isInFuture(nowMillis));
    }



    /**
     *
     * @return how many timers are in this collection
     */
    int timerCount(){
        return timers.size();
    }


    /**
     *
     * @return respawn time of most soon timer, or else -1 if no timers
     */
    long getRespawnAt(){
        removeIfPast();
        return timers.stream()
                .findFirst()
                .map(RespawnTimer::getRespawnAt)
                .orElse((long)-1);
    }


    /**
     * @param nowMillis the result of Instant.now().toEpochMilli();
     * @return seconds remaining of most soon timer, or else -1 if no timers
     */
    int getSecondsRemaining(long nowMillis){
        removeIfPast(nowMillis);
        return timers.stream()
                .findFirst()
                .map(t->t.getSecondsRemaining(nowMillis))
                .orElse(-1);
    }

}
