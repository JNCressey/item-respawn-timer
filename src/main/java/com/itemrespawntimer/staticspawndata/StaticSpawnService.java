package com.itemrespawntimer.staticspawndata;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.timermodel.RespawnTimer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Text;

import net.runelite.api.coords.WorldPoint;

@Slf4j
@Singleton
public class StaticSpawnService
{

    @Inject
    private ItemRespawnTimerConfig config;

    /**
     * The static spawn data that is to be tracked.
     * [0]: The data from TrackedSpawnsDefault.csv, or null if not in default set.
     * [i>0]: The data from config.trackedSpawns(), or null if that config says not to track
     * The priority will be that the last item of the list will be used.
     * If the last item is set as null, then the spawn won't be tracked.//todo null->optional null
     * On reloading the config, can remove all except first of the list.
     */
    private final HashMap<WorldPoint, LinkedList<Optional<StaticSpawn>>> trackedSpawns = new HashMap<>();


    /**
     * Get the static spawn data for a given location, or empty optional if not tracking a spawn at the location.
     * @param wp The spawn location.
     * @return The spawn data.
     */
    public Optional<StaticSpawn> getTrackedSpawn(WorldPoint wp){
        /*LinkedList<Optional<StaticSpawn>> l = trackedSpawns.get(wp);
        if (l==null){
            return Optional.empty();
        } else {
            return l.getLast();
        }*///todo remove
        return Optional.ofNullable(trackedSpawns.get(wp)).flatMap(LinkedList::getLast);
    }


    private void debugTrackedSpawns(){//todo remove
        String debug = "Tracked Spawns:\n"
        + trackedSpawns.entrySet().stream()
                .map(e -> {
                    WorldPoint wp = e.getKey();
                    return String.format("#%d, %d, %d:\n", wp.getX(),wp.getY(),wp.getPlane())
                            + e.getValue().stream()
                            .map(s ->
                                s.map(spawn -> String.format("    %d",spawn.getItemId()))
                                        .orElse("    null")
                            )
                            .collect(Collectors.joining("\n"));
                })
                .collect(Collectors.joining("\n"));
        log.debug(debug);
    }


    public void startUp(){
        String defaultTrackedSpawnsCsvText = TrackedSpawnsDefaultFileReader.readResource();
        parseTrackedSpawnsFromCsvText(defaultTrackedSpawnsCsvText)
                .forEach(e ->
                    trackedSpawns.put(
                            e.getWorldPoint(),
                            new LinkedList<>(Collections.singletonList(e.getStaticSpawn()))
                    )
                );
        reloadConfigOverrides();
    }


    //region reloadConfigOverrides
    public void reloadConfigOverrides(){
        clearConfigOverrides();
        String configTrackedSpawns = config.trackedSpawns();
        parseTrackedSpawnsFromCsvText(configTrackedSpawns)
                .forEach(e ->
                    trackedSpawns
                            .computeIfAbsent(
                                    e.getWorldPoint(),
                                    k -> new LinkedList<>(Collections.singletonList(Optional.empty()))
                            )
                            .add(e.getStaticSpawn())
                );
        log.debug("static spawn service: reloadConfigOverrides");
        debugTrackedSpawns();
    }

    private void clearConfigOverrides(){
        trackedSpawns.values().forEach(l -> {
            l.subList(1, l.size()).clear(); // remove all except first
        });
    }
    //endregion


    //region parseTrackedSpawnsFromCsvText
    /**
     * Parse the lines of tracked spawn data as {@link #parseTrackedSpawnFromCsvLine} into StaticSpawn data to add.
     * Lines that don't parse as spawn data are filtered from the stream.
     * @param spawnDataCsvText The CSV text to parse
     * @return The parsed data.
     */
    private Stream<TrackedSpawnsParsedRecord> parseTrackedSpawnsFromCsvText(String spawnDataCsvText){
        return spawnDataCsvText.lines()
                .map(this::parseTrackedSpawnFromCsvLine)
                .filter(Objects::nonNull);
    }


    /**
     * Parse a line of static spawn data into a StaticSpawn to add.
     * If the line doesn't parse as spawn data, the return value is null.
     * The rest of the line after a `#` character is ignored as a comment in the data.
     * @param spawnDataCsvLine The single line of CSV data to parse.
     * @return The parsed data.
     */
    private TrackedSpawnsParsedRecord parseTrackedSpawnFromCsvLine(String spawnDataCsvLine){
        String lineWithoutComment = spawnDataCsvLine.split("#",2)[0];
        if (lineWithoutComment.isEmpty()){
            return null; //skip empty lines
        }

        WorldPoint wp;
        StaticSpawn.StaticSpawnBuilder s = StaticSpawn.builder();

        try { // try build `s`
            /*
             * `lineData` is the record data:
             * [0]:x,[1]:y,[2]:plane
             * for positions to be tracked: [3]:baseRespawnTicks,[4]:itemId,[5]:quantity
             * for positions not to be tracked: [3]:"null"
             */
            List<String> lineData = Text.fromCSV(lineWithoutComment);

            wp = new WorldPoint(
                    Integer.parseInt(lineData.get(0)),
                    Integer.parseInt(lineData.get(1)),
                    Integer.parseInt(lineData.get(2)));

            if (lineData.get(3).equals("null")){
                return new TrackedSpawnsParsedRecord(wp,null); // entry indicates to not track this location
            }

            // set worldPoint
            s.worldPoint(wp);

            // set baseRespawnTicks
            s.baseRespawnTicks( Integer.parseInt(lineData.get(3)) );

            // set itemId
            s.itemId(           Integer.parseInt(lineData.get(4)) );

            // set quantity
            s.quantity(         Integer.parseInt(lineData.get(5)) );
        }
        catch (
                IndexOutOfBoundsException // skip this line if line doesn't have enough data cells
                | NumberFormatException // skip this line if cell data doesn't parse
                e
        ) {
            return null;
        }

        return new TrackedSpawnsParsedRecord(wp,s.build());
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