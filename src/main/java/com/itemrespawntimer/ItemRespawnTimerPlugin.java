package com.itemrespawntimer;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.HashMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.ItemDespawned;
import net.runelite.http.api.worlds.WorldResult;


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

	@Inject
	private WorldService worldService;



	//region objects
	@Inject
	private ItemRespawnTimerOverlay overlay;

	@Inject
	private StaticSpawnService staticSpawnService;

	@Inject
	private ActiveTimers activeTimers;


	// WorldPoint -> list of static spawns at that tile
	private Map<WorldPoint, List<StaticSpawn>> staticSpawnsByTile = new HashMap<>();
	//endregion


	//region set up
	@Override
	protected void startUp() throws Exception
	{
		log.debug("Item Respawn Timer plugin started!");
		overlayManager.add(overlay);


		loadStaticSpawns();

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



	Map<WorldIdAndWorldPoint, RespawnTimer> getActiveTimers()
	{
		return activeTimers.getActiveTimers();
	}


	//#region reloading spawn data
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		String configKey = event.getKey();

		System.out.println("Config changed: " + configKey);

		// Example: trigger specific logic when a certain key changes
		if (configKey.equals("trackedSpawns"))
		{
			loadStaticSpawns();
		}
	}

	private void loadStaticSpawns(){
		staticSpawnsByTile = staticSpawnService.loadStaticSpawns();
	}


	//#endregion



	//#region react to items in game
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
			if (spawn.getItemId()==item.getId() || spawn.getItemId()==-1)
			{
				handleStaticItemPickup(wp, spawn);
				break;
			}
		}
	}



	//todo trigger to remove timer if we see the item


	private void handleStaticItemPickup(WorldPoint wp, StaticSpawn spawn)
	{
		int worldId = client.getWorld();
		WorldIdAndWorldPoint location = new WorldIdAndWorldPoint(worldId, wp);

		//todo move this calc into RespawnTimer constructor
		int worldPopulation = getCurrentWorldPopulation();
		int respawnTicks = (int)Math.floor(spawn.getBaseRespawnTicks() * ((4000D-worldPopulation)/4000));
		int respawnSeconds = (int)(respawnTicks*0.6);
		long now = Instant.now().toEpochMilli();
		//long respawnAt = now + spawn.getRespawnSeconds() * 1000L;
		long respawnAt = now + respawnSeconds * 1000L;

		activeTimers.put(location, new RespawnTimer(respawnAt, respawnSeconds));
	}
	//#endregion

	private int getCurrentWorldPopulation(){
		int currentWorldId = client.getWorld();
		WorldResult worlds = worldService.getWorlds();

		if (worlds != null)
		{
			net.runelite.http.api.worlds.World world = worlds.findWorld(currentWorldId);
			if (world != null)
			{
				int population = world.getPlayers();
				return population;
			}
		}
		return 0;

	}

}
