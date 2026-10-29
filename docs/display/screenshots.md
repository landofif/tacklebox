# Screenshot preview

Asked for by luna 2026-10-28, instead of installing Essential for its screenshot preview. Setting `screenshotPreview` ("screenshot preview" on the world page, under "screenshots"), off by default like every feature. Nothing is uploaded.

## What it does

- After F2, once the game has written the file, the screenshot goes straight to the clipboard (luna picked copy on its own over buttons or a key) and a card slides in at the top right: the picture 120 gui px wide, "copied" in green or "could not copy" in grey, and an open button.
- The card stays 5 s, then slides out. In a screen (chat, inventory) the mouse on the card holds it, plus 1 s after it leaves. A new screenshot replaces the card.
- The open button works only while a screen is open (in game the mouse turns the camera): a left click opens the file in the system image viewer (`Util.getOperatingSystem().open`, xdg-open on linux) and the screen does not get the click.
- The game's own "saved screenshot as" chat line is untouched. Not in the hud mover: it sits in the corner for a few seconds over whatever is there. It is drawn with F1 too.

## Key files

- `mixin/ScreenshotRecorderMixin`: injects into the save lambda `method_22691(NativeImage, File, Consumer)` right after `NativeImage.writeTo`, on the io thread. Exceptions are caught there, or the game would print "couldn't save screenshot" for a saved file.
- `display/ScreenshotPreview`: makes a 360 px wide copy of the image on the io thread (the game closes the image after the lambda), copies on another io task so the chat line isn't held up, registers the texture on the render thread, draws through `HudElementRegistry`, reads the mouse with `ScreenEvents.afterRender` and takes clicks with `ScreenMouseEvents.allowMouseClick`.
- `display/ScreenshotCard`: the card without the game: slide timing, hold, layout and hit tests.
- `util/ImageClipboard`: runs `wl-copy --type image/png` with the file as its input, 3 s timeout. Linux with wayland only (luna said that's fine): glfw only copies text and minecraft runs java headless, so there is no clipboard of its own for images. Missing wl-copy or no wayland gives "could not copy" and a log line.

## Tests

`ScreenshotCardTest` (timing, corner and shape, button and card hit boxes, hold), `ImageClipboardTest` (command, failures, a real process gets the file on its input), `ScreenshotPreviewIntegrationTest` (saved → copied → card → left click on open opens the file; failed copy, replacement, no click after it's gone).

## Not checked yet

Not run in game yet: the mixin applying in the real client, the pasted image (Discord, browser), the look and size of the card, the slide, the open button from chat.
