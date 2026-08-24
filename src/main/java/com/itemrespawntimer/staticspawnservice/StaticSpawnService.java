package com.itemrespawntimer.staticspawnservice;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.timermodel.RespawnTimer;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.ChatMessageType;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;

@Slf4j
@Singleton
public class StaticSpawnService
{

    //region head
    @Inject
    private ItemRespawnTimerConfig config;


    @Inject
    private ChatMessageManager chatMessageManager;


    @Inject
    private ItemManager itemManager;
    //endregion


    //region trackedSpawns
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
     * Get the static spawn data that is tracked for a given location, or empty optional if not tracking a spawn at the location.
     * @param wp The spawn location.
     * @return The spawn data.
     */
    public Optional<StaticSpawn> getTrackedSpawn(WorldPoint wp) {
        return getTrackedSpawn( wp, false);
    }

    /**
     * Get the static spawn data that is tracked for a given location, or empty optional if not tracking a spawn at the location.
     * @param wp The spawn location.
     * @param forceOverrideMode Whether to force getting the override even if config has overrides disabled.
     * @return The spawn data.
     */
    public Optional<StaticSpawn> getTrackedSpawn(WorldPoint wp, boolean forceOverrideMode){
        return Optional.ofNullable(trackedSpawns.get(wp))
                .flatMap(spawns ->
                        config.overridesEnabled() || forceOverrideMode
                                ? spawns.getLast()
                                : spawns.getFirst()
                );
    }
    //endregion


    //region defaultBaseRespawnTicks
    /**
     * key: The item id.
     * value: Default value for baseRespawnTicks, for this item.
     */
    Map<Integer, Integer> defaultBaseRespawnTicks = new HashMap<>();

    public void addOverrideWithDefaultRespawnTicksIfAbsent(WorldPoint wp, TileItem item){//todo trigger this
        if (item.getId()==592) { return; } // skip ashes because ashes from fires are also detected as having OWNERSHIP_NONE
        if (!config.defaultRateAddOverrides()) { return; } // skip if configured not to add defaults
        //todo have a config to add with 9999 if we don't have a default (useful for making sure we go back to check if we saw a ground item but didn't wait)

        if (trackedSpawns.containsKey(wp)){ return; } // skip if already have data (an 'exclude' also blocks it, so does ever having an 'exclude' override during this session, even after removing that override, because adding an override computes if absent an empty for index [0])

        Optional.ofNullable(defaultBaseRespawnTicks.get(item.getId()))
                .ifPresent(baseRespawnTicks -> {
                    String newOverride = String.format( // add override with new data
                            "%s, %s, %s, %s, %s, %s",
                            wp.getX(),wp.getY(),wp.getPlane(),
                            baseRespawnTicks,
                            item.getId(),
                            item.getQuantity());

                    config.setTrackedSpawnsOverrides(
                            config.trackedSpawnsOverrides()
                                    +"\n" + newOverride
                    );

                    //chat message
                    {
                        ChatMessageBuilder observationMessageBuilder = new ChatMessageBuilder()
                                .append(ChatColorType.NORMAL)
                                .append("ItemRespawnTimer Discovery Mode: added override with default baseRespawnTicks, without waiting to time the respawn.")
                                .append(" ")
                                .append("(If this item isn't from an item respawn, turn off the config option \"Automatically add overrides using default respawn rates\".)");

                        //location
                        {
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
                        {
                            observationMessageBuilder
                                    .append(", baseRespawnTicks: ")
                                    .append(ChatColorType.HIGHLIGHT)
                                    .append(String.valueOf(baseRespawnTicks))
                                    .append(ChatColorType.NORMAL);
                        }

                        //item
                        {
                            int itemId = item.getId();
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
                                    .append(String.valueOf(item.getQuantity()))
                                    .append(ChatColorType.NORMAL);
                        }

                        // override CSV
                        {
                            observationMessageBuilder.append(String.format("\nAdd new override: %s", newOverride));

                        }

                        chatMessageManager
                                .queue(QueuedMessage.builder()
                                        .type(ChatMessageType.CONSOLE)
                                        .runeLiteFormattedMessage(observationMessageBuilder.build())
                                        .build());

                        log.debug("Add new override: {}", newOverride);
                    }
                });
    }
    //endregion


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

        defaultBaseRespawnTicks = (new DefaultBaseRespawnTicksReader())
                .getDefaultBaseRespawnTicks();
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