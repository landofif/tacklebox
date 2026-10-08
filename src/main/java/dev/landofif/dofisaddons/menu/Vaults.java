package dev.landofif.dofisaddons.menu;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.DofisAddons;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import dev.landofif.dofisaddons.util.Glyphs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the seven personal vaults (/pv 1-7) held when you last had them open, kept in
 * config/dofisaddons/vaults.nbt so the overview screen can show all of them at once. The mod never opens a
 * vault itself, a vault is remembered when you open it. Only the 45 item slots count: the last row of the
 * chest is placeholder panes plus the Close and Next buttons.
 */
public final class Vaults {
    public static final int COUNT = 7;
    public static final int SLOTS = 45;
    private static final Pattern TITLE = Pattern.compile("^personal vault #?(\\d+)");

    private static final List<List<ItemStack>> DATA = new ArrayList<>();
    private static boolean loaded;
    /** Bumped on every change, so the screen knows when to search again. */
    public static int version;

    private Vaults() {}

    /** Remembers the vault when the plain chest view (the "chest view" button) is closed after moving items. */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GenericContainerScreen chest && number(chest) > 0) {
                ScreenEvents.remove(screen).register(s -> snapshot(number(chest), chest.getScreenHandler()));
            }
        });
    }

    /** Which vault this screen is (1-7), or 0. */
    public static int number(Screen screen) {
        if (!(screen instanceof GenericContainerScreen)) {
            return 0;
        }
        Matcher m = TITLE.matcher(Glyphs.plain(screen.getTitle()));
        int n = m.find() ? Integer.parseInt(m.group(1)) : 0;
        return n >= 1 && n <= COUNT ? n : 0;
    }

    /** The remembered items of vault n, 45 stacks, or null when it was never opened. */
    public static List<ItemStack> get(int n) {
        load();
        return DATA.get(n - 1);
    }

    /** Copies the item slots of an open vault; saves only when something changed. */
    public static void snapshot(int n, ScreenHandler handler) {
        load();
        if (handler.slots.size() < SLOTS) {
            return;
        }
        List<ItemStack> old = DATA.get(n - 1);
        boolean same = old != null;
        List<ItemStack> now = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack st = handler.slots.get(i).getStack();
            same &= ItemStack.areEqual(st, old == null ? ItemStack.EMPTY : old.get(i));
            now.add(st);
        }
        if (same) {
            return;
        }
        now.replaceAll(ItemStack::copy);
        DATA.set(n - 1, now);
        version++;
        save();
    }

    private static Path file() {
        return DofiSettings.dir().resolve("vaults.nbt");
    }

    private static void load() {
        var world = MinecraftClient.getInstance().world;
        if (loaded || world == null) {
            return;
        }
        loaded = true;
        DATA.clear();
        for (int i = 0; i < COUNT; i++) {
            DATA.add(null);
        }
        if (!Files.isRegularFile(file())) {
            return;
        }
        try {
            NbtCompound root = NbtIo.readCompressed(file(), NbtSizeTracker.ofUnlimitedBytes());
            var ops = world.getRegistryManager().getOps(NbtOps.INSTANCE);
            for (int n = 1; n <= COUNT; n++) {
                NbtList list = root.get(String.valueOf(n)) instanceof NbtList l ? l : null;
                if (list == null || list.size() != SLOTS) {
                    continue;
                }
                List<ItemStack> items = new ArrayList<>(SLOTS);
                for (NbtElement e : list) {
                    items.add(ItemStack.OPTIONAL_CODEC.parse(ops, e).result().orElse(ItemStack.EMPTY));
                }
                DATA.set(n - 1, items);
            }
            version++;
        } catch (IOException e) {
            DofisAddons.LOGGER.error("Could not read {}", file(), e);
        }
    }

    private static void save() {
        var world = MinecraftClient.getInstance().world;
        if (world == null) {
            return;
        }
        var ops = world.getRegistryManager().getOps(NbtOps.INSTANCE);
        NbtCompound root = new NbtCompound();
        for (int n = 1; n <= COUNT; n++) {
            List<ItemStack> items = DATA.get(n - 1);
            if (items == null) {
                continue;
            }
            NbtList list = new NbtList();
            for (ItemStack st : items) {
                list.add(ItemStack.OPTIONAL_CODEC.encodeStart(ops, st).result().orElseGet(NbtCompound::new));
            }
            root.put(String.valueOf(n), list);
        }
        try {
            Files.createDirectories(file().getParent());
            NbtIo.writeCompressed(root, file());
        } catch (IOException e) {
            DofisAddons.LOGGER.error("Could not write {}", file(), e);
        }
    }
}
