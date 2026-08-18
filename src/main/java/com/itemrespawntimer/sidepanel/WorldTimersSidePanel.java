package com.itemrespawntimer.sidepanel;

import javax.swing.*;
import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.RespawnTimer;
import com.itemrespawntimer.timermodel.WorldIdAndWorldPoint;
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
        Map<WorldIdAndWorldPoint, RespawnTimer> timers = activeTimers.getActiveTimers();

        String txt = activeTimers.getOrderedStream()
                .map(entry-> {
                    RespawnTimer t = entry.getValue();
                    String itemName = getItemName(t.getSpawn().getItemId());
                    String countdown = t.getTMinusCountdown(nowMillis);
                    int worldId = entry.getKey().getWorldId();
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
