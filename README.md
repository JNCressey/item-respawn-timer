# Item Respawn Timer
Show timers for respawning items.

Show overlay countdown dials like there is for mining and woodcutting plugins.
Show a side panel of timers like the time tracking plugin.

Predictions can be slightly off. Because the respawn time depends on how many players are currently in the world, and the world populations are only periodically sent to the client.

# Tracking multiple worlds
The side panel shows all the timers being tracked.

Hotkeys for manually removing timers:
- Remove One Expired
  - When you press this hotkey, a single expired timer is removed.
  - Hotkey not set by default, configurable in the plugin config.
- Remove All Expired
  - When you press this hotkey, all expired timers are removed.
  - Hotkey not set by default, configurable in the plugin config.
- Clear All Timers
  - When you press this hotkey, all timers are removed.
  - Hotkey not set by default, configurable in the plugin config.


## dev to-dos
- also make timers for scenary object spawns
  - find out if the items that drop from hunter are ownership_none
- use the ongametick to record the time of the current tick, then despawn and spawn events should use the time of the tick instead of get current time millis
- do i need to subtract 1 tick when projecting the due time, (for the despawn event being 1 tick after the item is gone?)
- discovery mode ticks prediction is slghtly of, (getting 8 ticks instead of 10 ticks for burthorpe stone balls for example)
- discovery mode, is there any other reason why an item spawn event has owner=none
- should startup code in injected classes be put into injected constructor like the config has?
- add "you left the area before respawn observed" message for discovery mode.
- work for all respawning items, not just Lumbridge castle mind rune
  - fill the csv file
- side panel to track timers that you're waiting for a respawn
  - make each timer have a widget in the list
    - item icon
    - item name
    - world id
    - timer countdown as format "T-#" or "T+#"
    - button to delete timer
    - button to hide/unhide - add/remove from config list of hidden
  - button to filter or show the manually hidden widgets
- circle progress display appearance
  - make a consistent size
  - centred in the tile
  - appropriate height on ground or on top of table
