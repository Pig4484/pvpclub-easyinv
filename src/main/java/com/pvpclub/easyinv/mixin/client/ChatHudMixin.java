package com.pvpclub.easyinv.mixin.client;

import com.pvpclub.easyinv.PvpClubEasyInv;
import com.pvpclub.easyinv.config.ChatPattern;
import com.pvpclub.easyinv.config.ModConfig;
import com.pvpclub.easyinv.config.ModifierKey;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Injects a {@code /p invite <name>} {@link ClickEvent} onto every player
 * name in every chat line that gets added to the chat hud.
 *
 * <h2>Three name sources, in priority order</h2>
 * <ol>
 *   <li><b>Vanilla hover event</b> — the standard
 *       {@code HoverEvent$ShowEntity} vanilla attaches to every
 *       signed player-sender component. The click target is the
 *       component carrying the hover event, so a single click fires
 *       {@code /p invite}.</li>
 *   <li><b>Chat-pattern fallback</b> — for servers that don't sign
 *       chat (most anonymous PVP servers, including mcpvp.club),
 *       vanilla doesn't add a hover event. We fall back to a
 *       user-configured pattern like {@code "{name} » {message}"}
 *       and attach the click event to the <i>first sibling</i> of
 *       the line, which is by Minecraft chat convention the
 *       player name. We deliberately do NOT wrap the whole line —
 *       see {@link #pvpclub_easyinv$replaceNode} for why.</li>
 *   <li><b>Recorded-player stack</b> — the "Record" keybind pushes
 *       names onto a stack. The next {@code addMessage} call after a
 *       record pops one name and wraps the entire line (every
 *       sibling gets the same click event) so any click on the
 *       line fires {@code /p invite} for that player.</li>
 * </ol>
 *
 * <p>All chat rewriting is done with empty-string roots to avoid
 * duplicating the concatenated sibling string when reconstructing
 * the tree. See {@link #pvpclub_easyinv$replaceNode} for the full
 * explanation.</p>
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void pvpclub_easyinv$onAddMessage(Text message,
                                              MessageSignatureData signature,
                                              MessageIndicator indicator,
                                              CallbackInfo ci) {
        ModConfig config = ModConfig.get();

        // 1. Master switch.
        if (!config.enabled) {
            return;
        }

        // 2. Server filter (singleplayer = no filtering applied, always allowed).
        if (config.serverOnly && !isOnTargetServer(config.serverAddress)) {
            return;
        }

        // 3. Modifier key — when "Require Modifier Key" is on AND a key
        //    has been bound, drop messages that arrived while the user
        //    wasn't holding it. An unbound key counts as "no gate" so
        //    the mod still does something useful out of the box.
        ModifierKey modKey = config.getModifierKey();
        if (config.requireModifierKey && !modKey.isUnbound() && !modKey.isPressed()) {
            return;
        }

        // 4. Attach a click event to the player-name portion of the line.
        Text rewritten = pvpclub_easyinv$injectInviteClickEvent(message, config);

        // 5. If a player is waiting on the recorded-player stack, pop one
        //    and wrap the whole line. The wrap uses an empty-string root
        //    so the concatenated string isn't duplicated.
        if (!config.recordedPlayers.isEmpty()) {
            String recorded = config.recordedPlayers.pollLast();
            ClickEvent recordedClick = new ClickEvent.RunCommand("p invite " + recorded);
            rewritten = pvpclub_easyinv$wrapAllNodesWithClick(rewritten, recordedClick, true);
            PvpClubEasyInv.LOGGER.info("[{}] Recorded-player click attached: invite '{}'.",
                    PvpClubEasyInv.MOD_ID, recorded);
        }

        if (rewritten == message) {
            return; // Nothing changed; let vanilla process the line as-is.
        }

        // 6. Re-invoke addMessage with the rewritten text, bypassing our
        //    own @Inject wrapper to avoid infinite recursion.
        ci.cancel();
        ((ChatHudAccessor) (Object) this).pvpclub_easyinv$invokeAddMessage(rewritten, signature, indicator);
    }

    // ---- Name extraction --------------------------------------------------

    @Unique
    private static Optional<String> pvpclub_easyinv$findPlayerName(Text text, ModConfig config) {
        Optional<String> hoverName = pvpclub_easyinv$findHoverName(text);
        if (hoverName.isPresent()) {
            return hoverName;
        }
        if (config.autoAttachFromPattern) {
            ChatPattern pattern = config.getChatPattern();
            Optional<String> patternName = pattern.extractName(text.getString());
            if (patternName.isPresent()) {
                return patternName;
            }
        }
        return Optional.empty();
    }

    @Unique
    private static Optional<String> pvpclub_easyinv$findHoverName(Text text) {
        Optional<String> here = extractPlayerName(text.getStyle());
        if (here.isPresent()) {
            return here;
        }
        for (Text sibling : text.getSiblings()) {
            Optional<String> found = pvpclub_easyinv$findHoverName(sibling);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    @Unique
    private static Optional<String> extractPlayerName(Style style) {
        HoverEvent hover = style.getHoverEvent();
        if (hover == null || hover.getAction() != HoverEvent.Action.SHOW_ENTITY) {
            return Optional.empty();
        }
        if (!(hover instanceof HoverEvent.ShowEntity showEntity)) {
            return Optional.empty();
        }
        return showEntity.entity().name.map(Text::getString);
    }

    // ---- Text rewriting --------------------------------------------------

    @Unique
    private static Text pvpclub_easyinv$injectInviteClickEvent(Text message, ModConfig config) {
        Optional<String> playerName = pvpclub_easyinv$findPlayerName(message, config);
        if (playerName.isEmpty()) {
            return message;
        }
        String name = playerName.get();
        ClickEvent click = new ClickEvent.RunCommand("p invite " + name);

        // 1. Vanilla hover event path: replace the style of the exact node
        //    that carries the hover event so only that node is clickable.
        Optional<Text> hoverNode = pvpclub_easyinv$findHoverNode(message);
        if (hoverNode.isPresent()) {
            Text target = hoverNode.get();
            Style newStyle = target.getStyle().withClickEvent(click);
            if (newStyle.equals(target.getStyle())) {
                return message;
            }
            return pvpclub_easyinv$replaceNode(message, target, newStyle);
        }

        // 2. Pattern-fallback path: only attach the click event to the
        //    first sibling, which by Minecraft chat convention is the
        //    player name. We do NOT wrap the entire line — that would
        //    duplicate the displayed string (see replaceNode for why).
        if (!message.getSiblings().isEmpty()) {
            Text first = message.getSiblings().get(0);
            if (first.getStyle().getClickEvent() == null) {
                Style newStyle = first.getStyle().withClickEvent(click);
                return pvpclub_easyinv$replaceNode(message, first, newStyle);
            }
        }
        return message;
    }

    @Unique
    private static Optional<Text> pvpclub_easyinv$findHoverNode(Text text) {
        if (extractPlayerName(text.getStyle()).isPresent()) {
            return Optional.of(text);
        }
        for (Text sibling : text.getSiblings()) {
            Optional<Text> found = pvpclub_easyinv$findHoverNode(sibling);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /**
     * Walks the tree and rebuilds it, returning a new tree where the
     * node {@code target} has been replaced with a new node whose
     * style is {@code newStyle}. The rest of the tree is preserved
     * (and rebuilt only as needed so the rest stays referentially
     * shared with the original).
     *
     * <p><b>Empty-root discipline:</b> Minecraft chat text is built
     * with an empty-string root and the actual letters in the
     * siblings. {@link Text#getString()} returns the <i>concatenated
     * </i> string of root + all siblings. If we did
     * {@code Text.literal(root.getString())} we'd be emitting the
     * whole concatenated line <i>and then</i> appending every
     * sibling again — that's the double-message bug. So every
     * rebuild in this class starts from {@code Text.literal("")}.</p>
     */
    @Unique
    private static Text pvpclub_easyinv$replaceNode(Text current, Text target, Style newStyle) {
        if (current == target) {
            // Build a leaf replacement if the target has no siblings,
            // otherwise build an empty-root container with the
            // recursively rebuilt siblings.
            if (current.getSiblings().isEmpty()) {
                MutableText leaf = Text.literal(current.getString());
                leaf.setStyle(newStyle);
                return leaf;
            }
            MutableText container = Text.literal("");
            container.setStyle(newStyle);
            for (Text sibling : current.getSiblings()) {
                container.append(pvpclub_easyinv$replaceNode(sibling, target, newStyle));
            }
            return container;
        }
        if (current.getSiblings().isEmpty()) {
            return current;
        }
        boolean anyChanged = false;
        List<Text> newSiblings = new ArrayList<>();
        for (Text sibling : current.getSiblings()) {
            Text replaced = pvpclub_easyinv$replaceNode(sibling, target, newStyle);
            newSiblings.add(replaced);
            if (replaced != sibling) {
                anyChanged = true;
            }
        }
        if (!anyChanged) {
            return current;
        }
        // Empty-root container to avoid duplicating the sibling
        // concatenation (see class javadoc).
        MutableText rebuilt = Text.literal("");
        rebuilt.setStyle(current.getStyle());
        for (Text sibling : newSiblings) {
            rebuilt.append(sibling);
        }
        return rebuilt;
    }

    /**
     * Rebuilds the entire {@link Text} tree, attaching {@code click}
     * to every node. Used by the recorded-player path so any click
     * anywhere on the next chat line fires {@code /p invite
     * <recorded>}.
     *
     * <p>Always builds with an empty-string root to avoid the
     * duplicated-string bug (see
     * {@link #pvpclub_easyinv$replaceNode}).</p>
     *
     * @param overrideExisting if true, the click event is force-set
     *                          on every node; if false, only nodes
     *                          without their own click event get it.
     */
    @Unique
    private static Text pvpclub_easyinv$wrapAllNodesWithClick(Text original, ClickEvent click, boolean overrideExisting) {
        Style ownStyle = original.getStyle();
        Style newOwnStyle;
        if (overrideExisting || ownStyle.getClickEvent() == null) {
            newOwnStyle = ownStyle.withClickEvent(click);
        } else {
            newOwnStyle = ownStyle;
        }
        boolean ownChanged = newOwnStyle != ownStyle;

        List<Text> wrappedSiblings = null;
        for (Text sibling : original.getSiblings()) {
            Text wrapped = pvpclub_easyinv$wrapAllNodesWithClick(sibling, click, overrideExisting);
            if (wrappedSiblings == null && wrapped != sibling) {
                wrappedSiblings = new ArrayList<>();
                for (Text orig : original.getSiblings()) {
                    if (orig == sibling) break;
                    wrappedSiblings.add(orig);
                }
            }
            if (wrappedSiblings != null) {
                wrappedSiblings.add(wrapped);
            }
        }

        if (!ownChanged && wrappedSiblings == null) {
            return original;
        }
        MutableText out = Text.literal("");
        out.setStyle(newOwnStyle);
        if (wrappedSiblings != null) {
            for (Text s : wrappedSiblings) {
                out.append(s);
            }
        } else {
            for (Text s : original.getSiblings()) {
                out.append(s);
            }
        }
        return out;
    }

    // ---- Server filter ---------------------------------------------------

    @Unique
    private static boolean isOnTargetServer(String addressSubstring) {
        if (addressSubstring == null || addressSubstring.isBlank()) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.isInSingleplayer()) {
            return false;
        }
        ServerInfo info = client.getCurrentServerEntry();
        if (info == null || info.address == null) {
            return false;
        }
        return info.address.toLowerCase().contains(addressSubstring.toLowerCase());
    }
}