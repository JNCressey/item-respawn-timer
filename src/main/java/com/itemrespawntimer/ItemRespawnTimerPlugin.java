package com.itemrespawntimer;

import com.google.inject.Provides;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import java.util.*;
import java.time.Instant;

import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.NavigationButton.NavigationButtonBuilder;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.ItemDespawned;
import net.runelite.client.util.HotkeyListener;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.WorldUtil;


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

		keyManager.registerKeyListener(quickHopListener);

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
		timer.addPropertyChangeListener(evt ->{
			if("isFinished".equals(evt.getPropertyName()) && evt.getNewValue().equals(Boolean.TRUE)){
				int thenWorldId = client.getWorld();
				long thenMillis = Instant.now().toEpochMilli();
				activeTimers.removeWorldIfPast(thenWorldId, thenMillis); //todo: change process of removing timers based on the wider set of observations being supported
				System.out.print("timer completed ItemRespawnPlugin::handleStaticItemPickup");
			}
		});
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
	public void onWorldChanged(WorldChanged event){
		int worldId = client.getWorld();
		long nowMillis = Instant.now().toEpochMilli();
		activeTimers.removeWorldIfPast(worldId, nowMillis);
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


	//#region hopping
	//todo: cleanup hopping code. delegate code to appropriate classes.
	@Inject
	private KeyManager keyManager;
	@Inject
	private ClientThread clientThread;

	private final HotkeyListener quickHopListener = new HotkeyListener(() -> config.quickHopHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> hop());
		}
	};


	/**
	 * Hop to the world that is at the top of the world timers list.
	 * @see #hop(net.runelite.api.World)
	 */
	private void hop(){
		Map<Integer, OrderedTimerCollection> worldTimers = activeTimers.getActiveWorldTimers();

		int currentWorldId = client.getWorld();

		Optional<Integer> worldId = worldTimers.entrySet().stream()
				.filter(entry->entry.getKey()!=currentWorldId)
				.min(Comparator.comparingLong(entry -> entry.getValue().getRespawnAt()))
				.map(Map.Entry::getKey); //todo: remove code duplication. selecting the top world here and sorting the worlds for the side panel

		worldId.ifPresent(this::hop);
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
