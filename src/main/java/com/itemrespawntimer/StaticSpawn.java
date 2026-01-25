package com.itemrespawntimer;

import lombok.Value;

@Value
public class StaticSpawn
{
    int itemId;
    int quantity;
    int baseRespawnTicks;
}
