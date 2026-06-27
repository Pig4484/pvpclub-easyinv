package com.pvpclub.easyinv.gui;

import com.pvpclub.easyinv.config.ModConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Token-tagging wizard for the chat-pattern builder. The user picks a
 * chat line from {@link ChatHistoryPickerScreen} and lands here with
 * the line pre-split into whitespace-delimited words. They stamp each
 * word with one of three roles:
 *
 * <ul>
 *   <li><b>literal</b> — keep the word verbatim in the template</li>
 *   <li><b>{name}</b> — replace this word with the player-name
 *       placeholder in the chat pattern</li>
 *   <li><b>{message}</b> — replace this word with the message
 *       placeholder (which becomes {@code .*?} under the hood)</li>
 * </ul>
 *
 * <p><b>Layout (v1.3.0):</b> the two output boxes — labelled
 * "Chat pattern" (訊息) and "Invite command" (命令) — sit at the
 * <i>top</i> of the screen so the result is visible immediately and
 * updates live as the user stamps tokens. Below that comes the tag
 * selector, then the chat-line tokens, then the action buttons.</p>
 *
 * <p>Both boxes are also editable text fields so the user can
 * hand-tweak the result before pressing Apply.</p>
 *
 * <p>On Apply, both fields are written back to {@link ModConfig}. On
 * Cancel or close, nothing is written — same semantics as the main
 * config screen's Cancel button.</p>
 */
public class ChatPatternBuilderScreen extends Screen {

    /** Role of a single token in the source line. */
    private enum TokenTag {
        LITERAL, NAME, MESSAGE;

        /** Short label used both in the tag-selector buttons and as a
         *  suffix on token buttons to remind the user what they picked. */
        String shortLabel() {
            return switch (this) {
                case LITERAL -> "literal";
                case NAME -> "{name}";
                case MESSAGE -> "{message}";
            };
        }

        /** Color used for the tag-selector button when active, and for
         *  the token-button suffix. Vanilla chat color codes. */
        String colorCode() {
            return switch (this) {
                case LITERAL -> "§7";
                case NAME -> "§a";
                case MESSAGE -> "§b";
            };
        }
    }

    private static final int ROW_WIDTH = 380;
    private static final int FIELD_HEIGHT = 20;
    private static final int TOKEN_BUTTON_HEIGHT = 22;
    private static final int TOKEN_BUTTON_PAD = 4;

    private final Screen parent;
    private final String originalLine;
    private final List<String> tokens = new ArrayList<>();
    private final List<TokenTag> tags = new ArrayList<>();

    /** Role that the next token click will assign. */
    private TokenTag currentTag = TokenTag.NAME;

    /** Token buttons — recreated whenever the user stamps a token so
     *  their labels reflect the new tag. */
    private final List<ButtonWidget> tokenButtons = new ArrayList<>();
    private ButtonWidget literalSelector;
    private ButtonWidget nameSelector;
    private ButtonWidget messageSelector;

    private TextFieldWidget chatPatternField;
    private TextFieldWidget commandField;

    /** Set true while we're programmatically setting the text-field
     *  text from a tag change, so the change handler doesn't loop back
     *  into the token-stamping logic. */
    private boolean updatingFromTags = false;

    public ChatPatternBuilderScreen(Screen parent, String line) {
        super(Text.literal("Build Chat Pattern"));
        this.parent = parent;
        this.originalLine = line == null ? "" : line;
        // Tokenize. Strip Minecraft color codes first so they don't
        // end up glued to whatever word follows them.
        String stripped = this.originalLine.replaceAll("§.", "").trim();
        if (stripped.isEmpty()) {
            this.tokens.add("");
        } else {
            this.tokens.addAll(Arrays.asList(stripped.split("\\s+")));
        }
        for (int i = 0; i < tokens.size(); i++) {
            tags.add(TokenTag.LITERAL);
        }
    }

    // ---- Layout -----------------------------------------------------------

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int x = centerX - ROW_WIDTH / 2;

        // --- TOP: the two result boxes (訊息 + 命令) -----------------------
        // Putting the result first matches the user's mental model: see
        // the answer immediately, then scroll down to learn how to edit.
        // The chat-pattern field is editable; the command field starts
        // with the saved command so the user can keep what they had.

        int patternFieldY = 34;
        chatPatternField = new TextFieldWidget(
                this.textRenderer, x, patternFieldY, ROW_WIDTH, FIELD_HEIGHT,
                Text.literal("Chat pattern")
        );
        chatPatternField.setMaxLength(128);
        chatPatternField.setText(buildChatPattern());
        this.addDrawableChild(chatPatternField);

        int commandFieldY = patternFieldY + 28;
        commandField = new TextFieldWidget(
                this.textRenderer, x, commandFieldY, ROW_WIDTH, FIELD_HEIGHT,
                Text.literal("Invite command")
        );
        commandField.setMaxLength(128);
        commandField.setText(ModConfig.get().commandPattern);
        this.addDrawableChild(commandField);

        // --- MIDDLE: tag selector row (literal / {name} / {message}) ------
        int selectorY = commandFieldY + 38;
        int selectorW = 90;
        literalSelector = ButtonWidget.builder(
                selectorLabel(TokenTag.LITERAL),
                btn -> setCurrentTag(TokenTag.LITERAL)
        ).dimensions(centerX - selectorW * 3 / 2 - 4, selectorY, selectorW, FIELD_HEIGHT).build();
        nameSelector = ButtonWidget.builder(
                selectorLabel(TokenTag.NAME),
                btn -> setCurrentTag(TokenTag.NAME)
        ).dimensions(centerX - selectorW / 2, selectorY, selectorW, FIELD_HEIGHT).build();
        messageSelector = ButtonWidget.builder(
                selectorLabel(TokenTag.MESSAGE),
                btn -> setCurrentTag(TokenTag.MESSAGE)
        ).dimensions(centerX + selectorW / 2 + 4, selectorY, selectorW, FIELD_HEIGHT).build();
        this.addDrawableChild(literalSelector);
        this.addDrawableChild(nameSelector);
        this.addDrawableChild(messageSelector);

