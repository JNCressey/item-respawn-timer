# Item Respawn Timer
Show timers for respawning items

## dev to-dos
- don't start timer from going up stairs or running away, only when item is taken
- work for all respawning items, not just lumbridge castle mind rune
- clear the timer if respawn is witnessed
- side panel to track worlds where you're waiting for a respawn
  - hop to world on click
  - filter minimum item to track value
  - sort list by smallest time until respawn
- make circle progress display smaller
- add an option for another style of timer that stars when you discover an item is missing but did not witness it being taken
  - should be styled different (different colour) (accessible for colour blind? putting less than symbol before the number)
  - user option to disable this kind
  - this is useful for predicting a maximum time to wait. edge case where real respawn time could be longer than predicted: world population is more when you witness than when item was taken, this decreases your predicted total wait time. You could be early enough after it was taken that doesn't fully make up for your under-estimate.
- timers appear to predict a second late. do we need to subtract one tick from the timer for the observation of it being missing being one tick after the tick it was really taken?