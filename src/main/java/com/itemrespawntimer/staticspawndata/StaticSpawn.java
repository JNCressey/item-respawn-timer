package com.itemrespawntimer.staticspawndata;

import lombok.Builder;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;


@Value
@Builder
public class StaticSpawn
{
    /**
     * The location in the world where this spawns.
     */
    WorldPoint worldPoint;

    /**
     * The number of ticks used as the base respawn rate.
     * More populated words respawn faster.
     */
    int baseRespawnTicks;

    /**
     * The numerical id of the item.
     */
    int itemId;

    /**
     * The quantity of the stack, used for finding total stack value.
     */
    int quantity;

}
