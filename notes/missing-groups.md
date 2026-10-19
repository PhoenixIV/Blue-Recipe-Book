# Missing groups

A slot joins other recipes when they make the same item, and, with `groupRecipes` on, when they share a non-empty `group`. The key does not use vanilla's recipe-book tabs, so Search joins recipes those tabs would keep apart. A recipe that makes an item nothing else makes, with no group, is its own slot. How the page uses that key is [Recipe order](recipe-order.md), and the lightbox for several recipes is [Lightbox](lightbox.md).

Create's wooden windows are the example. `WindowGen` builds the shaped recipe and never calls `.group(...)`, so each wood is its own slot. Create does set `group` on some palette recipes, and the string is that block's own name, such as `cut_andesite_brick_slab`. Different stones still stay apart. A group of one looks the same as no group.

## Other books

No other book mod invents a group for different items the author left ungrouped.

Vanilla and most other book mods read `getGroup()`. Some can split a group that already has several recipes. They leave a missing group missing.

One book mod ignores `getGroup()` and keys the slot on the result item. Two recipes for one item share a slot. Oak window and birch window do not, because they are different items.

## A group we assign

The group string lives on the recipe. Vanilla reads it with `getGroup()`. This book reads it in `RecipeGroup.key`, and nowhere else. Returning a different string from that method changes this book's slots only. Vanilla never calls it.

Writing the field on the recipe is the other edit. `ShapedRecipe.group` is final and comes from the recipe JSON, and other recipe types keep the string on their own class. That write is what vanilla would display.

A list this mod owns, read from `RecipeGroup.key`, is the first edit. It can fill an empty `getGroup()`, or replace a name that is already set, such as `cut_andesite_brick_slab`. A result item id, or a recipe id, maps to a group name. The same name in one crafting category is one slot. Windows and window panes need different names, or they share an icon. The list does not notice a wood Create adds later.

That list is not implemented.
