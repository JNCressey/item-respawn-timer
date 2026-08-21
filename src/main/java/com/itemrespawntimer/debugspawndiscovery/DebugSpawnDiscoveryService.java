package com.itemrespawntimer.debugspawndiscovery;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.staticspawnservice.StaticSpawn;
import com.itemrespawntimer.staticspawnservice.StaticSpawnService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.WorldService;
import net.runelite.http.api.worlds.WorldResult;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;

@Slf4j
@Singleton
public class DebugSpawnDiscoveryService {

    @Inject
    private Client client;

    @Inject
    private StaticSpawnService staticSpawnService;


    @Inject
    private WorldService worldService;


    @Inject
    private ItemRespawnTimerConfig config;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private ItemManager itemManager;

    /**
     * The spawn observations currently being tracked
     */
    private final Map<WorldPoint,StaticSpawnObservation> observations = new HashMap<>();


    /**
     *
     * @param event The item that spawned
     */
    public void onItemSpawned(ItemSpawned event)//todo can i make this subscribed?
    {
        if(!config.discoveryModeEnabled()) { return; }

        Tile tile = event.getTile();
        TileItem item = event.getItem();
        if (tile == null || item == null) {
            return;
        }

        if (item.getOwnership() != TileItem.OWNERSHIP_NONE){
            return; // only react to items that were naturally spawned
        }

        WorldPoint wp = tile.getWorldLocation();

        StaticSpawnObservation observation = Optional.ofNullable(observations.get(wp))
                .orElseGet(() -> {
                    StaticSpawnObservation newObservation = new StaticSpawnObservation();
                    newObservation.setSpawn(
                            StaticSpawn.builder()
                                    .worldPoint(wp)
                                    .itemId(item.getId())
                                    .quantity(item.getQuantity())
                                    .build()
                    );
                    return newObservation;
                });

        long nowMillis = Instant.now().toEpochMilli();
        observation.setRespawnTimeMillis(nowMillis);

        Optional<StaticSpawn> optionalTrackedSpawn = staticSpawnService.getTrackedSpawn(wp, true);
        ChatMessageBuilder observationMessageBuilder = new ChatMessageBuilder()
                .append(ChatColorType.NORMAL)
                .append("ItemRespawnTimer discovery Mode: observed item spawned");
        if (!observation.isComplete()) {
            observationMessageBuilder.append(", but didn't observe when it was taken");
        }

        addObservationToMessageBuilder(observation,observationMessageBuilder);

        boolean observationSameAsData = optionalTrackedSpawn
                .map(
                        trackedSpawn -> addOverrideIfObservationDifferentFromData(
                                observation,
                                trackedSpawn,
                                observationMessageBuilder
                        )
                )
                .orElseGet(
                        () -> {
                            if (observation.isComplete()) {
                                addOverride(observation, observationMessageBuilder);
                            } else {
                                observationMessageBuilder.append("\nnew spawn discovered, no data for this point, but don't have full data yet");
                            }
                            return  false;
                        }
                );
        if( !observationSameAsData || config.discoveryModeNotifyCorrect() ) {
            chatMessageManager
                    .queue(QueuedMessage.builder()
                            .type(ChatMessageType.CONSOLE)
                            .runeLiteFormattedMessage(observationMessageBuilder.build())
                            .build());
        }

        observations.remove(wp,observation);

    }


