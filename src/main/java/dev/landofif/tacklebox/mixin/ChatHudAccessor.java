package dev.landofif.tacklebox.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** Read access to the chat lines and its layout pass, for copying a line (see chat/ChatCopy). */
@Mixin(ChatHud.class)
public interface ChatHudAccessor {
    @Accessor("messages")
    List<ChatHudLine> tacklebox$messages();

    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> tacklebox$visibleMessages();

    @Accessor("scrolledLines")
    int tacklebox$scrolled();

    @Accessor("scrolledLines")
    void tacklebox$setScrolled(int lines);

    @Accessor("hasUnreadNewMessages")
    boolean tacklebox$unread();

    @Invoker("getLineHeight")
    int tacklebox$lineHeight();

    @Invoker("refresh")
    void tacklebox$refresh();

    @Invoker("render")
    void tacklebox$render(ChatHud.Backend backend, int windowHeight, int currentTick, boolean expanded);
}
