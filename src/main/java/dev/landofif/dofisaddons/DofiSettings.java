package dev.landofif.dofisaddons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** The mods settings, one {@code config/dofisaddons/settings.json}. Read them with {@link #get()}. */
public class DofiSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static DofiSettings current = new DofiSettings();

    public static DofiSettings get() {
        return current;
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve(DofisAddons.MOD_ID);
    }

    public static void load() {
        Path path = dir().resolve("settings.json");
        current = new DofiSettings();
        if (Files.isRegularFile(path)) {
            try {
                DofiSettings read = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), DofiSettings.class);
                if (read != null) {
                    current = read;
                }
            } catch (IOException | JsonParseException e) {
                DofisAddons.LOGGER.error("Could not read {}; keeping a backup and starting fresh", path, e);
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
            DofisAddons.LOGGER.error("Could not save {}", path, e);
        }
    }

    // ---- look (foecollabs, see ui/Foe) ----
    /** Chests, the inventory and fishonmc menus as one box with no slot boxes (drops the server's menu art). */
    public boolean flatMenus = true;
    /** Every button as a flat see-through fill (lighter while hovered) instead of the bevelled texture. */
    public boolean flatButtons = true;
    /** A see-through circle in the rarity colour behind items in slots (foecollabs rarity, drawn by its item markers). */
    public boolean rarityCircles = false;
    /** The hotbar: moved by this many gui pixels from where vanilla puts it, and whether its slot background is drawn. */
    public int hotbarX = 0;
    public int hotbarY = 0;
    /** The old on/off for the hotbar background; only read once to fill {@link #hotbarLook} and {@link #equipLook}. */
    public boolean hotbarBackground = false;
    /** What sits behind the hotbar and foecollabs equipment slots, the distance between slots in pixels (20 and 22 are the defaults), and the shared box colours. */
    public BoxLook hotbarLook = null;
    public BoxLook equipLook = null;
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

    // ---- fishonmc menu (see menu/FishMenu) ----
    /** Show the main menu and /travel as the mods own screens instead of the chest gui. */
    public boolean fishMenu = true;
    /** The quests left hud: where it sits (gui units from the top left) and whether it shows. */
    public boolean questsHud = true;
    public int questsX = 4;
    public int questsY = 70;
    /** The sidebar styled server hud (hud/ServerHud): where it sits, its size in percent and how strong the picture behind it is (percent). */
    public boolean serverHud = true;
    public int serverHudX = 4;
    public int serverHudY = 120;
    public int serverHudScale = 100;
    public int serverHudImage = 25;
    /** Bite speed, reel speed and prospect as /menu showed them (read by hud/XpEstimate, shown by hud/ServerHud). */
    /** Lines of the server hud that are switched off, by the keys in ServerHud.LINES. */
    public java.util.Set<String> serverHudHide = new java.util.HashSet<>();
    public java.util.Map<String, String> profileStats = new java.util.LinkedHashMap<>();
    /** What the quests left hud has read in game (hud/XpEstimate): xp per quest, the /menu profile, location levels, fishing xp per level. */
    public double questXp, questLxp;
    public double[] profile;
    public java.util.Map<String, double[]> locLevels = new java.util.LinkedHashMap<>();
    public java.util.Map<String, Double> need = new java.util.LinkedHashMap<>();
    /** The /presets wardrobe screen, and for both custom guis: size in percent of the screen. */
    public boolean presetsMenu = true;
    /** /pv as all seven personal vaults on one screen with a search (screen/VaultScreen). */
    public boolean vaultMenu = true;
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
    public int menuSize = 100;
    public int presetsSize = 100;
    /** Where you put each menu button: "main:name" or "travel:id" to {column, row}. */
    public java.util.Map<String, int[]> menuCells = new java.util.HashMap<>();

    // ---- chat ----
    /** Lines matching one of these never show in chat. */
    public java.util.List<dev.landofif.dofisaddons.chat.ChatFilter> chatFilters = new java.util.ArrayList<>();
    /** Where the chat box sits, as an offset from the vanilla spot in gui units (right and down are positive). */
    public int chatX = 0;
    public int chatY = 0;
    /** Right click a chat line (with chat open) to copy it. */
    public boolean chatCopy = true;
    /** The welcome lines fishonmc prints at every login (instance, packs, welcome, help, rules, discord). */
    public boolean hideLoginChat = true;
    /** Lines with nothing in them (the server sends them as spacing) never reach the chat. */
    public boolean hideEmptyChat = true;
    /** Repeats of the same chat line become one line with a count, like "text (x3)". */
    public boolean chatStack = true;
    /** Separate chat boxes (dms, crew chat). null until sanitize fills in the defaults. */
    public java.util.List<dev.landofif.dofisaddons.chat.ChatBox> chatBoxes = null;
    /** Keys that run a command when pressed in game, see input/KeyCommands. */
    public java.util.List<dev.landofif.dofisaddons.input.KeyCommands.Entry> keyCommands = new java.util.ArrayList<>();

    // ---- word swaps (see display/WordSwap) ----
    public boolean wordSwap = true;
    public java.util.List<dev.landofif.dofisaddons.display.WordSwap.Entry> words = new java.util.ArrayList<>();

    // ---- bobber behaviour (only you see these) ----
    /** Hold the bobber still on the water instead of bobbing (it still dips on a bite). */
    public boolean stillBobber = true;
    /** Show your own cast flying smoothly instead of snapping back on a laggy server. */
    public boolean smoothCast = true;
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
        OFF("off"), DEFAULT("default"), FILL("fill"), OUTLINE("outline"), BOTH("fill + outline");

        public final String label;

        BoxLook(String label) {
            this.label = label;
        }
    }

    public enum SelectionStyle {
        FRAME("frame"), ROUND("round"), SIMPLE("simple"), NONE("off");

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
        hotbarSpacing = Math.max(16, Math.min(24, hotbarSpacing));
        equipSpacing = Math.max(16, Math.min(26, equipSpacing));
        boxOpacity = Math.max(0, Math.min(100, boxOpacity));
        boxOutlineColor &= 0xFFFFFF;
        if (hotbarSelection == null) hotbarSelection = SelectionStyle.FRAME;
        if (chatFilters == null) chatFilters = new java.util.ArrayList<>();
        chatFilters.removeIf(java.util.Objects::isNull);
        if (keyCommands == null) keyCommands = new java.util.ArrayList<>();
        keyCommands.removeIf(java.util.Objects::isNull);
        for (var k : keyCommands) {
            if (k.command == null) k.command = "";
        }
        if (words == null) words = new java.util.ArrayList<>();
        words.removeIf(java.util.Objects::isNull);
        for (var w : words) {
            if (w.from == null) w.from = "";
            if (w.to == null) w.to = "";
        }
        if (chatBoxes == null) chatBoxes = dev.landofif.dofisaddons.chat.ChatBoxes.defaults();
        chatBoxes.removeIf(java.util.Objects::isNull);
        for (dev.landofif.dofisaddons.chat.ChatBox b : chatBoxes) {
            if (b.name == null) b.name = "";
            if (b.rules == null) b.rules = new java.util.ArrayList<>();
            b.rules.removeIf(java.util.Objects::isNull);
            b.width = Math.max(80, Math.min(600, b.width));
            b.lines = Math.max(1, Math.min(30, b.lines));
            b.linesOpen = Math.max(b.lines, Math.min(40, b.linesOpen));
            b.fadeSeconds = b.fadeSeconds == 0 ? 10 : Math.max(-1, Math.min(600, b.fadeSeconds));
        }
        lineColor &= 0xFFFFFF;
        vmScale = Math.max(0.1, Math.min(3, vmScale));
        swingSlowness = Math.max(0.25, Math.min(4, swingSlowness));
        swingTurn = Math.max(0, Math.min(100, swingTurn));
        if (locLevels == null) locLevels = new java.util.LinkedHashMap<>();
        if (need == null) need = new java.util.LinkedHashMap<>();
        if (locationLevels == null) locationLevels = new java.util.LinkedHashMap<>();
        if (presetCache == null) presetCache = new java.util.LinkedHashMap<>();
        if (petHeads == null) petHeads = new java.util.LinkedHashMap<>();
        if (presetStats == null) presetStats = new java.util.LinkedHashMap<>();
        menuSize = Math.max(40, Math.min(100, menuSize));
        presetsSize = Math.max(40, Math.min(100, presetsSize));
    }
}
