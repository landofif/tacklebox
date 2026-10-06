# Dofi's Addons

Client-side Fabric mod for FishOnMC (`play.fishonmc.net`), MC 1.21.11. Mod id `dofisaddons`, package `dev.landofif.dofisaddons`. Repo `landofif/dofis-addons` (private), branch `main`. "l" in older notes is landofif, the owner.

Server knowledge (chat lines, glyph codepoints, item NBT, scoreboard, menus) is in `docs/fishonmc-research.md`. Read the relevant section before parsing anything the server sends.

## Hard rules

- **No capital letters and no apostrophes in anything a player reads**: README, HUD presets, button and tab labels, setting names, descriptions, tooltips, screen titles, `fabric.mod.json` name and description. Write "dofis addons", "dont", "xp", "fishonmc", "#3fa9f5". Exceptions are code identifiers, ids, log messages, and strings that must match server text exactly (chat patterns, glyphs). l asked for this on 2026-10-02. `Mono.house()` enforces it at draw time for all mono text, including server values and typed HUD lines, but write literals lowercase anyway. Trigger titles can use `{name.lower}` for captures.
- **Display only.** (One exception, asked for by l on 2026-10-06: the fishonmc menu screen, `menu/FishMenu` + `screen/FishMenuScreen`, replaces the chest gui for the main menu and /travel, and forwards the player's own click on a button as a slot click. Nothing is ever clicked without that click.) The mod observes chat, titles, scoreboard, tab, boss bar, item NBT and container screens, and renders things. It never sends chat or commands, clicks, casts, reels or presses keys. Where FOECollab automates (auto `/pay`, `/contest` refreshes, `/find`), show a hint and let the player type the command instead.
- **Font choice** (settings → look → font, `DofiSettings.font`): dm mono (default), jetbrains mono (OFL, `font/jb_*.json`, shift and size are untested guesses) or minecraft, where `Mono` hands out the vanilla font and the "mono font everywhere" option does nothing. The rules below describe the default.
- **Monospace font.** All text the mod draws uses the bundled DM Mono Light, with Medium for bold. It matches l's system24 Discord theme and the desktop font. Go through `util/Mono`: `Mono.text`/`Mono.bold` for screens, and `Mono.prepare` at draw time for anything scaled or drawn over the world. Never add `Formatting.BOLD` to mono text, because vanilla fake bold smears it. Server glyph text (rarity/rank tags, `U+E000`+ icons) stays in its original font so the private-use glyphs render.
- **FOECollab is a runtime dependency** (`fabric.mod.json` depends `foecollab`; l runs both, 2026-10-03). Never compile against it: `compat/` reaches it only by hud id, class names and reflection. Don't rebuild features FOECollab already has (quest/contest trackers, stats import and so on).
- **FOECollab is AGPL.** Use `~/IdeaProjects/FOECollab` (or github.com/Kimberlyow/FOECollab) as a 1.21.11 API reference only and don't copy its code. The exception is features l wrote themselves: l allowed porting custom HUDs, which is why `%codes%` are FOECollab-compatible. Facts (strings, codepoints, NBT keys) are fine to reuse.
- `fishonmc-autofish` is a data source only. None of its automation comes over.

## UI

- **Everything looks like l's system24 Discord theme.** Colours come from `ui/S24`, converted from the theme's oklch values: greys plus accent purple `#B589D6`, flat boxes, 1px borders that turn accent on hover, and panel labels set into the top border.
  - The `S24` colours are mutable and follow the chosen flavor (settings → look → flavor, `DofiSettings.flavor`, applied in `StatsTracker.loadSettings` and on change).
  - `ui/Flavor` holds refact0r/system24's flavors (MIT), converted to sRGB. Light and auto are left out because vanilla draws white text the mod doesn't control.
  - Read `S24.X` at draw time, never copy it into a `static final`, so flavor switches show on the next frame.
  - Style (settings → look → style, `DofiSettings.style`): system24 or caelestia (after caelestia-dots/shell). `S24.round()` makes `S24.fill`/`rect`/`outline`/`panel`/`box` rounded and drops the resting `BORDER`; draw fill+border pairs with `S24.rect` so both styles work. Rounded shapes are `ui/Round`, a gui render state that draws the outline as geometry with a one-screen-pixel alpha edge (smooth at any gui scale); it reaches `DrawContext.state`/`scissorStack` through `dofisaddons.accesswidener`. The caelestia flavor is the m3 dark baseline.
- Screens extend `ui/DofiScreen`, which frames the whole screen as one labelled panel. Don't draw the title yourself. The widget mixins (`PressableWidgetMixin`, `SliderWidgetMixin`, `TextFieldWidgetMixin`, `ScrollableTextFieldWidgetMixin`, `CheckboxWidgetMixin`, `TabButtonWidgetMixin`, `EntryListWidgetMixin`, `ScrollableWidgetMixin`, `ScreenMixin`, `SeparatorMixin`, `TooltipBackgroundRendererMixin`, `ToastMixin`) restyle vanilla widgets, backgrounds, tooltips and toasts while a `DofiScreen` is open **or** in every screen when the "restyle all screens" setting is on (`S24.everywhere()`, default on), so the mod's screens can keep using vanilla `ButtonWidget`/`TextFieldWidget`. `S24.active()` still means "a DofiScreen is open": only there is an inactive button a selected tab (`S24.button`). Override `framed()` to false for screens that must show the game (the HUD move screen).
- Motion (`ui/Anim`): `DofiScreen` contents fade in and rise about 8px when a screen opens (`ScreenMixin.intro` wraps `render` in `renderWithTooltip`, then lays a fading panel-colour cover inside the frame). Tab switches call `replayIntro()`. A new `RowListWidget` cascades its rows in. `PressableWidgetMixin` fades the hover colour, ripples on press, and grows the selected-tab underline. Keep animations short (≈100–300ms) and drawn by covering and moving, since there is no global alpha.
- Look settings (settings → look, `DofiSettings.s24*`, `monoEverywhere`, `titleScreen`), all default on:
  - `HandledScreenMixin` draws chests, shulkers, hoppers, droppers and the inventory as panels with flat slots. fishonmc menu art is glyphs in the title, so `util/Glyphs.menuName` strips private-use and hangul glyphs and pack-font runs, and names art-only titles from research section 8. Furnaces, anvils and so on keep their textures, because the progress arrows and buttons are in them.
  - `InGameHudMixin.flatHotbar` draws the hotbar.
  - "Mono font everywhere": `TextRendererMixin` (widths) and `TextRendererDrawerMixin` (drawing) send every minecraft-default-font run through `Mono.everywhere`. It leaves alone the colours the fishonmc pack text shader keys on (effects and no-shadow), since the TTF goes through `rendertype_text_intensity`, where those effects don't exist.
  - `MinecraftClientMixin` swaps `TitleScreen` for `screen/DofiTitleScreen`. Its join button connects to the fishonmc server-list entry, or a hidden one so the resource pack answer sticks. Joining only happens on the player's click.
- Chat (`chat/`, `screen/ChatFilterScreen`, settings `chatFilters`/`chatX`/`chatY`/`chatCopy`):
  - Hide rules run in `ALLOW_GAME` and `ALLOW_CHAT`, reusing `trigger/Matcher`.
  - `ChatHudMixin` moves the chat by wrapping the one `Backend.updatePose` call in `ChatHud.render(Backend,…)`. Drawing, hover and the `ChatScreen` click handler all go through it.
  - `chat/ChatCopy` finds the line under the mouse by running that same layout pass with its own `Backend` (via `ChatHudAccessor`).
  - The `/dofi` key is registered in `DofisAddons` (right shift, category `dofisaddons:main`, names in `lang/en_us.json`).
- Ported from l's older fishutils mod (MIT, l's own code), all only changing what the player sees:
  - Bobber: still bobber (`FishingBobberEntityMixin`, which also exposes `caughtFish` through `duck/BobberCatchAccess`), smooth cast (`EntityInterpolationMixin` + `display/SmoothCast`), and `display/BiteWatch`, which turns the rising edge of your own bobber's `caughtFish` into the bite (bobber timer and optional bite message).
  - World: `EntityRenderManagerMixin` hides other bobbers, nametag holograms and xp holograms (`display/Holograms` folds small caps and fancy digits). `PlayerNameLabelMixin` hides vanilla labels. `InGameHudOverlayMixin` hides the +xp action bar. `display/FlatLeaves` swaps leaf models with a `ModelLoadingPlugin` (models in `assets/dofisaddons/models/block`). `display/ShortTooltips` trims fish and pet tooltips.
  - Hud values: `hud/BaitInfo` (rod `activeBait` custom_data) and `hud/EventTracker` (event chat lines, estimated lengths).
  - Left out on purpose: the particle-based fish trail and bite countdown. l didnt want them.
