# Locked ghost

A recipe absent from the player's recipe book still gets a ghost in the grid. The option "Reveal all recipes" is what lists those recipes. The dev All button writes that option. The same client draws the ghost on a server that does not have this mod.

## What the server does

`ServerPlaceRecipe.recipeClicked` returns immediately unless `RecipeBook.contains` is true. It moves no items, and it sends no `ClientboundPlaceGhostRecipePacket`. The check is vanilla. It runs whether or not this mod is on that server.

The packet's client half is `RecipeBookComponent.setupGhostRecipe`. It stores the result and the ingredients on the vanilla book's `GhostRecipe`, and `PlaceRecipe` picks the slot positions. The crafting screen draws that ghost while this mod keeps the book hidden.

`LockedGhost` calls `setupGhostRecipe` and leaves the place packet unsent. The caller leaves that ghost in place when it is already this recipe: clearing it and asking again flickers, and `setupGhostRecipe` appends another copy. Scroll down clears the same ghost.

Before the ghost is drawn, `GridReturn.takeAll` shift-clicks each crafting slot back into the inventory. That is the client packet for what `ServerPlaceRecipe.clearGrid` does on the server, so it works when this mod is not installed there. The result slot stays, and the server clears it once the ingredients leave. When the inventory cannot take every stack, nothing is moved and the ghost is not drawn.
