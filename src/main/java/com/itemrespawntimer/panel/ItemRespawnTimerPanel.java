package com.itemrespawntimer.panel;

import javax.swing.*;
import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.RespawnTimer;
import lombok.Setter;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;


public class ItemRespawnTimerPanel extends PluginPanel {

    private final java.util.List<ItemRespawnTimeablePanel> spawnPanels;

    private final Set<RespawnTimer> shownTimers;

    //protected JLabel label;
    protected JTextArea textArea;

    @Inject
    public ItemRespawnTimerPanel()
    {
        /*
        todo: make tiles with buttons for each timer
         */
        //setLayout(new BorderLayout());
        textArea = new JTextArea("Hello RuneLite!");
        //add(textArea, BorderLayout.NORTH);// todo remove text mode

        spawnPanels = new ArrayList<>();
        shownTimers = new HashSet<>();

    }



    @Inject
    private ActiveTimers activeTimers;

    @Setter
    private int currentWorldId;

    @Inject
    private ItemManager itemManager;


    public void updateSidePanel()
    {
        long nowMillis = Instant.now().toEpochMilli();

        //remove deleted timers
        for  (ItemRespawnTimeablePanel panel : spawnPanels) {
            RespawnTimer timer = panel.getTimeable();
            if (timer.isDeleted()){
                spawnPanels.remove(panel);
                shownTimers.remove(timer);
                remove(panel);
                revalidate();
            }
        }

        //add new timer panels
        for(int i=0; i<activeTimers.getActiveTimers().size(); ++i){
            RespawnTimer t = activeTimers.getActiveTimers().get(i);

            if (shownTimers.contains(t)){ continue; }

            ItemRespawnTimeablePanel panel = new ItemRespawnTimeablePanel(t, itemManager);

            add(panel,i);
            spawnPanels.add(panel);
            shownTimers.add(t);
        }

        //update progress
        for (ItemRespawnTimeablePanel panel : spawnPanels){
            panel.update(currentWorldId, nowMillis);

        }
    }
}
