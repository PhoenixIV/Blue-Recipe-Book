# Recipe order

## Vanilla

The vanilla book does not sort slots by item name, item id, or creative-tab order. There is no comparator on the page.

On login the server sends `RecipeManager.getOrderedRecipes()`. While loading datapacks, recipe JSON is put into a `HashMap`, and `getOrderedRecipes()` walks that map grouped by recipe type: every crafting recipe in the order it was encountered, then the next type, and so on. `ClientRecipeBook.categorizeAndGroupRecipes` walks that list once. It skips special and incomplete recipes, chooses the tab from the crafting category, and appends a slot:

- An empty `group` string is its own slot.
- A non-empty `group`, in the same tab, joins the slot that already has that group. The slot stays where the first recipe of the group appeared. Later members do not take another cell.

That is why the plank recipes are one icon. They share `"group": "planks"`. The icon cycles through the group in that same encounter order, one step every 30 ticks. Two recipes with the same output and no shared group stay separate.

The page then drops slots without reordering what remains: the recipe does not fit the grid, none of its recipes are unlocked, the search text misses it, or the craftable-only toggle is on and nothing in the slot can be crafted. Remaining slots close up.

The search tab is four tabs concatenated, in the same order as the tab strip after search: equipment, building blocks, miscellaneous, redstone.

## This book

The page sorts by discovery, newest first, or with craftable recipes in front. The button in the top right picks which, and it does not filter anything. It uses vanilla's filter sprites at the same spot. On puts a slot with a craftable member ahead of the rest, and each side keeps newest first. That choice is `craftableFirst` in the config, on by default, and the settings screen leaves it out. Recipes from the same unlock packet stay in the order that packet listed them. Recipes this client never saw unlocked stay in recipe-manager order, behind the ones it did. The list rebuilds while the book is open, so a craft can move a slot. An open lightbox stays, and the page follows that slot. A recipe added to that group, or taken out of it, opens the popup again.

The page folds that list into slots. Recipes that make the same item share a slot, grouped or not. `groupRecipes` is on by default, and then a non-empty `group` also joins other recipes with that string, even when they make different items. The key does not use vanilla's recipe-book tabs. Search lists every crafting recipe that fits this station, so a group vanilla would split across tabs is one slot. A category tab still lists only that tab's recipes. A slot with several recipes opens [Lightbox](lightbox.md). The slot's rank is its newest member. Ties stay in the order the first member appears in the recipe manager. Recipes a mod never grouped, and what the other books do with that, are [Missing groups](missing-groups.md).

How the slot icon cycles, and what a click places, is [Group icon](group-icon.md). Ctrl holds that cycle still: [Ctrl in the vanilla book](ctrl-in-vanilla-book.md).

With the option off, a group string does not join recipes that make different items. Recipes that make the same item still share a slot.

The cycle is the vanilla signal for a click that crafts the item on show. Not built, three other displays are planned: the newest result with the count of the group, the newest result in front of the one before it and no spin, or the newest result with a blue corner so the frame is not the vanilla many-recipe button.

Vanilla cannot supply the time. `ServerRecipeBook` saves two sets in the player NBT: `recipes` (known ids) and `toBeDisplayed` (ids still highlighted). The highlight set is cleared once the recipe has been on screen. Neither set is ordered by when the recipe was learned. JEI does not record it either. Its history is the screen you came back from, and its bookmarks are pins.

The mod records the order on the client. The file does not store a clock time. It is one recipe id per line, oldest first:

- Local save: `<world>/bluerecipebook/<player uuid>/unlock-order.txt`, in the world folder next to `playerdata` and `advancements`. Copying or deleting the world includes the folder.
- A remote world: `config/bluerecipebook/remote/`. This client cannot write that world's save. How an address, a LAN world, and a realm become a folder, and the `format.json` marker beside these files: [Player data](player-data.md).

On load, a recipe id the recipe manager does not have is removed and the file is written back. The same removal applies to the strict list, the toast list, and a held item id that is not in the item registry. A read that throws is logged, and that file is left unchanged until a later read works. Unlock, reset, and the inventory scan still update memory for the session, and they do not write the file. A missing file is an empty list, and a later save may create it. The recipe list has to have arrived first. An empty list is left untouched, and the check runs again once the list is there. Each file that loses ids logs one warning. The line names the mods and says the saved records are gone because that content is not loaded.

The login packet is `INIT`: the full known set, still unordered. It does not append ids, so rejoining the same world on the same client keeps the saved order. A `REMOVE` drops that line, and a later unlock is appended. The dev Reset button calls `DiscoveryOrder.clear()` in the same click as the take and revoke commands, so the file is empty before those packets come back. The dev Unlock button sends `recipe give` and `advancement grant` and leaves the file to those `ADD` packets, which append only ids the player did not already know.

The config file is the record for a remote server. That server's player data has no discovery times, and a client cannot write them there. Two machines do not share the file.

When this mod is installed on a dedicated server, the same jar stores the order in the player data and sends it to the client. That half is not built yet. The server half does not draw the book. It does not need a second mod, and no existing mod is a data locker worth depending on for this list. The config file stays the fallback for a server that does not have this mod.

## Strict discovery

`strictlyRequireEveryIngredient` is off by default. With it on, the page is still the unlocked set, and then a second check: every non-empty entry from `getIngredients()` has been satisfied by an item this client has held. `RecipeBook.contains` is unchanged. The dev "all" toggle still skips both checks. `refreshCraft` is still whether a recipe that is already on the page can be crafted from the open inventory.

Vanilla does not store items that have been in the inventory. Pickup, crafted, and used are separate statistics, and none of them is that set. The client keeps one item id per line in that player's folder: `<world>/bluerecipebook/<uuid>/held-items.txt` in singleplayer, or in that folder for a remote world. The set is recorded while the option is off. Loading it adds whatever is in the inventory, the armor slots, the offhand, or the cursor and is missing from the file. An item stays found after it leaves. The dev Reset button empties that file too. Items still in the inventory count again on the next read. A recipe output does not count until that item has been held.

A blank shaped slot is an ingredient whose `getItems()` is empty, and it is skipped. One held item covers every slot that accepts it. Any one item a tag lists covers that tag. Special and incomplete recipes are already out of `RecipeIndex`. This check does not rebuild ingredients they never published. A tipped arrow is that case: it matches eight arrows and a lingering potion, and `getIngredients()` is empty.

The unlock packet still appends to the discovery file when the recipe is granted. A recipe granted early would then sort as old on the day its last ingredient is held, so `strict-order.txt` is a second list, oldest first, in the order the filter would have revealed them. It is updated while the option is off. With the option on, the page sorts by this list. Recipes this client never saw in an unlock packet still sort behind it, in recipe-manager order.

With the option on, the unlock toast waits until the ingredients have been held. `waiting-toasts.txt` is the ids still waiting. The recipe packet and the advancement are not cancelled.

## Later Minecraft

Stay on 1.21.1 until this book can show a Create recipe through JEI and Create has a build on the next version. As of 2026-10-07, Create still supports 1.21.1, skipped 1.21.2 through 1.21.11, and is porting to 26.1. The rules above do not change. The packet that carries them does, and its reader and the 1.21.2 replacement are in [Maintaining several versions](multi-version.md).
