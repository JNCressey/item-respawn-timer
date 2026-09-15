package com.itemrespawntimer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import com.itemrespawntimer.timermodel.ActiveTimers;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;

public class ItemRespawnTimerOverlay extends Overlay
{
    private final Client client;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
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

                    int plane = client.getTopLevelWorldView().getPlane();
                    int heightOffset = itemHeightOffset.getOrDefault(timer.getWorldPoint(), 0);

                    Point point = Perspective.localToCanvas(client, loc, plane, heightOffset);
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

    /**
     * The height offset for the item layer at a given location, for the timer to be displayed on top of tables.
     * Left unset for 0s, use getOrDefault(wp, 0).
     */
    private final Map<WorldPoint, Integer> itemHeightOffset = new HashMap<>();

    /**
     * Add the height data, to ensure {@link #itemHeightOffset} has the height data for any item spawn seen.
     * @param event the item spawned
     */
    public void onItemSpawned(ItemSpawned event){
        Tile tile = event.getTile();
        TileItem item = event.getItem();
        if (tile == null || item == null)
        {
            return;
        }

        if (item.getOwnership() != TileItem.OWNERSHIP_NONE){
            return; // only react to items that were naturally spawned
        }

        ItemLayer itemLayer = tile.getItemLayer();
        if (itemLayer == null)
        {
            return;
        }

        int heightOffset = itemLayer.getHeight();
        if (heightOffset == 0)
        {
            return; // don't record 0s. Can assume 0 from absent. And to prevent ashes from burned out fires bloating the size of the map.
        }

        WorldPoint wp = tile.getWorldLocation();

        itemHeightOffset.putIfAbsent(wp, heightOffset);
    }
}