package dev.landofif.tacklebox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigButton;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import dev.landofif.tacklebox.hud.HudPos;
import net.fabricmc.loader.api.FabricLoader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen;
import com.teamresourceful.resourcefulconfig.api.types.info.Translatable;
import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** The mods settings, one {@code config/tacklebox/settings.json}. Read them with {@link #get()}. */
@Config("tacklebox")
public class TackleboxSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static Configurator configurator;
    private static TackleboxSettings current = new TackleboxSettings();

    public static TackleboxSettings get() {
        return current;
    }

    private static Configurator configurator() {
        if (configurator == null) {
            configurator = new Configurator(Tacklebox.MOD_ID);
            configurator.register(TackleboxSettings.class);
        }
        return configurator;
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve(Tacklebox.MOD_ID);
    }

    public static void load() {
        Path path = dir().resolve("settings.json");
        current = new TackleboxSettings();
        String raw = null;
        if (Files.isRegularFile(path)) {
            try {
                raw = Files.readString(path, StandardCharsets.UTF_8);
                TackleboxSettings read = GSON.fromJson(raw, TackleboxSettings.class);
                if (read != null) {
                    current = read;
                }
            } catch (IOException | JsonParseException e) {
                Tacklebox.LOGGER.error("Could not read {}; keeping a backup and starting fresh", path, e);
                raw = null;
                try {
                    Files.copy(path, path.resolveSibling("settings.json.broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                    // nothing more we can do
                }
            }
        }
        if (!current.configMoved) { // the options lived in settings.json before Resourceful Config
            if (raw != null) {
                try {
                    importLegacy(JsonParser.parseString(raw).getAsJsonObject());
                } catch (RuntimeException e) {
                    Tacklebox.LOGGER.error("Could not move the options out of {}", path, e);
                }
            }
            current.configMoved = true;
            configurator().saveConfig(TackleboxSettings.class);
        } else {
            configurator().loadConfig(TackleboxSettings.class);
        }
        current.sanitize();
    }

    /** The options as the field initialisers set them, kept to start an import from. */
    private static final Map<Field, Object> DEFAULTS = new HashMap<>();

    private static List<Field> options() {
        List<Field> out = new ArrayList<>();
        for (Field f : TackleboxSettings.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.isAnnotationPresent(ConfigEntry.class)) {
                out.add(f);
            }
        }
        return out;
    }

    /** Sets every option to its default, then takes the ones {@code old} has: a pre Resourceful Config settings.json. */
    public static void importLegacy(JsonObject old) {
        for (Field f : options()) {
            try {
                Object value = DEFAULTS.get(f);
                JsonElement e = old.get(f.getName());
                if (e != null && !e.isJsonNull()) {
                    Class<?> t = f.getType();
                    if (t == boolean.class) value = e.getAsBoolean();
                    else if (t == int.class) value = e.getAsInt();
                    else if (t == double.class) value = e.getAsDouble();
                    else if (t.isEnum()) value = enumValue(t, e.getAsString(), value);
                    else if (t == String.class) value = e.getAsString();
                }
                f.set(null, value);
            } catch (IllegalAccessException | RuntimeException ex) {
                Tacklebox.LOGGER.warn("Could not move option {}", f.getName(), ex);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> type, String name, Object fallback) {
        try {
            return Enum.valueOf((Class<? extends Enum>) type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** The settings screen (Resourceful Config's); leaving it keeps the values in range and saves. */
    public static Screen openScreen(Screen parent) {
        if (!closeHooked) {
            closeHooked = true;
            ScreenEvents.AFTER_INIT.register((client, opened, w, h) -> {
                if (opened != pending) {
                    return;
                }
                pending = null;
                boolean leaves = flatLeaves;
                // the remove event only works on a screen that setScreen has already initialised
                ScreenEvents.remove(opened).register(closed -> {
                    current.sanitize();
                    save();
                    if (leaves != flatLeaves) {
                        MinecraftClient.getInstance().reloadResources();
                    }
                });
            });
        }
        Screen screen = ResourcefulConfigScreen.make(configurator(), TackleboxSettings.class).withParent(parent).build();
        pending = screen;
        return screen;
    }

    private static boolean closeHooked;
    private static Screen pending;

    /** A button of the settings screen: opens one of the mods own screens, which returns to the settings screen when closed. */
    public static void openLinked(java.util.function.Function<Screen, Screen> screen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Screen next = screen.apply(mc.currentScreen);
        if (next != null) {
            mc.setScreen(next);
        }
    }

    private static void openMenuPictureFolder() {
        try {
            Files.createDirectories(dev.landofif.tacklebox.display.MenuBackground.IMAGE.getParent());
        } catch (IOException ignored) {
            // opening the folder is the only thing it is for
        }
        net.minecraft.util.Util.getOperatingSystem().open(dev.landofif.tacklebox.display.MenuBackground.IMAGE.getParent());
    }

    public static void save() {
        current.sanitize();
        configurator().saveConfig(TackleboxSettings.class);
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
    // ---- buttons that open the mods other screens (first in the settings screen) ----
    @ConfigButton(title = "ᴄʜᴀᴛ ᴛʀɪɢɢᴇʀꜱ", text = "ᴏᴘᴇɴ")
    public static final Runnable openChatTriggers = () -> openLinked(dev.landofif.tacklebox.screen.TriggerListScreen::new);
    @ConfigButton(title = "ᴄʜᴀᴛ", text = "ᴏᴘᴇɴ")
    public static final Runnable openChat = () -> openLinked(dev.landofif.tacklebox.screen.ChatFilterScreen::new);
    @ConfigButton(title = "ᴋᴇʏ ᴄᴏᴍᴍᴀɴᴅꜱ", text = "ᴏᴘᴇɴ")
    public static final Runnable openKeyCommands = () -> openLinked(dev.landofif.tacklebox.screen.KeyCommandScreen::new);
    @ConfigButton(title = "ᴄᴏᴍᴍᴀɴᴅ ᴀʟɪᴀꜱᴇꜱ", text = "ᴏᴘᴇɴ")
    public static final Runnable openCommandAliases = () -> openLinked(dev.landofif.tacklebox.screen.AliasScreen::new);
    @ConfigButton(title = "ᴡᴏʀᴅ ꜱᴡᴀᴘꜱ", text = "ᴏᴘᴇɴ")
    public static final Runnable openWordSwaps = () -> openLinked(dev.landofif.tacklebox.screen.WordSwapScreen::new);
    @ConfigButton(title = "ʙᴀɪᴛ ᴘɪᴄᴋᴇʀ", text = "ᴏᴘᴇɴ")
    public static final Runnable openBaitPicker = () -> openLinked(dev.landofif.tacklebox.screen.BaitScreen::new);
    @ConfigButton(title = "ᴘᴏᴡᴇʀ", text = "ᴏᴘᴇɴ")
    public static final Runnable openPower = () -> openLinked(dev.landofif.tacklebox.screen.PowerScreen::new);
    @ConfigButton(title = "ʙᴏʙʙᴇʀ ᴛɪᴍᴇʀ", text = "ᴏᴘᴇɴ")
    public static final Runnable openBobberTimer = () -> openLinked(dev.landofif.tacklebox.screen.BobberScreen::new);
    @ConfigButton(title = "ᴠɪᴇᴡᴍᴏᴅᴇʟ", text = "ᴏᴘᴇɴ")
    public static final Runnable openViewmodel = () -> openLinked(dev.landofif.tacklebox.screen.ViewmodelScreen::new);
    @ConfigButton(title = "ꜱᴇʀᴠᴇʀ ʜᴜᴅ ʟɪɴᴇꜱ", text = "ᴏᴘᴇɴ")
    public static final Runnable openServerHudLines = () -> openLinked(dev.landofif.tacklebox.screen.ServerHudScreen::new);
    @ConfigButton(title = "ᴍᴏᴠᴇ ʜᴜᴅꜱ", text = "ᴏᴘᴇɴ")
    public static final Runnable openMoveHuds = () -> openLinked(dev.landofif.tacklebox.compat.FoeScreens::move);
    @ConfigButton(title = "ᴍᴇɴᴜ ᴘɪᴄᴛᴜʀᴇ ꜰᴏʟᴅᴇʀ", text = "ᴏᴘᴇɴ ꜰᴏʟᴅᴇʀ")
    public static final Runnable openMenuPicture = TackleboxSettings::openMenuPictureFolder;

    // ---- options shown in the settings screen (Resourceful Config reads these static fields) ----
    /** The two colours of the gradient on focused tiles, hovered buttons and toggles (rgb). */
    @ConfigOption.Separator(value = "ᴀᴄᴄᴇɴᴛ")
    @ConfigEntry(id = "accent1", translation = "tacklebox.config.accent1")
    @ConfigOption.Color
    public static int accent1 = 0x33CCFF;
    @ConfigEntry(id = "accent2", translation = "tacklebox.config.accent2")
    @ConfigOption.Color
    public static int accent2 = 0x00FF99;
    /** Chests, the inventory and fishonmc menus as one box with no slot boxes (drops the server's menu art). */
    @ConfigOption.Separator(value = "ᴍᴇɴᴜꜱ")
    @ConfigEntry(id = "flatMenus", translation = "tacklebox.config.flatMenus")
    public static boolean flatMenus = true;
    /** What shows behind the box-less menus (display/MenuBackground): a colour and a picture, each in percent opacity (0 = none). */
    @ConfigEntry(id = "menuAccent", translation = "tacklebox.config.menuAccent")
    public static boolean menuAccent;
    @ConfigEntry(id = "menuAccentOpacity", translation = "tacklebox.config.menuAccentOpacity")
    @ConfigOption.Range(min = 5, max = 100)
    @ConfigOption.Slider
    public static int menuAccentOpacity = 40;
    @ConfigEntry(id = "menuBgColor", translation = "tacklebox.config.menuBgColor")
    @ConfigOption.Color
    public static int menuBgColor = 0x000000;
    @ConfigEntry(id = "menuBgOpacity", translation = "tacklebox.config.menuBgOpacity")
    @ConfigOption.Range(min = 0, max = 100)
    @ConfigOption.Slider
    public static int menuBgOpacity = 0;
    @ConfigEntry(id = "menuImageOpacity", translation = "tacklebox.config.menuImageOpacity")
    @ConfigOption.Range(min = 0, max = 100)
    @ConfigOption.Slider
    public static int menuImageOpacity = 0;
    /** Every button as a flat see-through fill (lighter while hovered) instead of the bevelled texture. */
    @ConfigEntry(id = "flatButtons", translation = "tacklebox.config.flatButtons")
    public static boolean flatButtons = true;
    /** A see-through circle in the rarity colour behind items in slots (foecollabs rarity, drawn by its item markers). */
    @ConfigEntry(id = "rarityCircles", translation = "tacklebox.config.rarityCircles")
    public static boolean rarityCircles = false;
    /** Show the main menu and /travel as the mods own screens instead of the chest gui. */
    @ConfigEntry(id = "fishMenu", translation = "tacklebox.config.fishMenu")
    public static boolean fishMenu = true;
    @ConfigEntry(id = "menuSize", translation = "tacklebox.config.menuSize")
    @ConfigOption.Range(min = 40, max = 100)
    @ConfigOption.Slider
    public static int menuSize = 100;
    /** The /presets wardrobe screen, and for both custom guis: size in percent of the screen. */
    @ConfigOption.Separator(value = "ᴘʀᴇꜱᴇᴛꜱ")
    @ConfigEntry(id = "presetsMenu", translation = "tacklebox.config.presetsMenu")
    public static boolean presetsMenu = true;
    @ConfigEntry(id = "presetsSize", translation = "tacklebox.config.presetsSize")
    @ConfigOption.Range(min = 40, max = 100)
    @ConfigOption.Slider
    public static int presetsSize = 100;
    @ConfigEntry(id = "presetsOtherBg", translation = "tacklebox.config.presetsOtherBg")
    public static boolean presetsOtherBg = false;
    /** /pv as all seven personal vaults on one screen with a search (screen/VaultScreen). */
    @ConfigOption.Separator(value = "ᴠᴀᴜʟᴛꜱ")
    @ConfigEntry(id = "vaultMenu", translation = "tacklebox.config.vaultMenu")
    public static boolean vaultMenu = true;
    /** Caught fish tooltips without the scientific name, value and ecosystem, weight and length on one line (display/FishTooltip). */
    @ConfigOption.Separator(value = "ᴛᴏᴏʟᴛɪᴘꜱ")
    @ConfigEntry(id = "shortFishTooltips", translation = "tacklebox.config.shortFishTooltips")
    public static boolean shortFishTooltips = false;
    /** Pet tooltips without the kind line, empty equipment lines and the right click hint (display/PetTooltip). */
    @ConfigEntry(id = "shortPetTooltips", translation = "tacklebox.config.shortPetTooltips")
    public static boolean shortPetTooltips = false;
    /** Auction house pets you could sell to the pet store for more than they cost: a green tint and a line with the store's price (display/PetSale). */
    @ConfigEntry(id = "petProfitMarks", translation = "tacklebox.config.petProfitMarks")
    public static boolean petProfitMarks = false;
    /** What sits behind the hotbar and foecollabs equipment slots, the distance between slots in pixels (20 and 22 are the defaults), and the shared box colours. */
    @ConfigOption.Separator(value = "ʜᴏᴛʙᴀʀ")
    @ConfigEntry(id = "hotbarLook", translation = "tacklebox.config.hotbarLook")
    public static BoxLook hotbarLook = BoxLook.HYPR;
    @ConfigEntry(id = "hotbarSpacing", translation = "tacklebox.config.hotbarSpacing")
    @ConfigOption.Range(min = 16, max = 24)
    @ConfigOption.Slider
    public static int hotbarSpacing = 20;
    @ConfigEntry(id = "hotbarScale", translation = "tacklebox.config.hotbarScale")
    @ConfigOption.Range(min = 50, max = 200)
    @ConfigOption.Slider
    public static int hotbarScale = 100;
    /** How the held hotbar slot is marked, and in what colour; and whether the offhand box shows. */
    @ConfigEntry(id = "hotbarSelection", translation = "tacklebox.config.hotbarSelection")
    public static SelectionStyle hotbarSelection = SelectionStyle.FRAME;
    @ConfigEntry(id = "hotbarSelectColor", translation = "tacklebox.config.hotbarSelectColor")
    @ConfigOption.Color
    public static int hotbarSelectColor = 0xFFFFFF;
    @ConfigEntry(id = "hotbarOffhand", translation = "tacklebox.config.hotbarOffhand")
    public static boolean hotbarOffhand = true;
    /** The mods own armour and rod parts row (hud/EquipmentRow) instead of foecollabs, and the look version the saved settings were last moved to. */
    @ConfigOption.Separator(value = "ᴇǫᴜɪᴘᴍᴇɴᴛ")
    @ConfigEntry(id = "equipHud", translation = "tacklebox.config.equipHud")
    public static boolean equipHud = true;
    @ConfigEntry(id = "equipScale", translation = "tacklebox.config.equipScale")
    @ConfigOption.Range(min = 50, max = 200)
    @ConfigOption.Slider
    public static int equipScale = 100;
    @ConfigEntry(id = "equipPlace", translation = "tacklebox.config.equipPlace")
    public static EquipPlace equipPlace = EquipPlace.ABOVE;
    @ConfigEntry(id = "equipLook", translation = "tacklebox.config.equipLook")
    public static BoxLook equipLook = BoxLook.HYPR;
    @ConfigEntry(id = "equipSpacing", translation = "tacklebox.config.equipSpacing")
    @ConfigOption.Range(min = 16, max = 26)
    @ConfigOption.Slider
    public static int equipSpacing = 22;
    @ConfigEntry(id = "boxOutlineColor", translation = "tacklebox.config.boxOutlineColor")
    @ConfigOption.Color
    public static int boxOutlineColor = 0x808080;
    @ConfigEntry(id = "boxOpacity", translation = "tacklebox.config.boxOpacity")
    @ConfigOption.Range(min = 0, max = 100)
    @ConfigOption.Slider
    public static int boxOpacity = 50;
    /** Foecollabs rarity bar under each equipment slot as a circle behind the item, and a colour to use instead of the rarity's. */
    @ConfigEntry(id = "equipCircle", translation = "tacklebox.config.equipCircle")
    public static boolean equipCircle = true;
    @ConfigEntry(id = "equipRecolor", translation = "tacklebox.config.equipRecolor")
    public static boolean equipRecolor = false;
    @ConfigEntry(id = "equipColor", translation = "tacklebox.config.equipColor")
    @ConfigOption.Color
    public static int equipColor = 0xFFFFFF;
    /** The quests left hud: whether it shows. */
    @ConfigOption.Separator(value = "ǫᴜᴇꜱᴛꜱ ʟᴇꜰᴛ")
    @ConfigEntry(id = "questsHud", translation = "tacklebox.config.questsHud")
    public static boolean questsHud = true;
    @ConfigEntry(id = "questsAlign", translation = "tacklebox.config.questsAlign")
    public static HudAlign questsAlign = HudAlign.LEFT;
    /** The active boosts hud (hud/BoostsHud): boosters, chummer, weather events and moon, where it sits. */
    @ConfigOption.Separator(value = "ᴀᴄᴛɪᴠᴇ ʙᴏᴏꜱᴛꜱ")
    @ConfigEntry(id = "boostsHud", translation = "tacklebox.config.boostsHud")
    public static boolean boostsHud = true;
    @ConfigEntry(id = "boostsAlign", translation = "tacklebox.config.boostsAlign")
    public static HudAlign boostsAlign = HudAlign.LEFT;
    /** The level progress hud (hud/LevelBars): fishing level and location level bars, where it sits. */
    @ConfigOption.Separator(value = "ʟᴇᴠᴇʟ ᴘʀᴏɢʀᴇꜱꜱ")
    @ConfigEntry(id = "levelBars", translation = "tacklebox.config.levelBars")
    public static boolean levelBars = true;
    /** What the level progress hud shows: heading, the fishing line, the location line, the bar itself, the percent, the location xp, and the bar width. */
    @ConfigEntry(id = "levelBarsHeading", translation = "tacklebox.config.levelBarsHeading")
    public static boolean levelBarsHeading = true;
    @ConfigEntry(id = "levelBarsFishing", translation = "tacklebox.config.levelBarsFishing")
    public static boolean levelBarsFishing = true;
    @ConfigEntry(id = "levelBarsLocation", translation = "tacklebox.config.levelBarsLocation")
    public static boolean levelBarsLocation = true;
    @ConfigEntry(id = "levelBarsBar", translation = "tacklebox.config.levelBarsBar")
    public static boolean levelBarsBar = true;
    @ConfigEntry(id = "levelBarsPercent", translation = "tacklebox.config.levelBarsPercent")
    public static boolean levelBarsPercent = true;
    @ConfigEntry(id = "levelBarsXp", translation = "tacklebox.config.levelBarsXp")
    public static boolean levelBarsXp = true;
    @ConfigEntry(id = "levelBarsMainXp", translation = "tacklebox.config.levelBarsMainXp")
    public static boolean levelBarsMainXp = false;
    @ConfigEntry(id = "levelBarsDecimals", translation = "tacklebox.config.levelBarsDecimals")
    public static boolean levelBarsDecimals = true;
    @ConfigEntry(id = "levelBarsWidth", translation = "tacklebox.config.levelBarsWidth")
    @ConfigOption.Range(min = 40, max = 200)
    @ConfigOption.Slider
    public static int levelBarsWidth = 80;
    @ConfigEntry(id = "levelBarsAlign", translation = "tacklebox.config.levelBarsAlign")
    public static HudAlign levelBarsAlign = HudAlign.LEFT;
    /** The now playing hud (hud/NowPlaying): what Spotify, a browser tab or any other player is playing, where it sits. */
    @ConfigOption.Separator(value = "ɴᴏᴡ ᴘʟᴀʏɪɴɢ")
    @ConfigEntry(id = "nowPlaying", translation = "tacklebox.config.nowPlaying")
    public static boolean nowPlaying = true;
    /** What the now playing hud shows besides the title: its heading, the album art, the artist, a progress bar and the time. */
    @ConfigEntry(id = "nowPlayingHeading", translation = "tacklebox.config.nowPlayingHeading")
    public static boolean nowPlayingHeading = true;
    @ConfigEntry(id = "nowPlayingArt", translation = "tacklebox.config.nowPlayingArt")
    public static boolean nowPlayingArt = true;
    @ConfigEntry(id = "nowPlayingArtist", translation = "tacklebox.config.nowPlayingArtist")
    public static boolean nowPlayingArtist = true;
    @ConfigEntry(id = "nowPlayingBar", translation = "tacklebox.config.nowPlayingBar")
    public static boolean nowPlayingBar = true;
    @ConfigEntry(id = "nowPlayingTime", translation = "tacklebox.config.nowPlayingTime")
    public static boolean nowPlayingTime = true;
    @ConfigEntry(id = "nowPlayingAlign", translation = "tacklebox.config.nowPlayingAlign")
    public static HudAlign nowPlayingAlign = HudAlign.LEFT;
    /** The sidebar styled server hud (hud/ServerHud): where it sits, its size in percent and how strong the picture behind it is (percent). */
    @ConfigOption.Separator(value = "ꜱᴇʀᴠᴇʀ ʜᴜᴅ")
    @ConfigEntry(id = "serverHud", translation = "tacklebox.config.serverHud")
    public static boolean serverHud = true;
    @ConfigEntry(id = "serverHudAlign", translation = "tacklebox.config.serverHudAlign")
    public static HudAlign serverHudAlign = HudAlign.LEFT;
    /** Hold the bobber still on the water instead of bobbing (it still dips on a bite). */
    @ConfigOption.Separator(value = "ʙᴏʙʙᴇʀ")
    @ConfigEntry(id = "stillBobber", translation = "tacklebox.config.stillBobber")
    public static boolean stillBobber = true;
    /** Show your own cast flying smoothly instead of snapping back on a laggy server. */
    @ConfigEntry(id = "smoothCast", translation = "tacklebox.config.smoothCast")
    public static boolean smoothCast = true;
    /** Hide other players bobbers and their lines. */
    @ConfigEntry(id = "hideOtherBobbers", translation = "tacklebox.config.hideOtherBobbers")
    public static boolean hideOtherBobbers = false;
    @ConfigOption.Separator(value = "ꜱᴋɪɴ")
    @ConfigEntry(id = "bobberSkin", translation = "tacklebox.config.bobberSkin")
    public static BobberSkin bobberSkin = BobberSkin.DEFAULT;
    /** Also show your skin on other players' bobbers. */
    @ConfigEntry(id = "skinEveryone", translation = "tacklebox.config.skinEveryone")
    public static boolean skinEveryone = false;
    @ConfigOption.Separator(value = "ʟɪɴᴇ")
    @ConfigEntry(id = "customLineColor", translation = "tacklebox.config.customLineColor")
    public static boolean customLineColor = false;
    /** RGB of your own fishing line. */
    @ConfigEntry(id = "lineColor", translation = "tacklebox.config.lineColor")
    @ConfigOption.Color
    public static int lineColor = 0x3FA9F5;
    /** The welcome lines fishonmc prints at every login (instance, packs, welcome, help, rules, discord). */
    @ConfigOption.Separator(value = "ᴄʜᴀᴛ")
    @ConfigEntry(id = "hideLoginChat", translation = "tacklebox.config.hideLoginChat")
    public static boolean hideLoginChat = true;
    /** Lines with nothing in them (the server sends them as spacing) never reach the chat. */
    @ConfigEntry(id = "hideEmptyChat", translation = "tacklebox.config.hideEmptyChat")
    public static boolean hideEmptyChat = true;
    /** Huds that would sit under a chat window or the player list are not drawn. */
    @ConfigEntry(id = "hideBehindChat", translation = "tacklebox.config.hideBehindChat")
    public static boolean hideBehindChat = false;
    /** Hide the rank / name / power tag the server puts over your own head. */
    @ConfigOption.Separator(value = "ɴᴀᴍᴇᴛᴀɢꜱ")
    @ConfigEntry(id = "hideOwnNametag", translation = "tacklebox.config.hideOwnNametag")
    public static boolean hideOwnNametag = false;
    /** Hide every players nametag, the servers and the vanilla one. */
    @ConfigEntry(id = "hideAllNametags", translation = "tacklebox.config.hideAllNametags")
    public static boolean hideAllNametags = false;
    /** Hide the +66xp reward line in the action bar after a catch. */
    @ConfigOption.Separator(value = "ᴄʟᴜᴛᴛᴇʀ")
    @ConfigEntry(id = "hideXpPopups", translation = "tacklebox.config.hideXpPopups")
    public static boolean hideXpPopups = false;
    /** Draw leaves flat like vanilla instead of the server packs 3d leaves. */
    @ConfigEntry(id = "flatLeaves", translation = "tacklebox.config.flatLeaves")
    public static boolean flatLeaves = false;
    /** Stop drawing the servers item, block and text displays farther than this many blocks (8 to 64), a fps help at spawn (display/DisplayCull). */
    @ConfigOption.Separator(value = "ʟᴀɢ")
    @ConfigEntry(id = "hideFarDisplays", translation = "tacklebox.config.hideFarDisplays")
    public static boolean hideFarDisplays = false;
    @ConfigEntry(id = "farDisplayRange", translation = "tacklebox.config.farDisplayRange")
    @ConfigOption.Range(min = 8, max = 64)
    @ConfigOption.Slider
    public static int farDisplayRange = 24;
    /** A thin flat ring on the ground at the range of every placed chummer in view of the client (display/ChummerRing), and its colour (rgb). */
    @ConfigOption.Separator(value = "ᴄʜᴜᴍᴍᴇʀ")
    @ConfigEntry(id = "chummerRing", translation = "tacklebox.config.chummerRing")
    public static boolean chummerRing = true;
    @ConfigEntry(id = "chummerRingColor", translation = "tacklebox.config.chummerRingColor")
    @ConfigOption.Color
    public static int chummerRingColor = 0x41F55B;

    // ---- state and the rest of the look ----
    /** The hotbar: moved by this many gui pixels from where vanilla puts it, and whether its slot background is drawn. */
    public int hotbarX = 0;
    public int hotbarY = 0;
    /** The old on/off for the hotbar background; only read once to fill {@link #hotbarLook} and {@link #equipLook}. */
    public boolean hotbarBackground = false;
    /** Foecollabs equipment hud (armour and rod parts), moved by this much on top of the hotbar's offset. */
    public int equipX = 0;
    public int equipY = 0;
    public int lookVersion = 0;
    /** The options moved to Resourceful Config's own file (see {@link #load()}). */
    public boolean configMoved = false;

    // ---- fishonmc menu (see menu/FishMenu) ----
    /**
     * Where the five huds below sit lives in their {@code *Pos} (distance to the nearest window sides, see hud/HudPos). The plain
     * {@code *X} and {@code *Y} are the spot a hud is drawn at this frame, set from its pos on every draw; settings saved before
     * pos existed have them as the spot and sanitize makes the pos from them. Edit a pos, never these.
     */
    public HudPos questsPos, levelBarsPos, boostsPos, nowPlayingPos, serverHudPos;
    public int questsX = 4;
    public int questsScale = 100;
    public int questsY = 70;
    public int levelBarsX = 4;
    public int levelBarsScale = 100;
    public int levelBarsY = 40;
    public int boostsX = 182;
    public int boostsScale = 100;
    public int boostsY = 70;
    public int nowPlayingX = 4;
    public int nowPlayingScale = 100;
    public int nowPlayingY = 4;
    public int serverHudX = 4;
    public int serverHudY = 120;
    public int serverHudScale = 100;
    public int serverHudImage = 25;
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
    /** Preset rows seen on /presets, kept between sessions so the other page can be drawn: "armor:6" to its pieces as "item|color|floats|strings|model". */
    public java.util.Map<String, java.util.List<String>> presetCache = new java.util.LinkedHashMap<>();
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
    /** Repeats of the same chat line become one line with a count, like "text (x3)". */
    public boolean chatStack = true;
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

    // ---- world (only you see these) ----

    // ---- bobber looks (only you see these) ----

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

    public enum BobberSkin implements Translatable {
        DEFAULT("default", null), OCEAN("ocean", "ocean"), GOLD("gold", "gold"), EMERALD("emerald", "emerald"),
        MIDNIGHT("midnight", "midnight"), CANDY("candy", "candy"), DUCK("duck", "duck"), EMBER("ember", "ember"),
        PEARL("pearl", "pearl"), STAR("star", "star"), RAINBOW("rainbow", "rainbow");

        public final String label;
        public final String texture;

        BobberSkin(String label, String texture) {
            this.label = label;
            this.texture = texture;
        }

        @Override
        public String getTranslationKey() {
            return "tacklebox.config.enum.bobberskin." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** How the text of a hud lines up inside its box. */
    public enum HudAlign implements Translatable {
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

        @Override
        public String getTranslationKey() {
            return "tacklebox.config.enum.hudalign." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Where foecollabs equipment row sits next to the hotbar, before any dragging. */
    public enum EquipPlace implements Translatable {
        ABOVE("above"), LEFT("left"), RIGHT("right");

        public final String label;

        EquipPlace(String label) {
            this.label = label;
        }

        @Override
        public String getTranslationKey() {
            return "tacklebox.config.enum.equipplace." + name().toLowerCase(java.util.Locale.ROOT);
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
    public enum BoxLook implements Translatable {
        HYPR("accent"), OFF("off"), DEFAULT("default"), FILL("fill"), OUTLINE("outline"), BOTH("fill + outline");

        public final String label;

        BoxLook(String label) {
            this.label = label;
        }

        @Override
        public String getTranslationKey() {
            return "tacklebox.config.enum.boxlook." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public enum SelectionStyle implements Translatable {
        HYPR("accent"), FRAME("frame"), ROUND("round"), SIMPLE("simple"), NONE("off");

        public final String label;

        SelectionStyle(String label) {
            this.label = label;
        }

        @Override
        public String getTranslationKey() {
            return "tacklebox.config.enum.selectionstyle." + name().toLowerCase(java.util.Locale.ROOT);
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

    // must stay below every option field: it reads their initial values
    static {
        try {
            for (Field f : options()) {
                DEFAULTS.put(f, f.get(null));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
