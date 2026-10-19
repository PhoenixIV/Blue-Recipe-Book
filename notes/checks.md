# Checks

The record of what this build does. A line is something to check, in the game or in the files, then what should happen. The last section is the rest of that behavior, with the note that holds the detail. It is not a list of requirements.

Game tests never open this screen, see [Development loop](dev-loop.md), so this list is the check on how the book behaves. The short pass after a change is [Everyday checks](checks-everyday.md). The agent updates the matching line in the same change that alters the behavior, and names the lines to run after that change.

Start the game with `-Dbluerecipebook.devToggle=true` for the swap, feed, and development-control checks. Without it, only the blue book shows. Use a dev world and `/recipe give @s *` to unlock recipes quickly.

## Opening and closing

- [ ] Open the inventory. The blue book button appears, and clicking it opens the book and moves the screen to make room. Clicking again closes it.
- [ ] Open the crafting table. The same button works there.
- [ ] Press Escape with the book open on a wide screen. The screen closes, and the search box does not keep the key.
- [ ] Make the window narrower than 379 pixels. The book replaces the screen. A click outside the book does nothing. Escape closes the book and leaves the screen open. Placing a recipe closes the book.
- [ ] The first time this client prepares the book, it opens if the vanilla crafting book was open. Close it on a chosen tab and page, then open the inventory again: the book, the tab, and the page are as you left them. Join another world without restarting the client: it does not seed from that world's vanilla book. Restart the client and the open state follows the vanilla crafting book again.
- [ ] Start without the dev property. No swap button appears on the inventory or the crafting table.
- [ ] Start with the dev property. The corner button swaps to the vanilla book and back. Both stay loaded. The open blue panel uses the vanilla book texture. Unlocks and placing a recipe stay on the vanilla book. The search box still accepts typing afterward, and the screen draws without an error.

## Search

- [ ] Open the inventory with the book closed and press Ctrl+F. The book opens and the search box takes focus. The same on the crafting table.
- [ ] Press Ctrl+F again while the box already has text. The text is selected. A click on the magnifying glass does the same, and it does that only while the book is open.
- [ ] Press Ctrl+F in the world, outside any screen. Nothing opens.
- [ ] Type a word. The results narrow to recipes whose result name contains it. A group keeps only the results that match, and a group left with one result is a single slot.
- [ ] Delete characters. The results widen again.
- [ ] Type a word that matches nothing. The page is empty, and clearing the box brings the results back.
- [ ] Search while on a category tab. Only that category's results are filtered.
- [ ] Search while the search tab is selected. Recipes on every tab are searched.
- [ ] Type a word that matches a smelting or stonecutting result, such as charcoal or framed glass, on the crafting table. That recipe does not appear.
- [ ] Search for a term, then type more characters. The tabs on the left do not change.
- [ ] Open the inventory or the crafting table again. The search text is cleared.
- [ ] Unlock a recipe while a search is active that it matches. It appears in the results.
- [ ] Press F3+T with the inventory open, then search for a term used before. The names match the new language. This is a known gap, see below.

## Tabs

- [ ] The tabs are Search, Equipment, Building Blocks, Miscellaneous, and Redstone. Search is the compass tab.
- [ ] On the crafting table, a category tab appears only when that station has an unlocked crafting recipe in that group.
- [ ] The compass tab selects the search tab.
- [ ] Clicking a category tab shows only that category's recipes.
- [ ] The inventory lists recipes that fit a 2×2 grid. The crafting table lists recipes that fit a 3×3 grid. Search on either screen lists only the crafting recipes that fit that grid.

## Groups and icons

