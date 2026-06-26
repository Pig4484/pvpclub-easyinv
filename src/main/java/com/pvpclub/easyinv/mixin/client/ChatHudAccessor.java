package com.pvpclub.easyinv.mixin.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Exposes private members of {@link ChatHud} to other mixins / runtime
 * code so we can:
 * <ul>
 *   <li>Re-invoke the original {@code addMessage} with a rewritten
 *       {@link Text} without recursing through our own
 *       {@code @Inject} wrapper.</li>
 *   <li>Read the chat history list when the user presses the
 *       "Record" keybind, so we can pull the last player name out
 *       of the most recent line.</li>
 * </ul>
 */
@Mixin(ChatHud.class)
public interface ChatHudAccessor {

    @Invoker("addMessage")
    void pvpclub_easyinv$invokeAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator);

    @Accessor("messages")
    List<ChatHudLine> pvpclub_easyinv$getMessages();
}