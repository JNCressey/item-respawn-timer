package com.itemrespawntimer.staticspawnservice;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import net.runelite.client.util.Text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseRespawnTicksReader {


    /**
     * The mapping itemId->baseRespawnTicks, to built by {@link #getMapItemIdToBaseRespawnTicks()}
     * key: The item id.
     * value: baseRespawnTicks, for this item.
     */
    Map<Integer,Integer> mapItemIdToBaseRespawnTicks = new HashMap<>();


    //region readResource
    private static final String resourceFilename = "BaseRespawnTicks.csv";


    private String readResource(){
        try (InputStream in = ItemRespawnTimerConfig.class.getClassLoader().getResourceAsStream(resourceFilename)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourceFilename);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch(IOException e){
            return "";
        }
    }
    //endregion


    /**
     * Get the parsed records from the resource file, as a map itemId->baseRespawnTicks.
     * @return The mapping defined by the records.
     */
    public Map<Integer,Integer> getMapItemIdToBaseRespawnTicks(){
        readResource().lines()
                .forEach(this::parseCsvLine);

        return mapItemIdToBaseRespawnTicks;
    }


    /**
     * Parse a line of itemId->baseRespawnTicks data,
     *      and put the result in {@link #mapItemIdToBaseRespawnTicks}.
     * If the line doesn't parse as itemId->baseRespawnTicks data, the line will be skipped.
     * The rest of the line after a `#` character is ignored as a comment in the data.
     *
     * @param baseRespawnTicksCsvLine The single line of CSV data to parse.
     */
    @SuppressWarnings("UnnecessaryReturnStatement")
    private void parseCsvLine(String baseRespawnTicksCsvLine){
        String lineWithoutComment = baseRespawnTicksCsvLine.split("#",2)[0];

        /*
         * `lineData` is the record data:
         * [0]:itemId,[1]:baseRespawnTicks
         */
        List<String> lineData = Text.fromCSV(lineWithoutComment);

        try {

            int itemId = Integer.parseInt(lineData.get(0));
            int baseRespawnTicks = Integer.parseInt(lineData.get(1));

            mapItemIdToBaseRespawnTicks.put(itemId,baseRespawnTicks);

        }
        catch (
                IndexOutOfBoundsException // skip this line if line doesn't have enough data cells
                | NumberFormatException // skip this line if cell data doesn't parse
                        e
        ) {
            return;
        }

    }
    //endregion

}
