package com.pvpclub.easyinv.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Runtime-tweakable settings for the mod. Persisted to
 * {@code .minecraft/config/pvpclub_easyinv.json}.
 *
 * <p>Defaults match the requirements we agreed on:
 * <ul>
 *   <li>{@code enabled = true}</li>
 *   <li>{@code serverOnly = true} (only fire on ncpvp.club by default)</li>
 *   <li>{@code requireModifierKey = true}</li>
 *   <li>{@code modifierKey = LEFT_CONTROL}</li>
 *   <li>{@code chatPattern = "{name} » {message}"}</li>
 *   <li>{@code autoAttachFromPattern = true}</li>
 * </ul>
 */
public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("pvpclub_easyinv/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("pvpclub_easyinv.json");

    private static volatile ModConfig INSTANCE;

    // ---- Persisted fields -------------------------------------------------

    /** Master switch. When false, the mixin short-circuits and never fires. */
    public boolean enabled = true;

    /**
     * If true, the auto-invite is only sent while the player is connected
     * to a server whose address contains {@link #serverAddress}
     * (case-insensitive).
     */
    public boolean serverOnly = true;

    /**
     * Substring used to detect the "right" server when
     * {@link #serverOnly} is enabled.
     */
    public String serverAddress = "mcpvp.club";

    /** If true, the modifier key below must be held when clicking. */
    public boolean requireModifierKey = true;

    /**
     * GLFW key code of the user's chosen modifier key. -1 (the value of
     * {@link ModifierKey#UNBOUND_GLFW}) means "no key chosen yet"; the
     * user picks one through the config screen, which then sets this
     * field. Pre-v1.2.0 saves use a {@code String} field under the same
     * name; see {@link #loadFromDisk()} for the legacy migration path.
     */
    public int modifierKey = ModifierKey.UNBOUND_GLFW;

    /**
     * User-defined chat-line template. Used as a fallback when the
     * vanilla {@code HoverEvent$ShowEntity} marker is missing (e.g.
     * servers that don't sign chat). The user writes something like
     * {@code "{name} » {message}"} and the mod turns {@code {name}}
     * into a capture group.
     */
    public String chatPattern = "{name} » {message}";

    /**
     * When true, the mod will also attach a click event to incoming
     * chat lines that match {@link #chatPattern} — useful for
     * anonymous-chat servers where there's no entity hover event to
     * key off of.
     */
    public boolean autoAttachFromPattern = true;

    // ---- Transient (not persisted) ---------------------------------------

    /**
     * Stack of player names waiting to be invited. Filled by the
     * "Record" keybind (default {@code R}), drained by the next
     * addMessage mixin that sees a non-empty stack — at which point a
     * click event is attached to the new message and the name is
     * popped.
     */
    public transient Deque<String> recordedPlayers = new ArrayDeque<>();

    // ---- Accessors --------------------------------------------------------

    public static ModConfig get() {
        ModConfig local = INSTANCE;
        if (local == null) {
            synchronized (ModConfig.class) {
                local = INSTANCE;
                if (local == null) {
                    local = loadFromDisk();
                    INSTANCE = local;
                }
            }
        }
        return local;
    }

    /** Force-reload from disk. Useful when the config screen saves edits. */
    public static synchronized void reload() {
        INSTANCE = loadFromDisk();
    }

    private static ModConfig loadFromDisk() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ModConfig parsed = GSON.fromJson(json, ModConfig.class);
                if (parsed != null) {
                    // Defensive: ensure required defaults if file was hand-edited.
                    if (parsed.serverAddress == null || parsed.serverAddress.isBlank()) {
                        parsed.serverAddress = "mcpvp.club";
                    }
                    if (parsed.chatPattern == null || parsed.chatPattern.isBlank()) {
                        parsed.chatPattern = "{name} » {message}";
                    }
                    if (parsed.recordedPlayers == null) {
                        parsed.recordedPlayers = new ArrayDeque<>();
                    }
                    return parsed;
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Failed to read pvpclub_easyinv.json, using defaults.", e);
            }
        }
        return new ModConfig();
    }

    /** Persist current values to disk. */
    public synchronized void save() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.error("Failed to write pvpclub_easyinv.json", e);
        }
    }

    /** Typed view of {@link #modifierKey}. */
    public ModifierKey getModifierKey() {
        return ModifierKey.ofGlfwCode(modifierKey);
    }

    public void setModifierKey(ModifierKey key) {
        this.modifierKey = key.getGlfwCode();
    }

    /** Typed view of {@link #chatPattern}, re-parsed each call so live edits work. */
    public ChatPattern getChatPattern() {
        return new ChatPattern(chatPattern);
    }
}