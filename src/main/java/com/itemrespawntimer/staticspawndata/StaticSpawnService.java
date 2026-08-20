package com.itemrespawntimer.staticspawndata;

import java.util.*;
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