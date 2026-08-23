package com.itemrespawntimer.panel;

import javax.swing.*;
import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
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


    public void updateSidePanel()
    {
        long nowMillis = Instant.now().toEpochMilli();

        String txt = activeTimers.getActiveTimers().stream()
                .map(timer -> {
                    String itemName = getItemName(timer);
                    String countdown = timer.getTMinusCountdown(nowMillis);
                    int worldId = timer.getWorldId();
                    String currentWorldIndicator = (worldId == currentWorldId)? "*" : "";
                    /*
                        ┌pic┐ item-name | W#          ┌toggle show/hide button┐ ┌add hintarrow┐ ┌remove timer button┐
                        └   ┘ Done at <T>             └ (only show in mode)   ┘ └             ┘ └                   ┘
                        [******progress bar                                                                         ]
                     */
                    return String.format("┌pic┐ %s | W%s%s\t┌h┐┌↓┐┌x┐\n└pic┘ Done at <T>\t└h┘└↓┘└x┘\n[*******progress bar %s\t]\n", itemName, worldId, currentWorldIndicator,countdown);

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
