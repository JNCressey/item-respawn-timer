package com.itemrespawntimer.staticspawndata;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.itemrespawntimer.ItemRespawnTimerConfig;
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
        if (line.isEmpty() || line.charAt(0) == '#'){
            return; //skip comment lines
        }

        List<String> lineData = Text.fromCSV(line);
        // [0]:x,[1]:y,[2]:z,[3]:baseRespawnTicks,[4]:itemIdOrName,[5]:quantity

        WorldPoint wp;
        try {
            wp = new WorldPoint(
                    Integer.parseInt(lineData.get(0)),
                    Integer.parseInt(lineData.get(1)),
                    Integer.parseInt(lineData.get(2)));
        } catch(NumberFormatException e){
            return; // skip this line if coordinates can't be parsed
        }

        StaticSpawn.StaticSpawnBuilder s = StaticSpawn.builder();

        // set baseRespawnTicks
        s.baseRespawnTicks(Integer.parseInt(lineData.get(3)));

        // set itemId if present
        lineData.stream()
                .skip(4).findFirst() // nth value if present line has enough values
                .filter(itemId -> !itemId.isEmpty())
                .map(this::tryParseIntElseNull)
                .ifPresent(s::itemId); // set value of the static spawn


        // set quantity if present
        lineData.stream()
                .skip(5).findFirst() // nth value if present line has enough values
                .filter(quantity -> !quantity.isEmpty())
                .map(this::tryParseIntElseNull)
                .ifPresent(s::quantity); // set value of the static spawn


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

}