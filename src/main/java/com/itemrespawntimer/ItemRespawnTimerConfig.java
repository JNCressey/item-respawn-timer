package com.itemrespawntimer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

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
			keyName = "worldTrackerMinimumValue",
			name = "World tracker minimum value",
			description = "The minimum value for the world tracker side panel",
			position = 0
	)
	default int worldTrackerMinimumValue()
	{
		return 0;
	}

	@ConfigSection(
			name = "Tracked Spawns",
			description = "Parameters of locations of spawns to track",
			position = 1,
			closedByDefault = true
	)
	String trackedSpawnsSection = "trackedSpawnsSection";

	@ConfigItem(
			keyName = "discoveryModeEnabled",
			name = "Enable Discovery Mode",
			section = trackedSpawnsSection,
			description = "Will attempt to automatically update the list of tracked spawns while you observe spawns.",
			position = 0
	)
	default boolean discoveryModeEnabled()
	{
		return true;
	}

	//todo put tracked spawns within a collapsed section
	@ConfigItem(
			keyName = "trackedSpawns",
			name = "Tracked Item Spawns",
			section = trackedSpawnsSection,
			description = "List of data about known item spawns that this plugin will show timers for.",
			position = 1
	)
	default String trackedSpawns()
	{
		return TrackedSpawnsDefaultFileReader.readResource();
	}


}
