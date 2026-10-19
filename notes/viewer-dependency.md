# Which recipe viewer to depend on

Decision on which recipe viewer to depend on. Snapshot dates are the ones in `references/OVERVIEW.md` (pulled 2026-10-07).

## Decision

Do not hard-require a recipe viewer. The book runs without one.

When a viewer is installed, use it from the client only:

- JEI is the first layout backend. Create on Minecraft 1.21.1 already registers its categories there, and `IRecipeLayoutDrawable` can be drawn inside the book. Some book mods do this and list JEI under `depends`, so the mod refuses to load without it. That hard dependency is the part to avoid.
- EMI is the second backend, and the only tree. Do not write a recipe tree. When EMI is installed and the open recipe has an `EmiRecipe` with `supportsRecipeTree()`, a button calls EMI's own tree screen.
- REI is not a first backend on 1.21.1. Its `16.x-1.21` branch in this checkout last moved on 2025-02-16. Later branches exist for newer Minecraft.

JEI, EMI, and REI are MIT. Depend on them. Do not copy them in, and do not fork them.

Polymorph (LGPL) is the dependency for choosing which output the crafting grid, furnace, or smithing table produces. A viewer does not do that. Create's machines are outside Polymorph's scan.

## What the book does itself

These do not need JEI, EMI, or REI:

- The list of real recipes. On 1.21.1 the server loads datapacks and sends that list to the client on login and on `/reload`. After that, the client's recipe manager matches the server. Vanilla crafting, smelting, and a Create processing recipe that is a registered recipe are all in that list. Later game versions do not send holders; [Recipe sync](recipe-sync.md).
- The button on the inventory, crafting table, furnace, and brewing stand, and filling those vanilla grids. Vanilla already has a place-recipe packet. The server checks the recipe id.
- Unlocks, bookmarks, and categories. Those are this mod's data. Until the server half exists, unlock order, strict discovery, and held items stay in the client files.
- Categories that can be rebuilt, not only filtered by mod. Vanilla groups are the starting set. A category is an id, a name, an icon, and a membership test. Viewer categories (one per machine) are a useful seed. They are not the format.
- Search terms this mod invents: item name, registry id, namespace, tags, and a small alias list. Tooltip search and mod-name search are indexes a viewer has already built; use those when a viewer is loaded.
- Showing that two recipes share the same ingredients. Changing which one the grid crafts is Polymorph.

## What a viewer is for

Mods register the picture of a modded station with a viewer: slot positions, fluids, chances, animated inputs, and the station item (the catalyst). The recipe object has inputs and an output. It does not know how Create draws a basin.

No other mod will register those pictures a second time with this book. An API of our own only covers mods we integrate by hand. Reading Create's classes covers Create and stops there.

A page with a server recipe id is something the server can craft. A page that exists only because a viewer plugin invented it (an information tab, mob drops, composting) can be shown, and a click must not pretend the server will perform it.

Trust the synced recipe manager. Conditions and datapacks have already been applied on the server.

## JEI, EMI, and REI

| | JEI | EMI | REI |
|---|---|---|---|
| Who expects it | NeoForge packs. "The recipe mod." | Players who switch for startup, search, and a quieter screen. Often the Fabric default. | Pack makers who want filters and hiding. |
| What we can reuse | Categories, catalysts, and a drawable layout. | The same, through `EmiRecipe` inputs, outputs, and widgets. Also the tree screen. | Categories and displays. The API is built around REI's own screen. |
| Create on 1.21.1 | Create registers its categories here. This is the test path. | Create also ships a native plugin. | Depends on a REI plugin that is still maintained. |
| Plugin coverage | Widest on NeoForge. | Wide. A mod that only wrote a JEI plugin shows up in EMI through EMI's JEI bridge, and that bridge waits until a real JEI runtime has loaded. EMI alone does not provide `IJeiRuntime`. | Follows whichever viewer mods still update. |
| This checkout | `1.21.1` at 2026-10-04. The wiki also covers 1.21.11 and Minecraft 26.x. | `1.21` at 2026-08-17. | `16.x-1.21` at 2025-02-16. |

