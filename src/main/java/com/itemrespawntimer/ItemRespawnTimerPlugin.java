package com.itemrespawntimer;

import com.google.inject.Provides;

import javax.inject.Inject;

import com.itemrespawntimer.debugspawndiscovery.DebugSpawnDiscoveryService;
import com.itemrespawntimer.panel.ItemRespawnTimerPanel;
import com.itemrespawntimer.staticspawnservice.StaticSpawnService;
import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.worldhopper.WorldHopper;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.awt.image.BufferedImage;

import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.ItemDespawned;
import net.runelite.client.util.HotkeyListener;
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
	private ClientToolbar clientToolbar;


	@Inject
	private ItemRespawnTimerOverlay overlay;


	@Inject
	private StaticSpawnService staticSpawnService;


	@Inject
	private ActiveTimers activeTimers;


	@Inject
	private WorldHopper worldHopper;

	@Inject
	private DebugSpawnDiscoveryService debugSpawnDiscoveryService;
	//endregion


	private ItemRespawnTimerPanel panel;
	private NavigationButton navButton;


	//region set up
    @SuppressWarnings("RedundantThrows")
    @Override
	protected void startUp() throws Exception
	{
		log.debug("Item Respawn Timer plugin started!");
		overlayManager.add(overlay);

		staticSpawnService.startUp();

		startupSidePanel();

		keyManager.registerKeyListener(removeExpiredSingleListener);
		keyManager.registerKeyListener(removeExpiredAllListener);
		keyManager.registerKeyListener(clearTimersListener);

		worldHopper.startUp();
	}

	private void startupSidePanel(){
		panel = injector.getInstance(ItemRespawnTimerPanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(ItemRespawnTimerPlugin.class, "/icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Item Respawns")
			.panel(panel)
			.icon(icon)
			.build();

		clientToolbar.addNavigation(navButton);
	}


	@SuppressWarnings("RedundantThrows")
    @Override
	protected void shutDown() throws Exception
	{
		log.debug("Item Respawn Timer plugin  stopped!");
		overlayManager.remove(overlay);
		activeTimers.clear();//todo do i need to clear this?
		//todo do i need a shutdown for staticSpawnService?
		clientToolbar.removeNavigation(navButton);
		worldHopper.shutDown();
	}


	@SuppressWarnings("unused")
    @Provides
	ItemRespawnTimerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ItemRespawnTimerConfig.class);
	}
	//endregion


	@SuppressWarnings("unused")
    @Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		String configKey = event.getKey();

		System.out.println("Config changed: " + configKey);

		// Example: trigger specific logic when a certain key changes
		if (configKey.equals("trackedSpawnsOverrides"))
		{
			staticSpawnService.reloadConfigOverrides();
		}
	}


	@SuppressWarnings("unused")
    @Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		activeTimers.onItemSpawned(event);
		debugSpawnDiscoveryService.onItemSpawned(event);
	}

	@SuppressWarnings("unused")
    @Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		activeTimers.onItemDespawned(event);
		debugSpawnDiscoveryService.onItemDespawned(event);
	}


	@SuppressWarnings("unused")
    @Subscribe
	public void onGameTick(GameTick event) {
		activeTimers.onGameTick();
		debugSpawnDiscoveryService.onGameTick();
		panel.setCurrentWorldId(client.getWorld());//todo move to the onGameStateChanged event
		panel.updateSidePanel();
	}

	@SuppressWarnings("unused")
    @Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// This runs when the player has joined a world
			worldHopper.joinedWorld();
			debugSpawnDiscoveryService.joinedWorld();
		}
	}


	@SuppressWarnings("unused")
    @Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted) {
		worldHopper.onCommandExecuted(commandExecuted);
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
			clientThread.invoke(() -> activeTimers.deleteExpiredSingle());
		}
	};

	private final HotkeyListener removeExpiredAllListener = new HotkeyListener(() -> config.hotkeyRemoveExpiredAll())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> activeTimers.deleteExpiredAll());
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
