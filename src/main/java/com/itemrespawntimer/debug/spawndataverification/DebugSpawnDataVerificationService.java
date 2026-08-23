package com.itemrespawntimer.debug.spawndataverification;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.staticspawnservice.StaticSpawnService;
import com.itemrespawntimer.timermodel.DespawnEventVerificationService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class DebugSpawnDataVerificationService {

    //region head
    @Inject
    private Client client;

    @Inject
    private ItemRespawnTimerConfig config;


    @Inject
    private StaticSpawnService staticSpawnService;


    @Inject
    private DespawnEventVerificationService despawnEventVerificationService;

    @Inject
    private SpawnDataVerificationReader spawnDataVerificationReader;
    //endregion

    //region observations
    private final Map<WorldPoint, SpawnDataVerificationObservation> observations = new HashMap<>();

    private SpawnDataVerificationObservation observationComputeIfAbsent(WorldPoint spawnPoint){
        return observations.computeIfAbsent(
                spawnPoint,
                k -> {
                    SpawnDataVerificationObservation observation = new SpawnDataVerificationObservation();
                    staticSpawnService.getTrackedSpawn(spawnPoint).ifPresent(observation::setSpawn);
                    observation.setWorldpoint(spawnPoint);
                    return observation;
                });
    }

    private void addObservationToConfig(SpawnDataVerificationObservation observation){
        config.setSpawnDataVerificationObservations(
                config.spawnDataVerificationObservations()
                        + "\n" + observation.toCsvLine()
        );
    }
    //endregion


    /**
     * Load observations data from config {@link ItemRespawnTimerConfig#spawnDataVerificationObservations}.
     * Then re-set the value of the config with what was parsed.
     * The PREVIOUSLY_LOADED_AREA will be de-duped and will not survive if there's a later CONFIRMED or WRONG_ITEM.
     * (Surviving PREVIOUSLY_LOADED_AREA then imply
     *      either the spawn doesn't exist
     *      or you need to go try waiting for the item again)
     */
    public void startUp(){//todo can i make subscribed?
        StringBuilder comments = new StringBuilder(); // unparsed lines and comments to preserve

        // load observations from config
        spawnDataVerificationReader.parseObservationsFromCsvText(
                    config.spawnDataVerificationObservations(),
                    comments)
                .forEach(observation ->
                        observations.put(observation.getWorldpoint(), observation));

        StringBuilder newConfigValue = new StringBuilder(comments); // start with all the comments at the top

        // add all resulting observations
        for (SpawnDataVerificationObservation observation : observations.values()) {
            newConfigValue.append(observation.toCsvLine());
        }

        config.setSpawnDataVerificationObservations(newConfigValue.toString());
    }


    //region concludingObservation
    /**
     * @see #concludingObservation
     * @param event The event containing a tile where spawning item was witnessed.
     */
    public void onItemSpawned(ItemSpawned event){//todo can i make subscribed?
        concludingObservation(event);
    }


    /**
     * @see #concludingObservation
     * @param event The event containing a tile where spawning item was witnessed.
     */
    public void onItemDespawned(ItemDespawned event){//todo can i make subscribed?
        concludingObservation(event);
    }


    /**
     * Set the observation status for this location as
     * {@link SpawnDataVerificationStatus#CONFIRMED} if item matches,
     * or {@link SpawnDataVerificationStatus#WRONG_ITEM} if wrong item.
     * @param event The {@link ItemSpawned} or {@link ItemDespawned} event, containing a tile where spawning item was witnessed.
     */
    private void concludingObservation(Object event) throws IllegalArgumentException
    {
        Tile tile;
        TileItem item;
        if (event instanceof ItemSpawned) {
            tile = ((ItemSpawned)   event).getTile();
            item = ((ItemSpawned)   event).getItem();
        }
        else if (event instanceof ItemDespawned) {
            tile = ((ItemDespawned) event).getTile();
            item = ((ItemDespawned) event).getItem();
        } else {
            throw new IllegalArgumentException("argument `event` must be of type ItemSpawned or ItemDespawned");
        }

        if (tile == null || item == null) {
            return;
        }

        WorldPoint spawnPoint = tile.getWorldLocation();

        SpawnDataVerificationObservation observation = observationComputeIfAbsent(spawnPoint);

        if (item.getId() == observation.getSpawn().getItemId()
            && item.getQuantity() == observation.getSpawn().getQuantity()
        ){
            observation.setStatus(SpawnDataVerificationStatus.CONFIRMED);
            log.debug("confirmed spawn data"); //todo make game message
            addObservationToConfig(observation);
        } else {
            observation.setStatus(SpawnDataVerificationStatus.WRONG_ITEM);
            log.debug("spawn data is wrong item/quantity"); //todo make game message
            addObservationToConfig(observation);

        }
    }
    //endregion


    //region onGameTick
    public void onGameTick(){//todo can i make subscribed?
        WorldPoint playerPoint = client.getLocalPlayer().getWorldLocation();
        observations.values().forEach(o->onGameTick(o,playerPoint)); //todo maintain a set of active ones to tick, so won't be ticking potentially thousands of entries each tick
    }

    @SuppressWarnings({"UnnecessaryReturnStatement", "SwitchStatementWithoutDefaultBranch"})
    private void onGameTick(SpawnDataVerificationObservation observation, WorldPoint playerPoint){
        WorldPoint spawnPoint = observation.getSpawn().getWorldPoint();
        switch (observation.getStatus()){
            case CONFIRMED:
            case WRONG_ITEM:
                return; // no need to do anything, already concluded verification


            case LOADED_AREA:
                if (StaticSpawnService.isSpawnLocationWithinViewDistance(spawnPoint, playerPoint)){
                    return; // still in loaded area
                }
                // left loaded area
                leftLoadedArea(observation,true);
                return;

            case NOT_YET_LOADED_AREA:
            case PREVIOUSLY_LOADED_AREA:
                if (StaticSpawnService.isSpawnLocationWithinViewDistance(spawnPoint, playerPoint)){
                    return; // still in loaded area
                }
                // entered loaded area
                if (despawnEventVerificationService.spawnLocationMayHaveEnteredViewDistanceThisTick(spawnPoint)){
                    return; // to give the spawn/unspawn events the first chance to report a concluding observation before this reporting a loaded
                }
                observation.setStatus(SpawnDataVerificationStatus.LOADED_AREA);
                log.debug("spawn entered view distance but not yet seen item");//todo make game message
                return;
        }
    }
    //endregion

    //region leftLoadedArea
    public void onLeftWorld(){//todo trigger this on leaving a world/logging out
        leftLoadedAreaAll();
    }

    public void shutDown(){
        leftLoadedAreaAll();
    }

    private void leftLoadedAreaAll(){
        observations.values()
                .forEach(observation -> leftLoadedArea(observation,false));
    }

    private void leftLoadedArea(SpawnDataVerificationObservation observation, boolean ableToSendGameMessage){
        if (observation.getStatus() != SpawnDataVerificationStatus.LOADED_AREA){
            return;
        }
        observation.setStatus(SpawnDataVerificationStatus.PREVIOUSLY_LOADED_AREA);
        if (ableToSendGameMessage) {
            log.debug("spawn left view distance but not yet seen item"); // todo make game message
        }
        addObservationToConfig(observation);
    }
    //endregion


    /* todo
        chat command to add all the PREVIOUSLY_LOADED_AREA as excluding overrides*/
    /* todo
        chat command to add all the WRONG_ITEM as excluding overrides*/

    /* todo
        config option to automatically add excluding overrides when PREVIOUSLY_LOADED_AREA*/
    /* todo
        config option to automatically add excluding overrides when WRONG_ITEM*/

    /* todo
        chat command to print a list of all the PREVIOUSLY_LOADED_AREA
        these imply the spawn data might need to be removed or you should re-check check the area*/
    /* todo
        chat command to print a list of all the WRONG_ITEM*/

    /* todo
        chat command to print a list of all spawn data that have no verification observations*/

    /* todo
        chat command to print a list of all spawn data that don't have a CONFIRMED verification*/


    /* todo
        chat command to append a dump of all the observations into the config*/
}
