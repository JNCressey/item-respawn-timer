package com.itemrespawntimer.sidepanel;

import javax.swing.*;
import javax.inject.Inject;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.RespawnTimer;
import com.itemrespawntimer.timermodel.WorldIdAndWorldPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

public class WorldTimersSidePanel extends PluginPanel {

    //protected JLabel label;
    protected JTextArea textArea;

    @Inject
    public WorldTimersSidePanel()
    {
        /*
        todo: make list of worlds with timers
         */
        setLayout(new BorderLayout());
        textArea = new JTextArea("Hello RuneLite!");
        add(textArea, BorderLayout.NORTH);

    }


    @Inject
    private ItemManager itemManager;

    public String getItemName(int itemId)
    {
        return itemManager.getItemComposition(itemId).getName();
    }


    /**
     *
     * @param activeTimers
     * @param nowMillis the result of Instant.now().toEpochMilli();
     * @param currentWorldId
     */
    public void updateMessage(ActiveTimers activeTimers, long nowMillis, int currentWorldId){
        Map<WorldIdAndWorldPoint, RespawnTimer> timers = activeTimers.getActiveTimers();

        String txt = timers.entrySet().stream()
                .sorted(Comparator
                        .comparingLong((Map.Entry<WorldIdAndWorldPoint, RespawnTimer> entry) -> entry.getValue().getRespawnAt())
                        .thenComparingInt( entry -> entry.getKey().getWorldId())
                )
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
