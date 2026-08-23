package com.itemrespawntimer.debug.spawndataverification;

import com.itemrespawntimer.staticspawnservice.StaticSpawn;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

@Data
public class SpawnDataVerificationObservation {

    WorldPoint worldpoint;


    /**
     * Either spawn data from StaticSpawnService
     * or partial spawn data where baseRespawnTicks is unknown so left as 0
     */
    StaticSpawn spawn;

    SpawnDataVerificationStatus status = SpawnDataVerificationStatus.NOT_YET_LOADED_AREA;

    public String toCsvLine(){
        return String.join(", ",
                Integer.toString(worldpoint.getX()),
                Integer.toString(worldpoint.getY()),
                Integer.toString(worldpoint.getPlane()),

                Integer.toString(spawn.getItemId()),
                Integer.toString(spawn.getQuantity()),

                status.toString()
        );
    }

}
