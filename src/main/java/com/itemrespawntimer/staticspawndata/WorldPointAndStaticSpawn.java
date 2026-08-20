package com.itemrespawntimer.staticspawndata;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
public class WorldPointAndStaticSpawn {
    WorldPoint worldPoint;
    StaticSpawn staticSpawn;
}