        // --- MIDDLE: chat-line token buttons -------------------------------
        rebuildTokenButtons(x, selectorY + 34);

        // --- BOTTOM: Cancel / Apply ----------------------------------------
        int bottomY = this.height - 30;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel"),
                btn -> close()
        ).dimensions(centerX - 154, bottomY, 150, FIELD_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Apply"),
                btn -> apply()
        ).dimensions(centerX + 4, bottomY, 150, FIELD_HEIGHT).build());
    }

    private void setCurrentTag(TokenTag tag) {
        currentTag = tag;
        literalSelector.setMessage(selectorLabel(TokenTag.LITERAL));
        nameSelector.setMessage(selectorLabel(TokenTag.NAME));
        messageSelector.setMessage(selectorLabel(TokenTag.MESSAGE));
    }

    /**
     * Re-create token buttons from scratch. We need to do this (rather
     * than just updating labels) because the buttons have variable
     * widths based on the token text and we want them to fit on one
     * row when possible, wrap otherwise.
     */
    private void rebuildTokenButtons(int startX, int startY) {
        // Drop the old ones from the drawable list.
        for (ButtonWidget b : tokenButtons) {
            this.remove(b);
        }
        tokenButtons.clear();

        int x = startX;
        int y = startY;
        int maxX = startX + ROW_WIDTH;

        for (int i = 0; i < tokens.size(); i++) {
            String word = tokens.get(i);
            String label = tokenLabel(i, word);
            int w = Math.min(ROW_WIDTH, this.textRenderer.getWidth(label) + 12);
            if (x + w > maxX && x > startX) {
                // Wrap to a new row.
                x = startX;
                y += TOKEN_BUTTON_HEIGHT + TOKEN_BUTTON_PAD;
            }
            final int idx = i;
            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal(label),
                    clickBtn -> stampToken(idx)
            ).dimensions(x, y, w, TOKEN_BUTTON_HEIGHT).build();
            tokenButtons.add(btn);
            this.addDrawableChild(btn);
            x += w + TOKEN_BUTTON_PAD;
        }
    }

    private void stampToken(int idx) {
        if (idx < 0 || idx >= tokens.size()) {
            return;
        }
        tags.set(idx, currentTag);
        // Update only the affected button's label, so we don't lose
        // focus / scroll state on the chat-pattern field below.
        tokenButtons.get(idx).setMessage(Text.literal(tokenLabel(idx, tokens.get(idx))));
        // Re-generate the chat-pattern preview text.
        updatingFromTags = true;
        chatPatternField.setText(buildChatPattern());
        updatingFromTags = false;
    }

    // ---- Pattern building ------------------------------------------------

    /** Concatenates tokens with their assigned tags into the chat pattern. */
    private String buildChatPattern() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            switch (tags.get(i)) {
                case LITERAL -> sb.append(tokens.get(i));
                case NAME -> sb.append("{name}");
                case MESSAGE -> sb.append("{message}");
            }
            if (i < tokens.size() - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    // ---- Apply / Cancel --------------------------------------------------

    private void apply() {
        ModConfig config = ModConfig.get();
        String chatPat = chatPatternField.getText();
        if (chatPat != null && !chatPat.isBlank()) {
            config.chatPattern = chatPat;
        }
        String cmd = commandField.getText();
        if (cmd != null && !cmd.isBlank()) {
            config.commandPattern = cmd;
        }
        config.save();
        close();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    // ---- Labels ----------------------------------------------------------

    private Text selectorLabel(TokenTag tag) {
        boolean active = tag == currentTag;
        String prefix = active ? "§6» §r" : "§8  ";
        String body = active
                ? tag.colorCode() + "[" + tag.shortLabel() + "]§r"
                : "§7" + tag.shortLabel() + "§r";
        return Text.literal(prefix + body);
    }

    /** Token button label: shows the original word + a small tag suffix
     *  so the user can tell at a glance which words are placeholders. */
    private String tokenLabel(int idx, String word) {
        TokenTag tag = tags.get(idx);
        String wordDisplay = word.isEmpty() ? "§8<empty>§r" : word;
        return switch (tag) {
            case LITERAL -> "§7" + wordDisplay + "§r";
            case NAME -> "§7" + wordDisplay + " §a→ {name}§r";
            case MESSAGE -> "§7" + wordDisplay + " §b→ {message}§r";
        };
    }

    // ---- Render ----------------------------------------------------------

    @Override
    public void render(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x80000000);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        int labelX = this.width / 2 - ROW_WIDTH / 2;

        // Top labels: 訊息 / 命令
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("§7訊息 Chat pattern (use {name}, {message}):§r"),
                labelX, 22, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("§7命令 Invite command (use {name}):§r"),
                labelX, 62, 0xFFFFFF);

        // Tag selector hint
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("§7↓ Pick a tag, then click each word below to assign it§r"),
                labelX, 96, 0xFFFFFF);

        // Source line label
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("§7Chat line from server:§r"),
                labelX, 124, 0xFFFFFF);

        // Hint when the current pattern would extract no name.
        if (!buildChatPattern().contains("{name}")) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§e⚠ No word tagged as {name} — the chat pattern won't extract anything.§r"),
                    this.width / 2, this.height - 50, 0xFFFFFF);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }
}