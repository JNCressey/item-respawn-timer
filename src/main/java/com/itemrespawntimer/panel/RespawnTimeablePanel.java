package com.itemrespawntimer.panel;

import com.itemrespawntimer.net.runelite.client.plugins.timetracking.TimeablePanel;
import com.itemrespawntimer.timermodel.RespawnTimer;
import net.runelite.client.game.ItemManager;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


class RespawnTimeablePanel
extends TimeablePanel<RespawnTimer>
{
 
  String estimateAtTimePart;
  String estimateWorldPart;
  boolean isStaleColorsSet = false;

  RespawnTimeablePanel(RespawnTimer timer, ItemManager itemManager)
  {
    super(
            timer,
            itemManager.getItemComposition(timer.getSpawn().getItemId()).getName(), // item name
            timer.getTotalMillis()
    );


    itemManager.getImage(timer.getSpawn().getItemId()).addTo(getIcon());

    String itemName = getText().getText();
    getIcon().setToolTipText(itemName);

    getProgress().setVisible(true);
    remove(getNotifyButton());
    getProgress().setForeground(Color.GREEN);
    getProgress().setBackground(Color.DARK_GRAY);

    prepareEstimateParts();
    update(timer.getWorldId(),timer.getStart());
    
  }

  public void update(int currentWorldId, long nowMillis)
  {
    updateEstimateMessage(currentWorldId);
    updateProgress(nowMillis);
  }

  public void updateEstimateMessage(int currentWorldId)
  {
    RespawnTimer timer = getTimeable();
    String atTimePart = (timer.isExpired()) ? "" : estimateAtTimePart;
    String worldPart = (timer.getWorldId() == currentWorldId) ? "" : estimateWorldPart;

    getEstimate().setText(String.format("Done%s%s", atTimePart, worldPart));
  }


  private static final DateTimeFormatter formatterMinutesAndSeconds = DateTimeFormatter.ofPattern("mm:ss");

  private void prepareEstimateParts()
  {
    RespawnTimer timer = getTimeable();

    estimateWorldPart = String.format(" in W%s", timer.getWorldId());
   
    String doneAtMinutesAndSeconds = Instant.ofEpochMilli(timer.getRespawnAt()).atZone(ZoneId.of("UTC")).format(formatterMinutesAndSeconds);
    estimateAtTimePart = String.format(" at %s", doneAtMinutesAndSeconds);
  }


  public void updateProgress(long nowMillis)
  {
    RespawnTimer timer = getTimeable();
    int progress;
    if (timer.isExpired()){
      if (!isStaleColorsSet) { setStaleColors(); }
      progress = (int) (nowMillis - timer.getRespawnAt());
    } 
    else
    {
      progress = (int) (nowMillis - timer.getStart());
    }

    getProgress().setValue(progress);
  }

  private void setStaleColors()
  {
    isStaleColorsSet = true;
    getProgress().setForeground(Color.DARK_GRAY);
    getProgress().setBackground(Color.GREEN);
  }
}
