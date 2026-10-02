package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Viewmodel: size, position and rotation of the item in your hand, and how it swings. Visual only. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    private static final String FIRST_PERSON = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V";

    @ModifyVariable(method = FIRST_PERSON, at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private float dofisaddons$swing(float swingProgress) {
        DofiSettings s = StatsTracker.settings();
        return switch (s.swingStyle) {
            case NORMAL -> swingProgress;
            case NONE -> 0f;
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

    @Inject(method = FIRST_PERSON, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"))
    private void dofisaddons$viewmodel(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                       float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                       OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        DofiSettings s = StatsTracker.settings();
        if (!s.viewmodel) {
            return;
        }
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1f : -1f;
        matrices.translate(s.vmX * side, s.vmY, s.vmZ);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) s.vmRotX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) s.vmRotY * side));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) s.vmRotZ * side));
        float scale = (float) s.vmScale;
        matrices.scale(scale, scale, scale);
    }
}
