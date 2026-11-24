package com.itemrespawntimer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.RespawnTimer;
import lombok.Setter;
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


    //todo make colors configurable
    private Color pieFillColor;
    private final Color pieFillColorWithoutNumber = Color.YELLOW;
    private final Color pieFillColorWithNumber = new Color(120, 120, 0);
    private final Color pieBorderColor = Color.ORANGE;

    @Inject
    private ActiveTimers activeTimers;

    /**
     * The time to base tick time rounding on.
     */
    @Setter
    private long tickBaseMillis;


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
        boolean overlayPieEnabled = config.overlayPieEnabled();
        boolean overlayNumberEnabled = config.overlayNumberEnabled();

        if (!overlayPieEnabled && !overlayNumberEnabled)
        {
            return null;
        }

        pieFillColor = overlayNumberEnabled ? pieFillColorWithNumber : pieFillColorWithoutNumber;

        activeTimers.getActiveTimers().stream()
                .filter(timer -> timer.getWorldId() == currentWorldId)
                .filter(timer -> !timer.isExpired())
                .forEach(timer -> {
                    LocalPoint loc = LocalPoint.fromWorld(client, timer.getWorldPoint());
                    if (loc == null)
                    {
                        return;
                    }

                    int plane = client.getTopLevelWorldView().getPlane();
                    int heightOffset = itemHeightOffset.getOrDefault(timer.getWorldPoint(), 0);

                    Point point = Perspective.localToCanvas(client, loc, plane, heightOffset);
                    if (point == null)
                    {
                        return;
                    }

                    if (overlayPieEnabled) {
                        renderPieTimer(graphics,point,timer);
                    }

                    if (overlayNumberEnabled) {
                        renderNumberTimer(graphics,point,timer);
                    }
                });

        return null;
    }

    /**
     * Add a single pie timer to the graphics.
     * @param graphics The graphics to add the timer to.
     * @param point The local canvas point to draw at.
     * @param timer The timer state.
     */
    private void renderPieTimer(Graphics2D graphics, Point point, RespawnTimer timer){
        double percent = timer.getProgress(); // 0.0 -> 1.0

        ProgressPieComponent ppc = new ProgressPieComponent();
        ppc.setBorderColor(pieBorderColor);
        ppc.setFill(pieFillColor);
        ppc.setPosition(point);
        ppc.setProgress(percent);
        ppc.render(graphics);
    }

    /**
     * Add a single number timer to the graphics.
     * @param graphics The graphics to add the timer to.
     * @param point The local canvas point to draw at.
     * @param timer The timer state.
     */
    private void renderNumberTimer(Graphics2D graphics, Point point, RespawnTimer timer){

        // Text of time remaining
        String text;// = String.valueOf(timer.getSecondsRemaining()); //todo config to choose to use getTicksRemaining
        switch(config.overlayNumberMode()){
            case SECONDS:
                text = String.valueOf(timer.getSecondsRemaining());
                break;
            case TICKS:
            default:
                text = String.valueOf(timer.getTicksRemaining(tickBaseMillis));
                break;
        }


        // point has the center where the timer is. offset by half the text size, to get the origin to draw the text
        FontMetrics fm = graphics.getFontMetrics();
        int tx = point.getX() - (fm.stringWidth(text) / 2);
        int ty = point.getY() + (fm.getAscent() / 2);

        graphics.setColor(Color.WHITE);
        graphics.drawString(text, tx, ty);
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