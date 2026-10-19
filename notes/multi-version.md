# Maintaining several Minecraft versions

Do not open this note while writing a 1.21.1 feature. It is for the port.

Checked against the Minecraft 26.3 client jar, the release from 2026-09-15. 26.4 was a snapshot that day. This is the part of a port that is not a rename the compiler will list. The 1.21.1 decompile this mod compiles against is `references/sources/minecraft-1.21.1-neoforge`.

## What the client has

`ClientboundUpdateRecipesPacket` carries recipe property sets and the stonecutter list. It does not carry `RecipeHolder`s. `RecipeHolder` still exists. Its id is a `ResourceKey`, and `ResourceKey.identifier()` is an `Identifier`. The `ResourceLocation` class is gone.

The vanilla book on the client is `ClientRecipeBook`. It adds a `RecipeDisplayEntry` and removes a `RecipeDisplayId`. That id is an int for the session. `handlePlaceRecipe`, `ServerboundPlaceRecipePacket`, and the ghost packet use the int or a `RecipeDisplay`. The unlock-add entry is a display entry. The remove packet sends display ids. `stats.RecipeBook` has no `contains`. Known recipes are the display entries.

`Recipe.getIngredients()`, `isIncomplete()`, `getGroup()`, and `getResultItem` are gone. Ingredients are `placementInfo().ingredients()`. The group string is `group()`. A display entry's group is an `OptionalInt`, not that string. `isSpecial()` remains. `assemble` takes the input and no registry lookup. `Ingredient.getItems()` is `items()`.

`GuiGraphics` is `GuiGraphicsExtractor`, and screen `render` is `extractRenderState`. `RecipeBookComponent` remains. `visible` is still the field, `setVisible` is protected and still writes the saved open state and calls `initVisuals`, and `tick` still writes that saved state back. The ghost type is `GhostSlots`. Filling it is `fillGhostRecipe(RecipeDisplay)`, not `setupGhostRecipe`.

`PotionBrewing` is gone. Brewing is `BrewingRecipe`, a normal recipe. `CraftingBookCategory` still has building, redstone, equipment, and misc.

`ClientPacketListener.recipes()` returns `RecipeAccess`, which is property sets and stonecutter recipes. It is not `getRecipes()`.

## What to keep stable

The registry id string is the id this book saves. A display id does not outlive the session, so discovery, bookmarks, and placement stay on the registry id.

These reads already sit in one place. New code uses that place:

- The recipe list: `RecipeIndex`
- Unlock order: `DiscoveryOrder`, filled from the packet mixin
- Ingredients: `FoundIngredients`
- The group string: `RecipeGroup`
- The result stack, the vanilla known-check, placing, and the ghost: `BlueBook` and `LockedGhost`

A second call to `RecipeManager`, `getIngredients`, `getGroup`, `getResultItem`, `RecipeBook.contains`, or `handlePlaceRecipe` is another place the port has to edit.

The page can keep holding `RecipeHolder` on 1.21.1. The 26.3 client book does not list holders, so a wrapper invented before the port would be a guess.

## Unlock packet

On 1.21.1, `ClientPacketListenerMixin` injects at the return of `handleAddOrRemoveRecipes(ClientboundRecipePacket)`. `getState()` is `INIT`, `ADD`, or `REMOVE`. `INIT` is ignored. `ADD` calls `DiscoveryOrder.onUnlocked`. `REMOVE` calls `DiscoveryOrder.onRemoved`. The method is `handleAddOrRemoveRecipes`, not `handleRecipe`. Mixin does not check an `@Inject` string at compile time, so a wrong name crashes while the client listener loads. The log line "No refMap loaded" is normal here: NeoForge runs official names, and there is no refmap to translate a string.

`handleAddOrRemoveRecipes` and `ClientboundRecipePacket` are absent from 1.21.2. The replacement is present through 1.21.11:

- `handleRecipeBookAdd(ClientboundRecipeBookAddPacket)`. The packet has `entries` and a `replace` flag. `replace` is the `INIT` case: do not append. Otherwise append, in the packet's order. Each entry holds the recipe id. `getRecipes()` is not on this packet.
- `handleRecipeBookRemove(ClientboundRecipeBookRemovePacket)`. Drop those ids.
- `ClientboundRecipeBookSettingsPacket` is not discovery order.

The screen mixins (`init`, `render`, `tick`) are not this packet. 1.21.2 also changes the recipe objects themselves, so the port is more than this mixin. Re-check that the client recipe manager still receives the full list before assuming the book does.

Nothing needs to be depended on for this split. Architectury only shares Fabric and NeoForge on one Minecraft version; 13.0.11 is the 1.21.1 line. Stonecutter is a Gradle preprocessor that compiles one tree into a jar per version. It does not make `handleAddOrRemoveRecipes` exist later, and it is worth its layout cost only once two versions are actually shipping. TaterLib (server plugins, unused), ModernNetworking (a mod's own packets, still one artifact per version band), the ViaVersion recipe fix (keeps old clients from crashing), and PacketLib (a generated class per protocol version) do not read both unlock protocols.

When the port happens, two small readers call `DiscoveryOrder.onUnlocked` and `DiscoveryOrder.onRemoved`: one reads the 1.21.1 packet, and the other reads the add and remove packets above.
