package com.itemrespawntimer.timermodel;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.staticspawndata.StaticSpawn;
import com.itemrespawntimer.staticspawndata.StaticSpawnService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.game.WorldService;
import net.runelite.http.api.worlds.WorldResult;

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
    private WorldService worldService;
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
        recordThisTickPlayerLocation();
    }

    /**
     * Delete matching timer when observe an item spawn.
     * @param event the item spawned
     */
    public void onItemSpawned(ItemSpawned event){ //todo can i make this subscribed
        log.debug("#onItemSpawned");
        Tile tile = event.getTile();
        TileItem item = event.getItem();
        if (tile == null || item == null) {
            return;
        }

        WorldPoint spawnedWorldPoint = tile.getWorldLocation();
        int spawnedItemId = item.getId();
        int currentWorldId = client.getWorld();

        activeTimers.stream()
                .filter(timer -> (
                        timer.getWorldPoint().equals(spawnedWorldPoint)
                        && timer.getSpawn().getItemId() == spawnedItemId
                        && timer.getWorldId() == currentWorldId
                ))
                .forEach(RespawnTimer::delete);
    }

    //region public void onItemDespawned(ItemDespawned event)
    //todo move staticSpawnsByTile to activeTimers having a property for the StaticSpawnService

    /**
     * Add a timer for picked up item.
     * @param event the item despawned
     * @param staticSpawnsByTile
     */
    public void onItemDespawned(ItemDespawned event, Map<WorldPoint, List<StaticSpawn>> staticSpawnsByTile){//todo can i make this subscribed
        Tile tile = event.getTile();
        TileItem item = event.getItem();
        if (tile == null || item == null)
        {
            return;
        }

        WorldPoint wp = tile.getWorldLocation();
        if(spawnLocationMayHaveEnteredViewDistanceThisTick(wp)){
            return; // filter out delayed despawn events from returning to a location but not directly witnessing the item being taken
        }

        List<StaticSpawn> spawns = staticSpawnsByTile.get(wp);

        if (spawns == null || spawns.isEmpty())
        {
            return;
        }

        for (StaticSpawn spawn : spawns) //todo change staticspawnservice to only provide one static spawn existing per world point
        {
            if (spawn.getItemId()==item.getId())
            {

                long nowMillis = Instant.now().toEpochMilli();
                int worldId = client.getWorld();
                int worldPopulation = getCurrentWorldPopulation();

                RespawnTimer timer = new RespawnTimer(spawn,worldId,worldPopulation,nowMillis);

                add(timer);

                break;
            }
        }
    }


    //region spawnLocationMayHaveEnteredViewDistanceThisTick(WorldPoint spawnPoint)
    /**
     * The player location in [0] this tick and [1] the previous tick, so we can detect whether an onItemDespawn is actually from re-entering the area without directly observing it being taken.
     * Recording both to ensure we have the previous tick location:
     *  - if updating fires early in the tick [0] would have this tick location.
     *  - if updating fires late in the tick [1] would have location of 2 ticks ago.
     */
    private final WorldPoint[] previousTickPlayerLocation = new WorldPoint[2];


    /**
     * Update values of {@link #previousTickPlayerLocation}.
     */
    private void recordThisTickPlayerLocation()
    {

        previousTickPlayerLocation[1] = previousTickPlayerLocation[0];
        previousTickPlayerLocation[0] = client.getLocalPlayer().getWorldLocation();
    }


    /**
     * Check if the spawn location was outside the view distance in this tick or the previous tick.
     * @param spawnPoint The spawn location to check.
     * @return Result of the check
     */
    private boolean spawnLocationMayHaveEnteredViewDistanceThisTick(WorldPoint spawnPoint){
        return(
               !StaticSpawnService.isSpawnLocationWithinViewDistance(spawnPoint,previousTickPlayerLocation[0])
            || !StaticSpawnService.isSpawnLocationWithinViewDistance(spawnPoint,previousTickPlayerLocation[1])
        );
    }
    //endregion


    private int getCurrentWorldPopulation(){
        int currentWorldId = client.getWorld();
        WorldResult worlds = worldService.getWorlds();

        if (worlds != null)
        {
            net.runelite.http.api.worlds.World world = worlds.findWorld(currentWorldId);
            if (world != null)
            {
                return world.getPlayers();
            }
        }
        return 0;
    }
    //endregion


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
