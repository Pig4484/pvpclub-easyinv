package com.pvpclub.easyinv;

import com.pvpclub.easyinv.config.ChatPattern;
import com.pvpclub.easyinv.config.ModConfig;
import com.pvpclub.easyinv.config.ModifierKey;
import com.pvpclub.easyinv.gui.ConfigScreen;
import com.pvpclub.easyinv.mixin.client.ChatHudAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Entry point for the PVP Club Easy Invite client-side mod.
 *
 * <p>The actual click-to-invite logic lives in
 * {@code ChatHudMixin}. Config is in
 * {@link ModConfig}, edited through the Mod Menu screen at
 * {@code com.pvpclub.easyinv.gui.ConfigScreen}.</p>
 */
public final class PvpClubEasyInv implements ClientModInitializer {
    public static final String MOD_ID = "pvpclub_easyinv";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * "Record Last Player" — pushes the most recent chat-line's
     * player name (extracted by the configured {@link ChatPattern})
     * onto the recorded-player stack. Default key: {@code R}.
     */
    public static final KeyBinding RECORD_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.pvpclub_easyinv.record",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyBinding.Category.MISC
    ));

    @Override
    public void onInitializeClient() {
        // Touch the config so it loads from disk and (if absent) gets
        // written with defaults during the first launch.
        ModConfig config = ModConfig.get();
        LOGGER.info("[{}] PVP Club Easy Invite loaded.", MOD_ID);
        LOGGER.info("[{}] Config file: {}", MOD_ID,
                FabricLoader.getInstance().getConfigDir().resolve("pvpclub_easyinv.json"));
        LOGGER.info("[{}] enabled={}, serverOnly={}, server='{}', requireModifier={}, modifierKey={}",
                MOD_ID, config.enabled, config.serverOnly, config.serverAddress,
                config.requireModifierKey, config.getModifierKey().getDisplayName());
        LOGGER.info("[{}] chatPattern='{}' (autoAttach={})",
                MOD_ID, config.chatPattern, config.autoAttachFromPattern);
        ModifierKey currentKey = config.getModifierKey();
        if (currentKey.isUnbound()) {
            LOGGER.info("[{}] Modifier key is unbound — open Mod Menu to pick one, " +
                    "or click any player name in chat directly (no gate).",
                    MOD_ID);
        } else {
            LOGGER.info("[{}] Hold [{}] while clicking a player name in chat " +
                    "to auto-send /p invite <name>.", MOD_ID, currentKey.getDisplayName());
        }
        LOGGER.info("[{}] Press [{}] to record the most recent chat-line's player name; " +
                        "the next chat line will get a /p invite click event for that player.",
                MOD_ID, RECORD_KEY.getBoundKeyTranslationKey());

        // Tick handler for the Record keybind. Registered here so it
        // doesn't matter whether Mixin is in play.
        ClientTickEvents.END_CLIENT_TICK.register(PvpClubEasyInv::onEndClientTick);
    }

    private static void onEndClientTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        // Drain all key-presses so holding the key only fires once.
        while (RECORD_KEY.wasPressed()) {
            recordLatestPlayer(client);
        }
    }

    /**
     * Pulls the most recent chat line, runs the configured
     * {@link ChatPattern} over it, and pushes the resulting player
     * name onto {@link ModConfig#recordedPlayers}. Notifies the
     * player in the action bar.
     */
    private static void recordLatestPlayer(MinecraftClient client) {
        ChatHud chatHud = client.inGameHud.getChatHud();
        if (!(chatHud instanceof ChatHudAccessor accessor)) {
            client.player.sendMessage(
                    Text.literal("[pvpclub_easyinv] Internal error: chat hud accessor unavailable.").formatted(Formatting.RED),
                    true);
            return;
        }
        List<ChatHudLine> messages = accessor.pvpclub_easyinv$getMessages();
        if (messages == null || messages.isEmpty()) {
            client.player.sendMessage(
                    Text.literal("[pvpclub_easyinv] No chat history to record from.").formatted(Formatting.YELLOW),
                    true);
            return;
        }
        // ChatHud keeps the most recent message at the tail.
        ChatHudLine latest = messages.get(messages.size() - 1);
        String lineText = latest.content().getString();
        if (lineText == null || lineText.isEmpty()) {
            client.player.sendMessage(
                    Text.literal("[pvpclub_easyinv] Latest chat line is empty.").formatted(Formatting.YELLOW),
                    true);
            return;
        }
        ChatPattern pattern = ModConfig.get().getChatPattern();
        Optional<String> name = pattern.extractName(lineText);
        if (name.isEmpty()) {
            client.player.sendMessage(
                    Text.literal("[pvpclub_easyinv] Could not extract a player name from: \"")
                            .append(Text.literal(lineText).formatted(Formatting.GRAY))
                            .append("\"")
                            .append(Text.literal(" (check your chat pattern in the config)").formatted(Formatting.YELLOW))
                            .formatted(Formatting.RED),
                    true);
            return;
        }
        ModConfig.get().recordedPlayers.push(name.get());
        client.player.sendMessage(
                Text.literal("[pvpclub_easyinv] Recorded: ")
                        .append(Text.literal(name.get()).formatted(Formatting.GREEN))
                        .append(Text.literal(" (next click on chat invites them)").formatted(Formatting.GRAY)),
                true);
    }
}