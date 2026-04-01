package com.itemrespawntimer.timermodel;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ActiveTimers {

    // WorldIdAndWorldPoint -> RespawnTimer
    @Getter
    private final Map<WorldIdAndWorldPoint, RespawnTimer> activeTimers = new HashMap<>();


    // WorldId(integer) -> OrderedTimerCollection
    @Getter
    private final Map<Integer, OrderedTimerCollection> activeWorldTimers = new HashMap<>();




//    Map<WorldIdAndWorldPoint, RespawnTimer> getActiveTimers()
//    {
//        return activeTimers;
//    }

//    Map<Integer, OrderedTimerCollection> getActiveWorldTimers()
//    {
//        return activeWorldTimers;
//    }


    public void clear(){
        activeTimers.clear();
        activeWorldTimers.clear();
    }


    public void put(WorldIdAndWorldPoint location, RespawnTimer timer){
        activeTimers.put(location,timer);
        activeWorldTimers
                .computeIfAbsent(location.getWorldId(),key -> new OrderedTimerCollection())
                .add(timer); //todo filter for item value
    }


    /**
     * remove the world timer if it has no remaining future timers
     * @param worldId
     * @param nowMillis
     */
    public void removeWorldIfPast(int worldId, long nowMillis){
        //todo also remvove from activeTimers
        Optional.ofNullable(activeWorldTimers.get(worldId))
                .ifPresent(c -> {
                    c.removeIfPast(nowMillis);
                    if (c.isEmpty()){
                        activeWorldTimers.remove(worldId);
                    }
                });
    }


}
