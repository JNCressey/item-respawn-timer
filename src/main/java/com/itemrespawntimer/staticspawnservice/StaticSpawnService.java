package com.itemrespawntimer.staticspawnservice;

import java.util.*;
import javax.inject.Singleton;

import com.itemrespawntimer.timermodel.RespawnTimer;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.coords.WorldPoint;

@Slf4j
@Singleton
public class StaticSpawnService
{

    //region ashes spawns
    private static final List<WorldPoint> ashesSpawnPoints = List.of(
            new WorldPoint(1497,3173,0), // [[Bandit (Varlamore)|Bandit]]s' camp north-east of [[Ralos' Rise]]
            new WorldPoint(3752,9780,0), // [[Sisterhood Sanctuary]]
            new WorldPoint(1834,6187,0) // [[Realm of Memories]]
    );


    private static final int ashesItemId = 592;


    /**
     * Check whether a spawn is probably ashes from burned out fires.
     * @param wp The world point to check.
     * @param itemId The itemId to check
     * @return Result of the test.
     */
    private static boolean isAshesNotFromSelectSpawnPoints(WorldPoint wp, int itemId) {
        return itemId == ashesItemId // is an ashes item
                && ashesSpawnPoints.stream().filter(sp -> sp.equals(wp)).findAny().isEmpty(); // is not at an ashes spawn point
    }
    //endregion


    /**
     * Get a static spawn information using the baseRespawnTicks data. or empty optional if no baseRespawnTicks for this item id.
     * @param wp The world point to set.
     * @param itemId The itemId to look up and to set.
     * @param quantity The quantity to set.
     * @return The static spawn info or an empty optional.
     */
    public Optional<StaticSpawn> getTrackedSpawn(WorldPoint wp, int itemId, int quantity){
        if( isAshesNotFromSelectSpawnPoints(wp, itemId) ){ return Optional.empty(); }// skip ashes that are from fires burning out
        // POH served food is also OWNERSHIP_NONE, so if any of those items had an item spawn, a check would be needed here to exclude the POH. But we're ok for now as none of them have any spawns so they're not in the baseRespawnTicks data.

        return Optional.ofNullable(mapItemIdToBaseRespawnTicks.get(itemId))
                .map(baseRespawnTicks ->
                        new StaticSpawn(
                                wp,
                                baseRespawnTicks,
                                itemId,
                                quantity)
                );
    }


    /**
     * key: The item id.
     * value: baseRespawnTicks, for this item.
     */
    Map<Integer, Integer> mapItemIdToBaseRespawnTicks = new HashMap<>();


    public void startUp(){
        mapItemIdToBaseRespawnTicks = (new BaseRespawnTicksReader())
                .getMapItemIdToBaseRespawnTicks();
    }


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