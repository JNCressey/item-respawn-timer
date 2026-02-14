package com.itemrespawntimer;

import java.util.HashMap;
import java.util.Map;

public class ActiveTimers {

    // WorldIdAndWorldPoint -> RespawnTimer
    private final Map<WorldIdAndWorldPoint, RespawnTimer> activeTimers = new HashMap<>();


    // WorldId(integer) -> OrderedTimerCollection
    private final Map<Integer, OrderedTimerCollection> activeWorldTimers = new HashMap<>();


    Map<WorldIdAndWorldPoint, RespawnTimer> getActiveTimers()
    {
        return activeTimers;
    }

    Map<Integer, OrderedTimerCollection> getActiveWorldTimers()
    {
        return activeWorldTimers;
    }


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

}
