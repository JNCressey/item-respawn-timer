package com.itemrespawntimer;

import javax.swing.*;
import javax.inject.Inject;
import java.awt.*;
import net.runelite.client.ui.PluginPanel;

public class WorldTimersSidePanel extends PluginPanel {
    @Inject
    public WorldTimersSidePanel()
    {
        /*
        todo: make list of worlds with timers
         */
        setLayout(new BorderLayout());
        add(new JLabel("Hello RuneLite!"), BorderLayout.NORTH);
    }
}
