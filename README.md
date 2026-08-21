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
