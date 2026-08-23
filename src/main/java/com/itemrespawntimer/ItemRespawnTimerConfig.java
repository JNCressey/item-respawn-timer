package com.itemrespawntimer;

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
			keyName = "hotkeyRemoveExpiredSingle",
			name = "Remove One Expired",
			section = hotkeysSection,
			description = "When you press this hotkey, a single expired timer is removed.",
			position = 0
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
			position = 1
	)
	default Keybind hotkeyRemoveExpiredAll()
	{
		return Keybind.NOT_SET;
	}


	@ConfigItem(
			keyName = "hotkeyClearTimers",
			name = "Clear All Timers",
			section = hotkeysSection,
			description = "When you press this hotkey, all timers are removed.",
			position = 2
	)
	default Keybind hotkeyClearTimers()
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

	//todo section for hiding timers from side panel
	// minimum value
	// list of items to hide

	//region debugDiscoveryModeSection
	@ConfigSection(
			name = "Debug Options",
			description = "Debug options",
			position = 3,
			closedByDefault = true
	)
	String debugSection = "debugSection";

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @ConfigItem(
			keyName = "discoveryModeEnabled",
			name = "Enable Static Spawn Discovery",
			section = debugSection,
			description = "When enabled, will attempt to discover static spawn data from what is observed. Will notify with a game message in the chat if discovery is different from known data.",
			position = 0
	)
	default boolean discoveryModeEnabled()
	{
		return false;
	}
	//todo add a debug mode that logs in chat predictions for what baseRespawnTicks is for items spawns it observes
	//todo add debug mode that automatically updates overrides with observed baseRespawnTicks
	//todo add debug to shift+rightClick on the ground to add or remove tracking
	//todo have a checkbox for whether to apply the overrides or not


	@Range(min=0)//todo is it non-negative by default if i remove range?
	@ConfigItem(
			keyName = "discoveryBaseRespawnTicksThreshold",
			name = "Difference Threshold (baseRespawnTicks)",
			section = debugSection,
			description = "Discovery mode will consider a difference of more than this from the prediction of baseRespawnTicks to be wrong data.",
			position = 1
	)
	default int discoveryBaseRespawnTicksThreshold()
	{
		return 5;
	}

	@ConfigItem(
			keyName = "discoveryModeNotifyCorrect",
			name = "Also notify correct",
			section = debugSection,
			description = "Discovery mode will also notify for static spawn discoveries that match the known data.",
			position = 2
	)
	default boolean discoveryModeNotifyCorrect()
	{
		return false;
	}


	@ConfigItem(
			keyName = "discoveryModeAutoAddOverrides",
			name = "Automatically add to overrides",
			section = debugSection,
			description = "Discovery mode will automatically add discovered static spawn data to the overrides.",
			position = 3
	)
	default boolean discoveryModeAutoAddOverrides()
	{
		return false;
	}

	@ConfigItem(
			keyName = "overridesEnabled",
			name = "Enable Overrides",
			section = debugSection,
			description = "The tracked spawns data from the below config field will override the default data.",
			position = 4
	)
	default boolean overridesEnabled()
	{
		return false;
	}

	@ConfigItem(
			keyName = "trackedSpawnsOverrides",
			name = "Tracked Item Spawns Override Data",
			section = debugSection,
			description = "List of overrides to the tracked spawns. x, y, plane, [{\"null\" | baseRespawnTicks, {-1 | itemId}, quantity}]",
			position = 5
	)
	default String trackedSpawnsOverrides()
	{
		return(
			"#lumbridge bowl remove tracking"
			+ "\nx, y, plane, exclude"
			+ "\n#3208, 3214, 0, exclude"
		    + "\n\n#lumbridge bowl remove tracking shorthand"
			+ "\nx, y, plane"
			+ "\n#3208, 3214, 0"

			+ "\n\n#lumbrige bowl add tracking without itemId check"
			+ "\nx, y, plane, baseRespawnTicks, -1, quantity"
			+ "\n#3208, 3214, 0, 100, -1, 1"

			+ "\n\n#lumbrige bowl re-add tracking"
			+ "\nx, y, plane, baseRespawnTicks, itemId, quantity"
			+ "\n#3208, 3214, 0, 100, 1923, 1"
		);
	}


	@ConfigItem(
			keyName = "trackedSpawnsOverrides",
			name = "",
			description = ""
	)
	void setTrackedSpawnsOverrides(String key);
	//endregion debugDiscoveryModeSection

}
