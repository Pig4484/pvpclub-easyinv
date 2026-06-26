package com.pvpclub.easyinv.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * The key the user must hold down (when the gate is enabled) for a chat
 * click to fire {@code /p invite <name>}.
 *
 * <p>This used to be an enum of 8 fixed keys (NONE / L-Ctrl / R-Ctrl /
 * L-Shift / ...). It is now an arbitrary-key value, so the user can pick
 * any keyboard key — same UX as Minecraft's vanilla controls menu.
 * The {@link #UNBOUND} instance is the default; it represents "no key
 * picked yet", the same as vanilla's "Unbound" display.</p>
 */
public final class ModifierKey {

    /**
     * Sentinel GLFW key code meaning "no key chosen yet". GLFW's actual
     * unknown-key code is {@code -1}; we use the same value so {@code 0}
     * (GLFW_KEY_SPACE is {@code 32}, but {@code 0} is not a real GLFW
     * code) stays unambiguous and we never collide with a real key.
     */
    public static final int UNBOUND_GLFW = -1;

    public static final ModifierKey UNBOUND = new ModifierKey(UNBOUND_GLFW, "Unbound");

    private final int glfwCode;
    private final String displayName;

    private ModifierKey(int glfwCode, String displayName) {
        this.glfwCode = glfwCode;
        this.displayName = displayName;
    }

    /**
     * Wrap a GLFW key code as a {@code ModifierKey}. Codes we can't
     * resolve to a known Minecraft key are still wrapped — the display
     * name falls back to the raw code so the user can see what they
     * picked.
     */
    public static ModifierKey ofGlfwCode(int code) {
        if (code == UNBOUND_GLFW) {
            return UNBOUND;
        }
        String name;
        try {
            InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(code);
            name = key.getLocalizedText().getString();
            if (name == null || name.isBlank() || name.equals("Unknown")) {
                name = "Key " + code;
            }
        } catch (Throwable t) {
            name = "Key " + code;
        }
        return new ModifierKey(code, name);
    }

    public boolean isUnbound() {
        return glfwCode == UNBOUND_GLFW;
    }

    public int getGlfwCode() {
        return glfwCode;
    }

    /**
     * Human-readable label shown in the config screen. Either the
     * Minecraft-localised key name (e.g. "Left Control") or "Unbound".
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * True when the key is currently held down. Always false for
     * {@link #UNBOUND}.
     */
    public boolean isPressed() {
        if (isUnbound()) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        // 1.21.x: InputUtil.isKeyPressed(Window, int)
        return InputUtil.isKeyPressed(client.getWindow(), glfwCode);
    }

    /**
     * Read a key from the legacy string-encoded JSON config, falling
     * back to {@link #UNBOUND} when the value is missing or garbage.
     * Used by {@code ModConfig} when migrating from v1.1.x saves.
     */
    public static ModifierKey parseLegacyName(String legacyName) {
        if (legacyName == null || legacyName.isBlank()) {
            return UNBOUND;
        }
        try {
            return ModifierKey.valueOf(legacyName);
        } catch (IllegalArgumentException ignored) {
            // Old enum constant — try to remap the most common ones so
            // existing users don't lose their binding.
            return switch (legacyName) {
                case "NONE" -> UNBOUND;
                case "LEFT_CONTROL" -> ofGlfwCode(GLFW.GLFW_KEY_LEFT_CONTROL);
                case "RIGHT_CONTROL" -> ofGlfwCode(GLFW.GLFW_KEY_RIGHT_CONTROL);
                case "LEFT_SHIFT" -> ofGlfwCode(GLFW.GLFW_KEY_LEFT_SHIFT);
                case "RIGHT_SHIFT" -> ofGlfwCode(GLFW.GLFW_KEY_RIGHT_SHIFT);
                case "LEFT_ALT" -> ofGlfwCode(GLFW.GLFW_KEY_LEFT_ALT);
                case "RIGHT_ALT" -> ofGlfwCode(GLFW.GLFW_KEY_RIGHT_ALT);
                case "LEFT_SUPER" -> ofGlfwCode(GLFW.GLFW_KEY_LEFT_SUPER);
                case "RIGHT_SUPER" -> ofGlfwCode(GLFW.GLFW_KEY_RIGHT_SUPER);
                default -> UNBOUND;
            };
        }
    }

    /**
     * Stub for binary compatibility with the old enum-style
     * {@code ModifierKey.valueOf(String)} call sites during migration.
     * Throws {@link IllegalArgumentException} for unknown names so the
     * legacy parser falls through to the explicit switch.
     */
    private static ModifierKey valueOf(String name) {
        return switch (name) {
            case "UNBOUND", "NONE" -> UNBOUND;
            case "LEFT_CONTROL" -> ofGlfwCode(GLFW.GLFW_KEY_LEFT_CONTROL);
            case "RIGHT_CONTROL" -> ofGlfwCode(GLFW.GLFW_KEY_RIGHT_CONTROL);
            case "LEFT_SHIFT" -> ofGlfwCode(GLFW.GLFW_KEY_LEFT_SHIFT);
            case "RIGHT_SHIFT" -> ofGlfwCode(GLFW.GLFW_KEY_RIGHT_SHIFT);
            case "LEFT_ALT" -> ofGlfwCode(GLFW.GLFW_KEY_LEFT_ALT);
            case "RIGHT_ALT" -> ofGlfwCode(GLFW.GLFW_KEY_RIGHT_ALT);
            case "LEFT_SUPER" -> ofGlfwCode(GLFW.GLFW_KEY_LEFT_SUPER);
            case "RIGHT_SUPER" -> ofGlfwCode(GLFW.GLFW_KEY_RIGHT_SUPER);
            default -> throw new IllegalArgumentException("Unknown modifier key: " + name);
        };
    }
}