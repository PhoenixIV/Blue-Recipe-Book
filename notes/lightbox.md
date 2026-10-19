# Lightbox

A slot with more than one recipe opens a lightbox over the page. There are two kinds, and the same rule applies wherever the item sits, grouped or not.

## Rule

Every recipe that makes the same item shares a slot. The group lightbox shows that item once. Its other recipes, the alternative recipes, are one lightbox away.

- A slot whose recipes all make one item opens an alternative recipes lightbox: one cell per alternative recipe.
- A slot whose recipes make several items opens a group lightbox with one cell per item. Left click on a cell with several recipes opens an alternative recipes lightbox above the group, and dims the group, leaving the clicked cell clear.

An alternative recipes lightbox stacked on a group lightbox is the layer above a cell. A lightbox opens another lightbox only through a cell of a group lightbox. The stack is at most two deep in practice.

## Alternative recipes

Each recipe is drawn the way vanilla's recipe overlay draws it. The frame is `crafting_overlay`, or `furnace_overlay` for one ingredient that is not a crafting recipe. Each ingredient is 0.375 scale, placed on a 3x3 grid by vanilla's `PlaceRecipe` rule: a shaped recipe is centered, and shapeless recipes fill row by row. Ingredient alternatives cycle every 30 render ticks, as vanilla does.

## Order

A pinned item (the page icon is not cycling) is first in a group lightbox. Then craftable items, then newest first. Alternative recipes of one item follow the same rule. The page's order button does not change a lightbox.

## Clicks

- A cell with one recipe places it. The lightbox stays up.
- A cell with several recipes places its craftable recipe, the one `LastGroupUse` remembers when that is still craftable, or the first. Then it opens the alternative recipes lightbox above. Shift places as many as the inventory allows. Alt or Ctrl opens JEI on that recipe's result and places nothing.
- An alternative recipe places its recipe, and the lightbox stays up. Placing a recipe updates craftable flags and does not close a lightbox.
- A left click on a lower lightbox closes the layers above it, then does what the click means there. Any other button on a lightbox does not select a cell.
- A left click on the undimmed page slot that opened the lightbox places another round and closes the layers above the first. The first lightbox stays open. Any other button on that slot closes those upper layers and places nothing.
- A click outside every lightbox closes them all, and the click carries on to the page. Escape closes the top layer. Ctrl+F goes to search, so the lightboxes close.

## When the list moves

A new unlock is sorted in front and can push the open slot onto another page. Crafting or picking up an item can do the same when craftable recipes sort first. The lightbox stays open, with strict discovery on or off. Turning that option on or off closes it. The page turns so the undimmed slot that opened it stays on screen. The page does not follow the recipe that just unlocked. While that group's recipes stay the same, the popup stays where it is. When a recipe is added to it or taken out of it, the popup is opened again in the usual order, so it can move. An alternative recipes popup on a cell is opened again on that same item.

## Where it lives

`Lightbox` is one panel: its cells, its layout, and its drawing. `BlueBook` keeps the stack in `lightboxes`. Layer `n` draws at `LAYER_Z + LAYER_STEP * n`, so an upper layer's items and frame stay on top of the one below. `GroupOrder` is the order of the slot. A click keeps the cells from the read taken before the place. A later change to that group's recipes opens the popup again.

Which recipes share a slot is decided by `RecipeGroup.key`, not here. The key is the result item, so one item is one slot. With `groupRecipes` on, a shared group string also joins recipes that make different items. The key does not use vanilla's recipe-book tabs. A recipe with no result stays its own slot.
