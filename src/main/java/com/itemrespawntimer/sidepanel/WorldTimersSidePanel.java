package com.itemrespawntimer.sidepanel;

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

public class WorldTimersSidePanel extends PluginPanel {

    //protected JLabel label;
    protected JTextArea textArea;

    @Inject
    public WorldTimersSidePanel()
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

    private String getItemName(int itemId)
    {
        return itemManager.getItemComposition(itemId).getName();
    }


    public void updateSidePanel()
    {
        long nowMillis = Instant.now().toEpochMilli();

        String txt = activeTimers.getActiveTimers().stream()
                .map(timer -> {
                    String itemName = getItemName(timer.getSpawn().getItemId());
                    String countdown = timer.getTMinusCountdown(nowMillis);
                    int worldId = timer.getWorldId();
                    String currentWorldIndicator = (worldId == currentWorldId)? "*" : "";
                    /*
                        item-name [hide button]
                        [remove timer button] T-# [hop button] W#
                     */
                    return String.format("%s [-]\n[x] %s\t[h] W%s%s", itemName, countdown, worldId, currentWorldIndicator);
                })
                .collect(Collectors.joining("\n"));

        textArea.setText(txt);
    }
}
