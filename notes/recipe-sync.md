# What the client has, and when that changes

On 1.21.1 the client's `RecipeManager` matches the server after login. That is not true for every Minecraft version, and brewing is in that list only from 26.3. Read this before targeting a game version other than 1.21.1.

[Viewer dependency](viewer-dependency.md) describes the 1.21.1 split: the book lists server recipes; a viewer draws layouts. The sentences there about the join packet and about JEI on the server are for 1.21.1 only.

## Two packets, not one gate

Join in 1.21.1 sends data, then unlocks:

1. `ClientboundUpdateRecipesPacket` with `RecipeManager.getOrderedRecipes()`. Full `RecipeHolder`s (id, serializer, ingredients, result). The client calls `RecipeManager.replaceRecipes`. Datapacks and content mods on the server are in this list. Encounter order of that list is [Recipe order](recipe-order.md).
2. `ClientboundRecipePacket` `INIT` with the player's known ids and highlights. `ADD` / `REMOVE` follow later. This is the vanilla book and `doLimitedCrafting`. It is not the recipe JSON.

JEI on 1.21.1 indexes the manager, not the known set, so it shows locked recipes. This book's discovery file tracks the unlock list. Layouts come from the viewer. A page that exists only in a viewer (info tab, composting, generated brewing) is not something the server will craft; [viewer dependency](viewer-dependency.md) already draws that line.

There is no vanilla flag that "allows JEI to see recipes."

## What 1.21.1 actually sends

`RecipeType` is crafting, smelting, blasting, smoking, campfire cooking, stonecutting, and smithing. Those holders are in the packet.

Not in the packet: brewing, anvil, grindstone, fuel, composting. JEI invents those on the client. Brewing is `PotionBrewing` on the level, filled by `PotionBrewing.bootstrap` (hardcoded vanilla mixes, then NeoForge `RegisterBrewingRecipesEvent`). Client and server each build their own table. The brewing stand ticks on the server, so a server-only mix still brews. The GUI's `mayPlace` uses the local table, so a mix the client does not know may not go in the slot, and JEI will not list it. A content mod on both sides registers the mix twice. Vanilla does not replicate the table.

## 1.21.2 through 26.2

Vanilla does not send `Recipe` objects. The client gets `RecipeDisplay` entries and `RecipePropertySet`s so the recipe book and slot checks still work. Recipe ids used in saved data stay on the server. Yarn: recipes are not used on the client; a display has a runtime `NetworkRecipeId` and does not include the recipe id.

Fabric's `RecipeSynchronization` (and NeoForge `neoforge:recipe_content`) exist because of this. A viewer that still needs holders must opt into those channels. Mezz: on 1.21.1 or lower, JEI can run client-only against a vanilla server (transfer and some cheats aside); from 1.21.2, install JEI and Fabric or NeoForge **on the server**.

Paper cannot load that jar. Plugins that "send server recipes to JEI" impersonate `fabric:recipe_sync` / `neoforge:recipe_content`. Do not pull them into `references/`. They are not APIs this mod should call.

Brewing is unchanged in this range: still `PotionBrewing` / `BrewingRecipeRegistry`, still not a `RecipeType`.

## 26.3

Potion brewing becomes datapack JSON, type `minecraft:brewing` (`input`, `reagent`, `output`). Vanilla mixes move into that format. The stand uses recipe property sets for what may go in a slot.

From here a server datapack **is** how brewing is altered, the same way as crafting. Those recipes still follow the 1.21.2 rule: objects stay on the server; the vanilla client gets displays and property sets. JEI listing a custom brewing recipe still needs loader sync (or a Paper bridge).

Official write-up: [26.3 Snapshot 3](https://www.minecraft.net/en-us/article/minecraft-26-3-snapshot-3).

## When this mod leaves 1.21.1

What 26.3 does to these packets, and which reads stay in one place: [Maintaining several versions](multi-version.md).

- Do not assume `ClientboundUpdateRecipesPacket` still carries `RecipeHolder`s. The 1.21.1 login in `PlayerList` sends holders.
- Do not treat a vanilla `RecipeDisplay` as a holder. On 26.3 the book entry's id is a session int, and the place packet sends that int. This book's saved id stays the registry id. Displays are pictures for the vanilla book.
- JEI layouts of server recipes are only as complete as JEI's sync. On 1.21.2+ that means JEI on a Fabric or NeoForge dedicated server, not "the viewer stays client-only." Keep viewer *calls* in the client source set; the *jar* on the server is JEI's problem, not a classpath we take.
- `synchronizeRecipeSerializer` (Fabric) / the NeoForge recipe-content equivalent is the durable API. Register vanilla serializers JEI already marks, not a blind scan of every serializer on the server.
- Brewing: if the target is before 26.3, it is not in the manager and not in any recipe packet. If it is 26.3 or later, it is a recipe type and the brewing stand is a recipe screen; the book button on that stand then has real ids to list.
- `/reload` on 1.21.1 resends holders. Later, it resends displays and, if a loader is present, the opt-in sync. Discovery order from an `INIT` of known ids does not start meaning timestamps.
- This book's own server half (unlocks, bookmarks, place-recipe) does not replace recipe-content sync. Do not add a second channel that copies `RecipeHolder`s if JEI already receives them.