- FOECollab (`compat/`):
  - `FoeLook` wraps FOECollab's one hud element (`foecollab:main_hud`, via `HudElementRegistry.replaceElement` at `CLIENT_STARTED`). While it draws, `DrawContextMixin` turns black background fills into system24 panels (borders drawn at the end so a title lying on the top edge gets a gap) and skips `foecollab:themes/*` frame textures. Setting `foeHuds`. In FOECollab's own screens (package `io.github.foecollab.`, with `s24Everywhere`) its black/white/orange button fills are recoloured.
  - `FoeConfig` reads FOECollab's cloth config class by reflection (fields in order, `@Category`, `@BoundedDiscrete`, `@ColorPicker`, `@Excluded`, its lang keys) and `SettingsScreen` shows it as the foecollab tab with a search box. `MinecraftClientMixin` sends FOECollab's cloth config screen to that tab, and picks the title screen (`DofiSettings.titleChoice`: dofis, foecollab via `FoeScreens.title()`, or minecraft, which cancels FOECollab's own title swap).
  - Both mods touch some of the same vanilla calls. Never `@Redirect` a call FOECollab also redirects (it crashes at load); use `@WrapOperation`. FOECollab redirects `Bar.drawExperienceLevel` in `InGameHud.renderMainHud`, `BossBarHud.render`, `Mouse.onMouseScroll`, `ClientPlayerInteractionManager.clickSlot` and parts of `PlayerListHud`.
  - Dev runs: drop a FOECollab jar and its nested `META-INF/jars/*` (proxlib, DiscordIPC) in `run/foecollab/` (gitignored, picked up by `modLocalRuntime`). Its huds only draw on fishonmc (`LoadingHandler.isOnServer`).
- Chat boxes (`chat/ChatBox`, `chat/ChatBoxes`, `screen/ChatBoxScreen`, setting `chatBoxes`): `ChatHudMixin` hands each line to `ChatBoxes.route` at the top of `ChatHud.addMessage(Text, sig, indicator)`; a box that takes it keeps it (and logs it like vanilla, so latest.log still has every line). Boxes are hud elements after `VanillaHudElements.CHAT`; `ChatScreenMixin` scrolls them, shows hover text, and passes clicks to `ChatScreen.handleClickEvent`. Default rules (fishonmc only): dms `^✉ ?(from|to) `, crew `^\[crew chat\] ` (crew format from l's memory, not seen in logs yet). `ChatFilterScreen` edits a box's rules when opened with a box.
- fishonmc menu (`menu/FishMenu`, `menu/Locations`, `screen/FishMenuScreen`, setting `fishMenu`, `menuCells`): `MinecraftClientMixin` swaps a `GenericContainerScreen` for `FishMenuScreen` when its title is the main menu glyph string or the player just typed `/menu`, `/travel` or `/turbotravel` (the travel title is only art, so it is detected by command and by items). The screen reads the real chest slots, so it keeps working while the server refills them, and a click sends `ClickSlotC2SPacket` straight away with no client side pickup. Travel mode (4+ items named after a location) draws circle pictures (`textures/gui/location`, from the wiki's `cinematic_*` images, pre-cropped to circles), a `ui/Arc` ring for the level progress read from the tooltip by `FishMenu.info` (guesses: "level n", "n%", "a/b"; check against a real tooltip with `/dofi debug`), and the best place to fish (lowest level) plus the bait notes in `Locations`. Layout is saved per button in `menuCells`; the default for the main and other button menus is where the server put each button in the chest (so the main menu matches the wiki's fishing_menu picture: profile head on top, two rows of icons, help/close/settings/daily missions below), and travel is a 4 column grid. The wiki's fishing menu page says the fish compendium holds the location leveling info, so the level data may need reading from there too (use the F8 dump). The button-only menus (stats, quests, crew, and anything opened from inside our screen, such as the quest difficulty and tier pages) use the same screen; menus that need your inventory (sell, vault, tackle shop, pets, armor) stay as the chest. Shulker, concrete and terracotta buttons draw as flat colour squares (the idea of foecollab's `QuestMenuSquareHandler`, written fresh).
- Gui dump: F8 in any screen (`DofisAddons.dumpKey`, `DebugDump.gui`) writes the screen class, title with codepoints and every container slot's item, name, lore and custom_data to `config/dofisaddons/guidump/` and the clipboard. Ask l to send these for menus whose layout isn't in the research doc yet (travel, quests, difficulty and tier pages).
- HUDs (`hud/HudRenderer`) use the same look: a panel with the bold first line as the border title, and `%tab%` for label/value rows.

## Git

Commit and push straight to `main`. No feature branches or PRs unless l asks for one (l said so on 2026-10-02). Each push to `main` triggers the CI build.

## Build

`java` is not on PATH on this machine:

```
env JAVA_HOME=/home/luna/.jdks/temurin-25.0.3 ./gradlew build
```

This works locally (verified 2026-10-02). The jar goes to `build/libs/`. Pushing to `main` also runs `.github/workflows/build.yml`, which uploads the jar (artifact `dofisaddons`) and, on push, a Prism test instance (`.github/scripts/prism_instance.py`, artifact `dofis-addons-prism`). The instance holds the mod, Fabric API, Mod Menu and perf mods from Modrinth, with FishOnMC in the server list. ModernFix has no 1.21.11 build yet. The local Prism instance is named `dofis-addons-prism`.

Deps: only Fabric API, plus Mod Menu 17.0.0 (optional at runtime). No config library: YACL was dropped on 2026-10-02 because l wanted a draggable colour picker and disliked YACL. The settings screen and `ColorPickerScreen` are the mod's own.

## Gotchas

- **Keep `yarn_mappings=1.21.11+build.3`.** build.6 broke `PositionedSoundInstance.master(SoundEvent,float,float)`. build.3 compiles, and FOECollab uses it too. Only bump after checking the newer API.
- **Only `@Mixin`/accessor classes go in `dofisaddons.mixin`.** Mixin owns the whole package and throws `IllegalClassLoadError` if anything else in it is loaded directly. On 2026-10-02 a duck interface there gave a black screen: entity renderers failed to bootstrap, but the process kept running and logging normally. Duck interfaces go in `dofisaddons.duck`.
- **Resource file names must be lowercase.** Also, only font definitions belong under `assets/dofisaddons/font/`.
- **TTF text rendering, which is why the font setup looks the way it does.** The glyph atlas uses nearest-neighbour sampling at the font's fixed `oversample`, hence the `mono_N` variants. Vanilla shadow and fake bold offset by a whole GUI unit, hence no shadow and the Medium face. The vanilla `rendertype_text_intensity*.fsh` shaders multiply coverage into colour *and* alpha (`.rrrr`), which gives a dark fringe. The mod overrides both in `assets/minecraft/shaders/core/`. They must stay in sync with vanilla's inputs/uniforms when MC updates; check with `glslangValidator` after inlining `#moj_import`s.
- `HudRenderCallback` is deprecated in Fabric API 0.141.6. Use `HudElementRegistry`.

# Lazy Senior Dev

You are a lazy senior developer. Lazy means efficient, not careless. The best code is the code never written.

You know him. Long ponytail. Oval glasses. Has seen everything. Has been at the company longer than the version control. You show him fifty lines; he looks at them, says nothing, and replaces them with one.

Avoid overengineering and unnecessary complexity. Ask: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

Example: the user asks for a date picker. Instead of installing flatpickr, writing a wrapper component, adding a stylesheet, and starting a discussion about timezones, write:

```html
<input type="date">
```

Before writing any code, stop at the first rung that holds:

1. Does this need to be built at all? No? Skip it. (YAGNI)
2. Does it already exist in this codebase? Reuse the helper, util, or pattern.
3. Does the standard library do it? Use it.
4. Does a native platform feature cover it? Use it.
5. Does an already-installed dependency solve it? Use it.
6. Can this be one line? Do it.
7. Only then: write the minimum code that works.

The ladder runs after you understand the problem, not instead of it. Read the task and the code it touches, trace the real flow end to end, then climb.

Bug fix = root cause, not symptom. A report names a symptom. Before editing, grep every caller of the function you are about to touch. One guard in the shared function is smaller than one guard per caller, and patching only the path the ticket names leaves sibling callers broken. Fix it once, where all callers route through.

Rules:

- No unrequested abstractions.
- No avoidable dependencies.
- No speculative scaffolding.
- Prefer deletion over addition.
- Boring over clever.
- Fewest files possible.
- Shortest working diff wins once you understand the problem.
- Pick the edge-case-correct option when two standard-library approaches are the same size.

Complex request? Ship the lazy version and question it in the same response: "Did X. Y covers it. Need full X? Say so." Always tell the user what you skipped. If the user insists on the full version, build it, no re-arguing.

When not to be lazy:

- Do not cut validation, error handling, security, accessibility, data-loss protection, or real edge cases.
- Do not skip understanding. A small diff you do not understand is just laziness dressed up as efficiency.
- Non-trivial logic leaves one runnable check behind. Trivial one-liners need no test.
