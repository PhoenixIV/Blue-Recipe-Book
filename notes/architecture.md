# Architecture

Where code lives, and the shape of a category. A decision about that shape belongs here, including one the code does not have yet. A package that does not exist yet is marked later.

What the book does on screen belongs in the note for that topic, and in `notes/checks.md` when a change should be checked. The public roadmap stays in the README.

`common` holds the book. `fabric` and `neoforge` only load it. Architectury is the dependency for anything both loaders must share, including packets later. Do not copy Architectury in.

Viewer calls stay in `common`, behind a mod-present check. A class that names JEI or EMI loads only after that check, so a dedicated server and a client without those mods still start. What a viewer supplies, and what this book keeps for itself: `notes/viewer-dependency.md`.

Fabric's mod file is client-only today. The book belongs on both sides once unlocks, bookmarks, or a custom place-recipe packet exist. Until then, leave it client-only. The server then stores unlocks, bookmarks, and the category list, and checks any custom packet. Until that half exists, unlock order, strict discovery, and held items stay in the client files: `notes/player-data.md`.

## Categories

A category is an id, a name, an icon, and a membership rule. The rule is data: a recipe type, a namespace, a tag, or a list of recipe ids. It has to be saved and, later, synced. A Java predicate cannot be saved or synced.

The starting set is the vanilla crafting groups: equipment, building blocks, miscellaneous, redstone. They are ordinary categories, so they can be renamed, reordered, or replaced. Search is a text filter and a compass tab. It is not a category. Each tab wraps one of Minecraft's `CraftingBookCategory` values, and a recipe sits on the tab that matches its vanilla crafting category.

Viewer categories, one per machine, can seed a category. They are not the tabs. A Create pack gets a category the player defined, not a tab for every mixer and press.

## Packages

```
dev.bluerecipebook
  BlueRecipeBook                 mod id
  client                         entry point, settings, session memory, DevBooks
    screen                       button, attachment
      mixin                      mixins only; ordinary classes cannot live here
    book                         tabs, result grid, selected recipe
    category                     rules, the default groups
    index                        synced recipes, without special and incomplete ones
    search                       display name today; id, namespace, tags, aliases later
    view                         JEI today, EMI later; each loaded only after a mod-present check
    discover                     unlock order and the files that store it
    strict                       held items and strict discovery
    data                         brings the player-data folder up to this jar's format
    dev                          feed; registered only with the dev property
  network                        later
  server                         later
```

Each screen mixin calls one attachment.

The result grid reads `RecipeIndex`, not the vanilla book's collections. The known check reads the player's `RecipeBook`. Rebuild the index when recipes reload. A viewer layout is cached from recipe id to picture at that same time, not looked up by walking every category when the inventory opens.
