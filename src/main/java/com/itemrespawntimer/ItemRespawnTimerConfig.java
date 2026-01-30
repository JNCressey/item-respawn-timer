package com.itemrespawntimer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("itemrespawntimer")
public interface ItemRespawnTimerConfig extends Config
{
	@ConfigItem(
			keyName = "enabled",
			name = "Enable overlay",
			description = "Show respawn timers for static spawns",
			position = 0
	)
	default boolean enabled()
	{
		return true;
	}


	@ConfigItem(
			keyName = "discoveryModeEnabled",
			name = "Enable Discovery Mode",
			description = "Will attempt to automatically update the list of tracked spawns while you observe spawns.",
			position = 1
	)
	default boolean discoveryModeEnabled()
	{
		return true;
	}

	//todo put tracked spawns within a collapsed section
	@ConfigItem(
			keyName = "trackedSpawns",
			name = "Tracked Item Spawns",
			description = "List of data about known item spawns that this plugin will show timers for.",
			position = 2
	)
	default String trackedSpawns()
	{
		return TrackedSpawnsDefaultFileReader.readResource();
	}


}
