package com.itemrespawntimer;

import javax.swing.*;
import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
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
        //label = new JLabel("Hello RuneLite!");
        //add(label, BorderLayout.NORTH);
        textArea = new JTextArea("Hello RuneLite!");
        add(textArea, BorderLayout.NORTH);

    }

    public void updateMessage(ActiveTimers activeTimers){
        long now = Instant.now().toEpochMilli();
        Map<Integer, RespawnTimer> worldTimers = activeTimers.getActiveWorldTimers();
        String txt = worldTimers.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue().getRespawnAt()))
                .map(entry-> {
                    long timeDue = entry.getValue().getSecondsRemaining(now);
                    return String.format("%s %s", entry.getKey(),timeDue);
                })
                .collect(Collectors.joining("\r\n"));
        //label.setText(txt);
        textArea.setText(txt);
        //label.setText(activeTimers.getActiveWorldTimers().toString());
    }
}
