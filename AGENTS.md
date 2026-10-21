# Blue Recipe Book

An original progression recipe book for vanilla Minecraft & mods.

**What this is.** A Minecraft mod in Java, built with Gradle and Architectury Loom. It replaces the vanilla recipe book, with its own discovery, categories, and search. Target: Minecraft 1.21.1 first, then the current version. NeoForge first, Fabric next.

## Repo map
- `common/`: the mod. Most work happens here. How the book is laid out: `notes/architecture.md`.
- `neoforge/`, `fabric/`: loader entry points. Fabric is client-only and not the current target.
- `notes/`: knowledge base. Index: `notes/00_overview.md`. Open the index, then the one matching note by path. The articles stay out of the search index. When you add a note, add it to that overview.
- `references/`: gitignored source snapshots, for lookup only. Index: `references/OVERVIEW.md`. Missing? Run `py tools/pull_references.py` from the repo root.
- `build.cmd`, `gradle.properties`: build entry point and version pins.
- `notes/checks.md`: what the build does, written as what to check. It is not a list of requirements. Read it only when adding or changing a capability. When a change adds, removes, or alters a capability, update the matching line and name the lines to run.
- `README.md`: public project page and roadmap. Not needed for work.

## Terms
- **Vanilla book**: Minecraft's own green recipe book. This mod hides it.
- **Blue book**: this mod's recipe panel.
    - In ambiguous situations, make sure to clearly name the blue or the vanilla book. 
- **Discovery, unlock**: a recipe becomes visible once the player holds its ingredients or the unlock arrives. **Strict discovery** hides a recipe until every ingredient has been held.
- **Ghost**: the translucent recipe preview in a crafting grid. A locked recipe is drawn as a ghost.
- **Group**: recipes that share one page slot.
- **Group lightbox**: the popup / picker that opens when selecting a group. Has one cell per item.
- **Alternative recipes lightbox**: has one cell per way to craft the same item. One of those ways is an **alternative recipe**.
- **Category, tab**: the side tabs in the book. These are our own objects, not Minecraft's category enum. **Search** is a text filter with a compass tab, not a category.
- **Viewer**: a recipe viewer such as JEI, which draws a recipe layout. JEI is optional today, and the book works without it. JEI's role will grow, so do not treat today's integration as final.
- **Mixin**: patches vanilla classes at runtime. Used for the vanilla book and the screens.
- **Dev mode**: the JVM flag `-Dbluerecipebook.devToggle=true`, which adds development-only controls.

## Every session
- Build with `.\build.cmd`. `:neoforge:compileJava` does not update the jar the game loads. The jar is `neoforge/build/libs/bluerecipebook-neoforge-<mod_version>+mc<minecraft_version>.jar`.
- The user starts Minecraft and checks the behavior. The agent does not start the game, and no game launcher is installed by this project.
- Before a mechanic change, open the matching note. Before reading an API, open `references/OVERVIEW.md`.
- The game is 1.21.1. A later version is planned and out of scope. Do not open `notes/multi-version.md` while writing 1.21.1 code. A rename the compiler will list is cheaper to apply then.
- Ask when a decision is not written down. Do not search old chats.

**Lookup log.** When finding an answer took two or more searches, or a wrong place first, append one line to `local/agent-lookup-log.md`: what you needed to know, and where it was. Do not read the log at session start. When you append, read it for a pattern, and act on one that keeps costing searches. The log's header has the rules.

## Saved files

A file this mod wrote must still load in a later version. `PlayerDataUpdater` migrates the player-data folder when its layout changes.

## Reference code

`references/sources/` is for lookup. Search it only with a purpose: a named API, class, or behavior this mod needs to match. Open `references/OVERVIEW.md` first to pick the project. If it is missing, run `py tools/pull_references.py`; the script writes it. Workspace search skips `references/` even when the path is set. Use a shell `rg` there. Do not vendor it, and do not fork those repositories into this one.

- Minecraft classes for 1.21.1: `references/sources/minecraft-1.21.1-neoforge/net/minecraft/...`. Open the class by its name. Vanilla data and assets: `references/sources/minecraft-1.21.1-vanilla/`. That tree is Loom's decompile of the NeoForge-mapped game. `py tools/pull_references.py` builds it when the Minecraft or NeoForge version changes, and leaves it alone otherwise. Do not disassemble the class jar.
- Named projects:
    - Just Enough Items / JEI: `references/sources/jei`
    - Create, the test mod: `references/sources/create`
    - YetAnotherConfigLib / YACL, the config screen: `references/sources/yacl`
- The other snapshots are listed in `references/OVERVIEW.md`
- GPL: some snapshots are GPL-3.0. Read them for behavior and copy nothing from them. The license line in `references/OVERVIEW.md` says which.
- LGPL: Architectury, Polymorph, and YetAnotherConfigLib are dependencies when this mod needs that behavior. Do not copy them in.
- Do not copy textures from other mods.
