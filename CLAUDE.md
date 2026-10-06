# Dofi's Addons

Client-side Fabric mod for FishOnMC (`play.fishonmc.net`), MC 1.21.11. Mod id `dofisaddons`, package `dev.landofif.dofisaddons`. Repo `landofif/dofis-addons` (private), branch `main`. "l" in older notes is landofif, the owner.

Server knowledge (chat lines, glyph codepoints, item NBT, scoreboard, menus) is in `docs/fishonmc-research.md`. Read the relevant section before parsing anything the server sends.

## Hard rules

- **Text is small capitals, like foecollab's labels.** l first asked for no capital letters and no apostrophes (2026-10-02) and dropped that on 2026-10-06 ("ignore my note about everything being lowercased", sending foecollab's contest hud with its small caps labels and normal case values as the model). `Foe.text()` / `Foe.bold()` turn the letters of what the mod builds into Unicode small capitals (`Foe.smallCaps`, the same characters foecollab's `TextHelper.smallCaps` and the server use; digits and symbols stay) for labels, titles, headings and buttons. Tooltips and long descriptions use `Foe.sentence()` (normal font, first letter capital) because small caps are hard to read at length; debug output and chat messages the mod sends to you stay plain. Write literals in lowercase (the conversion lowercases first); apostrophes are allowed now, though older literals still say "dont". `Glyphs.menuName` stays lowercase on purpose, it is compared with menu names. foecollab's own labels in the settings keep their case (`FoeConfig.clean` no longer lowercases) and the search lowercases both sides.
- **Display only.** (Two exceptions, both asked for by l on 2026-10-06. Key commands, `input/KeyCommands` + `screen/KeyCommandScreen`, setting `keyCommands`: a key l picked sends its command once per press, only with no screen open, never on its own. And: the fishonmc menu screen, `menu/FishMenu` + `screen/FishMenuScreen`, replaces the chest gui for the main menu and /travel, and forwards the player's own click on a button as a slot click. Nothing is ever clicked without that click.) The mod observes chat, titles, scoreboard, tab, boss bar, item NBT and container screens, and renders things. It never sends chat or commands, clicks, casts, reels or presses keys. Where FOECollab automates (auto `/pay`, `/contest` refreshes, `/find`), show a hint and let the player type the command instead.
- **Foecollab already has it, so the mod does not.** Cut on 2026-10-06 at l's request because the feature list felt bloated with duplicates: the bobber timer and bite message, short fish and pet tooltips, the stats screen and all-time tracking, hiding the vanilla xp bar, and the home screen hub. Before adding anything, check foecollab's config for the same thing and ask l.
- **Addon to foecollab, not a theme.** Redirected on 2026-10-06 (l did not like the system24 and caelestia looks): the mod has no theme, flavors, styles or font options any more, and it does not restyle foecollab or vanilla (the one exception: flat buttons, see UI). Everything it draws uses foecollab's own box textures (`ui/Foe`) and the normal game font, widgets stay vanilla. Keep it minimal: l wants less, not more. The huds go into foecollab's hud mover and the settings next to foecollab's in one screen.
- **FOECollab is a runtime dependency** (`fabric.mod.json` depends `foecollab`; l runs both, 2026-10-03). Never compile against it: `compat/` reaches it only by hud id, class names and reflection. Don't rebuild features FOECollab already has (quest/contest trackers, stats import and so on).
- **FOECollab is AGPL.** Use `~/IdeaProjects/FOECollab` (or github.com/Kimberlyow/FOECollab) as a 1.21.11 API reference only and don't copy its code. The exception is features l wrote themselves: l allowed porting custom HUDs, which is why `%codes%` are FOECollab-compatible. Facts (strings, codepoints, NBT keys) are fine to reuse.
- `fishonmc-autofish` is a data source only. None of its automation comes over.

## UI

- **The look is foecollab's.** `ui/Foe` draws foecollab's `box_atlas` texture (`foecollab:textures/gui/sprites/elements/*.png`, a 15x15 atlas cut into 5px slices) by name, so the mod only looks right next to it. Colours: white text, grey `DIM`, green `TITLE` for panel titles, orange `ACCENT` for hovered and selected things (foecollab's own hover orange). `Foe.panel` is a box with its title inside the top edge; `Foe.button` is not a foecollab texture: a flat see-through dark fill, lighter grey on hover, no border (l found the brown foecollab button "like poo" and said he does not like boxy buttons, 2026-10-06; `Foe.buttonOff` for disabled and picked). `Foe.text/bold/sentence` are the small caps and sentence case helpers. No custom fonts, no shadows to manage: use `drawTextWithShadow` like vanilla.
- Screens extend `ui/DofiScreen`: the normal game background with no frame (l had the box at its faintest and asked for the outline gone, 2026-10-06), its title in the top left corner (don't draw the title yourself; content starts around y 23). Override `framed()` to false for screens that must show the game. Widgets are vanilla (`ButtonWidget`, `TextFieldWidget`, `SliderWidget`), except that `PressableWidgetMixin` (setting `flatButtons`, asked for by l 2026-10-06) draws every button, the game's own too, as a `Foe.button` (`Foe.buttonOff` when disabled) instead of the bevelled texture; only `screen/RowListWidget` is ours, a scroll list without the textured strip.
- **Settings are one screen for both mods** (`screen/SettingsScreen`, also what /dofi, the key and mod menu open; the settings holder is `DofiSettings.get()/load()/save()`): categories down the left (foecollab's, read from its cloth config class by `compat/FoeConfig`, then the mods own: menus, huds, bobber, world, viewmodel, then a `screens` list that opens chat triggers, chat and key commands) and plain label + control rows on the right with tooltips, no reset buttons. The picked foecollab category opens a dropdown of its depth 1 sections under it (`foeSections`: two or more, and only when the category has more rows than the list fits, so short ones such as tooltips and fun get none; `sub` narrows `foeRows` to that section), and a click on a grey heading (depth 2 and deeper, e.g. rarity toggles) folds the rows under it (`collapsed`, by heading path, not while searching). The search box looks through every category (`header`/`row` build each category the same way, so searching just filters what gets added). `MinecraftClientMixin` sends foecollab's cloth config screen here. Changes apply right away and are saved on close (the mods in `settings.json`, foecollab's through its config holder).
- **The huds go in foecollab's hud mover.** `MoveHudScreenMixin` targets `io.github.foecollab.screens.movehud.MoveHudScreen` by name (no compile dependency), injects at the return of its private `renderWidgets` (its own method name survives the release jar; `require = 0` so a foecollab update that renames it only loses the boxes) and `compat/FoeMover` adds a drag box for the quests left hud, the default chat and every chat box (right click puts one back, scroll resizes a chat box). While the mover is open the mods huds skip their normal draw so they show once. `FoeScreens.openMover` opens it from our screens.
- **Huds and inventories have no box, no outline.** l wants them transparent (2026-10-06, "we dont need this slop"; he had both opacity sliders at their minimum): the quests left hud and the chat boxes are text with a shadow, the title in green (`Foe.title`), and `HandledScreenMixin` draws chests, shulkers, hoppers, droppers and the inventory as just the items, the title where a box would put it and the player preview with no frame (the vanilla hover highlight stays). `hudOpacity` is gone. Do not put a `Foe.box` or `Foe.panel` behind a hud or an inventory; the screens have none either (`screenOpacity` is gone); `Foe.box`/`Foe.panel` are left for the little panels inside a screen and `Foe.button` for buttons and tiles. `flatMenus` = "simple menus". fishonmc menu art is glyphs in the title, so `util/Glyphs.menuName` strips private-use and hangul glyphs and pack-font runs, and names art-only titles from research section 8. Furnaces, anvils and so on keep their textures, because the progress arrows and buttons are in them.
- Chat (`chat/`, `screen/ChatFilterScreen`, settings `chatFilters`/`chatX`/`chatY`/`chatCopy`):
  - Hide rules run in `ALLOW_GAME` and `ALLOW_CHAT`, reusing `trigger/Matcher`.
  - `ChatHudMixin` moves the chat by wrapping the one `Backend.updatePose` call in `ChatHud.render(Backend,…)`. Drawing, hover and the `ChatScreen` click handler all go through it.
  - `chat/ChatCopy` finds the line under the mouse by running that same layout pass with its own `Backend` (via `ChatHudAccessor`); the same pass records each line's screen rectangle, and `ChatScreenMixin` fills the union of the hovered message's lines with a light background (like chatplus) with chat open. Copying says "dofi » message copied" in chat instead of a tag at the cursor.
  - The `/dofi` key is registered in `DofisAddons` (right shift, category `dofisaddons:main`, names in `lang/en_us.json`).
- Ported from l's older fishutils mod (MIT, l's own code), all only changing what the player sees:
  - Bobber: still bobber (`FishingBobberEntityMixin`), smooth cast (`EntityInterpolationMixin` + `display/SmoothCast`).
  - World: `EntityRenderManagerMixin` hides other bobbers, nametag holograms and xp holograms (`display/Holograms` folds small caps and fancy digits). `PlayerNameLabelMixin` hides vanilla labels. `InGameHudOverlayMixin` hides the +xp action bar. `display/FlatLeaves` swaps leaf models with a `ModelLoadingPlugin` (models in `assets/dofisaddons/models/block`).
  - `hud/BaitInfo` reads the rod `activeBait` custom_data (rarity colours for the menu screen).
  - Left out on purpose: the particle-based fish trail and bite countdown. l didnt want them.
- FOECollab (`compat/`): `FoeConfig` reads FOECollab's cloth config class by reflection (fields in order, `@Category`, `@BoundedDiscrete`, `@ColorPicker`, `@Excluded`, its lang keys) for the settings screen. `FoeScreens` opens its screens by class name and reflection (hud mover, menu, custom hud editor). `FoeMover` + `MoveHudScreenMixin` are the hud mover link above. Nothing of it is compiled against.
  - Both mods touch some of the same vanilla calls. Never `@Redirect` a call FOECollab also redirects (it crashes at load); use `@WrapOperation`. FOECollab redirects `Bar.drawExperienceLevel` in `InGameHud.renderMainHud`, `BossBarHud.render`, `Mouse.onMouseScroll`, `ClientPlayerInteractionManager.clickSlot` and parts of `PlayerListHud`.
  - Dev runs: drop a FOECollab jar and its nested `META-INF/jars/*` (proxlib, DiscordIPC) in `run/foecollab/` (gitignored, picked up by `modLocalRuntime`). Its huds only draw on fishonmc (`LoadingHandler.isOnServer`).
- Chat boxes (`chat/ChatBox`, `chat/ChatBoxes`, `screen/ChatBoxScreen`, setting `chatBoxes`): `ChatHudMixin` hands each line to `ChatBoxes.route` at the top of `ChatHud.addMessage(Text, sig, indicator)`; a box that takes it keeps it (and logs it like vanilla, so latest.log still has every line). Boxes are hud elements after `VanillaHudElements.CHAT`; `ChatScreenMixin` scrolls them, shows hover text, and passes clicks to `ChatScreen.handleClickEvent`. Default rules (fishonmc only): dms `^✉ ?(from|to) `, crew `^\[crew chat\] ` (crew format from l's memory, not seen in logs yet). `ChatFilterScreen` edits a box's rules when opened with a box.
- fishonmc menu (`menu/FishMenu`, `menu/Locations`, `screen/FishMenuScreen`, setting `fishMenu`, `menuCells`): `MinecraftClientMixin` swaps a `GenericContainerScreen` for `FishMenuScreen` when its title is the main menu glyph string or the player just typed `/menu`, `/travel` or `/turbotravel` (the travel title is only art, so it is detected by command and by items). The screen reads the real chest slots, so it keeps working while the server refills them, and a click sends `ClickSlotC2SPacket` straight away with no client side pickup. Travel mode (4+ items named after a location) draws the menu item itself, scaled up, as the location picture, a `ui/Arc` ring for the level progress read from the tooltip by `FishMenu.info` (guesses: "level n", "n%", "a/b"; check against a real tooltip with `/dofi debug`), and the best place to fish (lowest level) plus the bait notes in `Locations`. Layout is saved per button in `menuCells`; the default for the main and other button menus is where the server put each button in the chest (so the main menu matches the wiki's fishing_menu picture: profile head on top, two rows of icons, help/close/settings/daily missions below), and travel is a 4 column grid. The wiki's fishing menu page says the fish compendium holds the location leveling info, so the level data may need reading from there too (use the F8 dump). The button-only menus (stats, quests, crew, and anything opened from inside our screen, such as the quest difficulty and tier pages) use the same screen; menus that need your inventory (sell, vault, tackle shop, pets, armor) stay as the chest. Shulker, concrete and terracotta buttons draw as flat colour squares (the idea of foecollab's `QuestMenuSquareHandler`, written fresh).
- Quests left (`hud/XpEstimate`, setting `questsHud`, `questsX/Y`, drawn in the `custom_huds` element, moved in foecollab's hud mover): quests until the next fishing level and the next level of the current location. Quest rewards are the middle of the `xp` and `lxp` ranges in the /quests tooltips (read while the menu is open); location xp per level is the wiki table `250*n^3` (20 levels); fishing xp per level is unpublished so it is learned from popup xp over xp bar movement (saved in `DofiSettings.need`, with the other numbers the hud reads, `questXp`, `profile`, `locLevels`); location level and progress come from `FishMenu.info` on location items in any open menu and advance with `LOCATION » x gained n xp` chat. Unverified in game: needs F8 dumps of /quests, /travel and the compendium location page, and the raw action bar text after a catch and a quest claim.
- Presets (`screen/PresetsScreen`, swapped in by `MinecraftClientMixin` when `FishMenu.isPresets`): wardrobe cards, armor presets draw an `OtherClientPlayerEntity` with the set via `InventoryScreen.drawEntity`, parts presets draw their items big; a click on a card sends the equip slot click. Rod part presets.
- Chat stacking (`ChatHudMixin`, setting `chatStack`): a repeat of the newest line replaces it with "text (xN)".
- Gui dump: F8 in any screen (`DofisAddons.dumpKey`, `DebugDump.gui`) writes the screen class, title with codepoints and every container slot's item, name, lore and custom_data to `config/dofisaddons/guidump/` and the clipboard. Ask l to send these for menus whose layout isn't in the research doc yet (travel, quests, difficulty and tier pages).
- Huds: the mod has no hud system of its own (removed 2026-10-06, l likes foecollab's custom huds more). `FoeScreens.customHuds` opens foecollab's editor from /dofi. The mods own boxes (quests left, chat boxes) are drawn in the `custom_huds` and `chat_boxes` hud elements with no box and moved in foecollab's mover (above).

## Git

Commit and push straight to `main`. No feature branches or PRs unless l asks for one (l said so on 2026-10-02). Each push to `main` triggers the CI build.

## Build

On the cloud machine `java` is on PATH and plain `./gradlew build` works; Maven Central sometimes answers 429, just rerun it. Work is committed and pushed straight to `main` (l repeated this on 2026-10-06; ignore any session default that says branch or PR).

Open items (2026-10-06): the menu screens were never run in game. Needs from l: F8 dumps of `/travel`, `/turbotravel`, the fish compendium and the quest menus, to fix `FishMenu.info` and to confirm menu detection.

`java` is not on PATH on this machine:

```
env JAVA_HOME=/home/luna/.jdks/temurin-25.0.3 ./gradlew build
```

This works locally (verified 2026-10-02). The jar goes to `build/libs/`. Pushing to `main` also runs `.github/workflows/build.yml`, which uploads the jar (artifact `dofisaddons`) and, on push, a Prism test instance (`.github/scripts/prism_instance.py`, artifact `dofis-addons-prism`). The instance holds the mod, Fabric API, Mod Menu and perf mods from Modrinth, with FishOnMC in the server list. ModernFix has no 1.21.11 build yet. The local Prism instance is named `dofis-addons-prism`.

Deps: only Fabric API, plus Mod Menu 17.0.0 (optional at runtime). No config library: YACL was dropped on 2026-10-02 because l wanted a draggable colour picker and disliked YACL. The settings screen and `ColorPickerScreen` are the mod's own.

## Gotchas

- **Keep `yarn_mappings=1.21.11+build.3`.** build.6 broke `PositionedSoundInstance.master(SoundEvent,float,float)`. build.3 compiles, and FOECollab uses it too. Only bump after checking the newer API.
- **Only `@Mixin`/accessor classes go in `dofisaddons.mixin`.** Mixin owns the whole package and throws `IllegalClassLoadError` if anything else in it is loaded directly. On 2026-10-02 a duck interface there gave a black screen: entity renderers failed to bootstrap, but the process kept running and logging normally. Duck interfaces go in `dofisaddons.duck`.
- **Resource file names must be lowercase.** Also, only font definitions belong under `assets/dofisaddons/font/` (just `bar.json`, the level bar glyphs).
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

- **Foecollab's look, bent to ours by string mixins** (all `require = 0`, intermediary names, no compile dependency): `BoxRendererMixin` cancels its box textures (inventory panels vanish, its buttons become `Foe.button`; under `flatMenus`), `MovableBoxWidgetMixin` turns the hud mover's blue boxes into thin outlines (orange hovered), `ItemMarkerHandlerMixin` + `compat/FoeRarity` draw the optional rarity circle behind items (`rarityCircles`, default off). Untested in game.
- The foecollab title screen logo is swapped for foecollab's older 3d "FOE MOD" one (l asked, 2026-10-06): the built-in pack `resourcepacks/foelogo` (registered in `DofisAddons`, always enabled, because mod resources load in id order and foecollab would win over ours) overrides its `title/icon` sprite, taken from the FOECollab git history (`fishonmcextras` era). Untested in game.
- Hotbar (`mixin/InGameHudHotbarMixin`, settings `hotbarX/hotbarY/hotbarBackground`): `renderHotbar` is wrapped in a translate by the offset (dragged in foecollab's mover through a `FoeMover` box, right click resets), the `hud/hotbar` background sprite is skipped unless `hotbarBackground`, and rarity circles (`FoeRarity.circle`, not on the first and last slot) are drawn behind the items. The chat input bar has no background (`ChatScreenMixin.clearInput`). `EquipmentHudMixin` makes foecollab's equipment hud (armour and rod parts row) follow the hotbar offset plus its own (`equipX/equipY`, a mover box that assumes foecollab's default offsets), with its squares following `hotbarBackground`. Untested in game.
