# Ctrl in the vanilla book

Holding Ctrl freezes the cycling recipe icon. The click still places the recipe that is showing.

## The freeze

`RecipeButton.renderWidget` adds the frame time only when `Screen.hasControlDown()` is false. `currentIndex` is `floor(time / 30) % size`. With Ctrl down, time stops, so a grouped slot stays on one member. `getRecipe()` returns that member, and the click places it. `GhostRecipe.render` skips its timer the same way, so the ghost ingredients in the grid hold still too.

## Not an inventory click

Ctrl+click is not a slot action. `AbstractContainerScreen.mouseClicked` uses Shift (keys 340 and 344) for `ClickType.QUICK_MOVE`. Ctrl changes the drop key: in `keyPressed`, the drop binding sends `ClickType.THROW` with button 1 when Ctrl is down, which throws the whole stack. That is Ctrl+Q.

A click on this book's page is consumed by the book widget, so it never reaches that slot path.

## This book

`BlueBook.render` skips `iconTime` the same way, so the cycle holds still. Ctrl+click and Alt+click open the front icon's result in JEI. What that icon is, and when a left click places it, is [Group icon](group-icon.md). The same two chords on a type in that list open that type. Ctrl+scroll and Alt+scroll place or take back one craft, unless "Scroll to move items" is on, in which case those chords turn the page. The strip under the recipes only turns the page. Ctrl+F still focuses search.
