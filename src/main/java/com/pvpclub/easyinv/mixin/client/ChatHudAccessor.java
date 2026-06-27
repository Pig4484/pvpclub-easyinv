package com.pvpclub.easyinv.mixin.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the private {@code addMessage} of {@link ChatHud} so the
 * chat-hud mixin can re-invoke it with a rewritten {@link Text}
 * without recursing through its own {@code @Inject} wrapper.
 *
 * <p>The previously exposed {@code messages} accessor was removed in
 * v1.3.0 when the Record keybind (R) and the recorded-player stack
 * went away — nothing else reads the chat history list at runtime.</p>
 */
@Mixin(ChatHud.class)
public interface ChatHudAccessor {

    @Invoker("addMessage")
    void pvpclub_easyinv$invokeAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator);
}