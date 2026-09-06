package com.itemrespawntimer.panel;

import javax.swing.*;
import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.RespawnTimer;
import lombok.Setter;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

import net.runelite.client.plugins.timetracking.TimeablePanel;

public class ItemRespawnTimerPanel extends PluginPanel {

    private final java.util.List<TimeablePanel<RespawnTimer>> spawnPanels;

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

        setLayout(new DynamicGridLayout(0, 1, 0, 0));
        spawnPanels = new ArrayList<>();
        shownTimers = new HashSet<>();

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

        //textArea.setText(txt); //todo remove text mode

        //add timer panels
        for (RespawnTimer t : activeTimers.getActiveTimers()){
            if (shownTimers.contains(t)){ continue; }

            String itemName = getItemName(t);
            TimeablePanel<RespawnTimer> panel = new TimeablePanel<>(t,itemName,(int) (t.getRespawnAt()-t.getStart()));

            add(panel);
            spawnPanels.add(panel);
            shownTimers.add(t);
            //todo add in sorted position


            itemManager.getImage(t.getSpawn().getItemId()).addTo(panel.getIcon());
            panel.getIcon().setToolTipText(itemName);
            panel.getProgress().setVisible(true);
            panel.getNotifyButton().setSelected(false); //todo implement notifier
            panel.getProgress().setForeground(Color.GREEN);
            panel.getProgress().setBackground(Color.DARK_GRAY);

        }

        //update progress
        for (TimeablePanel<RespawnTimer> panel : spawnPanels){
            RespawnTimer timer = panel.getTimeable();

            int worldId = timer.getWorldId();
            String estimateWorldPart = (worldId == currentWorldId)
                    ? ""
                    : String.format(" in W%s",worldId);

            if (timer.isExpired()){
                panel.getEstimate().setText(String.format("Done%s", estimateWorldPart));
                panel.getProgress().setForeground(Color.DARK_GRAY);
                panel.getProgress().setBackground(Color.GREEN);

                panel.getProgress().setValue((int) (nowMillis - timer.getRespawnAt()));
            } else {

                String doneAtMinutesAndSeconds = Instant.ofEpochMilli(timer.getRespawnAt()).atZone(ZoneId.of("UTC")).format(formatterMinutesAndSeconds);
                panel.getEstimate().setText(String.format("Done at %s%s", doneAtMinutesAndSeconds, estimateWorldPart));
                panel.getProgress().setValue((int) (nowMillis - timer.getStart()));
            }

            if (timer.isDeleted()){
                spawnPanels.remove(panel);
                shownTimers.remove(timer);
                remove(panel);
                revalidate();
            }
        }


    }
}
