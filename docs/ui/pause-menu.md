# Pause menu

## Ask before disconnect (2026-10-29)

luna's ask. The Disconnect button in the pause menu opens a yes/no screen ("disconnect?", "leave the server?", buttons disconnect and stay). Stay goes back to the pause menu; disconnect goes back to it and presses the real button, so vanilla's leave (with its unsent report draft check) runs as before. Setting `confirmDisconnect`, on by default (luna's pick), toggle "ask before disconnect" on the settings screens page. Singleplayer's save and quit is not asked.

## Key files

- `mixin/GameMenuScreenMixin`: cancellable inject at HEAD of `method_19836`, the disconnect button's press lambda in `GameMenuScreen.initWidgets` (it sets the button inactive, then `AbuseReportContext.tryShowDraftScreen(..., client.disconnect(QUITTING_MULTIPLAYER_TEXT))`). Neither foecollab nor tacklebox touched the pause menu before.
- `screen/DisconnectConfirm`: the question (vanilla `ConfirmScreen` with small caps text) and a `leaving` flag that lets the second press through.

## Tests

`screen/DisconnectConfirmTest` (when it asks, yes and no), `screen/DisconnectConfirmIntegrationTest` (saved settings and the press, no, press, yes flow).

Not run in game yet: the question shows, stay keeps you on, disconnect leaves.
