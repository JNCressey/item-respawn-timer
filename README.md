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
- add config option for the number on the dial to be ticks or seconds
- get rid of the customisable clearing.
  - clear at 2T (the side panel will show progress bar fill up twice, first time green filling over gray, second time red filling over green)
  - clear when you see the item
- also make timers for scenary object spawns (rimmington spade and bronze axe logs respaws so needs a timer (check other spades))
  - [[log (bronze axe)]] with axe: 5581, without axe: 5582, gives item: 3151
  - [[spade (scenery)]] with spade: 9662, without spade: 10626, gives item: 952
- should startup code in injected classes be put into injected constructor like the config has?
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
    - progress bar fills green over gray til respawn, then fills red over green till 2T
  - button to filter or show the manually hidden widgets
- circle progress display appearance
  - make a consistent size
  - centred in the tile
  - appropriate height on ground or on top of table
