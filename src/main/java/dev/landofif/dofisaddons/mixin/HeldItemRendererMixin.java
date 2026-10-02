package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Viewmodel: size, position and rotation of the item in your hand, and how it swings. Visual only. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    private static final String FIRST_PERSON = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V";

    @Shadow
    @Final
    private ItemModelManager itemModelManager;

    /** "No swing": the swing progress vanilla would have used, waiting to be applied as a turn in place. */
    @Unique
    private float dofisaddons$spin = -1f;

    /** The held item's model centre per hand (right, left), cached per stack: vanilla rebuilds the model every frame. */
    @Unique
    private final ItemStack[] dofisaddons$centreStack = new ItemStack[2];
    @Unique
    private final Vector3f[] dofisaddons$centre = {new Vector3f(), new Vector3f()};

    @ModifyVariable(method = FIRST_PERSON, at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private float dofisaddons$swing(float swingProgress, @Local(argsOnly = true) ItemStack item) {
        DofiSettings s = StatsTracker.settings();
        return switch (s.swingStyle) {
            case NORMAL -> swingProgress;
            // a held item keeps the real progress, so swingArm can hand its turn over (see dofisaddons$spinOnly);
            // an empty hand or a map stays completely still
            case NONE -> item.isEmpty() || item.isOf(Items.FILLED_MAP) ? 0f : swingProgress;
            // ease in and out: slow start and end, smooth middle
            case SMOOTH -> (float) (0.5 - 0.5 * Math.cos(Math.PI * swingProgress));
            // most of the motion happens right away
            case SNAPPY -> (float) Math.sqrt(swingProgress);
        };
    }

    @ModifyVariable(method = FIRST_PERSON, at = @At("HEAD"), argsOnly = true, ordinal = 3, require = 0)
    private float dofisaddons$equip(float equipProgress) {
        DofiSettings s = StatsTracker.settings();
        return s.viewmodel && s.vmNoEquipDip ? 0f : equipProgress;
    }

    /**
     * "No swing" skips vanilla's swing, which moves the item sideways, up and back and turns it around the
     * grip. Only the turn is kept, and it is applied right before the item is drawn, around its centre.
     */
    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$spinOnly(float swingProgress, MatrixStack matrices, int side, Arm arm, CallbackInfo ci) {
        if (StatsTracker.settings().swingStyle == DofiSettings.SwingStyle.NONE) {
            dofisaddons$spin = swingProgress;
            ci.cancel();
        }
    }

    @Inject(method = FIRST_PERSON, at = @At("HEAD"))
    private void dofisaddons$resetSpin(CallbackInfo ci) {
        dofisaddons$spin = -1f;
    }

    @Inject(method = FIRST_PERSON, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"))
    private void dofisaddons$viewmodel(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                       float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                       OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        DofiSettings s = StatsTracker.settings();
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1f : -1f;

        // the viewmodel offset, rotation and size, built separately so the spin can find the item centre through it
        Matrix4f view = new Matrix4f();
        if (s.viewmodel) {
            float scale = (float) s.vmScale;
            view.translate((float) s.vmX * side, (float) s.vmY, (float) s.vmZ)
                    .rotateX((float) Math.toRadians(s.vmRotX))
                    .rotateY((float) Math.toRadians(s.vmRotY * side))
                    .rotateZ((float) Math.toRadians(s.vmRotZ * side))
                    .scale(scale, scale, scale);
        }

        float spin = dofisaddons$spin;
        dofisaddons$spin = -1f;
        if (spin > 0f) {
            ItemDisplayContext context = arm == Arm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            Vector3f pivot = view.transformPosition(dofisaddons$centre(player, item, context), new Vector3f());
            matrices.translate(pivot.x, pivot.y, pivot.z);
            dofisaddons$swingTurn(matrices, side, spin, (float) s.swingTurn / 100f);
            matrices.translate(-pivot.x, -pivot.y, -pivot.z);
        }
        if (s.viewmodel) {
            matrices.multiplyPositionMatrix(view);
        }
    }

    /** The rotation part of vanilla's swing (HeldItemRenderer.applySwingOffset), without the movement, scaled by amount. */
    @Unique
    private static void dofisaddons$swingTurn(MatrixStack matrices, float side, float progress, float amount) {
        float f = MathHelper.sin(progress * progress * (float) Math.PI) * amount;
        float g = MathHelper.sin(MathHelper.sqrt(progress) * (float) Math.PI) * amount;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45.0F + f * -20.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
    }

    /** Centre of the item model as it is drawn in this hand (its first person display transform applied). */
    @Unique
    private Vector3f dofisaddons$centre(AbstractClientPlayerEntity player, ItemStack item, ItemDisplayContext context) {
        int i = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0 : 1;
        if (item != dofisaddons$centreStack[i]) {
            ItemRenderState state = new ItemRenderState();
            itemModelManager.clearAndUpdate(state, item, context, player.getEntityWorld(), player, player.getId() + context.ordinal());
            Box box = state.getModelBoundingBox();
            Vector3f centre = dofisaddons$centre[i];
            centre.set((float) (box.minX + box.maxX) / 2, (float) (box.minY + box.maxY) / 2, (float) (box.minZ + box.maxZ) / 2);
            if (!centre.isFinite()) {
                centre.zero(); // a model without geometry: turn around the grip like vanilla
            }
            dofisaddons$centreStack[i] = item;
        }
        return dofisaddons$centre[i];
    }
}
