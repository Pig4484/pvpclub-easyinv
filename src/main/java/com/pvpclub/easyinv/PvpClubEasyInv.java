package com.pvpclub.easyinv;

import com.pvpclub.easyinv.config.ModConfig;
import com.pvpclub.easyinv.keybind.ModKeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the PVP Club Easy Invite client-side mod.
 *
 * <p>The actual click-to-invite logic lives in
 * {@code ChatHudMixin}. Config is in
 * {@link ModConfig}, edited through the Mod Menu screen at
 * {@code com.pvpclub.easyinv.gui.ConfigScreen} or via the keybind
 * registered in {@link ModKeyBindings}.</p>
 *
 * <p><b>v1.3.0:</b> the Record keybind (R) and the recorded-player
 * stack were removed — clicks always fire on chat lines when the
 * mod is enabled and the player is on the configured server.</p>
 *
 * <p><b>v1.4.0:</b> added the "Open Config Screen" keybind so the
 * config screen is reachable without Mod Menu.</p>
 */
public final class PvpClubEasyInv implements ClientModInitializer {
    public static final String MOD_ID = "pvpclub_easyinv";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        // Touch the config so it loads from disk and (if absent) gets
        // written with defaults during the first launch.
        ModConfig config = ModConfig.get();
        LOGGER.info("[{}] PVP Club Easy Invite loaded.", MOD_ID);
        LOGGER.info("[{}] Config file: {}", MOD_ID,
                FabricLoader.getInstance().getConfigDir().resolve("pvpclub_easyinv.json"));
        LOGGER.info("[{}] enabled={}, serverOnly={}, server='{}'",
                MOD_ID, config.enabled, config.serverOnly, config.serverAddress);
        LOGGER.info("[{}] chatPattern='{}' (autoAttach={})",
                MOD_ID, config.chatPattern, config.autoAttachFromPattern);
        LOGGER.info("[{}] Click any player name in chat to auto-send '{}'.",
                MOD_ID, config.commandPattern);

        // v1.4.0: register the open-config keybind (unbound by
        // default; the player picks a key in Options → Controls).
        ModKeyBindings.register();
        LOGGER.info("[{}] Open-config keybind registered (unbound by default; bind in Options → Controls).",
                MOD_ID);
    }
}