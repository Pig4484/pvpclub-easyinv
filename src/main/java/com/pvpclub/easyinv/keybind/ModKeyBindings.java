package com.pvpclub.easyinv.keybind;

import com.pvpclub.easyinv.PvpClubEasyInv;
import com.pvpclub.easyinv.gui.ConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the mod's keybind (open config screen) and wires it into
 * the client tick handler.
 *
 * <p><b>v1.4.0:</b> added because not every player has Mod Menu
 * installed. The keybind is unbound by default so it never clashes
 * with a player's existing habit — they pick a key in Minecraft's
 * Options → Controls screen.</p>
 *
 * <p>Category is the vanilla {@link KeyBinding.Category#MISC} because
 * Yarn mappings 1.21.11 freeze the category to one of the built-in
 * enum values (MISC is the standard home for mod-added keybinds).</p>
 */
public final class ModKeyBindings {

    /**
     * Translation key for the "Open Config Screen" keybind. Picked up
     * by the language file at
     * {@code assets/pvpclub_easyinv/lang/en_us.lang}.
     */
    public static final String OPEN_CONFIG_KEY = "key.pvpclub_easyinv.open_config";

    /**
     * The keybind handle. Static so anything in the mod can read
     * {@code .wasPressed()} if needed.
     */
    public static KeyBinding OPEN_CONFIG;

    private ModKeyBindings() {
    }

    /**
     * Register the keybind and the tick handler. Call once from
     * {@link PvpClubEasyInv#onInitializeClient()}.
     */
    public static void register() {
        OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                OPEN_CONFIG_KEY,
                InputUtil.Type.KEYSYM,
                // Unbound by default — player opts in via Controls.
                GLFW.GLFW_KEY_UNKNOWN,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(ModKeyBindings::onEndClientTick);
    }

    /**
     * If the open-config keybind was pressed this tick and the
     * player isn't already sitting on a screen that would eat the
     * press (chat, sign editor, etc.), open {@link ConfigScreen}
     * with the current screen as its parent.
     */
    private static void onEndClientTick(MinecraftClient client) {
        if (OPEN_CONFIG == null) {
            return;
        }
        if (!OPEN_CONFIG.wasPressed()) {
            return;
        }
        if (client.player == null) {
            return;
        }
        Screen current = client.currentScreen;
        // Don't yank the player out of screens that own text input
        // (chat, anvil, sign, command block, book). The config
        // screen has its own text fields; replacing those screens
        // silently would lose what the player was typing.
        if (current != null && !shouldReplace(current)) {
            return;
        }
        client.setScreen(new ConfigScreen(current));
    }

    /**
     * Screens that are safe to replace with the config screen. Keep
     * this list conservative — when in doubt, do nothing.
     */
    private static boolean shouldReplace(Screen current) {
        String cls = current.getClass().getName();
        // In-game HUD overlays (boss bar, sleep, debug, etc.) all
        // extend Screen but are passthrough — safe to swap.
        // Vanilla screens that own meaningful input (chat, command
        // block, anvil, sign, book) we leave alone.
        return !cls.startsWith("net.minecraft.client.gui.screen.ChatScreen")
                && !cls.startsWith("net.minecraft.client.gui.screen.ingame.CommandBlockScreen")
                && !cls.startsWith("net.minecraft.client.gui.screen.ingame.AnvilScreen")
                && !cls.startsWith("net.minecraft.client.gui.screen.SignScreen")
                && !cls.startsWith("net.minecraft.client.gui.screen.ingame.BookScreen")
                && !cls.equals("net.minecraft.client.gui.screen.SleepingChatScreen");
    }
}
