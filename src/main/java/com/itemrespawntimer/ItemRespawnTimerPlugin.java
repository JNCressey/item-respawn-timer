package com.itemrespawntimer;

import com.google.inject.Provides;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import com.itemrespawntimer.sidepanel.WorldTimersSidePanel;
import com.itemrespawntimer.staticspawndata.StaticSpawn;
import com.itemrespawntimer.staticspawndata.StaticSpawnService;
import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.OrderedTimerCollection;
import com.itemrespawntimer.timermodel.RespawnTimer;
import com.itemrespawntimer.timermodel.WorldIdAndWorldPoint;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.task.Schedule;

import java.time.temporal.ChronoUnit;
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
import net.runelite.client.util.WorldUtil;
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

		keyManager.registerKeyListener(quickHopListener);

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
		WorldIdAndWorldPoint location = new WorldIdAndWorldPoint(worldId, wp);
		int worldPopulation = getCurrentWorldPopulation();

		RespawnTimer timer = new RespawnTimer(worldPopulation,spawn,nowMillis);
		activeTimers.put(location, timer);
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
		panel.setCurrentWorldId(client.getWorld());
		panel.updateSidePanel();
	}


	//#region hopping
	//todo: cleanup hopping code. delegate code to appropriate classes.
	@Inject
	private KeyManager keyManager;
	@Inject
	private ClientThread clientThread;

	private final HotkeyListener quickHopListener = new HotkeyListener(() -> config.hotkeyQuickHopTop())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> hop());
		}
	};


	/**
	 * Hop to the world that is at the top of the timers list (skipping timers for the current world).
	 * @see #hop(net.runelite.api.World)
	 */
	private void hop(){
		int currentWorldId = client.getWorld();

		activeTimers.getOrderedStream()
				.map(entry -> entry.getKey().getWorldId())
				.filter(worldId -> worldId!=currentWorldId)
				.findFirst()
				.ifPresent(this::hop);
	}


	/**
	 * Overload of {@link #hop(net.runelite.api.World)},
	 * but provide numeric world id.
	 * @param worldId the world to hop to
	 */
	private void hop(int worldId){
		Optional<net.runelite.api.World> world = Optional.ofNullable(getWorldFromId(worldId));

		world.ifPresent(this::hop);
	}


	/**
	 * hop to world or change world if at login screen
	 * @param world the world to hop to
	 */
	private void hop(@Nonnull net.runelite.api.World world){
		assert client.isClientThread();
		if (client.getGameState() == GameState.LOGIN_SCREEN) {
			client.changeWorld(world);
		} else {
			client.hopToWorld(world);
		}
	}


	/**
	 *
	 * @param worldId The world to get.
	 * @return The world, or null if no such world.
	 */
	@Nullable
	private net.runelite.api.World getWorldFromId(int worldId){
		return Optional.ofNullable(worldService.getWorlds())
				.map(r -> r.findWorld(worldId))
				.map(world -> { // convert to the other world type
					final net.runelite.api.World rsWorld = client.createWorld();
					rsWorld.setActivity(world.getActivity());
					rsWorld.setAddress(world.getAddress());
					rsWorld.setId(world.getId());
					rsWorld.setPlayerCount(world.getPlayers());
					rsWorld.setLocation(world.getLocation());
					rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));
					return rsWorld;
				})
				.orElse(null);
	}

	//#endregion

}
