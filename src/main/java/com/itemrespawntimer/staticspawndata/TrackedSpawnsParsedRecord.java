package com.itemrespawntimer.staticspawndata;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The parsed data from a line of the tracked spawns default file or config overrides.
 */
@Value
public class TrackedSpawnsParsedRecord {

    /**
     * The world point where the item spawns.
     */
    WorldPoint worldPoint;

    @Nullable
    StaticSpawn staticSpawn;

    /**
     * The static spawn data to track.
     * If the tracked data line indicates this point shouldn't be tracked, this will be an empty optional.
     * @return The static spawn data to track.
     */
    Optional<StaticSpawn> getStaticSpawn(){
        return Optional.ofNullable(staticSpawn);
    }
}
