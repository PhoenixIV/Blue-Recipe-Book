# Vanilla book

How the blue book shares a screen with Minecraft's recipe book. The mod hides vanilla's widgets, draws over them, and takes input first. What the blue panel itself does: [Blue book](blue-book.md).

## Open state

The blue book is open when `RecipeBook.isOpen(RecipeBookType.CRAFTING)` is true. The inventory and the crafting table share that flag. The server stores it on the player. A recipe packet replaces the whole settings object, so the client follows the server's copy.

Opening or closing the blue book calls vanilla `setVisible` through the invoker `blueRecipeBook$applyOpen`. That writes the flag and sends `ServerboundRecipeBookChangeSettingsPacket`. The tab, the page, and the search text are not part of that flag. With "Open where you left off" on, they stay in this client's session. Off, the next screen opens on the search tab, the first page, and an empty search.

## Hiding

The vanilla widgets stay hidden. `setVisible` also sets the `visible` field, so after that call, and on each layout while the blue book is showing, the accessor `blueRecipeBook$setVisible` writes the field false and leaves the saved flag alone. Naming the accessor `setVisible` would make Mixin replace the vanilla method.

`setVisible(true)` calls `initVisuals()`, which creates the search box. `init()` skips `initVisuals()` when the book starts closed. Rendering with `visible` true and a null search box crashes. The field is set true only when the dev controls show the vanilla book, and only after `initVisuals` if the search box is still null. A furnace, smoker, or blast furnace is not a screen this mod owns, and each keeps its own open flag.

`tick()` writes the saved flag back into `visible`, which would show the vanilla book. Cancel it only on screens this mod owns. While that tick is cancelled, the blue book reads the saved flag on its own tick, so a recipe packet that changes the flag still opens or closes the panel and moves the screen.

## Ghosts

Ghost ingredients still draw when the book is hidden. Clear the ghost, then call `handlePlaceRecipe`. Do not cancel ghost rendering. Unlocks and placement stay in the vanilla book. Locked recipes: [Locked ghost](locked-ghost.md).

## Keys and focus

Escape and Ctrl+F are handled at the start of the screen's key press, before the vanilla screen sees them. Escape closes the top lightbox first, or the narrow book. Otherwise it falls through to the screen, which closes.

Ctrl+F focuses the search box in `layout()`, after `screen.setFocused`. A screen blurs the current widget and then focuses the target, even when both are the blue book, so a focus set earlier would be undone. `BlueBook.setFocused(false)` must leave the search box alone. The vanilla search box still draws its selection while unfocused, so a dropped focus looks selected and the next character does nothing. Why there is no global Ctrl+F: [No global Ctrl+F](no-global-ctrl-f.md).

## Tabs

Vanilla shows a side tab when some known recipe in its category fits the grid. `RecipeBookTabButton.updateVisibility` reads `hasKnownRecipes` and `hasFitting`, and a recipe counts as fitting when `canCraftInDimensions` holds for the grid size. The blue book's category tabs use that same test on the recipes it has unlocked for the station. Two settings make the blue book differ from vanilla. Strict discovery can hide a tab that vanilla shows, because it hides a recipe until its ingredients are held. Reveal-all lists every recipe, so it can show a tab that vanilla would hide. The search tab is always shown, as in vanilla.

## Clicks

The screen's `mouseClicked` injection runs `BookHooks.interceptClick` before vanilla handles the click. A click that the injection passes on reaches the blue book widget, which is registered as an event widget. The widget checks the lightboxes again, and that check finds nothing, because the injection has already dismissed them. Then the widget handles the click on its page.

## Hover and the wheel

Vanilla's recipe-book component always reports that the mouse is not over it. The blue book therefore takes the mouse wheel. While a lightbox covers the mouse, or the narrow book is open, the `isHovering` hook makes vanilla's slot hover return false. What the wheel does: [Blue book](blue-book.md#size-and-input).