    /**
     *
     * @param event The item that despawned
     */
    public void onItemDespawned(ItemDespawned event)//todo can i make this subscribed?
    {
        if(!config.discoveryModeEnabled()) { return; }

        Tile tile = event.getTile();
        TileItem item = event.getItem();
        if (tile == null || item == null) {
            return;
        }

        if (item.getOwnership() != TileItem.OWNERSHIP_NONE){
            return; // only react to items that were naturally spawned
        }

        WorldPoint wp = tile.getWorldLocation();
        if(spawnLocationMayHaveEnteredViewDistanceThisTick(wp)){
            return; // filter out delayed despawn events from returning to a location but not directly witnessing the item being taken
        }

        StaticSpawnObservation observation = new StaticSpawnObservation();
        observation.setSpawn(
                StaticSpawn.builder()
                        .worldPoint(wp)
                        .itemId(item.getId())
                        .quantity(item.getQuantity())
                        .build()
        );


        long nowMillis = Instant.now().toEpochMilli();
        observation.setPickupTimeMillis(nowMillis);

        int worldPopulation = getCurrentWorldPopulation();
        observation.setWorldPopulationAtPickup(worldPopulation);

        observations.put(wp,observation);

        Optional<StaticSpawn> optionalTrackedSpawn = staticSpawnService.getTrackedSpawn(wp, true);

        ChatMessageBuilder observationMessageBuilder = new ChatMessageBuilder()
                .append(ChatColorType.NORMAL)
                .append("ItemRespawnTimer discovery Mode: observed item taken");

        addObservationToMessageBuilder(observation,observationMessageBuilder);

        boolean observationSameAsData = optionalTrackedSpawn
                .map(
                        trackedSpawn -> addOverrideIfObservationDifferentFromData(
                                observation,
                                trackedSpawn,
                                observationMessageBuilder
                        )
                )
                .orElseGet(
                        () -> {
                            observationMessageBuilder.append("\nnew spawn discovered, no data for this point, but don't have full data yet");
                            return false;
                        }
                );

        if( !observationSameAsData || config.discoveryModeNotifyCorrect() ) {
            chatMessageManager
                    .queue(QueuedMessage.builder()
                            .type(ChatMessageType.CONSOLE)
                            .runeLiteFormattedMessage(observationMessageBuilder.build())
                            .build());
        }
    }


    /**
     *
     * @param observation The observed new spawn data.
     * @param observationMessageBuilder The output currently for log, but todo make it output to game message
     */
    private void addObservationToMessageBuilder(
            StaticSpawnObservation observation,
            ChatMessageBuilder observationMessageBuilder
    ){

        //location
        {
            WorldPoint wp = observation.getSpawn().getWorldPoint();
            observationMessageBuilder
                    .append("\nLocation: ")
                    .append(ChatColorType.HIGHLIGHT)
                    .append(String.valueOf(wp.getX()))
                    .append(ChatColorType.NORMAL)
                    .append(", ")
                    .append(ChatColorType.HIGHLIGHT)
                    .append(String.valueOf(wp.getY()))
                    .append(ChatColorType.NORMAL)
                    .append(", ")
                    .append(ChatColorType.HIGHLIGHT)
                    .append(String.valueOf(wp.getPlane()))
                    .append(ChatColorType.NORMAL);
        }

        // respawn ticks
        if (observation.isComplete()){
            observationMessageBuilder
                    .append(", baseRespawnTicks: ")
                    .append(ChatColorType.HIGHLIGHT)
                    .append(String.valueOf(observation.getBaseRespawnTicksPrediction()))
                    .append(ChatColorType.NORMAL);
        }

        //item
        {
            int itemId = observation.getSpawn().getItemId();
            String itemName = itemManager.getItemComposition(itemId).getName();
            observationMessageBuilder
                    .append(", Item: ")
                    .append(ChatColorType.HIGHLIGHT)
                    .append(itemName)
                    .append(ChatColorType.NORMAL)
                    .append(" (")
                    .append(ChatColorType.HIGHLIGHT)
                    .append(String.valueOf(itemId))
                    .append(ChatColorType.NORMAL)
                    .append(")");
        }

        //quantity
        {
            observationMessageBuilder
                    .append(", Quantity: ")
                    .append(ChatColorType.HIGHLIGHT)
                    .append(String.valueOf(observation.getSpawn().getQuantity()))
                    .append(ChatColorType.NORMAL);
        }
    }


