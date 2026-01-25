package com.itemrespawntimer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ItemRespawnTimerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ItemRespawnTimerPlugin.class);
		RuneLite.main(args);
	}
}