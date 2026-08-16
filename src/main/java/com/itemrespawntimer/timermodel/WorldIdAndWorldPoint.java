package com.itemrespawntimer.timermodel;

import net.runelite.api.coords.WorldPoint;
import lombok.Value;

/**
 * Both a world id as an 'int' and a WorldPoint, for scoping a location to a specific world
 */
@Value
public class WorldIdAndWorldPoint
{
    int worldId;
    WorldPoint worldPoint;
}
