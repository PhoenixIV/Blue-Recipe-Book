# Prototyping the book

The book is only visible inside a running client. Looking at it somewhere smaller is possible.

The part worth seeing early is layout: the panel, the tabs, the grid, the lightbox, and the book beside an inventory. 100% matching Minecraft's GUI is out of scope. Something that looks off can be corrected by hand.

If a prototype runs this mod's behavior, that behavior has to be the sources in `common`. A second book, written again for the prototype, drifts. Stand-ins for Minecraft are fine. They simulate only what the book calls, and they do not have to track vanilla. They grow when the book calls a method they do not have yet.

## A visual page

An HTML page can assemble the panel from the vanilla book texture and a few item images. Another tab, another page, a lightbox, a second lightbox, and a narrow window are states of that one page. A set of still images is the same scene drawn again, and the positions drift between files.

The page does not compile `BlueBook`. Search, grouping, and a slot lighting up because an item was added are whatever the page itself says.

## A jar of the real book

The other route compiles the book against stand-in classes that use Minecraft's names. Those classes exist for the prototype only. They stay out of the mod jar. Grouping, search, paging, and the lightboxes are the real methods. A stand-in player and a simple check, such as "the inventory holds these item ids," are enough to put an item in and watch the book update. The drawing can be a list of positions that a window paints, so it stays approximate.

This is a second program. The book reaches settings, discovery, the recipe index, and the config libraries those use, so the first compile is a pile of stand-ins, most of them empty shapes. The ones with behavior are the player, the inventory, an item stack, a recipe, the search box, and graphics. After that, a new Minecraft call fails the prototype build until a stand-in learns it.

TeaVM is a possible later step, not a requirement. The jar runs on a normal JVM without it. TeaVM compiles those same class files to a script the browser loads by itself. There is no server and no JVM behind the page, so the script can sit on any site that can load a file. The window stays a desktop-only class, and the browser build paints the same list of positions to a canvas. That stays a small step when the shared classes stay off Swing, files, and reflection.

## Transpilers

A Java-to-JavaScript tool does not bring Minecraft with it. It translates the calls. The stand-ins are still the work.

JSweet is the kind that wants the sources written against its own libraries. That is a rewrite of the book, the same cost as writing the prototype in JavaScript directly.
