# Dofi's Addons

Client-side Fabric mod for **FishOnMC** (Minecraft 1.21.11).

Display only: the mod reacts to what the server sends. It never sends chat, clicks or presses keys for you.

## Chat triggers

Show your own on-screen title and play a sound when a chat message appears.

**Open it:** type `/dofi` (or `/triggers`), or go to *Mods → Dofi's Addons → Configure* if you use Mod Menu.

### Making a trigger

1. **+ New trigger**: start blank or from a FishOnMC preset (quest completed, pet drop, rare catch, Fabled event, storm, moon, fish sold).
2. **When**: type the chat text to react to.
   - `*` matches anything.
   - `{name}` grabs that part so you can show it in the title, e.g. `QUESTS > Quest #{n} has been completed!`
   - `>` also matches `»`, and FishOnMC's icon glyphs and extra spaces are ignored.
   - Paste a real chat line into **Test with** to see straight away whether it fires and what `{n}` becomes.
3. **Title**: text, subtitle, colours, size, height, timing and animation (Fade, Pop, Slide, Typewriter, Shake).
   - Colours: `&a`, `&l` (bold), `&o`, `&#FF8800` …
   - Placeholders: your `{name}` captures, `{player}`, `{count}` (times fired this session), `{message}`.
4. **Sound**: click through popular sounds or type any sound id (also works with server-pack sounds), volume, pitch, repeats.
5. **▶ Preview** at any time, then **Save**.

Other bits: switch triggers on/off with the dot, reorder with the arrows, **Share** copies a code that others can **Import**, and a cooldown stops repeated lines from spamming.

Triggers are stored in `config/dofisaddons/triggers.json`.

## Font

All mod text uses Courier Prime, a monospaced typewriter-style font (SIL Open Font License 1.1).

## Building

```bash
./gradlew build
```

Java 21, Gradle 9.5 (wrapper included). The jar ends up in `build/libs/`.
