# Item Respawn Timer
Show timers for respawning items

Predictions can be slightly off. Because the respawn time depends on how many players are currently in the world, and the world populations are only periodically sent to the client.

## dev to-dos
- don't start timer from going up some stairs or running away, only when item is taken
- work for all respawning items, not just Lumbridge castle mind rune
  - fill the csv file
  - load the data in from the cav file
  - handle item names
- clear the timer if respawn is witnessed
- side panel to track worlds where you're waiting for a respawn
  - hop to world on click
  - filter minimum item to track value
  - sort list by smallest time until respawn
- make circle progress display smaller
- add an option for another style of timer that stars when you discover an item is missing but did not witness it being taken
  - should be styled different (different color) (accessible for colorblind? putting less than symbol before the number)
  - user option to disable this kind
  - this is useful for predicting a maximum time to wait.
