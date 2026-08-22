# Item Respawn Timer
Show timers for respawning items

Predictions can be slightly off. Because the respawn time depends on how many players are currently in the world, and the world populations are only periodically sent to the client.

## dev to-dos
- add "you left the area before respawn observed" message for discovery mode.
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