Players pick one viewer. Two overlays on one screen is a support problem. If both JEI and EMI are installed, pick one backend at runtime, with a setting.

The review to avoid is "this recipe book forces my viewer." The book is the progression screen. JEI or EMI stays the lookup screen. Usage lookup, the full item list, and cheat-give stay with the viewer.

## Performance

The cost players feel is the viewer indexing every recipe when the world loads. All three do it. EMI has the reputation for the shortest startup and the fastest search. JEI is what large NeoForge packs ship. REI has the reputation for the heaviest index. These are reputations, not measurements.

Do not build a second index. Ask the viewer that is already loaded, then keep a map from recipe id to layout. Rebuild that map when recipes reload.

A lookup that walks every category and every recipe to find one id causes the hitch. The same would happen on EMI or REI. Resolve once at reload, not when the inventory opens.

While the book is closed, a viewer dependency adds nothing per frame beyond the viewer the player already chose.

## Client and server

On 1.21.1, installing a viewer on the server does not publish recipes. JEI's server half is cheat-mode permission and the code that moves items between slots during a transfer. The recipe list is the vanilla join packet. From 1.21.2 the list stays on the server and JEI must be on a Fabric or NeoForge dedicated server to sync holders; [Recipe sync](recipe-sync.md).

- A content mod on the server (Create, and so on) puts its recipes in the packet the client already receives. A client-only content mod's recipes are replaced by that packet on a dedicated server.
- The viewer belongs on the client.
- This book belongs on the client and the server. That is what syncs unlocks, bookmarks, and unlock rules, and what lets a custom place-recipe packet run. There is no `network` package until that step.
- The book on the client only can still list server recipes. Unlocks live on that client until the server half exists. Filling a vanilla grid can still use the vanilla packet when the id is a real server recipe. A custom packet fails if the server does not have the mod.
- The book on the server only has no screen.

Keep viewer calls in the client source set, behind a mod-present check, so a dedicated server does not need JEI or EMI on the classpath.

## Recipe tree

EMI is the only one of the three with a tree. It is a bill of materials: one goal item, one chosen recipe, then a child for each input, with amounts, chances, leftovers, and catalysts. The player picks which recipe produces an item when several do. Cycles are cut. The cost is "how many raw materials for N batches," then what the inventory already covers.

`MaterialTree` and `BoMScreen` are not a widget we can draw inside the book. We do not reimplement them.

We can open EMI's screen. EMI's own tree button does this, and a button in the book can do the same when EMI is installed:

1. Resolve an `EmiRecipe` with `EmiApi.getRecipeManager().getRecipe(id)`.
2. `BoM.setGoal(recipe)`. This method is public, and EMI's button uses it. It lives in `dev.emi.emi.bom`, not in `dev.emi.emi.api`, so an EMI update can move it. `viewRecipeTree()` does not take a recipe; the goal has to be set first.
3. `EmiApi.viewRecipeTree()`.

`viewRecipeTree()` opens only when the current screen is a container screen (or EMI's own recipe screen). The book on the inventory, crafting table, furnace, or brewing stand qualifies. Escape closes the tree back onto that same screen.

Show the button only when EMI is loaded, the lookup returns a recipe, and `supportsRecipeTree()` is true. Load the class that mentions `EmiApi` and `BoM` only after that mod-present check, so the book still starts when EMI is absent.

The tree stays client-side. EMI stores the player's recipe choices on the client. Sync those choices only if they should follow the player to another computer.

## Related choices

- A click on a Create recipe shows the layout. Putting items into a mixer is that mod's transfer handler, and only while that screen is open. The first screens are the vanilla ones.
- Creative, or an operator, needs an explicit rule for whether discovery is skipped. That is server state.
- A locked recipe in a tree, if we ever draw our own, would be a closed node. EMI's tree does not know about our unlocks. Using EMI's screen means EMI's rules: it shows the full craft chain.
