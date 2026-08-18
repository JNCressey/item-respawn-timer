package com.itemrespawntimer;

import java.awt.*;
import java.time.Instant;
import java.util.Map;
import javax.inject.Inject;

import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.RespawnTimer;
import com.itemrespawntimer.timermodel.WorldIdAndWorldPoint;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

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
    public Dimension render(Graphics2D g)
    {
        int currentWorldId = client.getWorld();
        if (!config.overlayEnabled())
        {
            return null;
        }

        long now = Instant.now().toEpochMilli();

        for (Map.Entry<WorldIdAndWorldPoint, RespawnTimer> entry : activeTimers.getActiveTimers().entrySet())
        {
            RespawnTimer timer = entry.getValue();

            if (timer.getWorldId() != currentWorldId){
                continue; // skip rendering this timer because it's for another world
            }

            if (timer.getRespawnAt() <= now)
            {
                continue;// skip rendering expired timers
            }

            LocalPoint lp = LocalPoint.fromWorld(client, timer.getWorldPoint());
            if (lp == null)
            {
                continue;
            }

            Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, 1);
            if (poly == null)
            {
                continue;
            }

            double progress = timer.getProgress(now); // 0.0 -> 1.0
            drawCircularTimer(g, poly, progress, timer.getSecondsRemaining(now));
        }

        return null;
    }


    private void drawCircularTimer(Graphics2D g, Polygon poly, double progress, int secondsRemaining)
    {
        Rectangle bounds = poly.getBounds();
        int size = Math.min(bounds.width, bounds.height);

        int x = bounds.x;
        int y = bounds.y;

        // Background circle
        g.setColor(new Color(0, 0, 0, 120));
        g.fillOval(x, y, size, size);

        // Progress arc (yellow)
        g.setColor(new Color(255, 255, 0, 180));
        g.fillArc(x, y, size, size, 90, (int) -(360 * progress));

        // Border
        g.setColor(Color.BLACK);
        g.drawOval(x, y, size, size);

        // Text (seconds)
        String text = String.valueOf(secondsRemaining);
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (size - fm.stringWidth(text)) / 2;
        int ty = y + (size + fm.getAscent()) / 2 - 2;

        g.setColor(Color.WHITE);
        g.drawString(text, tx, ty);
    }
}