package com.itemrespawntimer.staticspawndata;

import lombok.Builder;
import lombok.Value;


@Value
@Builder
public class StaticSpawn
{
    int baseRespawnTicks;

    @Builder.Default
    int itemId = -1;

    @Builder.Default
    int quantity = -1;
}
