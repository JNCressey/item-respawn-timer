package com.itemrespawntimer;

import java.util.*;
import javax.inject.Singleton;

import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;

/**
 * Stub service: currently returns an empty map.
 *
 * You can later wire this to:
 *  - A generated data file of static spawns, or
 *  - A cache-reading utility run offline to produce that data.
 */
@Singleton
public class StaticSpawnService
{
    public Map<WorldPoint, List<StaticSpawn>> loadStaticSpawns()
    {
        // TODO: Replace with real static spawn loading from OSRS cache data.
        // For now, this returns an empty map but keeps the plugin compile-ready.
        HashMap<WorldPoint, List<StaticSpawn>> spawns = new HashMap<>();

        //region mind rune example, todo remove
        WorldPoint wp = new WorldPoint(3206,3208,0);
        StaticSpawn s = new StaticSpawn(558,1,100);
        spawns.put(wp,List.of(s));
        //endregion

        return spawns;

    }
}