package com.itemrespawntimer;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.HashMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.ItemDespawned;


@Slf4j
@PluginDescriptor(
	name = "Item Respawn Timer"
)
public class ItemRespawnTimerPlugin extends Plugin
{
	//region head
	@Inject
	private Client client;

	@Inject
	private ItemRespawnTimerConfig config;

	@Inject
	private OverlayManager overlayManager;


	//region objects
	@Inject
	private ItemRespawnTimerOverlay overlay;

	private Map<WorldPoint, List<StaticSpawn>> staticSpawns = new HashMap<>();

	@Inject
	private StaticSpawnService staticSpawnService;

	// WorldPoint -> RespawnTimer
	private final Map<WorldPoint, RespawnTimer> activeTimers = new HashMap<>();

	// WorldPoint -> list of static spawns at that tile
	private Map<WorldPoint, List<StaticSpawn>> staticSpawnsByTile = new HashMap<>();
	//endregion


	//region set up
	@Override
	protected void startUp() throws Exception
	{
		log.debug("Item Respawn Timer plugin started!");
		overlayManager.add(overlay);

		// Load static spawns (currently stubbed in StaticSpawnService)
		staticSpawnsByTile = staticSpawnService.loadStaticSpawns();

	}


	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Item Respawn Timer plugin  stopped!");
		overlayManager.remove(overlay);
		activeTimers.clear();
		staticSpawnsByTile.clear();
	}


	@Provides
	ItemRespawnTimerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ItemRespawnTimerConfig.class);
	}
	//endregion
	//endregion


	Map<WorldPoint, RespawnTimer> getActiveTimers()
	{
		return activeTimers;
	}


	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		/*todo filter out events that aren't the item being taken
		- going up stairs
		- walking away (teleporting away doesn't trigger it)
		*/
		Tile tile = event.getTile();
		TileItem item = event.getItem();

		if (tile == null || item == null)
		{
			return;
		}

		WorldPoint wp = tile.getWorldLocation();
		List<StaticSpawn> spawns = staticSpawnsByTile.get(wp);

		if (spawns == null || spawns.isEmpty())
		{
			return;
		}

		for (StaticSpawn spawn : spawns)
		{
			if (spawn.getItemId() == item.getId())
			{
				handleStaticItemPickup(wp, spawn);
				break;
			}
		}
	}

	//todo trigger to remove timer if we see the item


	private void handleStaticItemPickup(WorldPoint wp, StaticSpawn spawn)
	{
		//todo move this calc into RespawnTimer constructor
		long worldPopulation = 1000;//todo get population
		int respawnTicks = (int)Math.floor(spawn.getBaseRespawnTicks() * ((4000D-worldPopulation)/4000));
		int respawnSeconds = (int)(respawnTicks*0.6);
		long now = Instant.now().toEpochMilli();
		//long respawnAt = now + spawn.getRespawnSeconds() * 1000L;
		long respawnAt = now + respawnSeconds * 1000L;

		activeTimers.put(wp, new RespawnTimer(respawnAt, respawnSeconds));
	}

}
