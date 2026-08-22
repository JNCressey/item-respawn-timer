package com.itemrespawntimer.timermodel;

import com.itemrespawntimer.staticspawnservice.StaticSpawnService;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemDespawned;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This service is for verifying that an item despawn event is actually from an item being taken.
 * This is needed because you also get an item despawn event from the following situation:
 * When you leave the view distance from an item and then re-enter the view distance, you will get an item despawn event when you re-enter regardless of whether the item is still there or not.
 */
@Singleton
public class DespawnEventVerificationService {


    @Inject
    private Client client;


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
    public void onGameTick()// todo can i make this subscribed?
    {

        previousTickPlayerLocation[1] = previousTickPlayerLocation[0];
        previousTickPlayerLocation[0] = client.getLocalPlayer().getWorldLocation();
    }


    /**
     * Check if the spawn location was outside the view distance in this tick or the previous tick.
     * @param event The event with the spawn location to check.
     * @return Result of the check
     * @throws NullPointerException if event doesn't have an associated tile
     */
    public boolean despawnEventMaybeFromReenteringAnArea(ItemDespawned event) throws NullPointerException
    {
        Tile tile = event.getTile();
        WorldPoint spawnPoint = tile.getWorldLocation();
        return spawnLocationMayHaveEnteredViewDistanceThisTick(spawnPoint);
    }


    /**
     * Check if the spawn location was outside the view distance in this tick or the previous tick.
     * @param spawnPoint The spawn location to check.
     * @return Result of the check
     */
    public boolean spawnLocationMayHaveEnteredViewDistanceThisTick(WorldPoint spawnPoint){
        return(
                !StaticSpawnService.isSpawnLocationWithinViewDistance(spawnPoint,previousTickPlayerLocation[0])
                        || !StaticSpawnService.isSpawnLocationWithinViewDistance(spawnPoint,previousTickPlayerLocation[1])
        );
    }

}
