package com.itemrespawntimer.staticspawndata;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import com.itemrespawntimer.timermodel.RespawnTimer;
import lombok.Getter;
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
     * If the last item is set as null, then the spawn won't be tracked.
     * On reloading the config, can remove all except first of the list.
     */
    @Getter //todo make getter for single and keep this map private
    private HashMap<WorldPoint, List<StaticSpawn>> trackedSpawns = new HashMap<>();

    private void debugTrackedSpawns(){//todo remove
        String debug = "Tracked Spawns:\n"
        + trackedSpawns.entrySet().stream()
                .map(e -> {
                    WorldPoint wp = e.getKey();
                    return String.format("#%d, %d, %d:\n", wp.getX(),wp.getY(),wp.getPlane())
                            + e.getValue().stream()
                            .map(s -> {
                                if (s==null){
                                    return "    null";
                                } else {
                                    return String.format("    %d",s.getItemId());
                                }
                            })
                            .collect(Collectors.joining("\n"));
                })
                .collect(Collectors.joining("\n"));
        log.debug(debug);
    }

    public void startUp(){
        String defaultTrackedSpawns = TrackedSpawnsDefaultFileReader.readResource();
        staticSpawnStreamFromCsvText(defaultTrackedSpawns)
                .forEach(e -> {
                    trackedSpawns.put(
                            e.getWorldPoint(),
                            Arrays.asList(e.getStaticSpawn())
                    );
                });
        reloadConfigOverrides();
        log.debug("static spawn service: startUp");
        debugTrackedSpawns();
    }

    private void clearConfigOverrides(){
        trackedSpawns.values().forEach(l -> {
            l.subList(1, l.size()).clear(); // remove all except first
        });
        log.debug("static spawn service: clearConfigOverrides");
        debugTrackedSpawns();
    }

    public void reloadConfigOverrides(){
        clearConfigOverrides();
        String configTrackedSpawns = config.trackedSpawns();
        staticSpawnStreamFromCsvText(configTrackedSpawns)
                .forEach(e -> {
                    trackedSpawns
                            .computeIfAbsent(
                                    e.getWorldPoint(),
                                    k -> {
                                        List<StaticSpawn> l = new ArrayList<>();
                                        l.add(null);
                                        return l;
                                    }
                            )
                            .add(e.getStaticSpawn());
                });
        log.debug("static spawn service: reloadConfigOverrides");
        debugTrackedSpawns();
    }

    private Stream<WorldPointAndStaticSpawn> staticSpawnStreamFromCsvText(String spawnDataCsvText){
        return spawnDataCsvText.lines()
                .map(this::staticSpawnFromCsvLine)
                .filter(Objects::nonNull);
    }

    /**
     * Parse a lines of static spawn data into a StaticSpawn to add.
     * If the line doesn't parse as spawn data, the return value is null.
     * If the line indicates that the worldpoint shouldn't be tracked, the return value has its staticSpawn value as null.//todo
     * @param spawnDataCsvLine
     * @return
     */
    private WorldPointAndStaticSpawn staticSpawnFromCsvLine(String spawnDataCsvLine){
        String lineWithoutComment = spawnDataCsvLine.split("#",2)[0];
        if (lineWithoutComment.isEmpty()){
            return null; //skip empty lines
        }

        WorldPoint wp;
        StaticSpawn.StaticSpawnBuilder s = StaticSpawn.builder();

        try { // try build `s`
            List<String> lineData = Text.fromCSV(lineWithoutComment);
            // [0]:x,[1]:y,[2]:plane,[3]:baseRespawnTicks,[4]:itemId,[5]:quantity

            wp = new WorldPoint(
                    Integer.parseInt(lineData.get(0)),
                    Integer.parseInt(lineData.get(1)),
                    Integer.parseInt(lineData.get(2)));

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

        return new WorldPointAndStaticSpawn(wp,s.build());
    }



    public Map<WorldPoint, List<StaticSpawn>> loadStaticSpawns()
    {
        HashMap<WorldPoint, List<StaticSpawn>> spawns = new HashMap<>();

        config.trackedSpawns().lines()
                .forEach(line -> loadTrackedSpawnsLine(line,spawns));

        return spawns;
    }

    /**
     * Parse the data from a line of trackedSpawns and add the appropriate data to map
     * @param line a line of the trackedSpawns configuration
     * @param spawns the map of spawn data
     */
    private void loadTrackedSpawnsLine(String line, HashMap<WorldPoint, List<StaticSpawn>> spawns){
        String lineWithoutComment = line.split("#",2)[0];
        if (lineWithoutComment.isEmpty()){
            return; //skip empty lines
        }

        WorldPoint wp;
        StaticSpawn.StaticSpawnBuilder s = StaticSpawn.builder();

        try { // try build `s`
            List<String> lineData = Text.fromCSV(lineWithoutComment);
            // [0]:x,[1]:y,[2]:plane,[3]:baseRespawnTicks,[4]:itemId,[5]:quantity

            wp = new WorldPoint(
                    Integer.parseInt(lineData.get(0)),
                    Integer.parseInt(lineData.get(1)),
                    Integer.parseInt(lineData.get(2)));

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
            return;
        }

        spawns
                .computeIfAbsent(wp,k -> new ArrayList<>())
                .add(s.build());
    }

    /**
     *
     * @param s the string to parse
     * @return the parsed integer if possible, else returns null
     */
    private Integer tryParseIntElseNull(String s){
        try{
            return Integer.valueOf(s);
        } catch (NumberFormatException e){
            return null;
        }
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