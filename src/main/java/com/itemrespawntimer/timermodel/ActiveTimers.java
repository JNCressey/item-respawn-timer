package com.itemrespawntimer.timermodel;

import lombok.Getter;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ActiveTimers {

    // WorldIdAndWorldPoint -> RespawnTimer
    @Getter
    private final Map<WorldIdAndWorldPoint, RespawnTimer> activeTimers = new HashMap<>();


    // WorldId(integer) -> OrderedTimerCollection
    @Getter
    private final Map<Integer, OrderedTimerCollection> activeWorldTimers = new HashMap<>();



    public void clear(){
        activeTimers.clear();
        activeWorldTimers.clear();
    }


    public void put(WorldIdAndWorldPoint location, RespawnTimer timer){
        activeTimers.put(location,timer);
        activeWorldTimers
                .computeIfAbsent(location.getWorldId(),key -> new OrderedTimerCollection())
                .add(timer); //todo filter for item value
        ActiveTimers thisActiveTimers = this;
        timer.addPropertyChangeListener(evt ->{
            if("isExpired".equals(evt.getPropertyName()) && evt.getNewValue().equals(Boolean.TRUE)){
                thisActiveTimers.remove(location,timer);
                System.out.print("timer completed ActiveTimers::put");
            }
        });
    }

    public void remove(WorldIdAndWorldPoint location, RespawnTimer timer){
        activeTimers.remove(location,timer);
        int worldId = location.getWorldId();
        Optional.ofNullable(activeWorldTimers.get(worldId)).ifPresent(
                worldTimerCollection -> {
                    worldTimerCollection.remove(timer);
                    if(worldTimerCollection.isEmpty()){
                        activeWorldTimers.remove(worldId,worldTimerCollection);
                    }
                }
        );
    }

}
