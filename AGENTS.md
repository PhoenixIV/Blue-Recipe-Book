# Blue Recipe Book

An original progression recipe book for Minecraft mods. This is not a fork. Read other mods for behavior. Do not copy them into this tree.

## Start here

Read this section every session. The sections below are rules, to open when their topic comes up.

**What this is.** A Minecraft mod in Java, built with Gradle and Architectury Loom. It adds a second recipe book beside the vanilla one, with its own discovery, categories, and search. Target: Minecraft 1.21.1 first, then the current version. NeoForge first, Fabric next. It is in alpha and being prepared for public release on GitHub.

**Repo map.**
- `common/`: the mod. Most work happens here. How the book is laid out: `notes/architecture.md`.
- `neoforge/`, `fabric/`: loader entry points. Fabric is client-only and not the current target.
- `notes/`: knowledge base. Index: `notes/00_overview.md`. Open the index, then the one matching note by path. The articles stay out of the search index. When you add a note, add it to that overview.
- `references/`: gitignored source snapshots, for lookup only. Index: `references/OVERVIEW.md`. Missing? Run `py tools/pull_references.py` from the repo root.
- `build.cmd`, `gradle.properties`: build entry point and version pins.
- `notes/checks.md`: what the build does, written as what to check. It is not a list of requirements. Read it only when adding or changing a capability. When a change adds, removes, or alters a capability, update the matching line and name the lines to run.
- `README.md`: public project page and roadmap. Not needed for work.

**Terms.**
- **Vanilla book**: Minecraft's own green recipe book. This mod hides it, never removes it.
- **Blue book**: this mod's recipe panel.
- **Discovery, unlock**: a recipe becomes visible once the player holds its ingredients or the unlock arrives. **Strict discovery** hides a recipe until every ingredient has been held.
- **Ghost**: the translucent recipe preview in a crafting grid. A locked recipe is drawn as a ghost.
- **Group**: recipes that share one page slot.
- **Group lightbox**: the popup / picker that opens when selecting a group. Has one cell per item.
- **Alternative recipes lightbox**: has one cell per way to craft the same item. One of those ways is an **alternative recipe**.
- **Category, tab**: the side tabs in the book. These are our own objects, not Minecraft's category enum. **Search** is a text filter with a compass tab, not a category.
- **Viewer**: a recipe viewer such as JEI, which draws a recipe layout. JEI is optional today, and the book works without it. JEI's role will grow, so do not treat today's integration as final.
- **Mixin**: patches vanilla classes at runtime. Used for the vanilla book and the screens.
- **Dev mode**: the JVM flag `-Dbluerecipebook.devToggle=true`, which adds development-only controls.

**Every session.**
- Build with `.\build.cmd`. `:neoforge:compileJava` does not update the jar the game loads. The jar is `neoforge/build/libs/bluerecipebook-neoforge-0.1.0.jar`.
- The user starts Minecraft and checks the behavior. The agent does not start the game, and no game launcher is installed by this project.
- Before a mechanic change, open the matching note. Before reading an API, open `references/OVERVIEW.md`.
- The game is 1.21.1. A later version is planned and out of scope. Do not open `notes/multi-version.md` while writing 1.21.1 code. A rename the compiler will list is cheaper to apply then.
- Ask when a decision is not written down. Do not search old chats.

**Lookup log.** When finding an answer took two or more searches, or a wrong place first, append one line to `local/agent-lookup-log.md`: what you needed to know, and where it was. Do not read the log at session start. When you append, read it for a pattern, and act on one that keeps costing searches. The log's header has the rules.

## Saved files

A file this mod wrote still loads in a later version. `PlayerDataUpdater` migrates the player-data folder when its layout changes.

## Code placement

When a change adds a read of the recipe list, the unlock packet, ingredients, the group string, the result, the vanilla known-check, placement, or the ghost, put that read in the helper that already has it: `RecipeIndex`, `DiscoveryOrder`, `FoundIngredients`, `RecipeGroup`, `StrictDiscovery`, `BlueBook`, or `LockedGhost`. The id this book saves is `RecipeHolder.id()`. That choice does not need the later sources.

