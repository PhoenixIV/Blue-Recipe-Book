# How do other mods make recipe lookup fast, and what is slow in vanilla?

JEI, EMI, and REI build an inverted index once, when recipes reload. A lookup after that is a hash-map get. Vanilla stores recipes by type and id, and finds a match by walking those lists.

## The index

At reload each viewer walks every recipe, expands each ingredient into concrete stacks (an `#c:ingots/iron` tag becomes one entry per item in the tag), and stores:

- input stack → recipes that consume it ("uses")
- output stack → recipes that produce it ("recipes")

The keys are a recipe-relevant id, not object identity.

- JEI: `RecipeMap` links ingredients to recipes through `RecipeIngredientTable` and `IngredientUidIndex`. The uid comes from `getUid(..., UidContext.Recipe)`.
- EMI: `EmiRecipes.Manager` bakes `byInput` and `byOutput`, flattening every input with `getEmiStacks()`. The bake runs on a background thread. Stacks use `EmiStackList.ComparisonHashStrategy`.
- REI: `DisplayCacheImpl` fills `displaysByInput` and `displaysByOutput` ("for optimal lookup performance") and hashes with `EntryStacks.hashFuzzy`.

A book that draws layouts from JEI keeps no index of its own. JEI already has this map.

The cost is paid up front: memory proportional to ingredients across all recipes, and a reload hitch. The maps stay valid until the next reload.

## What vanilla does instead

`RecipeManager` stores recipes by type, then by recipe id. Fetching a recipe by id is fast. Finding which recipe fits a grid, furnace slot, or brewing stand is not: `getRecipeFor` / `getFirstMatch` walks every recipe of that type and calls `matches()` until one accepts the input. The only shortcut is the last successful recipe id. The first craft, or any input that no longer matches that id, scans the whole type again. A machine that asks every tick without holding that id repeats the scan.

There is no input index, so "what uses this item?" does not exist in vanilla. The recipe book only groups recipes that share an output into a `RecipeCollection`, and only for its own categories, after a full pass at reload. Highlighting what you can craft tests each visible collection against a count of your items (`StackedContents`). That is cheaper than placing items into a grid, and it is still proportional to how many recipes the open category shows.

## Do those tables answer "what can I craft from my inventory right now?"

No. The tables are keyed by one item. "What can I craft from the inventory I have right now?" changes whenever the inventory changes, so that result is recomputed, not stored.

EMI uses the input table as a candidate filter. `EmiPlayerInventory.getCraftables` takes each stack you are holding, reads `byInput` for that stack, and unions those recipes. A recipe that consumes nothing you hold never enters the set. What remains is still checked for counts: one iron ingot does not mean a hopper is craftable.

REI goes the other way. Its craftable filter looks up recipes by output, checks those inputs against a count map of the open inventory, and caches the yes or no until that map changes.

Vanilla's recipe book answers the same question by testing recipe collections against `StackedContents`. It has no item-to-recipe table, so it cannot skip straight to the recipes that touch something you hold.
