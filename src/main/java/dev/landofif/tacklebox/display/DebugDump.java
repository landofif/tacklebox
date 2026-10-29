package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.Tacklebox;
import dev.landofif.tacklebox.screen.FishMenuScreen;
import dev.landofif.tacklebox.util.Glyphs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import com.mojang.serialization.JsonOps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * /tacklebox debug: prints server data to chat (and the log) for working out new features, ported from
 * fishutils. Read only.
 */
public final class DebugDump {
    private DebugDump() {}

    /** The held item's id, lore and raw custom_data (where fishonmc keeps bait, fish and pet data). */
    public static int heldItem() {
        ClientPlayerEntity self = MinecraftClient.getInstance().player;
        if (self == null) {
            return 0;
        }
        ItemStack stack = self.getMainHandStack();
        if (stack.isEmpty()) {
            say("hold an item in your main hand first.", Formatting.RED);
            return 0;
        }
        say("item " + Registries.ITEM.getId(stack.getItem()) + " x" + stack.getCount(), Formatting.AQUA);
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                say("lore: " + escape(line.getString()), Formatting.GRAY);
            }
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        String snbt = custom == null || custom.isEmpty() ? "(none)" : custom.copyNbt().toString();
        for (int i = 0; i < snbt.length(); i += 180) {
            say("custom_data: " + snbt.substring(i, Math.min(snbt.length(), i + 180)), Formatting.GRAY);
        }
        saveStyled(MinecraftClient.getInstance(), stack);
        return 1;
    }

    /**
     * The held item again, but with every Text as JSON (styles, colours, fonts, siblings) and the tooltip as the
     * client builds it, after other mods' tooltip callbacks. The chat lines above only carry plain strings.
     * Saved to config/tacklebox/guidump and copied to the clipboard.
     */
    private static void saveStyled(MinecraftClient client, ItemStack stack) {
        StringBuilder out = new StringBuilder();
        out.append("item ").append(Registries.ITEM.getId(stack.getItem())).append(" x").append(stack.getCount()).append('\n');
        out.append("custom_name=").append(json(client, stack.get(DataComponentTypes.CUSTOM_NAME))).append('\n');
        out.append("item_name=").append(json(client, stack.get(DataComponentTypes.ITEM_NAME))).append('\n');
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                out.append("lore=").append(json(client, line)).append('\n');
            }
        }
        out.append("\ntooltip (basic, as the client builds it)\n");
        for (Text line : stack.getTooltip(Item.TooltipContext.create(client.world), client.player, TooltipType.BASIC)) {
            out.append("tooltip=").append(json(client, line)).append('\n');
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom != null && !custom.isEmpty()) {
            out.append("\ncustom_data=").append(escape(custom.copyNbt().toString())).append('\n');
        }
        String text = out.toString();
        client.keyboard.setClipboard(text);
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve(Tacklebox.MOD_ID).resolve("guidump");
            Files.createDirectories(dir);
            Path file = dir.resolve("item-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt");
            Files.writeString(file, text);
            Tacklebox.LOGGER.info("[guidump] saved {}", file);
            say("styled dump copied, saved to guidump/" + file.getFileName(), Formatting.AQUA);
        } catch (IOException e) {
            Tacklebox.LOGGER.warn("[guidump] could not save", e);
            say("styled dump copied to the clipboard (saving failed)", Formatting.RED);
        }
    }

    /** A Text as JSON with non-ascii characters escaped, so fonts and glyphs show up in the dump. */
    private static String json(MinecraftClient client, Text text) {
        if (text == null) {
            return "(none)";
        }
        return TextCodecs.CODEC.encodeStart(client.world.getRegistryManager().getOps(JsonOps.INSTANCE), text).result()
                .map(json -> escape(json.toString())).orElse("(could not encode) " + escape(text.getString()));
    }

    /** Private-use and other non-ascii characters as \\uXXXX, so glyphs show up in a dump. */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(c < 0x20 || c > 0x7E ? String.format(java.util.Locale.ROOT, "\\u%04X", (int) c) : String.valueOf(c));
        }
        return sb.toString();
    }

    /**
     * Everything about the open screen worth sending along: class, title (raw and with codepoints) and each
     * container slot's item, name, lore and custom_data. Saved to config/tacklebox/guidump and copied to
     * the clipboard. Works in the mods own menu screens too, because they sit on the servers real chest.
     */
    public static void gui(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        StringBuilder out = new StringBuilder();
        Text title = screen instanceof FishMenuScreen menu ? menu.original().getTitle() : screen.getTitle();
        out.append("screen ").append(screen.getClass().getName()).append('\n');
        if (screen instanceof FishMenuScreen) {
            out.append("shown as the mods menu screen over the chest above\n");
        }
        out.append("title text=\"").append(escape(title.getString())).append("\"\n");
        out.append("title plain=\"").append(Glyphs.plain(title)).append("\" menuName=").append(Glyphs.menuName(title)).append('\n');
        ScreenHandler handler = client.player == null ? null : client.player.currentScreenHandler;
        if (handler != null && handler != client.player.playerScreenHandler) {
            out.append("handler ").append(handler.getClass().getSimpleName()).append(" syncId=").append(handler.syncId).append('\n');
            for (Slot slot : handler.slots) {
                if (slot.inventory instanceof PlayerInventory || slot.getStack().isEmpty()) {
                    continue;
                }
                ItemStack stack = slot.getStack();
                if (stack.isOf(net.minecraft.item.Items.BREAD) && (stack.get(DataComponentTypes.LORE) == null || stack.get(DataComponentTypes.LORE).lines().isEmpty()) && "Bread".equals(stack.getName().getString())) {
                    continue; // the server's placeholder for an empty slot
                }
                out.append("\nslot ").append(slot.id).append(' ').append(Registries.ITEM.getId(stack.getItem())).append(" x").append(stack.getCount()).append('\n');
                out.append("  name=\"").append(escape(stack.getName().getString())).append("\"\n");
                LoreComponent lore = stack.get(DataComponentTypes.LORE);
                if (lore != null) {
                    for (Text line : lore.lines()) {
                        out.append("  lore=\"").append(escape(line.getString())).append("\"\n");
                    }
                }
                NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
                if (custom != null && !custom.isEmpty()) {
                    out.append("  custom_data=").append(custom.copyNbt()).append('\n');
                }
            }
        }
        String text = out.toString();
        client.keyboard.setClipboard(text);
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve(Tacklebox.MOD_ID).resolve("guidump");
            Files.createDirectories(dir);
            Path file = dir.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt");
            Files.writeString(file, text);
            Tacklebox.LOGGER.info("[guidump] saved {}", file);
            toast(client, "gui dump copied, saved to " + dir.getFileName() + "/" + file.getFileName());
        } catch (IOException e) {
            Tacklebox.LOGGER.warn("[guidump] could not save", e);
            toast(client, "gui dump copied to the clipboard (saving failed)");
        }
    }

    /**
     * Everything within radius blocks that a placed server thing (a chummer, say) could be: what the crosshair
     * points at, every entity (type, position, name, held and worn items with custom_data and skull profile,
     * display entity contents, raw nbt) and every block entity (skull profile, raw nbt). Saved to
     * config/tacklebox/guidump/entities-<time>.txt and copied to the clipboard; chat only gets the counts.
     */
    public static int entities(int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity self = client.player;
        ClientWorld world = client.world;
        if (self == null || world == null) {
            return 0;
        }
        Vec3d me = self.getEntityPos();
        StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.ROOT, "within %d blocks of %.3f %.3f %.3f in %s\n", radius, me.x, me.y, me.z, world.getRegistryKey().getValue()));

        out.append("\ntarget (the crosshair, reach is only a few blocks)\n");
        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = block.getBlockPos();
            out.append("block ").append(pos.toShortString()).append(' ').append(world.getBlockState(pos)).append('\n');
            BlockEntity be = world.getBlockEntity(pos);
            if (be != null) {
                appendBlockEntity(out, be, world, me);
            }
        } else if (hit instanceof EntityHitResult entityHit) {
            appendEntity(out, entityHit.getEntity(), world, me);
        } else {
            out.append("nothing\n");
        }

        List<Entity> found = world.getOtherEntities(self, self.getBoundingBox().expand(radius), e -> e.getEntityPos().distanceTo(me) <= radius);
        found.sort(Comparator.comparingDouble(e -> e.getEntityPos().distanceTo(me)));
        long players = found.stream().filter(e -> e instanceof PlayerEntity).count();
        out.append("\nentities: ").append(found.size()).append(" (").append(players).append(" players)\n");
        for (Entity entity : found) {
            appendEntity(out, entity, world, me);
        }

        List<BlockEntity> blocks = new java.util.ArrayList<>();
        for (int cx = (int) Math.floor(me.x - radius) >> 4; cx <= (int) Math.floor(me.x + radius) >> 4; cx++) {
            for (int cz = (int) Math.floor(me.z - radius) >> 4; cz <= (int) Math.floor(me.z + radius) >> 4; cz++) {
                if (world.getChunkManager().isChunkLoaded(cx, cz)) {
                    for (BlockEntity be : world.getChunk(cx, cz).getBlockEntities().values()) {
                        if (Vec3d.ofCenter(be.getPos()).distanceTo(me) <= radius) {
                            blocks.add(be);
                        }
                    }
                }
            }
        }
        blocks.sort(Comparator.comparingDouble(be -> Vec3d.ofCenter(be.getPos()).distanceTo(me)));
        out.append("\nblock entities: ").append(blocks.size()).append('\n');
        for (BlockEntity be : blocks) {
            appendBlockEntity(out, be, world, me);
        }

        Path file = saveDump(client, "entities", out.toString());
        say("entities " + (found.size() - players) + ", players " + players + ", block entities " + blocks.size()
                + (file == null ? ", dump copied (saving failed)" : ", saved to guidump/" + file.getFileName() + " and copied"), Formatting.AQUA);
        return 1;
    }

    /** {@code /tacklebox debug displays}: session-only toggle that stops drawing display entities, a lag test. */
    public static int toggleDisplays() {
        OtherBobbers.hideDisplays = !OtherBobbers.hideDisplays;
        say("item, block and text displays " + (OtherBobbers.hideDisplays ? "hidden" : "shown") + " (until restart)", Formatting.AQUA);
        return 1;
    }

    /** {@code /tacklebox debug count}: every loaded entity grouped by type, to chat only. */
    public static int count() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return 0;
        }
        List<String> ids = new java.util.ArrayList<>();
        for (Entity entity : world.getEntities()) {
            ids.add(Registries.ENTITY_TYPE.getId(entity.getType()).toString());
        }
        say("loaded entities " + ids.size(), Formatting.AQUA);
        for (String line : EntityTally.lines(ids)) {
            say(line, Formatting.GRAY);
        }
        return 1;
    }

    private static void appendEntity(StringBuilder out, Entity entity, ClientWorld world, Vec3d me) {
        Vec3d pos = entity.getEntityPos();
        out.append("\nentity ").append(Registries.ENTITY_TYPE.getId(entity.getType())).append(" (").append(entity.getClass().getSimpleName())
                .append(String.format(Locale.ROOT, ") at %.3f %.3f %.3f, %.1f away", pos.x, pos.y, pos.z, pos.distanceTo(me))).append('\n');
        if (entity instanceof PlayerEntity player) {
            out.append("  player ").append(player.getGameProfile().name()).append('\n');
            return;
        }
        if (entity.hasCustomName()) {
            out.append("  name=\"").append(escape(entity.getCustomName().getString())).append("\"\n");
        }
        if (entity.isInvisible()) {
            out.append("  invisible\n");
        }
        if (entity instanceof ItemEntity item) {
            out.append("  item: ").append(stackLine(item.getStack())).append('\n');
        }
        if (entity instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!living.getEquippedStack(slot).isEmpty()) {
                    out.append("  ").append(slot.asString()).append(": ").append(stackLine(living.getEquippedStack(slot))).append('\n');
                }
            }
        }
        if (entity instanceof DisplayEntity.ItemDisplayEntity display && display.getData() != null) {
            out.append("  item display: ").append(stackLine(display.getData().itemStack())).append('\n');
        }
        if (entity instanceof DisplayEntity.BlockDisplayEntity display && display.getData() != null) {
            out.append("  block display: ").append(display.getData().blockState()).append('\n');
        }
        if (entity instanceof DisplayEntity.TextDisplayEntity display) {
            out.append("  text display: \"").append(escape(display.getText().getString())).append("\"\n");
        }
        try {
            NbtWriteView view = NbtWriteView.create(ErrorReporter.EMPTY, world.getRegistryManager());
            entity.writeData(view);
            out.append("  nbt=").append(escape(view.getNbt().toString())).append('\n');
        } catch (RuntimeException e) {
            out.append("  nbt not available: ").append(e).append('\n');
        }
    }

    private static void appendBlockEntity(StringBuilder out, BlockEntity be, ClientWorld world, Vec3d me) {
        out.append("\nblock entity ").append(Registries.BLOCK_ENTITY_TYPE.getId(be.getType())).append(" at ").append(be.getPos().toShortString())
                .append(String.format(Locale.ROOT, ", %.1f away, ", Vec3d.ofCenter(be.getPos()).distanceTo(me))).append(be.getCachedState()).append('\n');
        if (be instanceof SkullBlockEntity skull && skull.getOwner() != null) {
            out.append("  profile: ").append(profileLine(skull.getOwner())).append('\n');
        }
        try {
            NbtCompound nbt = be.createNbt(world.getRegistryManager());
            out.append("  nbt=").append(escape(nbt.toString())).append('\n');
        } catch (RuntimeException e) {
            out.append("  nbt not available: ").append(e).append('\n');
        }
    }

    /** An item as one line: id, count, name, custom_data and the skull profile (name, id, texture value). */
    private static String stackLine(ItemStack stack) {
        if (stack.isEmpty()) {
            return "(empty)";
        }
        StringBuilder sb = new StringBuilder(Registries.ITEM.getId(stack.getItem()).toString()).append(" x").append(stack.getCount())
                .append(" name=\"").append(escape(stack.getName().getString())).append('"');
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom != null && !custom.isEmpty()) {
            sb.append(" custom_data=").append(escape(custom.copyNbt().toString()));
        }
        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
        if (profile != null) {
            sb.append(" profile: ").append(profileLine(profile));
        }
        return sb.toString();
    }

    private static String profileLine(ProfileComponent component) {
        GameProfile profile = component.getGameProfile();
        StringBuilder sb = new StringBuilder("name=").append(profile.name()).append(" id=").append(profile.id());
        for (Property texture : profile.properties().get("textures")) {
            sb.append(" texture=").append(texture.value());
        }
        return sb.toString();
    }

    /** Copies a dump to the clipboard and saves it as config/tacklebox/guidump/<prefix>-<time>.txt; null when saving failed. */
    private static Path saveDump(MinecraftClient client, String prefix, String text) {
        client.keyboard.setClipboard(text);
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve(Tacklebox.MOD_ID).resolve("guidump");
            Files.createDirectories(dir);
            Path file = dir.resolve(prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt");
            Files.writeString(file, text);
            Tacklebox.LOGGER.info("[guidump] saved {}", file);
            return file;
        } catch (IOException e) {
            Tacklebox.LOGGER.warn("[guidump] could not save", e);
            return null;
        }
    }

    private static void toast(MinecraftClient client, String line) {
        client.getToastManager().add(new net.minecraft.client.toast.SystemToast(net.minecraft.client.toast.SystemToast.Type.PERIODIC_NOTIFICATION,
                Foe.text("gui dump"), Text.literal(line)));
    }

    private static void say(String line, Formatting color) {
        Tacklebox.LOGGER.info("[debug] {}", line);
        ClientPlayerEntity self = MinecraftClient.getInstance().player;
        if (self != null) {
            self.sendMessage(Text.literal(line).formatted(color), false);
        }
    }
}
