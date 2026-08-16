package com.itemrespawntimer;

import com.itemrespawntimer.staticspawndata.TrackedSpawnsDefaultFileReader;
import com.itemrespawntimer.timermodel.RemoveExpiredTimerEvent;
import com.itemrespawntimer.timermodel.RemoveExpiredTimerHotkeyMode;
import net.runelite.client.config.*;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
	default Set<RemoveExpiredTimerEvent> removeTimersEvents()
	{
		return EnumSet.of(
				RemoveExpiredTimerEvent.CAN_SEE_LOCATION,
				RemoveExpiredTimerEvent.TWICE_RESPAWN_TIME
		);
	}

	@Range(
			max=300
	)
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


	//region hotkeysSection
	@ConfigSection(
			name = "Hotkeys",
			description = "Configure Hotkeys.",
			position = 2,
			closedByDefault = true
	)
	String hotkeysSection = "hotkeysSection";

	@ConfigItem(
			keyName = "hotkeyQuickHopTop",
			name = "Quick-Hop",
			section = hotkeysSection,
			description = "When you press this hotkey, you'll hop to the world at the top of the list in the side panel.",
			position = 0
	)
	default Keybind hotkeyQuickHopTop()
	{
		return new Keybind(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "hotkeyRemoveExpired",
			name = "Remove Expired Timers",
			section = hotkeysSection,
			description = "When you press this hotkey, expired timers are removed.",
			position = 1
	)
	default Keybind hotkeyRemoveExpired()
	{
		return new Keybind(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "removeHotkeyMode",
			name = "Remove hotkey mode",
			section = hotkeysSection,
			description = "How many expired timers to remove when you press the hotkey.",
			position = 2
	)
	default RemoveExpiredTimerHotkeyMode removeHotkeyMode()
	{
		return RemoveExpiredTimerHotkeyMode.SINGLE;
	}
	//endregion hotkeysSection
}
