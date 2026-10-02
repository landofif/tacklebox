package dev.landofif.dofisaddons;

/** Small global settings. Saved to {@code config/dofisaddons/settings.json}. */
public class DofiSettings {
    /** Stop the "time fishing" clock after this many seconds without a catch. */
    public int pauseAfterSeconds = 120;

    /** Hide the vanilla XP bar and level number on FishOnMC (the level HUD replaces them). */
    public boolean hideVanillaXp = true;

    // ---- look (system24 for the whole game, see ui/S24) ----
    /** Buttons, sliders, fields, lists, screen backgrounds and tooltips in every screen, not just the mod's. */
    public boolean s24Everywhere = true;
    /** Flat hotbar boxes instead of the stone hotbar. */
    public boolean s24Hotbar = true;
    /** Chests, the inventory and fishonmc menus as panels with flat slots (drops the server's menu art). */
    public boolean s24Menus = true;
    /** Draw every text in the game with DM Mono instead of the minecraft font. */
    public boolean monoEverywhere = true;
    /** Replace the minecraft title screen with the mod's own, with a join fishonmc button. */
    public boolean titleScreen = true;

    // ---- chat ----
    /** Lines matching one of these never show in chat. */
    public java.util.List<dev.landofif.dofisaddons.chat.ChatFilter> chatFilters = new java.util.ArrayList<>();
    /** Where the chat box sits, as an offset from the vanilla spot in gui units (right and down are positive). */
    public int chatX = 0;
    public int chatY = 0;
    /** Right click a chat line (with chat open) to copy it. */
    public boolean chatCopy = true;

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
        if (bobberTimer == null) bobberTimer = BobberMode.ABOVE_BOBBER;
        if (bobberSkin == null) bobberSkin = BobberSkin.DEFAULT;
        if (swingStyle == null) swingStyle = SwingStyle.NORMAL;
        if (chatFilters == null) chatFilters = new java.util.ArrayList<>();
        chatFilters.removeIf(java.util.Objects::isNull);
        lineColor &= 0xFFFFFF;
        biteMessageColor &= 0xFFFFFF;
        if (biteMessageText == null) biteMessageText = "";
        vmScale = Math.max(0.1, Math.min(3, vmScale));
        swingSlowness = Math.max(0.25, Math.min(4, swingSlowness));
        pauseAfterSeconds = Math.max(10, Math.min(1800, pauseAfterSeconds));
        bobberSlowSeconds = Math.max(0, Math.min(120, bobberSlowSeconds));
        bobberWarnSeconds = Math.max(0, Math.min(120, bobberWarnSeconds));
        bobberColor &= 0xFFFFFF;
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
