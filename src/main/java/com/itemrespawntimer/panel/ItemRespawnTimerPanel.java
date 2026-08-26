package com.itemrespawntimer.panel;

import javax.swing.*;
import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.RespawnTimer;
import lombok.Setter;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

public class ItemRespawnTimerPanel extends PluginPanel {

    //protected JLabel label;
    protected JTextArea textArea;

    @Inject
    public ItemRespawnTimerPanel()
    {
        /*
        todo: make tiles with buttons for each timer
         */
        setLayout(new BorderLayout());
        textArea = new JTextArea("Hello RuneLite!");
        add(textArea, BorderLayout.NORTH);

    }



    @Inject
    private ActiveTimers activeTimers;

    @Setter
    private int currentWorldId;

    @Inject
    private ItemManager itemManager;

    private String getItemName(RespawnTimer timer)
    {
        return itemManager.getItemComposition(timer.getSpawn().getItemId()).getName();
    }


    private static final DateTimeFormatter formatterMinutesAndSeconds = DateTimeFormatter.ofPattern("mm:ss");

    public void updateSidePanel()
    {
        long nowMillis = Instant.now().toEpochMilli();

        String txt = activeTimers.getActiveTimers().stream()
                .map(timer -> {
                    String itemName = getItemName(timer);
                    String countdown = timer.getTMinusCountdown(nowMillis);
                    int worldId = timer.getWorldId();
                    String worldIndicator = (worldId == currentWorldId)
                            ? "        "
                            : String.format(" | *W%s",worldId);
                    String doneAtMinutesAndSeconds = Instant.ofEpochMilli(timer.getRespawnAt()).atZone(ZoneId.of("UTC")).format(formatterMinutesAndSeconds);
                    String donePart = (timer.isExpired())
                            ? "Done         "
                            : String.format("Done at %s", doneAtMinutesAndSeconds);
                    /*
                        ┌pic┐ item-name | W#          ┌toggle show/hide button┐ ┌add hintarrow┐ ┌remove timer button┐
                        └   ┘ Done at <T>             └ (only show in mode)   ┘ └             ┘ └                   ┘
                        [******progress bar                                                                         ]
                     */
                    return String.format("┌pic┐ %s%s\t┌h┐┌↓┐┌x┐\n└pic┘ %s\t└h┘└↓┘└x┘\n[*******progress bar %s\t]\n",
                            itemName, worldIndicator, donePart, countdown);

                    /*
                        item-name [hide button] [remove timer button]
                        T-# W#
                     */
                    //return String.format("%s [-]\n[x] %s\t[h] W%s%s", itemName, countdown, worldId, currentWorldIndicator);
                })
                .collect(Collectors.joining("\n"));

        textArea.setText(txt);
    }
}
