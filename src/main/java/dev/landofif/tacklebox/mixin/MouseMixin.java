package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.chat.ChatPeek;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The wheel scrolls vanilla chat while the peek key is held (chat/ChatPeek). Cancelled before the
 * hotbar part: foecollab redirects setSelectedSlot in there, so that call is left alone.
 */
@Mixin(Mouse.class)
abstract class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/input/Scroller;update(DD)Lorg/joml/Vector2i;"), cancellable = true)
    private void tacklebox$peekScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!ChatPeek.active()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        client.inGameHud.getChatHud().scroll(ChatPeek.lines(vertical, client.isShiftPressed()));
        ci.cancel();
    }
}
