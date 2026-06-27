package com.pvpclub.easyinv.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Runtime-tweakable settings for the mod. Persisted to
 * {@code .minecraft/config/pvpclub_easyinv.json}.
 *
 * <p>Defaults match the requirements we agreed on:
 * <ul>
 *   <li>{@code enabled = true}</li>
 *   <li>{@code serverOnly = true} (only fire on mcpvp.club by default)</li>
 *   <li>{@code chatPattern = "{name} » {message}"}</li>
 *   <li>{@code autoAttachFromPattern = true}</li>
 * </ul>
 *
 * <p><b>v1.3.0:</b> removed the {@code requireModifierKey} / {@code modifierKey}
 * fields and the {@code recordedPlayers} stack — clicks always fire on chat
 * lines (gated only by {@link #enabled} and the server filter), and the
 * Record keybind (R) was removed in favour of an in-game pattern builder
 * that is reached from the config screen.</p>
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

    /**
     * Command template sent when the user clicks a chat name. Supports
     * two placeholders:
     * <ul>
     *   <li>{@code {name}} — the extracted player name (mandatory;
     *       leave it out and the invite won't have a target)</li>
     *   <li>{@code {message}} — the rest of the line, when the chat
     *       pattern used a {@code {message}} group</li>
     * </ul>
     * Default is the mcpvp.club party-invite command.
     */
    public String commandPattern = "/p invite {name}";

    // ---- Transient (not persisted) ---------------------------------------

    /**
     * Rolling buffer of the last few chat lines seen by the mixin, in
     * arrival order. Used by the in-game {@code ChatHistoryPickerScreen}
     * so the user can build a chat pattern by clicking words in a real
     * line instead of typing regex-like syntax by hand. Memory only,
     * never persisted.
     */
    public transient java.util.List<String> recentChatLines = new ArrayList<>();

    /**
     * Maximum number of chat lines kept in {@link #recentChatLines}.
     * Small enough that we don't grow unbounded, large enough that the
     * user almost always sees their last few messages.
     */
    public static final int MAX_RECENT_CHAT_LINES = 30;

    /**
     * Push a raw chat line onto the rolling history. No-op for blank
     * lines. Oldest entries fall off the front once the cap is hit.
     */
    public synchronized void captureChatLine(String raw) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        recentChatLines.add(trimmed);
        while (recentChatLines.size() > MAX_RECENT_CHAT_LINES) {
            recentChatLines.remove(0);
        }
    }

    /**
     * Convenience used by the mixin: render the {@link #commandPattern}
     * by substituting the captured {@code name} (and optional
     * {@code message}). Unknown placeholders are left in place so the
     * user notices typos in the config.
     */
    public String renderCommand(String name, String message) {
        if (commandPattern == null || commandPattern.isEmpty()) {
            // Fallback: behave like the old hardcoded command.
            return "p invite " + (name == null ? "" : name);
        }
        String out = commandPattern;
        if (name != null) {
            out = out.replace("{name}", name);
        }
        if (message != null) {
            out = out.replace("{message}", message);
        }
        return out;
    }

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
                    if (parsed.commandPattern == null || parsed.commandPattern.isBlank()) {
                        parsed.commandPattern = "/p invite {name}";
                    }
                    if (parsed.recentChatLines == null) {
                        parsed.recentChatLines = new ArrayList<>();
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

    /** Typed view of {@link #chatPattern}, re-parsed each call so live edits work. */
    public ChatPattern getChatPattern() {
        return new ChatPattern(chatPattern);
    }
}