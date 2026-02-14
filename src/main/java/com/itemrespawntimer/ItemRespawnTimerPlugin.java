package com.itemrespawntimer;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.WorldChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.task.Schedule;
import net.runelite.client.util.AsyncBufferedImage;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.NavigationButton.NavigationButtonBuilder;
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

	@Inject
	private ClientToolbar clientToolbar;

	private Boolean shuttingDown;

	private NavigationButton navButton;

	@Inject
	private ItemManager itemManager;



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
		shuttingDown = false;
		log.debug("Item Respawn Timer plugin started!");
		overlayManager.add(overlay);

		loadStaticSpawns();

		startupSidePanel();

	}

	private void startupSidePanel(){
		AsyncBufferedImage itemIcon = itemManager.getImage(245); //Wine of Zamorak

		WorldTimersSidePanel panel = injector.getInstance(WorldTimersSidePanel.class);

		NavigationButtonBuilder navButtonBuilder = NavigationButton.builder()
				.tooltip("Item Respawns")
				.panel(panel);

		itemIcon.onLoaded(()->{
			navButton = navButtonBuilder
				.icon(itemIcon)
				.build();

			clientToolbar.addNavigation(navButton);
			if (shuttingDown){ // ensure it is removed if onLoaded adds the navigation after it's supposed to be shutting down
				clientToolbar.removeNavigation(navButton);
			}
		});
	}


	@Override
	protected void shutDown() throws Exception
	{
		shuttingDown = true;
		log.debug("Item Respawn Timer plugin  stopped!");
		overlayManager.remove(overlay);
		activeTimers.clear();
		staticSpawnsByTile.clear();
		clientToolbar.removeNavigation(navButton);
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

		long nowMillis = Instant.now().toEpochMilli();
		for (StaticSpawn spawn : spawns)
		{
			if (spawn.getItemId()==item.getId() || spawn.getItemId()==-1)
			{

				handleStaticItemPickup(wp, spawn, nowMillis);
				break;
			}
		}
	}



	//todo trigger to remove timer if we see the item

	/**
	 *
	 * @param wp
	 * @param spawn
	 * @param nowMillis the result of Instant.now().toEpochMilli();
	 */
	private void handleStaticItemPickup(WorldPoint wp, StaticSpawn spawn, long nowMillis)
	{
		int worldId = client.getWorld();
		WorldIdAndWorldPoint location = new WorldIdAndWorldPoint(worldId, wp);

		//todo move this calc into RespawnTimer constructor
		int worldPopulation = getCurrentWorldPopulation();
		int respawnTicks = (int)Math.floor(spawn.getBaseRespawnTicks() * ((4000D-worldPopulation)/4000));
		int respawnSeconds = (int)(respawnTicks*0.6);
		long respawnAt = nowMillis + respawnSeconds * 1000L;

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
				return world.getPlayers();
			}
		}
		return 0;

	}


	@Schedule(
			period = 1,
			unit = ChronoUnit.SECONDS
	)
	public void updateSidePanel()
	{
		long nowMillis = Instant.now().toEpochMilli();
		int worldId = client.getWorld();
		((WorldTimersSidePanel) navButton.getPanel())
				.updateMessage(activeTimers,nowMillis,worldId);
	}


}