- [ ] Recipes that share a group string sit in one slot. The icon walks between them about every half second. When several items are craftable, the front icon walks only those, and a click leaves that item in front while the icon behind keeps walking the others, one pixel down and to the right. When none are craftable, the front icon walks the whole slot.
- [ ] Red stained glass shows once in the group, with its two alternative recipes. The icon does not cycle between them, and the slot uses the many-recipe frame. Clicking it places the craftable alternative, or the one placed last this client run while that one is still craftable. Restarting the client drops that memory.
- [ ] Clicking red stained glass places the craftable alternative recipe and opens an alternative recipes lightbox, craftable first, drawn as a 3×3 grid. Clicking an alternative recipe places that recipe, and the lightbox stays up.
- [ ] Clicking a group whose items differ opens only that group lightbox, one cell per item, and still places the front item. A pinned item is the first cell, then craftable items, then newest first. The order button does not reshuffle those cells. The lightbox sits with its top edge on the bottom of the slot, centered on it, and stays inside the page from side to side.
- [ ] Clicking a cell that has two recipes opens an alternative recipes lightbox on top and dims the group. Escape closes that lightbox first. The group cell that opened it stays undimmed.
- [ ] Left-click a group you cannot craft. The lightbox opens and the grid stays empty. When the front recipe is locked, the click draws a ghost instead. Right-click a slot. Nothing is placed, and no lightbox opens.
- [ ] On Search, recipes vanilla would put on different tabs still share a slot when they share a group or the same result. A category tab still shows only its own recipes.
- [ ] Group Recipes is on by default. Turn it off. A group string no longer joins different items, but red stained glass still shows once. An open lightbox closes when the option changes.
- [ ] Hold Ctrl. The icon stops walking.
- [ ] A slot with a recipe you can craft is bright. A slot with none is faded. Adding or removing ingredients updates it while the book stays open.
- [ ] Left-click a craftable slot. The recipe is placed in the grid.
- [ ] Left-click one unlocked recipe you cannot craft, on a station that can hold it. The server draws the missing ingredients as a ghost.
- [ ] Shift-click a craftable slot. As many crafts as the inventory allows are placed.
- [ ] Scroll the wheel over the book. The page turns, wrapping from the first to the last and from the last to the first. A page holds twenty recipes. The arrow buttons do the same, and both show when there is more than one page. The page number shows then too. A single page shows no arrows and no page number.
- [ ] Hold Ctrl or Alt and scroll over a recipe. One craft is placed. Scroll down, and one is taken back. The same chords over empty space do not turn the page. The book stays open. When the inventory cannot fill the craft, the server draws the missing ingredients as a ghost, and another scroll up leaves that ghost. Scroll down clears it. A locked recipe draws that ghost on this client. A recipe this station's grid cannot hold ignores the wheel.
- [ ] Scroll on the strip under the recipes, where the page number and arrows are. The page turns. Holding Ctrl or Alt still only turns the page.
- [ ] "Scroll to move items" is off by default. Turn it on. Scroll over a recipe places one craft, and scroll down takes one back. Ctrl+scroll and Alt+scroll turn the page. The strip under the recipes still only turns the page, with or without those keys. An open lightbox stays up. Restart the game: the choice is still there. Turn it off to return to page turning on the wheel.
- [ ] With JEI installed, Ctrl+click or Alt+click a slot. JEI opens on that result. Left-click a recipe this station's grid cannot hold. JEI opens on that result and nothing is placed. Without JEI, those clicks do nothing.
- [ ] While the blue book is the one in use, JEI's item list stays off the inventory and the crafting table, including when the panel is closed. Swap to the vanilla book and the list returns.
- [ ] With "Reveal all recipes" on, click a locked recipe. Items already in the grid go back first, then a ghost appears. When those items do not fit, the grid stays as it was and no ghost is drawn.
- [ ] Click a tab, a page arrow, another recipe, or an inventory slot outside an open lightbox. The lightboxes close, and that click still happens. A click on a lightbox stays with it. On a narrow screen, a click outside the book closes the lightboxes and does not reach the inventory.

## Discovery

