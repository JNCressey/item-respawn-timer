package com.itemrespawntimer.timermodel;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.ItemRespawnTimerPlugin;
import com.itemrespawntimer.staticspawnservice.StaticSpawnService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;

@Slf4j
@Singleton
public class ActiveTimers {
    //region head
    @Inject
    private Client client;


    @Inject
    private ItemRespawnTimerConfig config;


    @Inject
    private ItemRespawnTimerPlugin plugin;


    @Inject
    private StaticSpawnService staticSpawnService;

    @Inject
    private DespawnEventVerificationService despawnEventVerificationService;
    //endregion


    /**
     * The collection of active 'RespawnTimer's.
     * Ordered by respawn time (ties broken by worldId then by worldPoint).
     * No more than 1 timer for the same worldId and worldPoint.
     */
    @Getter
    public final LinkedList<RespawnTimer> activeTimers = new LinkedList<>();


    //region add(RespawnTimer newTimer)
    /**
     * Add a respawn timer to the list.
     * @param newTimer The timer to add.
     */
    public void add(RespawnTimer newTimer){
        removeIfSameWorldAndLocationAs(newTimer);
        activeTimers.add(insertionIndex(newTimer), newTimer);
    }


    /**
     * Remove any timers at the same location in the same world, to ensure there's only ever one per location.
     * @param newTimer The timer about to be added to the list.
     */
    private void removeIfSameWorldAndLocationAs(RespawnTimer newTimer)
    {
        activeTimers.removeIf(t-> (
                (newTimer.getWorldId()==t.getWorldId())
                        && (newTimer.getWorldPoint().equals(t.getWorldPoint()))
        ));
    }


    /**
     * Ordering for the timers list to be maintained in.
     */
    private final Comparator<RespawnTimer> ordering =  Comparator
            .comparingLong(RespawnTimer::getRespawnAt)
            .thenComparingInt(RespawnTimer::getWorldId)
            .thenComparingInt(t->t.getWorldPoint().getPlane())
            .thenComparingInt(t->t.getWorldPoint().getY())
            .thenComparingInt(t->t.getWorldPoint().getX());


    /**
     * Find the index to insert at to maintain the order of the timers list.
     * @param newTimer The timer about to be added to the list.
     * @return The insertion index.
     */
    private int insertionIndex(RespawnTimer newTimer){
        int insertionIndex = 0;
        for (RespawnTimer t : activeTimers){
            // want first index where newTimer is sorted before existing element
            if (ordering.compare(newTimer, t) < 0) {
                break;
            }
            ++insertionIndex;
        }
        return insertionIndex;
    }
    //endregion


    //region clear
    public void clear(){
        activeTimers.clear();
    }


    private void clearDeletedTimers(){
        activeTimers.removeIf(RespawnTimer::isDeleted);
    }
    //endregion

    public void onGameTick(){//todo can i make this subscribe?
        removeTick();
    }

    /**
     * Delete matching timer when observe an item spawn.
     * @param event the item spawned
     */
    public void onItemSpawned(ItemSpawned event){ //todo can i make this subscribed
        Tile tile = event.getTile();
        TileItem item = event.getItem();
        if (tile == null || item == null) {
            return;
        }

        if (item.getOwnership() != TileItem.OWNERSHIP_NONE){
            return; // only react to items that were naturally spawned
        }

        WorldPoint spawnedWorldPoint = tile.getWorldLocation();
        int spawnedItemId = item.getId();
        int currentWorldId = client.getWorld();

        deleteMatchingTimer(spawnedWorldPoint,spawnedItemId,currentWorldId);
    }

    private void deleteMatchingTimer(
            WorldPoint spawnedWorldPoint,
            int spawnedItemId,
            int currentWorldId
    ){
        activeTimers.stream()
                .filter(timer -> (
                        timer.getWorldPoint().equals(spawnedWorldPoint)
                                && timer.getSpawn().getItemId() == spawnedItemId
                                && timer.getWorldId() == currentWorldId
                ))
                .forEach(RespawnTimer::delete);
    }




    /**
     * Add a timer for picked up item.
     * @param event the item despawned
     */
    public void onItemDespawned(ItemDespawned event){//todo can i make this subscribed
        Tile tile = event.getTile();
        TileItem item = event.getItem();
        if (tile == null || item == null)
        {
            return;
        }

        if (item.getOwnership() != TileItem.OWNERSHIP_NONE){
            return; // only react to items that were naturally spawned
        }

        if (despawnEventVerificationService.despawnEventMaybeFromReenteringAnArea(event)){
            return;// filter out delayed despawn events from returning to a location but not directly witnessing the item being taken
        }

        WorldPoint wp = tile.getWorldLocation();

        staticSpawnService.getTrackedSpawn(wp, item.getId(), item.getQuantity())
                .ifPresent(spawn -> {
                    long nowMillis = Instant.now().toEpochMilli();
                    int worldId = client.getWorld();
                    int worldPopulation = plugin.getCurrentWorldPopulation();

                    RespawnTimer timer = new RespawnTimer(spawn,worldId,worldPopulation,nowMillis);
                    add(timer);
                });
    }


    //region removeTick
    /**
     * Sets the deleted state for timers matching the config auto-removal conditions. Then calls {@link #clearDeletedTimers}.
     * @see RespawnTimer#delete
     */
    private void removeTick(){
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
        return selectedRemoveTimerEvents.stream()
                .anyMatch(selectedRemoveTimerEvent -> {
                    switch(selectedRemoveTimerEvent){
                        case CAN_SEE_LOCATION:
                            return timer.isExpired()
                                    && (timer.getWorldId()==currentWorldId)
                                    && StaticSpawnService.isSpawnLocationWithinViewDistance(timer,playerPoint);

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
