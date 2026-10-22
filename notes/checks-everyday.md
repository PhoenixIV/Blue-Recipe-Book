# Everyday checks

The short pass after a change. The full checklist stays in [Checks](checks.md), and that file is the record of what the build does. When a change alters behavior, update the matching line there, then update this file by the rules below.

- Leave a check off this list when failing it would already show up during another line here. Opening, closing, Escape, a plain left-click, and editing a search are that kind of check.
- When several lines in the full checklist are the same behavior with different inputs, keep one line here. The feed sequences are one timer.
- Keep a line when the failure stays hidden unless that action is done on purpose: a modifier click, a setting, a file on disk, the crafting table as well as the inventory, or cancelling a sequence that is already running.
- Known gaps, and checks that exist only for a release jar, stay on the full list.

Start with `-Dbluerecipebook.devToggle=true` for the swap, the feed, and Unlock, Reset, and Options. Use a dev world and `/recipe give @s *` to unlock recipes quickly.

## Opening

- [ ] Close the book, leave the world, and rejoin. It stays closed. Open it and rejoin: it is open. The crafting table matches. Another world follows that world's vanilla crafting book.

## Search and tabs

- [ ] With the inventory open and the book closed, Ctrl+F opens the book and focuses the search box. Text already there is selected. The same keys in the world do nothing.
- [ ] On a category tab, search filters that category. The compass tab searches every crafting tab, and a smelting or stonecutting result does not appear. A category tab appears only when that station has an unlocked crafting recipe in the group. The inventory lists 2×2 recipes and the crafting table lists 3×3.
- [ ] "Open where you left off" is off. A tab, a page, and a search are gone when the inventory opens again. Turn it on and they come back, on the crafting table too. Quit and relaunch: the option stays on and that place starts over.

## Groups

- [ ] Recipes that share a group sit in one slot, and the icon walks about every half second. Ctrl holds it still. A click leaves the front item in place while the icon behind keeps walking. Red stained glass shows once and does not walk between its two alternative recipes.
- [ ] Click red stained glass. The craftable alternative recipe is placed, and an alternative recipes lightbox opens with that recipe first. Clicking an alternative recipe places that recipe, and the lightbox stays up. Close the book and click it again: the same alternative is placed.
- [ ] Click a group whose items differ. The group lightbox has one cell per item, the front item is placed, and a pinned item is the first cell. A cell with two recipes opens an alternative recipes lightbox on top and dims the group. Escape closes that lightbox first, and the cell that opened it stays undimmed. A click on another tab closes the lightbox and selects that tab.
- [ ] On Search, recipes that share a group or the same result share a slot. A category tab still shows only its own recipes.
- [ ] Turn Group Recipes off. Different items no longer share a slot. Red stained glass still shows once. An open lightbox closes.
- [ ] Shift-click places as many crafts as the inventory allows. The wheel turns the page, wrapping from the first to the last and back. Hold Ctrl or Alt and scroll over a recipe to place one craft, and scroll down to take one back. When the inventory cannot fill it, a ghost stays, and a second scroll leaves that ghost. A locked recipe draws its ghost on this client. A recipe this grid cannot hold ignores the wheel. Both arrows show when there is more than one page, and the page number shows then too. The strip under the recipes only turns the page, with or without Ctrl or Alt.
- [ ] "Scroll to move items" is off. Turn it on. The wheel places one craft and scroll down takes one back. Ctrl+scroll and Alt+scroll turn the page. The strip under the recipes still only turns the page. An open lightbox stays up. Restart: the choice is still there.
- [ ] With JEI installed, Ctrl+click or Alt+click a slot. JEI opens on that result. A left click on a recipe this station cannot hold does the same. While this book is in use, JEI's item list stays off, including when the panel is closed.
- [ ] With "Reveal all recipes" on, click a locked recipe. The grid is cleared, then a ghost appears.

## Discovery

- [ ] Craftable-first is the default. An unlock you can craft appears ahead of ones you cannot, and recipes you cannot craft stay on the page. The slot pops once, and its tab stretches once. The search tab stays still. With a group lightbox open, a new item in that group pops in the popup as well. Switch to newest-first and an unlock appears first. Clicking the button closes an open lightbox and shows the first page. The button keeps its last state after a restart.
- [ ] Close the book, unlock a recipe, and open it. That slot pops too. Rejoin: recipes already known stay still.
- [ ] With craftable-first on, craft while the book is open. The slot can move. An open lightbox stays.
- [ ] `/recipe take @s <recipe id>` removes that recipe from the open page, including one granted before the mod ran in that world. An unlock that matches the current search appears in those results.
- [ ] After an unlock in singleplayer, the world folder has `bluerecipebook/<player uuid>/unlock-order.txt` listing the id, and `format.json` is `{"format":1,"since":"0.1.0"}`. On a server, that folder is `config/bluerecipebook/remote/server/<host>/<port>/`. Ids from one unlock stay in that packet's order.
- [ ] With strict discovery off, an item in the hotbar, the armor, the offhand, or on the cursor is listed in `held-items.txt` and stays listed after it leaves.
- [ ] With strict discovery off, a recipe you have not held the ingredients for still shows. Turn it on and the recipe stays hidden until every ingredient has been held, and the toast waits for that too. One held item covers every slot that accepts it. With "Reveal all recipes" on, that recipe still appears. Turning strict discovery on or off closes an open lightbox.
- [ ] "Reveal all recipes" is off. Turn it on and a locked recipe appears. Turn it off and the recipe leaves. The dev corner button flips the same option. Restart: the choice is still there.

## Development

- [ ] The corner button swaps to the vanilla book and back, open or closed with the blue book. Both stay loaded, and the blue panel uses the vanilla book texture. The search box still accepts typing afterward.
- [ ] Unlock grants recipes through the server. Reset takes them and leaves the discovery files empty. Options opens the config screen, and the file is `config/bluerecipebook.json`.
- [ ] Feed opens its list. Click any one sequence and let it run. Start another before it finishes: the first stops and the second runs. Escape closes the list.
