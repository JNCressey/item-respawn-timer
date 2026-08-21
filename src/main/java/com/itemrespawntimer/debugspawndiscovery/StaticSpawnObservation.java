package com.itemrespawntimer.debugspawndiscovery;

import com.itemrespawntimer.staticspawnservice.StaticSpawn;
import lombok.Data;

@Data
public class StaticSpawnObservation {
    /**
     * Partial spawn data, but baseRespawnTicks is unknown so left as 0
     */
    StaticSpawn spawn;


    /**
     * The time when the item was taken, as a millisecond timestamp.
     */
    long pickupTimeMillis;


    /**
     * The world population at the time when the item was taken.
     */
    int worldPopulationAtPickup;


    /**
     * The time when the item was respawned, as a millisecond timestamp.
     */
    long respawnTimeMillis;


    /**
     * The total time the item took to respawn, in milliseconds.
     */
    long getTotalRespawnTimeMillis(){
        return respawnTimeMillis - pickupTimeMillis;
    }


    /**
     * Predict the value of baseRespawnTicks.
     * @return The predicted value;
     */
    int getBaseRespawnTicksPrediction(){
        //formula for respawn duration is: floor(baseDuration * (4000-population)/4000)
        // the inverse is baseDuration = duration*4000/(4000-population)
        double baseRespawnMillis = getTotalRespawnTimeMillis() * 4000.0
                / (4000.0-worldPopulationAtPickup);
        return (int) Math.round(baseRespawnMillis/600);

    }

}
