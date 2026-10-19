# Development loop

There is no slim Minecraft that mods run on. A recipe book needs the real client: registries, screens, and the inventory. What modders use instead are ways to skip the slow parts of that same client.

Stay in one running game when the change allows it. Press F3+T to reload textures, models, and language. That covers the dim blue button without a restart. `/reload` reloads recipes and datapacks. Java changes, mixins, and new buttons still need a new launch.

Hotswap small Java edits. Run the client from the IDE debugger and reload changed classes. Method-body tweaks in the book widget often apply. New classes, mixin edits, and anything that runs only at startup still need a restart. Mixin and the game's class loading are why a general hot-reload tool never caught on.

Make the launch land in the test. The Gradle client keeps its world, options, and login in a `run/` folder, so the second launch is the fast one. A run argument such as `--quickPlaySingleplayer <world>` skips the main menu and opens a saved test world. In that world, cheats and `/recipe give @s *` put you straight onto the book. Mods such as ModernFix or FerriteCore are sometimes added to that dev run only, to shorten startup. They are not a harness.

Game tests are the closest thing to an automated check, and they are a poor fit here. NeoForge and Fabric can boot a throwaway world and assert server behavior: a recipe matches, a block drops an item. That is still a full game boot, and it never opens the inventory screen. It will not show whether Ctrl+F focused the search box.

The fast loop is the one already set up. The agent compiles. You launch the client you are already logged into and land in a dev world. The blue book is testable without the dev flag. The swap, Unlock, Reset, Options, the Discovered/All button, and Feed need `-Dbluerecipebook.devToggle=true`, which `:neoforge:runClient` sets.

`build.cmd` builds the jar. It points Gradle at the user profile `.gradle`, because a sandboxed shell may set `GRADLE_USER_HOME` to an empty directory. Cursor and VS Code run that script as the default build task.
