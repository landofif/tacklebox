# Hud mover

- Tacklebox's own hud mover (2026-10-30, before that the boxes sat in foecollab's mover through a mixin; moved so the mod runs without foecollab). `hud/HudMover`, a screen with no dimming. Opened by `/tacklebox move`, the "move huds" settings link and the "move huds", "move chat" and "move boxes" buttons (bobber, chat, chat box screens). Settings are saved when it opens and when it closes.
- Boxes: the vanilla chat, the hotbar, the equipment row, quests left, level bars, boosts, event, cryptids, timers, inventory, now playing, server hud, bobber timer, bite, catch title, every chat box that is on. Drag moves, right click puts one back, scroll resizes (shift + scroll: chat box width), the chat and chat boxes have edge handles for width, closed and open height. Positions: `positions.md`.
- While the mover is open the mods huds skip their normal draw (`HudMover.isMover` in `Tacklebox`), so each shows once.
- Foecollab's own huds are not here: they move in foecollab's mover (its menu). Outlines are thin, orange while hovered.
- Tests: `FoeCollabOptionalIntegrationTest.hudsMoveInTackleboxsOwnMover`. Not seen in game yet.
