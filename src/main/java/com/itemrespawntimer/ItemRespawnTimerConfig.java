package com.itemrespawntimer;

import com.itemrespawntimer.staticspawndata.TrackedSpawnsDefaultFileReader;
import com.itemrespawntimer.timermodel.RemoveTimerEvent;
import net.runelite.client.config.*;

import java.util.EnumSet;
import java.util.Set;

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


	//region removeTimersSection
	@ConfigSection(
			name = "Automatically Remove Timers",
			description = "Automatically remove the expired timers",
			position = 1,
			closedByDefault = true
	)
	String removeTimersSection = "removeTimersSection";

	@ConfigItem(
			keyName = "removeTimersEvents",
			name = "when",
			section = removeTimersSection,
			description = "Automatically remove timers at any of the selected conditions.",
			position = 0
	)
	default Set<RemoveTimerEvent> removeTimersEvents()
	{
		return EnumSet.of(
				RemoveTimerEvent.CAN_SEE_LOCATION,
				RemoveTimerEvent.TWICE_RESPAWN_TIME
		);
	}

	@ConfigItem(
			keyName = "removeTimersX",
			name = "X seconds (for above)",
			section = removeTimersSection,
			description = "The time 'X' in seconds for the above 'remove when at T+X' option.",
			position = 1
	)
	default int removeTimersX()
	{
		return 5;
	}
	//endregion removeTimersSection


	//region trackedSpawnsSection
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
	//endregion trackedSpawnsSection

	@ConfigItem(
			keyName = "quickHopHotkey",
			name = "Quick-hop Hotkey",
			description = "When you press this hotkey you'll hop to the world at the top of the world timers list.",
			position = 2
	)
	default Keybind quickHopHotkey()
	{
		return null;//new Keybind(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}
}
