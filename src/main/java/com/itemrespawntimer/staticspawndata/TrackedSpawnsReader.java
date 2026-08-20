package com.itemrespawntimer.staticspawndata;

import com.itemrespawntimer.ItemRespawnTimerConfig;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.Text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TrackedSpawnsReader {

    //region readResource
    private static final String defaultResourceFilename = "TrackedSpawnsDefault.csv";


    private static String readResource(){
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
     * Get the parsed records, as from {@link #parseTrackedSpawnsFromCsvText(String)}, of the tracked spawns default resource file.
     * @return The record stream.
     */
    public static Stream<TrackedSpawnsParsedRecord> getDefaultTrackedSpawns(){
        String defaultTrackedSpawnsCsvText = TrackedSpawnsReader.readResource();
        return parseTrackedSpawnsFromCsvText(defaultTrackedSpawnsCsvText);
    }


    //region parseTrackedSpawnsFromCsvText
    /**
     * Parse the lines of tracked spawn data as {@link #parseTrackedSpawnFromCsvLine} into StaticSpawn data to add.
     * Lines that don't parse as spawn data are filtered from the stream.
     * @param spawnDataCsvText The CSV text to parse
     * @return The parsed data.
     */
    public static Stream<TrackedSpawnsParsedRecord> parseTrackedSpawnsFromCsvText(String spawnDataCsvText){
        return spawnDataCsvText.lines()
                .map(TrackedSpawnsReader::parseTrackedSpawnFromCsvLine)
                .filter(Objects::nonNull);
    }


    /**
     * Parse a line of static spawn data into a StaticSpawn to add.
     * If the line doesn't parse as spawn data, the return value is null.
     * The rest of the line after a `#` character is ignored as a comment in the data.
     * @param spawnDataCsvLine The single line of CSV data to parse.
     * @return The parsed data.
     */
    private static TrackedSpawnsParsedRecord parseTrackedSpawnFromCsvLine(String spawnDataCsvLine){
        String lineWithoutComment = spawnDataCsvLine.split("#",2)[0];
        if (lineWithoutComment.isEmpty()){
            return null; //skip empty lines
        }

        WorldPoint wp;
        StaticSpawn.StaticSpawnBuilder s = StaticSpawn.builder();

        try { // try build `s`
            /*
             * `lineData` is the record data:
             * [0]:x,[1]:y,[2]:plane
             * for positions to be tracked: [3]:baseRespawnTicks,[4]:itemId,[5]:quantity
             * for positions not to be tracked: [3]:"null"
             */
            List<String> lineData = Text.fromCSV(lineWithoutComment);

            wp = new WorldPoint(
                    Integer.parseInt(lineData.get(0)),
                    Integer.parseInt(lineData.get(1)),
                    Integer.parseInt(lineData.get(2)));

            if (lineData.get(3).equals("null")){
                return new TrackedSpawnsParsedRecord(wp,null); // entry indicates to not track this location
            }

            // set worldPoint
            s.worldPoint(wp);

            // set baseRespawnTicks
            s.baseRespawnTicks( Integer.parseInt(lineData.get(3)) );

            // set itemId
            s.itemId(           Integer.parseInt(lineData.get(4)) );

            // set quantity
            s.quantity(         Integer.parseInt(lineData.get(5)) );
        }
        catch (
                IndexOutOfBoundsException // skip this line if line doesn't have enough data cells
                | NumberFormatException // skip this line if cell data doesn't parse
                        e
        ) {
            return null;
        }

        return new TrackedSpawnsParsedRecord(wp,s.build());
    }
    //endregion

}
