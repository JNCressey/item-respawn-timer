package com.itemrespawntimer;

import java.awt.*;
import javax.inject.Inject;

import com.itemrespawntimer.timermodel.ActiveTimers;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;

public class ItemRespawnTimerOverlay extends Overlay
{
    private final Client client;
    private final ItemRespawnTimerPlugin plugin;
    private final ItemRespawnTimerConfig config;

    @Inject
    private ActiveTimers activeTimers;


    @Inject
    public ItemRespawnTimerOverlay(Client client, ItemRespawnTimerPlugin plugin, ItemRespawnTimerConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }


    @Override
    public Dimension render(Graphics2D graphics)
    {
        int currentWorldId = client.getWorld();
        if (!config.overlayEnabled())
        {
            return null;
        }

        activeTimers.getActiveTimers().stream()
                .filter(timer -> timer.getWorldId() == currentWorldId)
                .filter(timer -> !timer.isExpired())
                .forEach(timer -> {
                    LocalPoint loc = LocalPoint.fromWorld(client, timer.getWorldPoint());
                    if (loc == null)
                    {
                        return;
                    }

                    double percent = timer.getProgress(); // 0.0 -> 1.0

                    //int tileHeight = Perspective.getTileHeight(client, loc, client.getPlane());
                    //Point point = Perspective.localToCanvas(client, loc, client.getPlane(), tileHeight);
                    Point point = Perspective.localToCanvas(client, loc, client.getPlane(),80); //todo: height offset should be 0 for on the floor and something (>80?) for on table

                    if (point == null)
                    {
                        return;
                    }

                    Color pieFillColor = Color.YELLOW;
                    Color pieBorderColor = Color.ORANGE;

                    ProgressPieComponent ppc = new ProgressPieComponent();
                    ppc.setBorderColor(pieBorderColor);
                    ppc.setFill(pieFillColor);
                    ppc.setPosition(point);
                    ppc.setProgress(percent);
                    ppc.render(graphics);
                });

        return null;
    }
}