package com.itemrespawntimer;

import com.itemrespawntimer.staticspawndata.TrackedSpawnsDefaultFileReader;
import com.itemrespawntimer.timermodel.RemoveExpiredTimerEvent;
import net.runelite.client.config.*;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.Set;

@ConfigGroup("itemrespawntimer")
public interface ItemRespawnTimerConfig extends Config
{
	@ConfigItem(
			keyName = "overlayEnabled",
			name = "Enable Overlay",
			description = "Show timers, in the game screen, where the item will respawn.",
			position = 0
	)
	default boolean overlayEnabled()
	{
		return true;
	}


	//region hotkeysSection
	@ConfigSection(
			name = "Hotkeys",
			description = "Configure Hotkeys.",
			position = 1,
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
			keyName = "hotkeyRemoveExpiredSingle",
			name = "Remove One Expired",
			section = hotkeysSection,
			description = "When you press this hotkey, a single expired timer is removed.",
			position = 1
	)
	default Keybind hotkeyRemoveExpiredSingle()
	{
		return new Keybind(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "hotkeyRemoveExpiredAll",
			name = "Remove All Expired",
			section = hotkeysSection,
			description = "When you press this hotkey, all expired timers are removed.",
			position = 2
	)
	default Keybind hotkeyRemoveExpiredAll()
	{
		return Keybind.NOT_SET;
	}
	//endregion hotkeysSection


	//region removeTimersSection
	@ConfigSection(
			name = "Automatically Remove Timers",
			description = "Automatically remove the expired timers",
			position = 2,
			closedByDefault = true
	)
	String removeTimersSection = "removeTimersSection";

	@ConfigItem(
			keyName = "removeTimersEvents",
			name = "when",
			section = removeTimersSection,
			description = "Automatically remove timers when any of the selected conditions are met.",
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
			min=-999,
			max=999
	)
	@ConfigItem(
			keyName = "removeTimersCustom1",
			name = "Custom Offset 1 (s)",
			section = removeTimersSection,
			description = "The custom offset in seconds for the above 'remove when at T + custom offset 1' option. Use negative for before T or positive for after T.",
			position = 1
	)
	default int removeTimersCustom1()
	{
		return 10;
	}

	@Range(
			min=-999,
			max=999
	)
	@ConfigItem(
			keyName = "removeTimersCustom2",
			name = "Custom Offset 2 (s)",
			section = removeTimersSection,
			description = "The custom offset in seconds for the above 'remove when at 2T + custom offset 2' option. (Where 2T is after twice the respawn time). Use negative for before 2T or positive for after 2T.",
			position = 2
	)
	default int removeTimersCustom2()
	{
		return -10;
	}
	//endregion removeTimersSection


	//region trackedSpawnsSection
	@ConfigSection(
			name = "Tracked Spawns",
			description = "Parameters of locations of spawns to track",
			position = 3,
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
}
