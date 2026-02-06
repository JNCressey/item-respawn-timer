package com.itemrespawntimer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ActiveTimers {

    // WorldIdAndWorldPoint -> RespawnTimer
    private final Map<WorldIdAndWorldPoint, RespawnTimer> activeTimers = new HashMap<>();

    // WorldId(integer) -> RespawnTimer
    private final Map<Integer, RespawnTimer> activeWorldTimers = new HashMap<>();


    Map<WorldIdAndWorldPoint, RespawnTimer> getActiveTimers()
    {
        return activeTimers;
    }

    Map<Integer, RespawnTimer> getActiveWorldTimers()
    {
        return activeWorldTimers;
    }


    public void clear(){
        activeTimers.clear();
        activeWorldTimers.clear();
    }


    public void put(WorldIdAndWorldPoint location, RespawnTimer timer){
        activeTimers.put(location,timer);
        substituteWorldTimerIfSooner(location.getWorldId(),timer);
    }

    private void substituteWorldTimerIfSooner(int worldId, RespawnTimer newTimer){
        Boolean keepOldTimer = Optional.of(activeWorldTimers)
                .map(m->m.get(worldId))
                .map(t -> t.isInFuture() && t.isSoonerThan(newTimer))
                .orElse(false);
        if (!keepOldTimer){
            activeWorldTimers.put(worldId,newTimer);
        }

    }

}
