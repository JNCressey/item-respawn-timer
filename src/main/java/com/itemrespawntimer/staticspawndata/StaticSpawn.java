package com.itemrespawntimer.staticspawndata;

import lombok.Builder;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;


@Value
@Builder
public class StaticSpawn
{
    WorldPoint worldPoint;

    int baseRespawnTicks;

    int itemId;

    int quantity;

}
