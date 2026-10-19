# Notes

Knowledge base for Blue Recipe Book, and a resource agents may read when a topic is relevant. One topic per file, written as the answer to come back to. This overview is the indexed map. The articles stay out of the search index, so open one by path.

Each note sits in one group. The group says how to use it. Images for a topic go in `images/` and are linked from that topic.

## Behavior

What the build does. A note here changes together with `checks.md` when the behavior changes.

- [Recipe order](recipe-order.md) — vanilla groups slots by encounter order; this book sorts unlocked recipes newest first
- [Player data](player-data.md) — per-player files in the local save, and under `remote/` for a server, a LAN world, or a realm
- [Lightbox](lightbox.md) — a group lightbox has one cell per item; an alternative recipes lightbox has one cell per way to craft the same item
- [Group icon](group-icon.md) — the front icon walks the craftable members, and a click places the one showing
- [Locked ghost](locked-ghost.md) — a recipe the player has not unlocked is drawn as a ghost on this client
- [Blue book](blue-book.md) — the blue panel's search, tabs, wheel, sizing, and button
- [Vanilla book](vanilla-book.md) — how the blue book shares a screen with vanilla's recipe book: hiding, ghosts, focus, and clicks
- [Ctrl in the vanilla book](ctrl-in-vanilla-book.md) — Ctrl freezes the cycling recipe icon; this book's Ctrl+click opens that result in JEI
- [Development mode](dev-mode.md) — the JVM argument that shows the swap button and the other development controls
- [Checks](checks.md) — what the build does, written as what to check, in the game or in the files. Game tests never open this screen
- [Everyday checks](checks-everyday.md) — the short pass after a change. The full record is Checks

## Decisions

Follow these when adding a package, category, or viewer, even where the code does not have that shape yet.

- [Architecture](architecture.md) — where code lives, and the shape of a category
- [Viewer dependency](viewer-dependency.md) — the book runs without a viewer; JEI draws layouts, EMI supplies the tree, and REI waits
- [Missing groups](missing-groups.md) — a recipe with no group stays its own slot; a list we own can supply the string, and no other book invents one
- [No global Ctrl+F](no-global-ctrl-f.md) — Ctrl+F works only on screens this mod owns, because from the world it is the sprint and swap-offhand chord

## Background

Lookup only, not a contract.

- [Recipe lookup](recipe-lookup.md) — item-to-recipe indexes in JEI, EMI, and REI, and what vanilla scans instead
- [Recipe sync](recipe-sync.md) — 1.21.1 sends holders on join; 1.21.2 keeps recipes on the server; brewing joins that list in 26.3
- [Maintaining several versions](multi-version.md) — for the port; leave unread while writing a 1.21.1 feature
- [Prototyping the book](prototyping.md) — looking at the book outside the game: a visual HTML page, or a jar compiled against stand-in classes; TeaVM is a possible later step
- [Development loop](dev-loop.md) — the agent compiles, the user launches, and game tests never open this screen