Discovery order, the file layout, the strict order, and the held-items list are in `notes/recipe-order.md`. When this mod is on the server, that same jar stores the order in the player data and sends it to the client. Do not add a second mod for this, and do not depend on another mod as a data locker. The client file remains the record only for a server that does not have this mod.

## Vanilla book

Mechanics, with the reasons: `notes/vanilla-book.md`.

- Hide the vanilla book by setting its `visible` field. Never call `setVisible` or `toggleVisibility` on it. Those write the saved open state and send a packet.
- The accessor that writes `visible` is `blueRecipeBook$setVisible`. Naming it `setVisible` makes Mixin replace the vanilla method, which creates the search box.
- Cancel `tick()` only on screens this mod owns. `tick()` otherwise restores the saved state.
- Do not cancel ghost rendering. Clear the ghost, then call `handlePlaceRecipe`. Unlocks and placement stay in the vanilla book.
- Escape and Ctrl+F are handled at the start of the screen key press. Ctrl+F focuses the search box in `layout()`, after `screen.setFocused`.
- `BlueBook.setFocused(false)` must leave the search box alone.

## Dev controls

- Development controls exist only when the client run sets `-Dbluerecipebook.devToggle=true`. A built jar does not set it, and putting the jar in `mods` does not turn it on. The Gradle task `:neoforge:runClient` sets it. Other launches need it on the Java command.
- With the flag, a corner button swaps the vanilla book and the blue book on the inventory and the crafting table. Both stay loaded. Further buttons on that row exist only in this mode. Testing the blue book does not need the flag. Testing the swap does. Release behavior must not depend on it.
- Any later control that exists only to make development easier follows the same rule: a system property the client run sets and a built jar does not. When the property is absent, do not register that control.
- What the mode shows: `notes/dev-mode.md`. The launch loop, hotswap, and why game tests do not cover this screen: `notes/dev-loop.md`.

## Build

Minecraft 1.21.1. Architectury 13.0.11, Architectury Loom 1.11.458, Gradle 9.5. Compile with Java release 21. Do not add a Foojay toolchain plugin.

The neoforge project must have `loom.platform` set to `neoforge` before the Loom plugin is applied. The root `subprojects` block applies Loom, so the property is set there. The dependency is `neoForge "net.neoforged:neoforge:..."`. Without that property the configuration does not exist, and the plain Maven coordinate is a POM with no `@Mod` class.

Client run JVM flags use `vmArg "-D..."`. `systemProperty` is not a method on the run config. NeoForge jars remap to the named namespace.

## Reference code

`references/sources/` is for lookup. Search it only with a purpose: a named API, class, or behavior this mod needs to match. Open `references/OVERVIEW.md` first to pick the project. If it is missing, run `py tools/pull_references.py`; the script writes it. Workspace search skips `references/` even when the path is set. Use a shell `rg` there. Do not vendor it, and do not fork those repositories into this one.

- Minecraft classes for 1.21.1: `references/sources/minecraft-1.21.1-neoforge/net/minecraft/...`. Open the class by its name. Vanilla data and assets: `references/sources/minecraft-1.21.1-vanilla/`. That tree is Loom's decompile of the NeoForge-mapped game. `py tools/pull_references.py` builds it when the Minecraft or NeoForge version changes, and leaves it alone otherwise. Do not disassemble the class jar.
- Named projects: JEI is `references/sources/jei`. Create, the test mod, is `references/sources/create`. YetAnotherConfigLib is `references/sources/yacl`. The other snapshots are listed in `references/OVERVIEW.md`.
- GPL: some snapshots are GPL-3.0. Read them for behavior and copy nothing from them. The license line in `references/OVERVIEW.md` says which.
- LGPL: Architectury, Polymorph, and YetAnotherConfigLib are dependencies when this mod needs that behavior. Do not copy them in.
- Do not copy textures from other mods.
- The config screen is YetAnotherConfigLib 3.8.1. Do not survey another config library unless asked.
