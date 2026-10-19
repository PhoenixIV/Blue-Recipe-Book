# Development mode

Development mode is the JVM argument `-Dbluerecipebook.devToggle=true` on the process that starts the game. Putting the jar in `mods` does not turn it on. A built jar does not set it. The Gradle task `:neoforge:runClient` is defined with that argument, so starting the game through that task has development mode. Any other launch has it only when that argument is set on the Java command.

With the argument, a corner button swaps the vanilla book and the blue book on the inventory and the crafting table. Both stay loaded. Further buttons on that row exist only in this mode. The Discovered / All button writes the option "Reveal all recipes". The book reads that option. The button is only a shortcut, and the book does not call into this mode. Without the argument, do not register these controls, and show only the blue book. Testing the blue book does not need the argument. Testing the swap does. Release behavior must not depend on it.

Any later control that exists only to make development easier follows the same rule: a system property the client run sets and a built jar does not. When the property is absent, do not register that control.
