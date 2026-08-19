package com.itemrespawntimer;

import com.google.inject.Provides;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import com.itemrespawntimer.sidepanel.WorldTimersSidePanel;
import com.itemrespawntimer.staticspawndata.StaticSpawn;
import com.itemrespawntimer.staticspawndata.StaticSpawnService;
import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.RespawnTimer;
import com.itemrespawntimer.worldhopper.WorldHopper;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.*;
import java.time.Instant;
import java.awt.image.BufferedImage;

import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.ItemDespawned;
import net.runelite.client.util.HotkeyListener;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.ImageUtil;


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

	private WorldTimersSidePanel panel;
	private NavigationButton navButton;


	//region objects
	@Inject
	private ItemRespawnTimerOverlay overlay;

	@Inject
	private StaticSpawnService staticSpawnService;

	@Inject
	private ActiveTimers activeTimers;

	@Inject
	private WorldHopper worldHopper;

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

		startupSidePanel();

		keyManager.registerKeyListener(removeExpiredSingleListener);
		keyManager.registerKeyListener(removeExpiredAllListener);
		keyManager.registerKeyListener(clearTimersListener);

		worldHopper.registerKeyListeners();
	}

	private void startupSidePanel(){
		panel = injector.getInstance(WorldTimersSidePanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(ItemRespawnTimerPlugin.class, "/icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Item Respawns")
			.panel(panel)
			.icon(icon)
			.build();

		clientToolbar.addNavigation(navButton);
	}


	@Override
	protected void shutDown() throws Exception
	{
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



	//todo: handle observations to submit to timers: entering the area, leaving the area, seeing the item has returned.

	/**
	 *
	 * @param wp
	 * @param spawn
	 * @param nowMillis the result of Instant.now().toEpochMilli();
	 */
	private void handleStaticItemPickup(WorldPoint wp, StaticSpawn spawn, long nowMillis)
	{
		int worldId = client.getWorld();
		int worldPopulation = getCurrentWorldPopulation();

		RespawnTimer timer = new RespawnTimer(spawn,worldId,wp,worldPopulation,nowMillis);
		activeTimers.add(timer);
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


	@Subscribe
	public void onGameTick(GameTick event) {
		activeTimers.removeTick();
		panel.setCurrentWorldId(client.getWorld());
		panel.updateSidePanel();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("joined world: {}", client.getWorld());
			// This runs when the player has joined a world
			worldHopper.joinedWorld();
		}
	}


	@Inject
	private KeyManager keyManager;
	@Inject
	private ClientThread clientThread;

	private final HotkeyListener removeExpiredSingleListener = new HotkeyListener(() -> config.hotkeyRemoveExpiredSingle())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> activeTimers.removeExpiredSingle());
		}
	};

	private final HotkeyListener removeExpiredAllListener = new HotkeyListener(() -> config.hotkeyRemoveExpiredAll())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> activeTimers.removeExpiredAll());
		}
	};

	private final HotkeyListener clearTimersListener = new HotkeyListener(() -> config.hotkeyClearTimers())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> activeTimers.clear());
		}
	};

}
