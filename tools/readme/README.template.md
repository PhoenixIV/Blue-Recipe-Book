<!--

This is the template for:
    - GitHub README.md
    - Modrinth project description
    - CurseForge project description

They are generated from this template via the script `render_readme.py`


## Link lookup table
- Architectury
    - https://modrinth.com/mod/architectury-api
    - https://www.curseforge.com/minecraft/mc-mods/architectury-api
- YetAnotherConfigLib
    - https://modrinth.com/mod/yacl
    - https://www.curseforge.com/minecraft/mc-mods/yacl
- Mouse Tweaks
    - https://modrinth.com/mod/mouse-tweaks
    - https://www.curseforge.com/minecraft/mc-mods/mouse-tweaks
- Better Recipe Book (Extended)
    - https://www.curseforge.com/minecraft/mc-mods/better-recipe-book-extended
    - https://modrinth.com/mod/better-recipe-book-extended
- Just Enough Items (JEI)
    - https://modrinth.com/mod/jei
    - https://www.curseforge.com/minecraft/mc-mods/jei
- JEI
    - https://modrinth.com/mod/jei
    - https://www.curseforge.com/minecraft/mc-mods/jei
- Roughly Enough Items (REI)
    - https://modrinth.com/mod/rei
    - https://www.curseforge.com/minecraft/mc-mods/roughly-enough-items
- EMI
    - https://modrinth.com/mod/emi
    - https://www.curseforge.com/minecraft/mc-mods/emi
- Inventorio
    - https://modrinth.com/mod/inventorio
    - https://www.curseforge.com/minecraft/mc-mods/inventorio-forge
- Mod Recipe Book
    - https://modrinth.com/mod/mod-recipe-book
    - https://www.curseforge.com/minecraft/mc-mods/mod-recipe-book


-->

<div style="text-align: center;" align="center">

<!-- only: github -->
<h1>
<img src="common/src/main/resources/icon.png" alt="" width="48" height="48" align="absmiddle"> Blue Recipe Book
</h1>

A better recipe book for Minecraft that grows with you, whether you play vanilla or with many mods.
<!-- end -->

