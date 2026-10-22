# Blue book

The blue panel: how search and tabs filter it, how it sits beside the screen, and the button that opens it. Each rule that can be tested is a line in [Checks](checks.md). This note explains the mechanics those lines depend on. The hooks into vanilla's screen are in [Vanilla book](vanilla-book.md).

## Search and tabs

Search is a text filter on the current group, and a compass tab. It is not a category. "Open where you left off" is off by default. Opening the inventory, the crafting table, or the book then clears the search text, and the next screen opens on the search tab and the first page. On, the tab, the page, and the search text stay for this client session. The inventory and the crafting table share them. Quitting the game clears that place and leaves the option on. Ctrl+F on a book that is already open selects the current text.

The search tab lists the crafting recipes that fit this station, from every crafting tab. A smelting or stonecutting recipe stays off this book. While the search box has text, a slot holds only members whose result name contains it: [Group icon](group-icon.md).

Crafting tabs follow vanilla, after the search tab: equipment, building blocks, miscellaneous, and redstone. Each category tab wraps one of Minecraft's `CraftingBookCategory` values. A recipe sits on the tab that matches its vanilla crafting category. Category tabs list crafting recipes for that station only.

## Size and input

Below a width of 379 the open book replaces the screen. Escape closes the book. A click that misses the book is swallowed.

The wheel turns the page, wrapping from the first page to the last and back. Ctrl+scroll or Alt+scroll on a recipe places one craft or takes one back. The option "Scroll to move items" swaps those two. The strip under the recipes, where the page number and arrows sit, only turns the page, with or without Ctrl or Alt. Both arrows show when there is more than one page.

## Button

The blue button is two recolored copies of vanilla's `recipe_book/button.png` and `button_highlighted.png` sprites. The alpha matches vanilla's exactly, and the colors are shifted toward blue. No textures are copied from other mods.

## Arrival

A slot pops the first time it is drawn with an item that has just qualified. Qualifying is an unlock, or strict discovery revealing the recipe once every ingredient has been held. The same pop plays when that happens while the book is closed, the next time the slot is drawn. The category tab stretches when it still contains such an item, including on that next open. The search tab stays still.

The pop lasts 15 ticks, as in the vanilla book. The slot scales both ways, up to a tenth larger and back, around its center. A tab scales on the vertical axis only, around the middle of its height. A second crafting recipe for an item already waiting does not add another pop. After the slot has been drawn, a later recipe for that item pops it again. Search, switching tabs, the order button, turning on Reveal all recipes, and turning strict discovery off do not. A group lightbox that is already open pops that new cell as well, with the same motion as the slot. An alternative recipes lightbox stays still. Opening a lightbox later does not. Leaving the world forgets which items were waiting.
