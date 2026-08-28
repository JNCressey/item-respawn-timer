package com.itemrespawntimer;

import com.google.inject.Provides;

import javax.inject.Inject;

import com.itemrespawntimer.panel.ItemRespawnTimerPanel;
import com.itemrespawntimer.staticspawnservice.StaticSpawnService;
import com.itemrespawntimer.timermodel.ActiveTimers;
import com.itemrespawntimer.timermodel.DespawnEventVerificationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.awt.image.BufferedImage;
import java.time.Instant;

import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.ItemDespawned;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.ImageUtil;
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
	private ClientToolbar clientToolbar;


	@Inject
	private ItemRespawnTimerOverlay overlay;


	@Inject
	private StaticSpawnService staticSpawnService;


	@Inject
	private ActiveTimers activeTimers;


	@Inject
	private DespawnEventVerificationService despawnEventVerificationService;


	@Inject
	private WorldService worldService;
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
	public void onItemSpawned(ItemSpawned event)
	{
		activeTimers.onItemSpawned(event);
	}

	@SuppressWarnings("unused")
    @Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		activeTimers.onItemDespawned(event);
	}


	@SuppressWarnings("unused")
    @Subscribe
	public void onGameTick(GameTick event) {
		despawnEventVerificationService.onGameTick();
		activeTimers.onGameTick();
		panel.setCurrentWorldId(client.getWorld());//todo move to the onGameStateChanged event
		panel.updateSidePanel();
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


	public int getCurrentWorldPopulation(){
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

}
