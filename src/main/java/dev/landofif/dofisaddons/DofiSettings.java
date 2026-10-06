package dev.landofif.dofisaddons;

/** Small global settings. Saved to {@code config/dofisaddons/settings.json}. */
public class DofiSettings {
    /** Stop the "time fishing" clock after this many seconds without a catch. */
    public int pauseAfterSeconds = 120;

    /** Hide the vanilla XP bar and level number on FishOnMC (foecollabs bar and level hud replace them). */
    public boolean hideVanillaXp = true;

    // ---- look (system24 for the whole game, see ui/S24) ----
    /** Which system24 flavor the colours come from. */
    public dev.landofif.dofisaddons.ui.Flavor flavor = dev.landofif.dofisaddons.ui.Flavor.SYSTEM24;
    /** Square system24 panels, or caelestia shell's rounded borderless ones. */
    public Style style = Style.SYSTEM24;
    /** Buttons, sliders, fields, lists, screen backgrounds and tooltips in every screen, not just the mod's. */
    public boolean s24Everywhere = true;
    /** Flat hotbar boxes instead of the stone hotbar. */
    public boolean s24Hotbar = true;
    /** Chests, the inventory and fishonmc menus as panels with flat slots (drops the server's menu art). */
    public boolean s24Menus = true;
    /** The font of the mods own text (and of the whole game with the option below). Order matches util/Mono. */
    public Font font = Font.DM_MONO;
    /** Draw every text in the game with DM Mono instead of the minecraft font. */
    public boolean monoEverywhere = true;
    /** Which title screen to show. null until sanitize, so old files fall back to {@link #titleScreen}. */
    public TitleChoice titleChoice = null;
    /** Old on/off switch for the mod's title screen, only read to fill in {@link #titleChoice}. */
    public boolean titleScreen = true;
    /** Draw foecollabs hud boxes as system24 panels (see compat/FoeLook). */
    public boolean foeHuds = true;

    // ---- fishonmc menu (see menu/FishMenu) ----
    /** Show the main menu and /travel as the mods own screens instead of the chest gui. */
    public boolean fishMenu = true;
    /** The /presets wardrobe screen, and for both custom guis: size in percent of the screen and accent colour. */
    public boolean presetsMenu = true;
    public int menuSize = 100;
    public int presetsSize = 100;
    public int menuColor = DEFAULT_GUI_COLOR;
    public int presetsColor = DEFAULT_GUI_COLOR;
    /** The system24 accent: a gui colour left at this follows the flavor. */
    public static final int DEFAULT_GUI_COLOR = 0xB589D6;
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
    /** Repeats of the same chat line become one line with a count, like "text (x3)". */
    public boolean chatStack = true;
    /** Separate chat boxes (dms, crew chat). null until sanitize fills in the defaults. */
    public java.util.List<dev.landofif.dofisaddons.chat.ChatBox> chatBoxes = null;
    /** Keys that run a command when pressed in game, see input/KeyCommands. */
    public java.util.List<dev.landofif.dofisaddons.input.KeyCommands.Entry> keyCommands = new java.util.ArrayList<>();

    // ---- bobber timer ----
    public BobberMode bobberTimer = BobberMode.ABOVE_BOBBER;
    /** Turn the timer text the warn colour after this many seconds (0 = never). */
    public double bobberWarnSeconds = 13;
    /** Turn the timer text the slow colour after this many seconds (0 = never). */
    public double bobberSlowSeconds = 20;
    /** RGB of the timer text: normal, warn, slow, and once something bites. */
    public int bobberColor = 0xEEEEEE;
    public int bobberWarnColor = 0xD7B65E;
    public int bobberSlowColor = 0xDF7A9B;
    public int bobberBiteColor = 0x3BB693;
    /** Show "bite!" in front of the time once the server announces a bite. */
    public boolean bobberShowBite = true;
    /** Draw the timer in the medium (bold) face. */
    public boolean bobberBold = false;
    /** Digits after the point (0 to 2). */
    public int bobberDecimals = 1;
    /** Size of the timer text, 1 = normal. */
    public double bobberScale = 1.0;
    /** Blocks above the bobber (above bobber mode). */
    public double bobberHeight = 0.75;
    /** Opacity of the box behind the timer above the bobber, 0 to 100. */
    public int bobberBacking = 33;
    /** Show the timer above the bobber through blocks and water. */
    public boolean bobberThroughWalls = true;
    /** Gui units below the crosshair (under crosshair mode). */
    public int bobberCrosshairY = 12;

