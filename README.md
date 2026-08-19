# Item Respawn Timer
Show timers for respawning items

Predictions can be slightly off. Because the respawn time depends on how many players are currently in the world, and the world populations are only periodically sent to the client.

## dev to-dos
- for timer add, use an iterator to find insert index to avoid using linkedlist.get(index)
- for static spawn overrides, add to array for that location
  - clear overrides can fireach location remove all past first index: list.subList(1, list.size()).clear()
  - file load should write over index 0 if there's a collision, and should give warning
- change plan to not predict unobserved pickups
  - remove the auto discovery mode
  - add a debug mode that logs in chat predictions for what baserespawntime is for items spawns it observes
- unconditional timer removal based on event of the item being there
  - clear the timer when an item spawn event
  - clear the timer if load the area (world login or movement into area) and the item is already there 
- don't start timer from going up some stairs or running away, only when item is taken
- work for all respawning items, not just Lumbridge castle mind rune
  - fill the csv file
  - handle item names
- side panel to track timers that you're waiting for a respawn
  - make each timer have a widget in the list
    - item icon
    - item name
    - world id
    - timer countdown as format "T-#" or "T+#"
    - button to hop to world on click
    - button to remove timer
    - button to hide/unhide
  - button to filter or show the manually hidden widgets
- action button in config panel
  - clear the manually hidden from the side panel
  - add/remove/change a static spawn data
    - store overrides in a persistent data store but remove that big textbox from the config panel
  - reset the static spawn data
- circle progress display appearance
  - make a consistent size
  - centred in the tile
  - appropriate height on ground or on top of table