    /**
     * If observation item is different from tracked spawn, then add new override.
     * @param observation The observed new spawn data.
     * @param trackedSpawn The spawn data currently tracked.
     * @param observationMessageBuilder The output currently for log, but todo make it output to game message
     * @return If the observation matches the data.
     */
    private boolean addOverrideIfObservationDifferentFromData(
            StaticSpawnObservation observation,
            StaticSpawn trackedSpawn,
            ChatMessageBuilder observationMessageBuilder
    ){
        boolean trackedItemIsDifferent = checkObservationDifferentFromData(
                observation,
                trackedSpawn,
                observationMessageBuilder
        );

        if (trackedItemIsDifferent) {
            addOverride(observation, observationMessageBuilder);
        } else {
            observationMessageBuilder.append("\n");
            if (observation.isComplete()){
                observationMessageBuilder.append("baseRespawnTicks, ");
            }
            observationMessageBuilder.append("itemId, and quantity data are correct");
        }

        return !trackedItemIsDifferent;
    }


    /**
     * Check if observation item is different from tracked spawn.
     * @param observation The observed new spawn data.
     * @param trackedSpawn The spawn data currently tracked.
     * @param observationMessageBuilder The output currently for log, but todo make it output to game message
     * @return Result of the check.
     */
    private boolean checkObservationDifferentFromData(
            StaticSpawnObservation observation,
            StaticSpawn trackedSpawn,
            ChatMessageBuilder observationMessageBuilder
    ){
        StaticSpawn observedSpawn = observation.getSpawn();
        boolean trackedItemIsDifferent = false;

        if (trackedSpawn.getItemId()!=observedSpawn.getItemId()){
            trackedItemIsDifferent = true;
            observationMessageBuilder.append("\nobserved item id is different to data");//todo make game message
        }

        if(trackedSpawn.getQuantity()!=observedSpawn.getQuantity()){
            trackedItemIsDifferent = true;
            observationMessageBuilder.append("\nobserved quantity is different to data");//todo make game message
        }

        if(
                observation.isComplete()
                && Math.abs(trackedSpawn.getBaseRespawnTicks() - observation.getBaseRespawnTicksPrediction()) > config.discoveryBaseRespawnTicksThreshold()
        ){
            trackedItemIsDifferent = true;
            observationMessageBuilder.append("\nobserved baseRespawnTicks is different to data");//todo make game message
        }


        return trackedItemIsDifferent;
    }



    /**
     * Add an override according to this observed data.
     * If observation complete the override will be for it to be tracked.
     * If observation is incomplete the override will be to block the tracking at that point.
     * @param observation The observed new spawn data.
     * @param observationMessageBuilder The output currently for log, but todo make it output to game message
     */
    private void addOverride(
            StaticSpawnObservation observation,
            ChatMessageBuilder observationMessageBuilder
    ){
        if (!config.discoveryModeAutoAddOverrides()){ return; }

        WorldPoint wp = observation.getSpawn().getWorldPoint();
        String newOverride = observation.isComplete()
                ? String.format( // add override with new data
                "%s, %s, %s, %s, %s, %s",
                wp.getX(),wp.getY(),wp.getPlane(),
                observation.getBaseRespawnTicksPrediction(),
                observation.getSpawn().getItemId(),
                observation.getSpawn().getQuantity())
                : String.format( // add override to exclude bad data for this location
                "%s, %s, %s, exclude",
                wp.getX(),wp.getY(),wp.getPlane());

        observationMessageBuilder.append(String.format("\nadded new override: %s",newOverride));//todo make game message
        log.debug("add new override: {}", newOverride);
        //todo add new override to config
    }



    /**
     * Remove observations that go out of view distance
     */
    public void onGameTick(){//todo can i make this subscribe?
        if(!config.discoveryModeEnabled()) { return; }

        observations.entrySet()
                .removeIf(entry-> {
                    StaticSpawn spawn = entry.getValue()
                            .getSpawn();
                    WorldPoint playerPoint = client.getLocalPlayer().getWorldLocation();
                    return !StaticSpawnService.isSpawnLocationWithinViewDistance(spawn,playerPoint);
                });
        recordThisTickPlayerLocation();
    }

    /**
     * Clear observations if hopped to a new world
     */
    public void joinedWorld(){//todo can i make this subscribed?
        if(!config.discoveryModeEnabled()) { return; }

        observations.clear();
    }


    //todo this region is repeated from ActiveTimers, move to shared
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


    //todo this region is repeated from ActiveTimers, move to shared
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
}
