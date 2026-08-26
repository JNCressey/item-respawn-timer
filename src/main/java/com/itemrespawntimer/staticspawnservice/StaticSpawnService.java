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

    /**
     * Get a static spawn information using the baseRespawnTicks data. or empty optional if no baseRespawnTicks for this item id.
     * @param wp The world point to set.
     * @param itemId The itemId to look up and to set.
     * @param quantity The quantity to set.
     * @return The static spawn info or an empty optional.
     */
    public Optional<StaticSpawn> getTrackedSpawn(WorldPoint wp, int itemId, int quantity){
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