    /** Show a short message in the middle of the screen when your fish bites. */
    public boolean biteMessage = false;
    public String biteMessageText = "fish on!";
    public int biteMessageColor = 0x3BB693;

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
    /** Trim caught fish tooltips: latin name, value, ecosystem, metric units. */
    public boolean shortFishTooltips = false;
    /** Trim pet tooltips: maxed stat pairs and empty skin / item / trail slots. */
    public boolean shortPetTooltips = false;

    // ---- bobber looks (only you see these) ----
    public BobberSkin bobberSkin = BobberSkin.DEFAULT;
    /** Also show your skin on other players' bobbers. */
    public boolean skinEveryone = false;
    public boolean customLineColor = false;
    /** RGB of your own fishing line. */
    public int lineColor = 0x3FA9F5;

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

    public enum Font {
        DM_MONO("dm mono"), JETBRAINS_MONO("jetbrains mono"), MINECRAFT("minecraft");

        public final String label;

        Font(String label) {
            this.label = label;
        }
    }

    public enum Style {
        SYSTEM24("system24"), CAELESTIA("caelestia"), MINECRAFT("minecraft");

        public final String label;

        Style(String label) {
            this.label = label;
        }
    }

    public enum TitleChoice {
        DOFIS("dofis addons"), FOECOLLAB("foecollab"), MINECRAFT("minecraft");

        public final String label;

        TitleChoice(String label) {
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

    public enum BobberMode {
        ABOVE_BOBBER("above bobber"), UNDER_CROSSHAIR("under crosshair"), OFF("off");

        public final String label;

        BobberMode(String label) {
            this.label = label;
        }

        public BobberMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public void sanitize() {
        if (flavor == null) flavor = dev.landofif.dofisaddons.ui.Flavor.SYSTEM24;
        if (style == null) style = Style.SYSTEM24;
        if (font == null) font = Font.DM_MONO;
        if (titleChoice == null) titleChoice = titleScreen ? TitleChoice.DOFIS : TitleChoice.MINECRAFT;
        if (bobberTimer == null) bobberTimer = BobberMode.ABOVE_BOBBER;
        if (bobberSkin == null) bobberSkin = BobberSkin.DEFAULT;
        if (swingStyle == null) swingStyle = SwingStyle.NORMAL;
        if (chatFilters == null) chatFilters = new java.util.ArrayList<>();
        chatFilters.removeIf(java.util.Objects::isNull);
        if (keyCommands == null) keyCommands = new java.util.ArrayList<>();
        keyCommands.removeIf(java.util.Objects::isNull);
        for (var k : keyCommands) {
            if (k.command == null) k.command = "";
        }
        if (chatBoxes == null) chatBoxes = dev.landofif.dofisaddons.chat.ChatBoxes.defaults();
        chatBoxes.removeIf(java.util.Objects::isNull);
        for (dev.landofif.dofisaddons.chat.ChatBox b : chatBoxes) {
            if (b.name == null) b.name = "";
            if (b.rules == null) b.rules = new java.util.ArrayList<>();
            b.rules.removeIf(java.util.Objects::isNull);
            b.width = Math.max(80, Math.min(600, b.width));
            b.lines = Math.max(1, Math.min(30, b.lines));
            b.fadeSeconds = b.fadeSeconds == 0 ? 10 : Math.max(-1, Math.min(600, b.fadeSeconds));
        }
        lineColor &= 0xFFFFFF;
        biteMessageColor &= 0xFFFFFF;
        if (biteMessageText == null) biteMessageText = "";
        vmScale = Math.max(0.1, Math.min(3, vmScale));
        swingSlowness = Math.max(0.25, Math.min(4, swingSlowness));
        swingTurn = Math.max(0, Math.min(100, swingTurn));
        pauseAfterSeconds = Math.max(10, Math.min(1800, pauseAfterSeconds));
        bobberSlowSeconds = Math.max(0, Math.min(120, bobberSlowSeconds));
        bobberWarnSeconds = Math.max(0, Math.min(120, bobberWarnSeconds));
        bobberColor &= 0xFFFFFF;
        menuSize = Math.max(40, Math.min(100, menuSize));
        presetsSize = Math.max(40, Math.min(100, presetsSize));
        menuColor &= 0xFFFFFF;
        presetsColor &= 0xFFFFFF;
        bobberWarnColor &= 0xFFFFFF;
        bobberSlowColor &= 0xFFFFFF;
        bobberBiteColor &= 0xFFFFFF;
        bobberDecimals = Math.max(0, Math.min(2, bobberDecimals));
        bobberScale = Math.max(0.5, Math.min(3, bobberScale));
        bobberHeight = Math.max(0, Math.min(3, bobberHeight));
        bobberBacking = Math.max(0, Math.min(100, bobberBacking));
        bobberCrosshairY = Math.max(-120, Math.min(120, bobberCrosshairY));
    }
}
