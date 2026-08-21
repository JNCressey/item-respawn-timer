package com.itemrespawntimer.staticspawnservice;

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
     * The numerical id of the item. Or -1 to track any item.
     */
    int itemId;

    /**
     * Check if the spawned item id matches this static spawn data. Always matches if this itemId is -1.
     * @param spawnedItemId The spawned item to check.
     * @return The result of the check.
     */
    public boolean matchItemId(int spawnedItemId) {
        return spawnedItemId==itemId || itemId==-1;
    }


    /**
     * The quantity of the stack, used for finding total stack value.
     */
    int quantity;

    public static class StaticSpawnBuilder {
        /**
         * Sets baseRespawnTicks by parsing as {@link Integer#parseInt}.
         * @param s The string value to parse.
         * @return The builder for chaining.
         * @throws NumberFormatException If value doesn't parse.
         */
        StaticSpawnBuilder parseBaseRespawnTicks(String s){
            this.baseRespawnTicks = Integer.parseInt(s);
            return this;
        }

        /**
         * Sets itemId by parsing as {@link Integer#parseInt}, or -1 if value doesn't parse.
         * @param s The string value to parse.
         * @return The builder for chaining.
         */
        StaticSpawnBuilder parseItemId(String s){
            try {
                this.itemId = Integer.parseInt(s);
            } catch ( NumberFormatException e ){
                this.itemId = -1;
            }
            return this;
        }

        /**
         * Sets quantity by parsing as {@link Integer#parseInt}.
         * @param s The string value to parse.
         * @return The builder for chaining.
         * @throws NumberFormatException If value doesn't parse.
         */
        StaticSpawnBuilder parseQuantity(String s){
            this.quantity = Integer.parseInt(s);
            return this;
        }
    }

}
