package com.itemrespawntimer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface ItemRespawnTimerConfig extends Config
{
	@ConfigItem(
			keyName = "enabled",
			name = "Enable overlay",
			description = "Show respawn timers for static spawns"
	)
	default boolean enabled()
	{
		return true;
	}

}
