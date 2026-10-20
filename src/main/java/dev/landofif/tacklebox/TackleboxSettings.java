package dev.landofif.tacklebox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.landofif.tacklebox.hud.HudPos;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** The mods settings, one {@code config/tacklebox/settings.json}. Read them with {@link #get()}. */
public class TackleboxSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static TackleboxSettings current = new TackleboxSettings();

    public static TackleboxSettings get() {
        return current;
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve(Tacklebox.MOD_ID);
    }

    public static void load() {
        Path path = dir().resolve("settings.json");
        current = new TackleboxSettings();
        if (Files.isRegularFile(path)) {
            try {
                TackleboxSettings read = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), TackleboxSettings.class);
                if (read != null) {
                    current = read;
                }
            } catch (IOException | JsonParseException e) {
                Tacklebox.LOGGER.error("Could not read {}; keeping a backup and starting fresh", path, e);
                try {
                    Files.copy(path, path.resolveSibling("settings.json.broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                    // nothing more we can do
                }
            }
        }
        current.sanitize();
    }

    public static void save() {
        current.sanitize();
        Path path = dir().resolve("settings.json");
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling("settings.json.tmp");
            Files.writeString(tmp, GSON.toJson(current), StandardCharsets.UTF_8);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Tacklebox.LOGGER.error("Could not save {}", path, e);
        }
    }

    // ---- look (foecollabs, see ui/Foe) ----
    /** Chests, the inventory and fishonmc menus as one box with no slot boxes (drops the server's menu art). */
    public boolean flatMenus;
    /** What shows behind the box-less menus (display/MenuBackground): a colour and a picture, each in percent opacity (0 = none). */
    public boolean menuAccent;
    public int menuAccentOpacity = 40;
    public int menuBgColor = 0x000000, menuBgOpacity = 0, menuImageOpacity = 0;
    /** Every button as a flat see-through fill (lighter while hovered) instead of the bevelled texture. */
    public boolean flatButtons;
    /** The two colours of the gradient on focused tiles, hovered buttons and toggles (rgb). */
    public int accent1 = 0x33CCFF, accent2 = 0x00FF99;
    /** A see-through circle in the rarity colour behind items in slots (foecollabs rarity, drawn by its item markers). */
    public boolean rarityCircles = false;
    /** Caught fish tooltips without the scientific name, value and ecosystem, weight and length on one line (display/FishTooltip). */
    public boolean shortFishTooltips = false;
    /** Pet tooltips without the kind line, empty equipment lines and the right click hint (display/PetTooltip). */
    public boolean shortPetTooltips = false;
    /** Auction house pets you could sell to the pet store for more than they cost: a green tint and a line with the store's price (display/PetSale). */
    public boolean petProfitMarks = false;
    /** The hotbar: moved by this many gui pixels from where vanilla puts it, and whether its slot background is drawn. */
    public int hotbarX = 0;
    public int hotbarY = 0;
    /** The old on/off for the hotbar background; only read once to fill {@link #hotbarLook} and {@link #equipLook}. */
    public boolean hotbarBackground = false;
    /** What sits behind the hotbar and foecollabs equipment slots, the distance between slots in pixels (20 and 22 are the defaults), and the shared box colours. */
    public BoxLook hotbarLook = BoxLook.DEFAULT;
    public BoxLook equipLook = BoxLook.DEFAULT;
    public int hotbarSpacing = 20;
    public int equipSpacing = 22;
    public int boxOutlineColor = 0x808080;
    public int boxOpacity = 50;
    public int hotbarScale = 100;
    /** How the held hotbar slot is marked, and in what colour; and whether the offhand box shows. */
    public SelectionStyle hotbarSelection = SelectionStyle.FRAME;
    public int hotbarSelectColor = 0xFFFFFF;
    public boolean hotbarOffhand = true;
    /** Foecollabs equipment hud (armour and rod parts), moved by this much on top of the hotbar's offset. */
    public int equipX = 0;
    public int equipScale = 100;
    public EquipPlace equipPlace = EquipPlace.ABOVE;
    /** Foecollabs rarity bar under each equipment slot as a circle behind the item, and a colour to use instead of the rarity's. */
    public boolean equipCircle = true;
    public boolean equipRecolor = false;
    public int equipColor = 0xFFFFFF;
    public int equipY = 0;
    /** The mods own armour and rod parts row (hud/EquipmentRow) instead of foecollabs, and the look version the saved settings were last moved to. */
    public boolean equipHud;
    public int lookVersion = 1;

    // ---- fishonmc menu (see menu/FishMenu) ----
    /** Show the main menu and /travel as the mods own screens instead of the chest gui. */
    public boolean fishMenu;
    /**
     * Where the five huds below sit lives in their {@code *Pos} (distance to the nearest window sides, see hud/HudPos). The plain
     * {@code *X} and {@code *Y} are the spot a hud is drawn at this frame, set from its pos on every draw; settings saved before
     * pos existed have them as the spot and sanitize makes the pos from them. Edit a pos, never these.
     */
    public HudPos questsPos, levelBarsPos, boostsPos, nowPlayingPos, serverHudPos;
    /** The quests left hud: whether it shows. */
    public boolean questsHud;
    public int questsX = 4;
    public int questsScale = 100;
    public int questsY = 70;
    public HudAlign questsAlign = HudAlign.LEFT;
    /** The level progress hud (hud/LevelBars): fishing level and location level bars, where it sits. */
    public boolean levelBars;
    public int levelBarsX = 4;
    public int levelBarsScale = 100;
    public int levelBarsY = 40;
    /** What the level progress hud shows: heading, the fishing line, the location line, the bar itself, the percent, the location xp, and the bar width. */
    public boolean levelBarsHeading = true;
    public boolean levelBarsFishing = true;
    public boolean levelBarsLocation = true;
    public boolean levelBarsBar = true;
    public boolean levelBarsPercent = true;
    public boolean levelBarsXp = true;
    public boolean levelBarsDecimals = true;
    public boolean levelBarsMainXp = false;
    public int levelBarsWidth = 80;
    public HudAlign levelBarsAlign = HudAlign.LEFT;
    /** The active boosts hud (hud/BoostsHud): boosters, chummer, weather events and moon, where it sits. */
    public boolean boostsHud;
    public int boostsX = 182;
    public int boostsScale = 100;
    public int boostsY = 70;
    public HudAlign boostsAlign = HudAlign.LEFT;
    /** The now playing hud (hud/NowPlaying): what Spotify, a browser tab or any other player is playing, where it sits. */
    public boolean nowPlaying;
    public int nowPlayingX = 4;
    public int nowPlayingScale = 100;
    public int nowPlayingY = 4;
    /** What the now playing hud shows besides the title: its heading, the album art, the artist, a progress bar and the time. */
    public boolean nowPlayingHeading = true;
    public boolean nowPlayingArt = true;
    public boolean nowPlayingArtist = true;
    public boolean nowPlayingBar = true;
    public boolean nowPlayingTime = true;
    public HudAlign nowPlayingAlign = HudAlign.LEFT;
    /** The sidebar styled server hud (hud/ServerHud): where it sits, its size in percent and how strong the picture behind it is (percent). */
    public boolean serverHud;
    public int serverHudX = 4;
    public int serverHudY = 120;
    public int serverHudScale = 100;
    public int serverHudImage = 25;
    public HudAlign serverHudAlign = HudAlign.LEFT;
    /** Bite speed, reel speed and prospect as /menu showed them (read by hud/XpEstimate, shown by hud/ServerHud). */
    /** Lines of the server hud that are switched off, by the keys of ServerHud.order(). */
    public java.util.Set<String> serverHudHide = new java.util.HashSet<>();
    /** The order of the server hud's lines, by key; empty = the start order (ServerHud.order fills in what is missing). */
    public java.util.List<String> serverHudOrder = new java.util.ArrayList<>();
    /** The catches line as 4.3K instead of 4,392. */
    public boolean serverHudShortCatches;
    /** The community goal as last seen and the player's own catches towards it (hud/CommunityGoal); -1 = never seen. */
    public long communityCurrent = -1, communityGoal = -1, communityMine;
    /** The prospect chance as 1/35 instead of 1/34.96. */
    public boolean serverHudRoundProspect;
    public java.util.Map<String, String> profileStats = new java.util.LinkedHashMap<>();
    /** What the quests left hud has read in game (hud/XpEstimate): xp per quest, the /menu profile, location levels, fishing xp per level. */
    public double questXp, questLxp;
    public double[] profile;
    public java.util.Map<String, double[]> locLevels = new java.util.LinkedHashMap<>();
    public java.util.Map<String, Double> need = new java.util.LinkedHashMap<>();
    /** The /presets wardrobe screen, and for both custom guis: size in percent of the screen. */
    public boolean presetsMenu;
    /** /pv as all seven personal vaults on one screen with a search (screen/VaultScreen). */
    public boolean vaultMenu;
    /** Preset rows seen on /presets, kept between sessions so the other page can be drawn: "armor:6" to its pieces as "item|color|floats|strings|model". */
    public java.util.Map<String, java.util.List<String>> presetCache = new java.util.LinkedHashMap<>();
    public boolean presetsOtherBg = false;
    /** The head of each pet type ("fox", "seaturtle") as a profile component in json, learned from pet items seen in menus (display/PetHeads), for the totem pop. */
    public java.util.Map<String, String> petHeads = new java.util.LinkedHashMap<>();
    /** Luck and scale of each armour preset seen on /presets, "armor:n" to "subtropical_luck" and so on (menu/BaitPicker reads them). */
    public java.util.Map<String, java.util.Map<String, Double>> presetStats = new java.util.LinkedHashMap<>();
    /** The bait picker screen: location id, where luck and scale come from ("worn" or "armor:n"), extra luck and scale on top (rod, pet), bait tiers on (bit per rarity), lures, bigger sizes count. */
    public String baitLoc = "hawaii";
    public String baitSource = "worn";
    public int baitLuck = 0;
    public int baitScale = 0;
    public int baitTiers = 0b00111;
    public boolean baitLures = true;
    /** Location levels read from the fish compendium, "level|progress" by location id, shown in /turbotravel which has none. */
    public java.util.Map<String, String> locationLevels = new java.util.LinkedHashMap<>();
    /** The /stats "Power Breakdown" item as last read (menu/Power): total, leveling, compendium, collections, unique, and "n.normal" and so on (unique fish per variant). */
    public java.util.Map<String, Double> power = new java.util.LinkedHashMap<>();
    /** When {@link #power} was last read, epoch millis. */
    public long powerAt;
    /** The next unfinished level of each collection (menu/Power): category to "level|have|need|power". */
    public java.util.Map<String, String> collections = new java.util.LinkedHashMap<>();
    /** The variant lines of each compendium location page: caught and total for normal, albino, melanistic, trophy, fabled, gigantic (menu/Power). */
    public java.util.Map<String, int[]> variants = new java.util.LinkedHashMap<>();
    /** The rest of a compendium location item as last read: catches, runes discovered and runes total (-1 = no runes line), by location id (menu/Power, shown by hud/ServerHud). */
    public java.util.Map<String, long[]> dex = new java.util.LinkedHashMap<>();
    /** Each species of a compendium species page by location and variant ("keywest|albino") and whether it is caught (menu/Compendium). */
    public java.util.Map<String, java.util.Map<String, Boolean>> species = new java.util.LinkedHashMap<>();
    public int menuSize = 100;
    public int presetsSize = 100;
    /** Where you put each menu button: "main:name" or "travel:id" to {column, row}. */
    public java.util.Map<String, int[]> menuCells = new java.util.HashMap<>();

    // ---- chat ----
    /** Lines matching one of these never show in chat. */
    public java.util.List<dev.landofif.tacklebox.chat.ChatFilter> chatFilters = new java.util.ArrayList<>();
    /**
     * Where the vanilla chat sits, as an offset from the vanilla spot in gui units (right and down are positive). Sideways it is
     * {@code chatPos} (only its x is used: the gap to the left or right edge, so the chat stays on its side when the window
     * changes size); {@code chatX} is what that was before, read once to make it. Up and down the chat is already tied to the bottom.
     */
    public HudPos chatPos;
    public int chatX = 0;
    public int chatY = 0;
    /** Right click a chat line (with chat open) to copy it. */
    public boolean chatCopy = true;
    /** The welcome lines fishonmc prints at every login (instance, packs, welcome, help, rules, discord). */
    public boolean hideLoginChat;
    /** Lines with nothing in them (the server sends them as spacing) never reach the chat. */
    public boolean hideEmptyChat;
    /** Repeats of the same chat line become one line with a count, like "text (x3)". */
    public boolean chatStack = true;
    /** Huds that would sit under a chat window or the player list are not drawn. */
    public boolean hideBehindChat = false;
    /** Every chat line starts with its arrival time: 0 off (time only on hover), 1 [HH:MM:SS], 2 [HH:MM]. */
    public int chatTimeMode = 0;
    /** Separate chat boxes (dms, crew chat). null until sanitize fills in the defaults. */
    public java.util.List<dev.landofif.tacklebox.chat.ChatBox> chatBoxes = null;
    /** Keys that run a command when pressed in game, see input/KeyCommands. */
    public java.util.List<dev.landofif.tacklebox.input.KeyCommands.Entry> keyCommands = new java.util.ArrayList<>();

    /** Command aliases, see input/CommandAliases. */
    public java.util.List<dev.landofif.tacklebox.input.CommandAliases.Entry> aliases = new java.util.ArrayList<>();

    // ---- word swaps (see display/WordSwap) ----
    public boolean wordSwap = true;
    public java.util.List<dev.landofif.tacklebox.display.WordSwap.Entry> words = new java.util.ArrayList<>();

    // ---- bobber behaviour (only you see these) ----
    /** Hold the bobber still on the water instead of bobbing (it still dips on a bite). */
    public boolean stillBobber;
    /** Show your own cast flying smoothly instead of snapping back on a laggy server. */
    public boolean smoothCast;
    /** Hide other players bobbers and their lines. */
    public boolean hideOtherBobbers = false;

    // ---- world (only you see these) ----
    /** Hide the rank / name / power tag the server puts over your own head. */
    public boolean hideOwnNametag = false;
    /** Hide every players nametag, the servers and the vanilla one. */
    public boolean hideAllNametags = false;
    /** Hide the +66xp reward line in the action bar after a catch. */
    public boolean hideXpPopups = false;
    /** Draw leaves flat like vanilla instead of the server packs 3d leaves. */
    public boolean flatLeaves = false;
    /** A thin flat ring on the ground at the range of every placed chummer in view of the client (display/ChummerRing), and its colour (rgb). */
    public boolean chummerRing;
    public int chummerRingColor = 0x41F55B;
    /** Stop drawing the servers item, block and text displays farther than this many blocks (8 to 64), a fps help at spawn (display/DisplayCull). */
    public boolean hideFarDisplays = false;
    public int farDisplayRange = 24;

    // ---- bobber looks (only you see these) ----
    public BobberSkin bobberSkin = BobberSkin.DEFAULT;
    /** Also show your skin on other players' bobbers. */
    public boolean skinEveryone = false;
    public boolean customLineColor = false;
    /** RGB of your own fishing line. */
    public int lineColor = 0x3FA9F5;

    // ---- bobber timer and bite notification (display/BobberTimer, screen/BobberScreen) ----
    public TimerMode timerMode = TimerMode.HUD;
    /** Hud position, percent of the screen; the text is centred on it. */
    public double timerX = 50;
    public double timerY = 58;
    /** Text size in percent, for the hud and for over the bobber. */
    public int timerSize = 100;
    public int timerWorldSize = 100;
    /** How many blocks above the bobber the text floats. */
    public double timerHeight = 0.6;
    public int timerColor = 0xFFFFFF;
    public boolean timerShadow = true;
    public int timerDecimals = 1;
    public boolean timerSuffix = true;

    public boolean biteOn = true;
    public String biteText = "bite!";
    public double biteX = 50;
    public double biteY = 40;
    public int biteSize = 250;
    public int biteColor = 0xFFD54A;
    public boolean biteShadow = true;
    public double biteSeconds = 1.5;
    public BiteAnim biteAnim = BiteAnim.POP;
    /** Hide the servers own BITE! title while the notification is on. */
    public boolean biteHideTitle = true;
    /** Focus the game window through hyprctl on a bite (display/BiteFocus). Hyprland only. */
    public boolean biteFocus = false;

    public enum TimerMode {
        OFF("off"), HUD("hud"), BOBBER("on the bobber"), BOTH("both");
        public final String label;

        TimerMode(String label) {
            this.label = label;
        }
    }

    public enum BiteAnim {
        NONE("none"), FADE("fade"), POP("pop"), SLIDE("slide"), SHAKE("shake"), PULSE("pulse");
        public final String label;

        BiteAnim(String label) {
            this.label = label;
        }
    }

    // ---- viewmodel (held item) ----
    public boolean viewmodel = false;
    public double vmScale = 1.0;
    public double vmX = 0;
    public double vmY = 0;
    public double vmZ = 0;
    public double vmRotX = 0;
    public double vmRotY = 0;
    public double vmRotZ = 0;
    /** Skip the dip when you switch items. */
    public boolean vmNoEquipDip = false;
    public SwingStyle swingStyle = SwingStyle.NORMAL;
    /** 1 = normal, 2 = twice as slow, 0.5 = twice as fast. Only the animation changes. */
    public double swingSlowness = 1.0;
    /** How low your own camera dips when sneaking: 1.5 = vanilla, 1.8 = no dip. */
    public double sneakHeight = 1.5;
    /** "No swing": how far the item turns, in percent of vanilla's turn. */
    public double swingTurn = 100;

    public enum BobberSkin {
        DEFAULT("default", null), OCEAN("ocean", "ocean"), GOLD("gold", "gold"), EMERALD("emerald", "emerald"),
        MIDNIGHT("midnight", "midnight"), CANDY("candy", "candy"), DUCK("duck", "duck"), EMBER("ember", "ember"),
        PEARL("pearl", "pearl"), STAR("star", "star"), RAINBOW("rainbow", "rainbow");

        public final String label;
        public final String texture;

        BobberSkin(String label, String texture) {
            this.label = label;
            this.texture = texture;
        }
    }

    /** How the text of a hud lines up inside its box. */
    public enum HudAlign {
        LEFT("left"), CENTRE("centre"), RIGHT("right");

        public final String label;

        HudAlign(String label) {
            this.label = label;
        }

        /** The left edge for something {@code w} wide inside the span {@code from} to {@code from + span}. */
        public int x(int from, int span, int w) {
            return switch (this) {
                case LEFT -> from;
                case CENTRE -> from + (span - w) / 2;
                case RIGHT -> from + span - w;
            };
        }
    }

    /** Where foecollabs equipment row sits next to the hotbar, before any dragging. */
    public enum EquipPlace {
        ABOVE("above"), LEFT("left"), RIGHT("right");

        public final String label;

        EquipPlace(String label) {
            this.label = label;
        }
    }

    /** How far the equipment row is from its default place above the hotbar for {@link #equipPlace}: {dx, dy} in gui pixels. */
    public int[] equipOffset() {
        if (equipPlace == EquipPlace.ABOVE) {
            return new int[]{0, 0};
        }
        int side = 91 * hotbarScale / 100 + 6 + 65 * equipScale / 100; // from the hotbar's middle to the rows middle
        return new int[]{equipPlace == EquipPlace.LEFT ? -side : side, 27}; // level with the hotbar
    }

    /** DEFAULT is the vanilla hotbar sprite / foecollabs own squares, which only fit at their own spacing. */
    public enum BoxLook {
        HYPR("hyprland"), OFF("off"), DEFAULT("default"), FILL("fill"), OUTLINE("outline"), BOTH("fill + outline");

        public final String label;

        BoxLook(String label) {
            this.label = label;
        }
    }

    public enum SelectionStyle {
        HYPR("hyprland"), FRAME("frame"), ROUND("round"), SIMPLE("simple"), NONE("off");

        public final String label;

        SelectionStyle(String label) {
            this.label = label;
        }
    }

    public enum SwingStyle {
        NORMAL("normal"), SMOOTH("smooth"), SNAPPY("snappy"), NONE("no swing");

        public final String label;

        SwingStyle(String label) {
            this.label = label;
        }
    }

    /** The pitch of the hotbar slots: the vanilla 20 when the vanilla sprite is drawn, since it only fits that. */
    public int hotbarPitch() {
        return hotbarLook == BoxLook.DEFAULT ? 20 : hotbarSpacing;
    }

    public int equipPitch() {
        return equipLook == BoxLook.DEFAULT ? 22 : equipSpacing;
    }

    public void sanitize() {
        if (bobberSkin == null) bobberSkin = BobberSkin.DEFAULT;
        if (timerMode == null) timerMode = TimerMode.HUD;
        if (biteAnim == null) biteAnim = BiteAnim.POP;
        if (biteText == null) biteText = "bite!";
        if (swingStyle == null) swingStyle = SwingStyle.NORMAL;
        if (equipPlace == null) equipPlace = EquipPlace.ABOVE;
        if (hotbarLook == null) hotbarLook = hotbarBackground ? BoxLook.DEFAULT : BoxLook.OFF;
        if (equipLook == null) equipLook = hotbarBackground ? BoxLook.DEFAULT : BoxLook.OFF;
        if (lookVersion < 1) { // the hyprland look is the new default for the hotbar and equipment row
            hotbarLook = BoxLook.HYPR;
            equipLook = BoxLook.HYPR;
            hotbarSelection = SelectionStyle.HYPR;
            lookVersion = 1;
        }
        hotbarSpacing = Math.max(16, Math.min(24, hotbarSpacing));
        equipSpacing = Math.max(16, Math.min(26, equipSpacing));
        boxOpacity = Math.max(0, Math.min(100, boxOpacity));
        boxOutlineColor &= 0xFFFFFF;
        menuBgColor &= 0xFFFFFF;
        if (hotbarSelection == null) hotbarSelection = SelectionStyle.FRAME;
        if (chatFilters == null) chatFilters = new java.util.ArrayList<>();
        chatFilters.removeIf(java.util.Objects::isNull);
        if (keyCommands == null) keyCommands = new java.util.ArrayList<>();
        keyCommands.removeIf(java.util.Objects::isNull);
        for (var k : keyCommands) {
            if (k.command == null) k.command = "";
        }
        if (aliases == null) aliases = new java.util.ArrayList<>();
        aliases.removeIf(java.util.Objects::isNull);
        for (var a : aliases) {
            if (a.name == null) a.name = "";
            if (a.command == null) a.command = "";
        }
        if (words == null) words = new java.util.ArrayList<>();
        words.removeIf(java.util.Objects::isNull);
        for (var w : words) {
            if (w.from == null) w.from = "";
            if (w.to == null) w.to = "";
        }
        if (questsPos == null) questsPos = new HudPos(questsX, questsY);
        if (levelBarsPos == null) levelBarsPos = new HudPos(levelBarsX, levelBarsY);
        if (boostsPos == null) boostsPos = new HudPos(boostsX, boostsY);
        if (nowPlayingPos == null) nowPlayingPos = new HudPos(nowPlayingX, nowPlayingY);
        if (serverHudPos == null) serverHudPos = new HudPos(serverHudX, serverHudY);
        if (chatPos == null) chatPos = new HudPos(chatX, 0);
        if (chatBoxes == null) chatBoxes = dev.landofif.tacklebox.chat.ChatBoxes.defaults();
        chatBoxes.removeIf(java.util.Objects::isNull);
        for (dev.landofif.tacklebox.chat.ChatBox b : chatBoxes) {
            if (b.name == null) b.name = "";
            if (b.rules == null) b.rules = new java.util.ArrayList<>();
            b.rules.removeIf(java.util.Objects::isNull);
            b.width = Math.max(80, Math.min(600, b.width));
            b.lines = Math.max(1, Math.min(30, b.lines));
            b.linesOpen = Math.max(b.lines, Math.min(40, b.linesOpen));
            b.fadeSeconds = b.fadeSeconds == 0 ? 10 : Math.max(-1, Math.min(600, b.fadeSeconds));
        }
        dev.landofif.tacklebox.chat.ChatBoxes.addCrewsRule(chatBoxes);
        lineColor &= 0xFFFFFF;
        chummerRingColor &= 0xFFFFFF;
        farDisplayRange = dev.landofif.tacklebox.display.DisplayCull.clampRange(farDisplayRange);
        vmScale = Math.max(0.1, Math.min(3, vmScale));
        swingSlowness = Math.max(0.25, Math.min(4, swingSlowness));
        sneakHeight = dev.landofif.tacklebox.display.SneakHeight.clamp(sneakHeight);
        swingTurn = Math.max(0, Math.min(100, swingTurn));
        if (locLevels == null) locLevels = new java.util.LinkedHashMap<>();
        if (need == null) need = new java.util.LinkedHashMap<>();
        if (locationLevels == null) locationLevels = new java.util.LinkedHashMap<>();
        if (power == null) power = new java.util.LinkedHashMap<>();
        if (collections == null) collections = new java.util.LinkedHashMap<>();
        if (variants == null) variants = new java.util.LinkedHashMap<>();
        variants.values().removeIf(v -> v == null || v.length != 12);
        if (serverHudOrder == null) serverHudOrder = new java.util.ArrayList<>();
        if (dex == null) dex = new java.util.LinkedHashMap<>();
        dex.values().removeIf(v -> v == null || v.length != 3);
        if (species == null) species = new java.util.LinkedHashMap<>();
        species.values().removeIf(m -> m == null);
        if (presetCache == null) presetCache = new java.util.LinkedHashMap<>();
        if (petHeads == null) petHeads = new java.util.LinkedHashMap<>();
        if (presetStats == null) presetStats = new java.util.LinkedHashMap<>();
        menuSize = Math.max(40, Math.min(100, menuSize));
        presetsSize = Math.max(40, Math.min(100, presetsSize));
    }
}
