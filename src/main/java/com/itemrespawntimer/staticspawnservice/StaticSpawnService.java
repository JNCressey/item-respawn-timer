package com.itemrespawntimer.staticspawnservice;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.timermodel.RespawnTimer;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.coords.WorldPoint;

@Slf4j
@Singleton
public class StaticSpawnService
{

    @Inject
    private ItemRespawnTimerConfig config;

    /**
     * The static spawn data that is to be tracked.
     * [0]: The data from TrackedSpawnsDefault.csv, or empty optional if not in default set.
     * [i>0]: The data from config.trackedSpawnsOverrides(), or empty optional if that config says not to track
     * The priority will be that the last item of the list will be used if overrides are enabled, or [0] will be used if overrides are disabled.
     * If the used item is empty, then the spawn won't be tracked.
     * On reloading the config, can remove all except first of the list.
     */
    private final HashMap<WorldPoint, LinkedList<Optional<StaticSpawn>>> trackedSpawns = new HashMap<>();


    /**
     * Get the static spawn data for a given location, or empty optional if not tracking a spawn at the location.
     * @param wp The spawn location.
     * @return The spawn data.
     */
    public Optional<StaticSpawn> getTrackedSpawn(WorldPoint wp){
        return Optional.ofNullable(trackedSpawns.get(wp))
                .flatMap(spawns ->
                        config.overridesEnabled()
                                ? spawns.getLast()
                                : spawns.getFirst()
                );
    }


    public void startUp(){
        TrackedSpawnsReader.getDefaultTrackedSpawns()
                .forEach(e -> {
                    WorldPoint wp = e.getWorldPoint();
                    if (trackedSpawns.containsKey(wp)){
                        log.warn("repeated coordinates in default data: {}, {}, {}",
                                wp.getX(),wp.getY(),wp.getPlane());
                    }
                    trackedSpawns.put(
                            wp,
                            new LinkedList<>(Collections.singletonList(e.getStaticSpawn()))
                    );
                });
        reloadConfigOverrides();
    }


    //region reloadConfigOverrides
    public void reloadConfigOverrides(){
        clearConfigOverrides();
        String configTrackedSpawns = config.trackedSpawnsOverrides();
        TrackedSpawnsReader.parseTrackedSpawnsFromCsvText(configTrackedSpawns)
                .forEach(e ->
                    trackedSpawns
                            .computeIfAbsent(
                                    e.getWorldPoint(),
                                    k -> new LinkedList<>(Collections.singletonList(Optional.empty()))
                            )
                            .add(e.getStaticSpawn())
                );
    }

    private void clearConfigOverrides(){
        trackedSpawns.values().forEach(l -> {
            l.subList(1, l.size()).clear(); // remove all except first
        });
    }
    //endregion


    //region isSpawnLocationWithinViewDistance
    /**
     * Check if the spawn location is within the range that the player can see if the item respawns.
     * @param spawnPoint The spawn location
     * @param playerPoint The current player position.
     * @return The result of the check
     */
    public static boolean isSpawnLocationWithinViewDistance(WorldPoint spawnPoint, WorldPoint playerPoint){
        //can see the current zone plus a range of 3 zones, a 7zones*7zones area. (a zone is 8tiles*8tiles)
        return (playerPoint.getPlane() == spawnPoint.getPlane())
                && Math.abs(playerPoint.getX()/8 - spawnPoint.getX()/8) <= 3
                && Math.abs(playerPoint.getY()/8 - spawnPoint.getY()/8) <= 3;
    }


    /**
     * Check if the spawn location is within the range that the player can see if the item respawns.
     * @param timer The timer to check the spawn location of.
     * @param playerPoint The current player position.
     * @return The result of the check
     */
    public static boolean isSpawnLocationWithinViewDistance(RespawnTimer timer, WorldPoint playerPoint){
        return isSpawnLocationWithinViewDistance(timer.getWorldPoint(), playerPoint);
    }


    /**
     * Check if the spawn location is within the range that the player can see if the item respawns.
     * @param spawn The static spawn data to check the spawn location of.
     * @param playerPoint The current player position.
     * @return The result of the check
     */
    public static boolean isSpawnLocationWithinViewDistance(StaticSpawn spawn, WorldPoint playerPoint){
        return isSpawnLocationWithinViewDistance(spawn.getWorldPoint(), playerPoint);
    }
    //endregion

}