# Group icon

The front icon is the one drawn on top. When the results are different items, a second icon sits one pixel down and right. A left click places the front icon when that item's recipe is craftable. Which recipes share a slot is [Recipe order](recipe-order.md).

The icon walks items, never the recipes of one item. An item with several recipes is one entry on the walk. Its alternative recipes appear only in the alternative recipes lightbox: [Lightbox](lightbox.md).

While the search box has text, the slot holds only members whose result name contains that text. The walk, the icon behind, and the lightbox use that shorter list. One member left has no walk and uses the single-recipe frame.

## Craftable

Craftable means the open inventory, including the crafting grid, can supply the recipe, and this station's grid can hold it. Those members are bright. The others are ghosts.

Members are ordered with a pinned item first (the icon is not cycling), then craftable, then newest first. The page's order button does not change a slot. `GroupOrder` is that order. `choice` and `unpinnedCycle` are on that class. It is read when the slot is drawn or clicked, and the read that opens a lightbox is taken before the place, so the panel keeps that order. It is not stored. The next draw of the page icon sees the new pin, including after a locked recipe, which does not rebuild the page.

## What stays in front

`choice` pins the front icon, and the click places that pin:

- One craftable item: that item's craftable recipe.
- Several craftable items, and the recipe `LastGroupUse` remembers is still craftable: that item.

The memory is the recipe last placed from the slot during this client run. A place from the slot, the lightbox, or the scroll wheel writes it. It is not saved.

While an item is pinned, the front icon does not move. The icon behind walks every other item, ghosts included.

## The walk, when nothing is pinned

`unpinnedCycle` is the list the front icon walks. Two or more craftable items: only those. None craftable: every item in the slot.

The front icon is the current step of that list. When the list is only the craftable items, the icon behind walks every other item of the whole slot, ghosts included. When the front icon is walking the whole slot, the icon behind is the next step. It is drawn one pixel down and right when the results are different items.

One step every 30 render ticks. The world tick is paused on this screen, so the counter is render time. Holding Ctrl stops `iconTime`. The vanilla book freezes its icon the same way: [Ctrl in the vanilla book](ctrl-in-vanilla-book.md).

On the craftable walk, a left click places the item showing and opens the lightbox. A group of several items opens only the group lightbox. The alternative recipes lightbox opens when that item is clicked in the group, and dims the group. That place is remembered, so the next click is the pinned one above. The place does not close the lightbox. Shift+click places as many as the inventory allows of that recipe. Below a width of 379 the place closes the book, so the lightbox does not stay up. Hold Ctrl to keep one item of the walk before the click.

When none are craftable, the click opens the lightbox and the grid stays empty. A recipe the player has not unlocked still draws a ghost: [Locked ghost](locked-ghost.md).

A click outside the lightboxes closes them and still does what that click would do. A tab changes category, a page arrow turns the page, another recipe places that recipe, and an inventory slot picks up or places. A click on a lightbox stays with it. On a narrow screen the book replaces the GUI, so a click outside the book closes the lightboxes and does not reach a hidden slot.

Ctrl+click and Alt+click open the front icon's result in JEI and leave the grid alone. The wheel turns the page. Ctrl+scroll or Alt+scroll over the slot asks the server to place one craft of the front icon. "Scroll to move items" swaps those: the wheel places, and Ctrl or Alt turns the page. The strip under the recipes only turns the page, with or without those keys. When the inventory cannot supply the craft, the server answers with ghost ingredients. A further scroll up while that ghost is showing leaves it there. Scroll down takes a placed craft back, and clears a ghost of that recipe when the grid is empty. A recipe this station's grid cannot hold ignores the wheel. Right click does nothing.

A single recipe has no walk. Left click places it. A locked recipe that fits this station draws a ghost.
