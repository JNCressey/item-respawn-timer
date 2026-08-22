package com.itemrespawntimer.debug.spawndiscovery;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.ItemRespawnTimerPlugin;
import com.itemrespawntimer.staticspawnservice.StaticSpawn;
import com.itemrespawntimer.staticspawnservice.StaticSpawnService;
import com.itemrespawntimer.timermodel.DespawnEventVerificationService;
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

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;

@Slf4j
@Singleton
public class DebugSpawnDiscoveryService {

    //region head
    @Inject
    private Client client;


    @Inject
    private ItemRespawnTimerPlugin plugin;


    @Inject
    private ItemRespawnTimerConfig config;


    @Inject
    private StaticSpawnService staticSpawnService;


    @Inject
    private ChatMessageManager chatMessageManager;


    @Inject
    private ItemManager itemManager;

    @Inject
    private DespawnEventVerificationService despawnEventVerificationService;
    //endregion


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
                .append("ItemRespawnTimer Discovery Mode: observed item spawned");
        if (!observation.isComplete()) {
            observationMessageBuilder.append(", but didn't observe when it was taken");
        }
        observationMessageBuilder.append(".");

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
                            observationMessageBuilder.append("\nNew spawn discovered, no data for this point.");
                            if (observation.isComplete()) {
                                addOverride(observation, observationMessageBuilder);
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

        if (despawnEventVerificationService.despawnEventMaybeFromReenteringAnArea(event)){
            return;// filter out delayed despawn events from returning to a location but not directly witnessing the item being taken
        }

        WorldPoint wp = tile.getWorldLocation();

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

        int worldPopulation = plugin.getCurrentWorldPopulation();
        observation.setWorldPopulationAtPickup(worldPopulation);

        observations.put(wp,observation);

        Optional<StaticSpawn> optionalTrackedSpawn = staticSpawnService.getTrackedSpawn(wp, true);

        ChatMessageBuilder observationMessageBuilder = new ChatMessageBuilder()
                .append(ChatColorType.NORMAL)
                .append("ItemRespawnTimer Discovery Mode: observed item taken.");

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
                            observationMessageBuilder.append("\nNew spawn discovered, no data for this point, but don't have full observation yet");
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
     * @param observationMessageBuilder A message about this observation to show to the player.
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
     * @param observationMessageBuilder A message about this observation to show to the player.
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
     * @param observationMessageBuilder A message about this observation to show to the player.
     * @return Result of the check.
     */
    private boolean checkObservationDifferentFromData(
            StaticSpawnObservation observation,
            StaticSpawn trackedSpawn,
            ChatMessageBuilder observationMessageBuilder
    ){
        StaticSpawn observedSpawn = observation.getSpawn();
        boolean trackedItemIsDifferent = false;

        // notify if different baseRespawnTicks
        {
            int observedBaseRespawnTicks = observation.getBaseRespawnTicksPrediction();
            int trackedBaseRespawnTicks = trackedSpawn.getBaseRespawnTicks();
            if ( observation.isComplete()
                 && ( Math.abs(observedBaseRespawnTicks - trackedBaseRespawnTicks)
                      > config.discoveryBaseRespawnTicksThreshold())
            ) {
                trackedItemIsDifferent = true;
                observationMessageBuilder
                        .append("\nObserved baseRespawnTicks ")
                        .append(ChatColorType.HIGHLIGHT)
                        .append(String.valueOf(observedBaseRespawnTicks))
                        .append(ChatColorType.NORMAL)
                        .append(" is different to data ")
                        .append(ChatColorType.HIGHLIGHT)
                        .append(String.valueOf(trackedBaseRespawnTicks))
                        .append(ChatColorType.NORMAL)
                        .append(".");
            }
        }

        // notify if different item id
        {
            int observedItemId = observedSpawn.getItemId();
            int trackedItemId = trackedSpawn.getItemId();
            if (observedItemId != trackedItemId) {
                trackedItemIsDifferent = true;
                observationMessageBuilder
                        .append("\nObserved item id ")
                        .append(ChatColorType.HIGHLIGHT)
                        .append(String.valueOf(observedItemId))
                        .append(ChatColorType.NORMAL)
                        .append(" is different to data ")
                        .append(ChatColorType.HIGHLIGHT)
                        .append(String.valueOf(trackedItemId))
                        .append(ChatColorType.NORMAL)
                        .append(".");
            }
        }

        // notify if different quantity
        {
            int observedQuantity = observedSpawn.getQuantity();
            int trackedQuantity = trackedSpawn.getQuantity();
            if (observedQuantity != trackedQuantity) {
                trackedItemIsDifferent = true;
                observationMessageBuilder
                        .append("\nObserved quantity ")
                        .append(ChatColorType.HIGHLIGHT)
                        .append(String.valueOf(observedQuantity))
                        .append(ChatColorType.NORMAL)
                        .append(" is different to data ")
                        .append(ChatColorType.HIGHLIGHT)
                        .append(String.valueOf(trackedQuantity))
                        .append(ChatColorType.NORMAL)
                        .append(".");
            }
        }

        return trackedItemIsDifferent;
    }


    //region addOverride
    /**
     * Add an override according to this observed data.
     * If observation complete the override will be for it to be tracked.
     * If observation is incomplete the override will be to block the tracking at that point.
     * @param observation The observed new spawn data.
     * @param observationMessageBuilder A message about this observation to show to the player.
     */
    private void addOverride(
            StaticSpawnObservation observation,
            ChatMessageBuilder observationMessageBuilder
    ){
        if (!config.discoveryModeAutoAddOverrides()){ return; }

        String newOverride = getNewOverride(observation);

        observationMessageBuilder.append(String.format("\nAdd new override: %s",newOverride));
        log.debug("Add new override: {}", newOverride);

        if (config.discoveryModeAutoAddOverrides()){
            // append new override to config
            appendLineToConfigTrackedSpawnsOverrides(newOverride);
        }
    }


    /**
     * Add a line of text to the config 'trackedSpawnsOverrides'.
     * @param line A line of text to add.
     */
    private void appendLineToConfigTrackedSpawnsOverrides(String line){
        config.setTrackedSpawnsOverrides(
                config.trackedSpawnsOverrides()
                        +"\n" + line
        );

    }


    /**
     * Make an override entry from an observation.
     * @param observation The observed new spawn data.
     * @return The new override as a CSV line.
     */
    @Nonnull
    private static String getNewOverride(StaticSpawnObservation observation) {
        WorldPoint wp = observation.getSpawn().getWorldPoint();
        return observation.isComplete()
                ? String.format( // add override with new data
                    "%s, %s, %s, %s, %s, %s",
                    wp.getX(),wp.getY(),wp.getPlane(),
                    observation.getBaseRespawnTicksPrediction(),
                    observation.getSpawn().getItemId(),
                    observation.getSpawn().getQuantity())
                : String.format( // add override to exclude bad data for this location
                    "%s, %s, %s, exclude",
                    wp.getX(),wp.getY(),wp.getPlane());
    }
    //endregion


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
    }


    /**
     * Clear observations if hopped to a new world
     */
    public void joinedWorld(){//todo can i make this subscribed?
        if(!config.discoveryModeEnabled()) { return; }

        observations.clear();
    }

}
