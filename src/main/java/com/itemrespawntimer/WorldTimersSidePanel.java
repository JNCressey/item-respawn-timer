package com.itemrespawntimer;

import javax.swing.*;
import javax.inject.Inject;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

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


    /**
     *
     * @param activeTimers
     * @param nowMillis the result of Instant.now().toEpochMilli();
     * @param worldId
     */
    public void updateMessage(ActiveTimers activeTimers, long nowMillis, int worldId){
        Map<Integer, OrderedTimerCollection> worldTimers = activeTimers.getActiveWorldTimers();

        String txt = worldTimers.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getValue().getRespawnAt()))
                .map(entry -> {
                    long secondsRemaining = entry.getValue().getSecondsRemaining(nowMillis);
                    return String.format("%s %s", entry.getKey(), secondsRemaining);
                })
                .collect(Collectors.joining("\n"));

        textArea.setText(txt);
    }
}
