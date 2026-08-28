# Item Respawn Timer
Show timers for respawning items.

Show overlay countdown dials like there is for mining and woodcutting plugins.

Show a side panel of timers like the time tracking plugin.
- The progress bar fills with green until the respawn time.
- After you see the item, the timer is deleted from the panel.
  - If you don't return to the spawn, the progress bar will fill a second time with red as an indicator of how stale the information is.
    - After the stale bar fills the timer is deleted.
- If you're in a different world to the spawn, the world number will be shown on the timer.

Predictions can be slightly off. Because the respawn time depends on how many players are currently in the world, and the world populations are only periodically sent to the client.

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
- wilderness wine of zamorak seems to be shorter than baseRespawnTicks 10. find out what it is
- respawn delay predicion seems to be 1 longer than measured time for despawnEvent->spawnEvent
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