- [ ] With the top-right button on (the default), unlock a recipe you can craft. It appears in front of the page, ahead of recipes you cannot craft.
- [ ] Click the top-right button. The sprite changes, the tooltip reads "Newest recipes first", and the page returns to newest first. Click again to return to "Craftable recipes first". An open lightbox closes, and the book shows the first page.
- [ ] Turn the button off, unlock a recipe, then look at the page. It appears first, as before.
- [ ] With the button on, craft an item while a lightbox is open. The slot can move onto another page. The lightbox stays, and the page follows it.
- [ ] Open a lightbox on a later page, then unlock a recipe that sorts ahead of that slot. The lightbox stays open, the page turns to that slot, and the popup does not move. The same holds with strict discovery on.
- [ ] Open the plank group, then give yourself another plank type. The new plank appears in the popup, and the popup may move. With an alternative recipes popup open on one of those planks, that popup stays and shows that plank's recipes.
- [ ] The button does not hide recipes. A recipe you cannot craft still appears on the page.
- [ ] Restart the game. The button keeps its last state.
- [ ] The settings screen has no entry for the order.
- [ ] Remove a recipe with `/recipe take @s <recipe id>` while the inventory is open. It leaves the page without reopening the screen, including a recipe granted before the mod ran in that world.
- [ ] After an unlock in singleplayer, the world folder has `bluerecipebook/<player uuid>/unlock-order.txt` listing the recipe id, and `bluerecipebook/format.json` is `{"format":1,"since":"0.1.0"}`. On a server, the recipe file is `config/bluerecipebook/remote/server/<host>/<port>/<player uuid>/unlock-order.txt`, and `format.json` is in that server folder.
- [ ] Join a LAN world from the LAN list, unlock a recipe, and leave. The file is under `config/bluerecipebook/remote/lan/<host player> - <world name>/<player uuid>/`, in lower case. The host closes and reopens the world to LAN on a new port. Rejoin: the order is still there. Two worlds from that host with the same name share the folder.
- [ ] Join a realm and unlock a recipe. The file is under `config/bluerecipebook/remote/realms/<realm id>/<player uuid>/`, and `realm-name.txt` in the realm folder holds the realm's name.
- [ ] Join a server whose address has no port. The folder port is `25565`. An international host name is a lower-case punycode folder. An IP and a domain for the same machine stay two folders.
- [ ] Unlock several recipes in one go. `unlock-order.txt` lists those ids oldest first, in the order that packet sent them. A recipe this client never saw unlocked sorts after those on the page, in recipe-manager order. Leave and rejoin the same world: the file is unchanged.
- [ ] Set `format.json` to `{"format":2,"since":"9.9.9"}` and join that world. The game disconnects, the screen names version 9.9.9, and it names that world's `bluerecipebook` folder. The button returns to the title screen. The player files are unchanged. Quit to the menu, set it to 2 again, and rejoin without restarting the game: it disconnects again. Restore `{"format":1,"since":"0.1.0"}` before playing there again. Delete `format.json` and join: it is written back as format 1, and the world stays open. A file that is only `{"format":1}` stays open. Replace it with `not-json` and join: the game disconnects, and the player files are unchanged.
- [ ] Remove a mod, then enter a world that had its recipes. That world's discovery files no longer list them. The log has a warning from Blue Recipe Book for each file that lost ids.
- [ ] "Strictly require every ingredient" is off by default. With it on, a recipe stays hidden until every ingredient has been held, and the new-recipe toast waits for that too. One held item covers every slot that accepts it, and any item from a tag covers that tag. The page sorts by when those ingredients were found. The recipe is still unlocked in the vanilla book. Turning the option on or off closes an open lightbox.
- [ ] With strict discovery off, put an item in the hotbar, the armor slots, the offhand, and on the cursor, then drop it. `held-items.txt` in that player's folder lists those ids, and they stay listed. `strict-order.txt` and `waiting-toasts.txt` are in the same folder.
- [ ] "Reveal all recipes" is off by default and tagged Vanilla. Turn it on: a recipe you have not unlocked appears, and clicking it draws a ghost. Turn it off: that recipe leaves the page. An open lightbox closes when the option changes. Restart the game: the choice is still there. With the option on and strict discovery on, a recipe whose ingredients you have not held still appears.
- [ ] With the dev property, the corner button reads Discovered while that option is off and All while it is on. Clicking it flips the option, the page updates, and the settings screen shows the same state.

## Development controls

- [ ] With the dev property, Unlock grants recipes through the server. Reset takes them and leaves `unlock-order.txt`, `strict-order.txt`, `held-items.txt`, and `waiting-toasts.txt` empty. Options opens the config screen. The file is `config/bluerecipebook.json`.
- [ ] Start without the dev property. Unlock, Reset, and Options are absent.

## Development feed

- [ ] Start with the dev property. The inventory shows a Feed button between Unlock and Reset. Clicking it opens a dimmed lightbox with Poppy, Planks, Fence, Give & Take plank, and Give & Take glass. Escape closes it and returns to the inventory.
- [ ] Click Poppy. The lightbox closes, and three seconds later one poppy is given. Nothing else arrives.
- [ ] Click Planks. After three seconds oak planks arrive, then spruce six seconds later, then birch six seconds after that.
- [ ] Click Fence. After three seconds one oak planks arrives, then one stick six seconds later.
- [ ] Click Give & Take plank. After three seconds the oak planks recipe unlocks, and six seconds later it is taken again.
- [ ] Click Give & Take glass. After three seconds the red, green, and blue stained glass pane recipes unlock one after another, six seconds apart, then are taken again the same way. Check that `/recipe give` reports no error for each id.
- [ ] Start Planks, then click Poppy before it finishes. The Planks sequence stops and the Poppy sequence runs.
- [ ] Start without the dev property. No Feed button appears.

## Known gaps

Items here are behavior the code does not yet match. Remove an item when its check passes.

- The name cache is cleared only when the recipe list is rebuilt. A language change with F3+T can leave names in the old language until the screen is reopened.

## Detail in other notes

A line above is the part to check. These notes hold the rest of the same behavior.

- How a server address, a LAN name, and a realm id become a folder, including the encoding: [Player data](player-data.md).
- Discovery order, held items, and strict discovery, including the dedicated-server half that is not built: [Recipe order](recipe-order.md).
- How the vanilla book stays hidden, and the rules for its ghost and its search box: [Vanilla book](vanilla-book.md).
- How search, tabs, the wheel, the narrow layout, and the button work: [Blue book](blue-book.md).
