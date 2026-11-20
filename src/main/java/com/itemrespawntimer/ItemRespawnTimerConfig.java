package com.itemrespawntimer;

import net.runelite.client.config.*;

@ConfigGroup("itemrespawntimer")
public interface ItemRespawnTimerConfig extends Config
{

	//region overlay
	@ConfigSection(
			name = "Overlay",
			description = "Configure Overlay. Timers shown, in the game screen, where the item will respawn.",
			position = 0,
			closedByDefault = false
	)
	String overlaySection = "overlaySection";


	@ConfigItem(
			keyName = "overlayPieEnabled",
			name = "Enable Pie Overlay",
			section = "overlaySection",
			description = "Show timers, in the game screen, where the item will respawn.", //todo description specific to overlay type
			position = 0
	)
	default boolean overlayPieEnabled()
	{
		return true;
	}


	@ConfigItem(
			keyName = "overlayNumberEnabled",
			name = "Enable Number Overlay",
			section = "overlaySection",
			description = "Show timers, in the game screen, where the item will respawn.", //todo description specific to overlay type
			position = 1
	)
	default boolean overlayNumberEnabled()
	{
		return false;
	}


	@ConfigItem(
			keyName = "overlayNumberMode",
			name = "Overlay Number Mode",
			section = "overlaySection",
			description = "Show timers, in the game screen, where the item will respawn.", //todo description specific to overlay type
			position = 2
	)
	default OverlayNumberMode overlayNumberMode()
	{
		return OverlayNumberMode.SECONDS;
	}
	//endregion overlay


	@ConfigItem(
			keyName = "panelEnabled",
			name = "Enable Side-panel",
			description = "A side panel with a list of active timers.",
			position = 2
	)
	default boolean panelEnabled()
	{
		return true;
	}

}
