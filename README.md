# Item Respawn Timer
Show timers for respawning items

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
- discovery mode shouldn't think ashes from fires are a new spawn discovery
  - can i tell the difference between a respawn and a fire
  - there are two ashes respawns which I should make sure is in my data
    - Bandits' camp north-east of Ralos' Rise
    - Sisterhood Sanctuary
- discovery mode, is there any other reason why an item spawn event has owner=none
- should startup code in injected classes be put into injected constructor like the config has?
- add "you left the area before respawn observed" message for discovery mode.
- And a config option for an audit mode
  - when leaving an area before respawn observed add an override with baseRespawnTicks of 9999 to indicate unknown.
  - this option should also disable the function that infers from the default [item id, baseRespawnTicks] data, so you know the 9999s are unconfirmed
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
