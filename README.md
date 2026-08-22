# Item Respawn Timer
Show timers for respawning items

Predictions can be slightly off. Because the respawn time depends on how many players are currently in the world, and the world populations are only periodically sent to the client.

# Tracking multiple worlds
The side panel shows all the timers being tracked across multiple worlds.

You can quick-hop to these worlds.
- todo:A button on each timer is still to do.
- Hop to the world of the top timer.
  - Hotkey (Ctrl+Shift+Up) configurable in the plugin config.
  - Chat commands:
    - `::irthop 1`
    - `::irthop top`
    - `::irthoptop`
- Hop through the worlds sequentially.
  - Hotkey (Ctrl+Shift+Down) configurable in the plugin config.
  - Chat commands:
    - `::irthop`
    - `::irthop next`
    - `::irthopnext`
- Hop to the world of the timer at a position of the list (1-indexed from the top).
  - Chat command:
    - `::irthop <index>`

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
- add "you left the area before respawn observed" message for discovery mode.
- add debug mode that checks tells you you when you load area that should have static spawn but item isn't there. and tells you when leaving an area and you never saw it spawn.
  - use a Map<Worldpoint, ConfirmationEntry> with confirmation enties having a staticspawn and a ConfirmationStatus enum of [loadedAreaAndWaitingToSeeSpawn, previouslyLoadedAreaButNowUnloadedStillNeedToSeeSpawn,Confirmed].
  - spawns will start as not being in the map
  - add to map when area loaded
  - change status when leaving or reloading or observed spawn. once confirmed, the status should stay confirmed and not change back to one of the unconfirmed statuses.
  - can imply the status of an item not in the map as being unconfirmed (maybe make a gatter that does orsElseGet() with such a status, but never need to set that status as a value in the map
  - when overrides config is changed, new staticspawns are generated, so the confirmation map should react
    - for each ConfirmationEntry, if the static spawn is the same data as the new tracked spawn, set the entry's spawn field with the new tracked spawn object. if it's different then the entry is removed (and new entry is added if area is loaded).
- work for all respawning items, not just Lumbridge castle mind rune
  - fill the csv file
- side panel to track timers that you're waiting for a respawn
  - make each timer have a widget in the list
    - item icon
    - item name
    - world id
    - timer countdown as format "T-#" or "T+#"
    - button to hop to world on click
    - button to delete timer
    - button to hide/unhide - add/remove from config list of hidden
  - button to filter or show the manually hidden widgets
- circle progress display appearance
  - make a consistent size
  - centred in the tile
  - appropriate height on ground or on top of table
