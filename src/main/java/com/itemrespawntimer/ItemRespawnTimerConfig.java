package com.itemrespawntimer;

import net.runelite.client.config.*;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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


	//todo section for hiding timers from side panel
	// minimum value
	// list of items to hide

}
