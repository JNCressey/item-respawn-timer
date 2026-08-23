package com.itemrespawntimer.debug.spawndataverification;

import com.itemrespawntimer.staticspawnservice.StaticSpawn;
import com.itemrespawntimer.staticspawnservice.StaticSpawnService;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.Text;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Singleton
public class SpawnDataVerificationReader {


    @Inject
    private StaticSpawnService staticSpawnService;


    /**
     * Parse the lines of verification data as {@link #parseObservationFromCsvLine} into SpawnDataVerificationObservation data to add.
     * Lines that don't parse as verification data are filtered from the stream.
     * Unparsed lines and comment parts are retained by adding them to the provided string builder.
     * @param verificationObservationsCsvText The CSV text to parse
     * @param comments The string builder to add comments to.
     * @return The parsed data.
     */
    public Stream<SpawnDataVerificationObservation> parseObservationsFromCsvText(String verificationObservationsCsvText, StringBuilder comments){
        return verificationObservationsCsvText.lines()
                .map(line -> parseObservationFromCsvLine(line,comments))
                .filter(Objects::nonNull);
    }


    /**
     * Parse a line of verification data into a SpawnDataVerificationObservation to add.
     * If the line doesn't parse as verification data, the return value is null.
     * The rest of the line after a `#` character is ignored as a comment in the data.
     * Unparsed lines and comment parts are retained by adding them to the provided string builder.
     * @param verificationObservationCsvLine The single line of CSV data to parse.
     * @param comments The string builder to add comments to.
     * @return The parsed data.
     */
    @Nullable
    public  SpawnDataVerificationObservation parseObservationFromCsvLine(String verificationObservationCsvLine, StringBuilder comments){

        String[] splitCommentLineParts = verificationObservationCsvLine.split("#", 2);

        String lineWithoutComment = splitCommentLineParts[0];

        /*
         * `lineData` is the record data:
         * [0]:x,[1]:y,[2]:plane
         * [3]:itemId,[4]:quantity
         * [5]:verificationStatus
         */
        List<String> lineData = Text.fromCSV(lineWithoutComment);

        try {
            SpawnDataVerificationObservation observation = new SpawnDataVerificationObservation();

            WorldPoint wp = new WorldPoint(
                    Integer.parseInt(lineData.get(0)),
                    Integer.parseInt(lineData.get(1)),
                    Integer.parseInt(lineData.get(2)));

            observation.setWorldpoint(wp);

            { // set spawn
                int itemId = Integer.parseInt(lineData.get(3));
                int quantity = Integer.parseInt(lineData.get(4));

                Optional<StaticSpawn> optionalSpawn = staticSpawnService.getTrackedSpawn(wp);

                boolean matchesSpawnData = optionalSpawn
                        .map(spawn ->
                                spawn.getItemId() == itemId
                                && spawn.getQuantity() == quantity)
                        .orElse(false);


                if (matchesSpawnData){
                    observation.setSpawn(optionalSpawn.get());
                } else {
                    // reject this observation
                    addComment(comments, verificationObservationCsvLine); // add rejected line to string builder
                    return null;
                }
            }

            observation.setStatus(SpawnDataVerificationStatus.valueOf(lineData.get(5).trim()));

            if (splitCommentLineParts.length > 1) {
                addComment(comments, splitCommentLineParts[1]);  // add comment to string builder
            }
            return  observation;
        }
        catch (
                IndexOutOfBoundsException // skip this line if line doesn't have enough data cells
                | IllegalArgumentException // skip this line if cell data doesn't parse
                        e
        ) {
            addComment(comments, verificationObservationCsvLine); // add unparsed line to string builder
            return null;
        }
    }


    /**
     * Add a line to comments with a '#' prefix.
     * @param comments The string builder to add comments to.
     * @param text The text to add as a comment.
     */
    private void addComment(StringBuilder comments, String text){
        comments.append("#").append(text).append("\n");
    }

}
