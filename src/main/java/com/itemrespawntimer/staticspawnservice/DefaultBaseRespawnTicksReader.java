package com.itemrespawntimer.staticspawnservice;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import net.runelite.client.util.Text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultBaseRespawnTicksReader {


    Map<Integer,Integer> defaultBaseRespawnTicks = new HashMap<>();


    //region readResource
    private static final String defaultResourceFilename = "DefaultBaseRespawnTicks.csv";


    private String readResource(){
        try (InputStream in = ItemRespawnTimerConfig.class.getClassLoader().getResourceAsStream(defaultResourceFilename)) {
            if (in == null) {
                throw new IOException("Resource not found: " + defaultResourceFilename);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch(IOException e){
            return "";
        }
    }
    //endregion



    /**
     * Get the parsed records, as a map itemId->baseRespawnTicks, of the default baseRespawnTicks resource file.
     * @return The mapping defined by the records.
     */
    public Map<Integer,Integer> getDefaultBaseRespawnTicks(){
        readResource().lines()
                .forEach(this::parseCsvLine);

        return defaultBaseRespawnTicks;
    }


    /**
     * Parse a line of itemId->baseRespawnTicks data,
     *      and put the result in {@link #defaultBaseRespawnTicks}.
     * If the line doesn't parse as itemId->baseRespawnTicks data, the line will be skipped.
     * The rest of the line after a `#` character is ignored as a comment in the data.
     *
     * @param defaultBaseRespawnTicksCsvLine The single line of CSV data to parse.
     */
    @SuppressWarnings("UnnecessaryReturnStatement")
    private void parseCsvLine(String defaultBaseRespawnTicksCsvLine){
        String lineWithoutComment = defaultBaseRespawnTicksCsvLine.split("#",2)[0];

        /*
         * `lineData` is the record data:
         * [0]:itemId,[1]:baseRespawnTicks
         */
        List<String> lineData = Text.fromCSV(lineWithoutComment);

        try {

            int itemId = Integer.parseInt(lineData.get(0));
            int baseRespawnTicks = Integer.parseInt(lineData.get(1));

            defaultBaseRespawnTicks.put(itemId,baseRespawnTicks);

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
