package dev.landofif.dofisaddons.mixin;

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
    List<ChatHudLine> dofisaddons$messages();

    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> dofisaddons$visibleMessages();

    @Accessor("scrolledLines")
    int dofisaddons$scrolled();

    @Accessor("scrolledLines")
    void dofisaddons$setScrolled(int lines);

    @Accessor("hasUnreadNewMessages")
    boolean dofisaddons$unread();

    @Invoker("getLineHeight")
    int dofisaddons$lineHeight();

    @Invoker("refresh")
    void dofisaddons$refresh();

    @Invoker("render")
    void dofisaddons$render(ChatHud.Backend backend, int windowHeight, int currentTick, boolean expanded);
}
