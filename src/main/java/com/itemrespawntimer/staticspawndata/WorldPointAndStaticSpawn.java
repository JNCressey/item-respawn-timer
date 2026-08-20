package com.itemrespawntimer.staticspawndata;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.Optional;

@Value
public class WorldPointAndStaticSpawn {
    WorldPoint worldPoint;

    @Nullable
    StaticSpawn staticSpawn;

    Optional<StaticSpawn> getStaticSpawn(){
        return Optional.ofNullable(staticSpawn);
    }
}