Inspired by [About Oliver's blind Create mod series](#origin-story)

<!-- only: github -->
Find this project on:<br>
[<img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/modrinth_vector.svg" alt="" height="20" align="absmiddle"> Modrinth](https://modrinth.com/project/blue-recipe-book)
&nbsp;·&nbsp;
[<img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/curseforge_vector.svg" alt="" height="20" align="absmiddle"> CurseForge](https://www.curseforge.com/minecraft/mc-mods/blue-recipe-book)
<!-- end -->
<br>

Requires:<br>
[Architectury]() &nbsp;·&nbsp; [YetAnotherConfigLib]()

<br>

**Goals:**

✅ Minecraft 1.21.1 · ⬜ latest Minecraft

✅ NeoForge · ⬜ Fabric

</div>

<br>

> [!WARNING]
>
> **This project is in alpha.**
>
> It does not have recipe screens for mods such as Create yet, and it only opens on the crafting grid. More coming soon. See [Roadmap](#roadmap)
>
> I am still preparing many vanilla functionalities before moving deeper into mod territory. With the features and quality I have in mind this is already a lot more work than expected. 60+ hours so far, even with AI (and fixing AI code 😉).

<br>

> [!NOTE]
>
> The name "Blue Recipe Book" stems from ["the blue book mod" that Oliver uses](#origin-story) (it has no official name). The name "Blue Recipe Book" is his to take if he wants to. But I hope we will work together on it.

<br>
<br>

## Roadmap

Checked ✓ versions already shipped. The rest are milestones. More releases can land between them to add more features etc.

<br>

### ✅️ v0.1 - Demo stage

**Released:** 2026-10-19<br>
(~90 hours of work, including initial setup, research etc.)

Minecraft 1.21.1, NeoForge, client-only.

Replaces the vanilla recipe book only for crafting grids.
Fully functioning & pleasant alternative.

Depends on [YetAnotherConfigLib]() for basic settings.

- Items are sorted by the time they were discovered
- Option to strictly hide recipes until every ingredient was discovered. No more endgame spoilers from mods!
    - E.g. planks unlock fences, even without sticks in vanilla.
    - Some mods decide to show their end-game recipes at the start of a world.
- Scroll to flip pages
- Scroll to place items in the crafting station, or take them back. Inspired by [Mouse Tweaks]()
- `Ctrl + F` to search items
- Improved item grouping
    - Better visibility of group popups via a dimmed background (lightbox)
    - Only craftable items cycle (if available)
    - Clicking an item freezes cycling. No more stolen focus like in vanilla!
    - Alternative crafting recipes of the same item are an additional popup
    - Turn item grouping on or off
- Option to reveal all recipes

<br>

### ❌ v0.2 - Better than vanilla

Replaces the vanilla recipe book on every vanilla screen that has one: inventory, crafting table, furnace, smoker, blast furnace.

- New recipe animation: Icons animate like in vanilla

<br>

### ❌ v0.xx - Better than Oliver

Yay! It can now be used to support blind gaming and Create mod!

Minecraft 1.21.1, NeoForge, +Fabric, client-only.

- Display recipes for modded crafting stations similar to [JEI]() ([Preview screenshot](https://user-images.githubusercontent.com/59342563/184558160-20c9baea-aca4-4919-8163-909561eeac6e.png))
    - Focus on Create mod for this release
    - Hide undiscovered stations and related items
    - Improve GUI compared to JEI to make it feel more vanilla
- Backfill item discovery order
    - Helps players who add this mod to an existing world
    - Can only guess based on already unlocked recipe tree; vanilla does not store the order of discovery

<br>

### ❌ v0.xx - Broad mod support

While the previous milestone was focused on Create mod, this milestone officially supports and is tested against a wide variety of mods that add their own crafting stations / recipes etc.

<br>

### ❌ v1.0

Enough testing / playtime gathered from players to find the most important bugs and consider the mod stable.

<br>

### ❌ v2.0

Minecraft 1.21.1, +newest Minecraft, NeoForge, Fabric, client-only.

- Multi-version support
- Option to reveal all recipes still works
    - The server hides recipes from the client in newer versions of Minecraft
    - With JEI installed on the server all recipes are available to the client
    - Without JEI on the server the default recipes are assumed
        - Indicate they are assumptions
        - Remove assumption indicator as soon as possible

<br>

### ❌ v3.0

Most planned features are implemented.

- Presets for Settings
- Onboarding
- Localization / languages
    - Adding additional languages earlier, while many features are in development, would be painful to maintain
- Proper mod showcase

<br>

### ✨ Planned features

- Optional indicators for new recipes
    - Items that were not viewed / acknowledged by the player yet
    - Items the player never held before & might therefore unlock new recipes
    - Items that will certainly unlock new recipes (clairvoyance / foreshadowing)
- Option to increase size of book UI
    - Automatically match size when mods like [Inventorio]() enlarge the inventory screen
- Match search patterns of [JEI]()
    - @ for mod names
    - Arrow up for last search term(s)
    - "search with spaces"
- Add your own search terms
    - Golden apple? Gapple!
    - Cog? Gear!
    - Potentially create an API for mods to define alternative search terms
    - Ship a list of good alternative search terms (language-dependent)
    - Indicate matched term in item tooltip
    - "Add missing search term" button when search results are empty
- Optional crafting book for brewing stand
    - Only unlocks recipes you discovered
- Manage your own item tabs
    - Show / hide tabs
    - Move items between tabs
    - Place them only on the left or all around like [Better Recipe Book (Extended)]() ([screenshot](docs/images/better-recipe-book-extended.webp))
- Create missing item groups
    - Example: Create does not group [windows](https://create.fandom.com/wiki/Window).
        - [Create glass types](https://create.fandom.com/wiki/Framed_Glass) are `stonecutter` items; they could be grouped automatically.
    - Players can edit groups
    - Ship known good rulesets
- Show recipe tree from [EMI]() ([screenshot](https://cdn.modrinth.com/data/fRiHVvU7/images/f3c8b452eae6dde028c37e3a3e3792459468d865.png))

<br>

### 🤷‍♀ Optional features

- Sync player data with servers
    - Sync the order in which items were discovered etc.
    - Would allow players to play on the same server from different computers
        - Players can already copy the data themselves instead
    - Requires servers to install this mod / a plugin
        - Unlikely as long as `Blue Recipe Book` only has a small number of users
- Instant crafting option
    - Undoable; items preview like holding them with the mouse
    - Not undoable; items go directly into inventory
- An overview to show any recipe a given item is used in
    - Any recipe tree is likely overwhelming / too many variants
        - A max depth and width could limit that
        - Could be expanded by clicking on items
    - Unsolved question: Some items are atoms / cannot be crafted and therefore do not usually show up in the recipe book
        - Example: Logs, Flowers

<br>
<br>

## Origin Story

[About Oliver](https://www.youtube.com/@AboutOliver) is a streamer I enjoy who [plays Minecraft blind](https://www.youtube.com/watch?v=K3H-WPuu5Q8&list=PL68V5Cxs_CvTpTY9o7KJ75nLPqlCRxze0). In 2026, he started a [(mostly) blind Create Mod series](https://www.youtube.com/watch?v=CqcwbzLFfGk&list=PLAE9Qoccp2tE), in which he makes use of a blue recipe book, next to vanilla's green one. It reveals Create's recipes step by step like vanilla. It is under development, still has issues and is not meant to be public. At least for now. I do not know for certain who the author of that mod is. Feel free to get in touch.

This mod, `Blue Recipe Book`, is meant to follow in these footsteps and replace the vanilla recipe book with an alternative that...

- is suitable for playing blind
- improves on the vanilla book
- includes complex recipes for modded content
- keeps a vanilla feel

Current state of the art for playing with Create and other mods is to use [Just Enough Items (JEI)](), [Roughly Enough Items (REI)](), [EMI]() etc., which offer in-game access to complex recipes for various mods. They sit on top of the vanilla UI, break the vanilla feel and reveal all available items right from the start. They are good for people who just want to create their builds. But they are not suited for playing blind or keeping a vanilla feel.

### Honorable mention

While searching for this "blue recipe book" that was used by Oliver I found [Mod Recipe Book]() by [MaestroNek](https://github.com/MaestroNek). It turns out he also had the idea to recreate the blue recipe book he saw in that stream. I tried his mod, had good conversations with him via GitHub and decided my own vision diverges enough from his to justify creating my own "blue recipe book". I recommend you check out his work! [Modrinth](https://modrinth.com/mod/mod-recipe-book) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/mod-recipe-book)

<br>
<br>

<!-- only: github -->
## Hytale

Also check out [Hytale](https://hytale.com/).

Hytale is a sandbox block game somewhat similar to Minecraft. It was created by [Hypixel's](https://hypixel.net/) Minecraft server team. They know what modders need. Even the [authors of "Create" are on board with the core team now](https://www.gamesradar.com/games/rpg/hytale-devs-hire-minecraft-modders-behind-incredible-create-mod-teasing-that-it-opens-the-door-to-automation-and-a-whole-lot-more-in-their-sandbox-rpg/).

It was developed for modding from the ground up - which makes so much sense for a creative game like this.

(Blue Recipe Book is in no shape or form related to Hytale. This is just free advertisement.)

### Criticism about Minecraft

Official mod support is a promise Mojang / Microsoft [made, but never delivered on](https://minecraft.fandom.com/wiki/Add-on#History). I developed Minecraft server plugins when Mojang was still an independent studio. Minecraft has always been that highly creative "build my ideas" game for me, like the interlocking toy bricks I also dug as a child. That freedom includes modding for me. And in the past it seemed Mojang also saw it that way.

Today over at Mojang we have to log in using Microsoft accounts. Which was advertised as a "security improvement" ...because adding 2FA to the existing login system would have been more work than rewriting everything to work with Microsoft, of course (lol).

Yes, I am still salty that this beautiful sandbox game was sold to someone who makes PR claims like this...

<br>
<br>
<!-- end -->

## AI Disclaimer 🤖

I make use of AI to generate Code for this project. A lot of its work I double check and correct. I know how to code by hand. Architectural decisions are mostly in my control. I take great care to keep the code clean.

<!-- only: github -->
I decided to include AI instruction files etc. in the repository. To me those are programming tools, just like helper scripts, that you should have access to.
<!-- end -->

<!-- only: modrinth curseforge -->
<div style="text-align: center;" align="center">

<br>
<br>
<br>

Find this project on:<br>
[GitHub](https://github.com/PhoenixIV/Blue-Recipe-Book)
&nbsp;·&nbsp;
[Modrinth](https://modrinth.com/project/blue-recipe-book)
&nbsp;·&nbsp;
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/blue-recipe-book)

</div>
<!-- end -->