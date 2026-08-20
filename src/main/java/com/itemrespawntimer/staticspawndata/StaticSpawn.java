package com.itemrespawntimer.staticspawndata;

import lombok.Builder;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;


@Value
@Builder
public class StaticSpawn
{
    int baseRespawnTicks;

    @Builder.Default // remove default, always want an item id
    int itemId = -1;

    @Builder.Default
    int quantity = -1;

    WorldPoint worldPoint;
}
