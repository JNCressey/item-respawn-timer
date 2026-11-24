package com.itemrespawntimer;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("itemrespawntimer")
public interface ItemRespawnTimerConfig extends Config
{

	@ConfigItem(
			keyName = "panelEnabled",
			name = "Enable Side-panel",
			description = "A side panel with a list of active timers.",
			position = 0
	)
	default boolean panelEnabled()
	{
		return true;
	}

	//region overlay
	@ConfigSection(
			name = "Overlay",
			description = "Configure Overlay. Timers shown, in the game screen, where the item will respawn.",
			position = 1
	)
	String overlaySection = "overlaySection";


	@ConfigItem(
			keyName = "overlayPieEnabled",
			name = "Enable Pie Overlay",
			section = "overlaySection",
			description = "Show pie-style timers in the overlay.",
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
			description = "Show number-style timers in the overlay.",
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
			description = "What unit to use for the  number-style timers in the overlay.",
			position = 2
	)
	default OverlayNumberMode overlayNumberMode()
	{
		return OverlayNumberMode.SECONDS;
	}
	//endregion overlay


	//region colors
	@ConfigSection(
			name = "Overlay Colors",
			description = "Configure overlay colors.",
			position = 2,
			closedByDefault = true
	)
	String colorSection = "colorSection";


	@Alpha
	@ConfigItem(
			keyName = "pieFillColorWithoutNumber",
			name = "Pie without number",
			section = "colorSection",
			description = "The background color of the overlay pie without numbers.",
			position = 0
	)
	default Color pieFillColorWithoutNumber()
	{
		return Color.YELLOW;
	}


	@Alpha
	@ConfigItem(
			keyName = "pieFillColorWithNumber",
			name = "Pie with number",
			section = "colorSection",
			description = "The background color of the overlay pie with numbers.",
			position = 1
	)
	default Color pieFillColorWithNumber()
	{
		return new Color(120, 120, 0);
	}


	@Alpha
	@ConfigItem(
			keyName = "pieBorderColor",
			name = "Pie Border",
			section = "colorSection",
			description = "The outline color of the overlay pie.",
			position = 2
	)
	default Color pieBorderColor()
	{
		return Color.ORANGE;
	}


	@Alpha
	@ConfigItem(
			keyName = "overlayNumberColor",
			name = "Number Text",
			section = "colorSection",
			description = "The text color of the overlay numbers.",
			position = 3
	)
	default Color overlayNumberColor()
	{
		return Color.WHITE;
	}
	//endregion colors


}
