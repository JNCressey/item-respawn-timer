package com.itemrespawntimer;

import net.runelite.client.config.*;

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

	@ConfigItem(
			keyName = "panelEnabled",
			name = "Enable Side-panel",
			description = "A side panel with a list of active timers.",
			position = 1
	)
	default boolean panelEnabled()
	{
		return true;
	}

}
