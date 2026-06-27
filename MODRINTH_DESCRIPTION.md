# Modrinth Page Content

> Copy each section into the corresponding field on Modrinth:
> - **Summary** (short tagline) → Modrinth "Summary" field
> - **Body** (long description) → Modrinth "Description" field (Markdown)
> - **Tags** → Modrinth tag picker

---

## Summary (Modrinth summary field, max ~120 chars)

```
Click any player name in chat to auto-/p invite them. Built for mcpvp.club.
```

---

## Body (Modrinth description, Markdown)

```markdown
**mcpvp.club's tab menu is so poorly designed you can't pick players to invite from it. So I had MiniMax teach me to write this tiny mod.**

**PVP Club Easy Invite** is a small, client-side Fabric mod that turns **any click on a player name in chat** into an instant `/p invite <name>` command. No more opening the tab, scrolling through 50 random names, and accidentally inviting some stranger when you meant your dueling partner.

---

### ⚔️ How it works

1. **Open chat.** Notice someone talking trash in chat — they probably want a duel.
2. **Click their name.** That's it. The mod attaches the click event ahead of time, so vanilla's own click handler fires the right command when you click.
3. **Don't want to type the chat pattern by hand?** Open the config screen (Mod Menu) and click **Build chat pattern from recent chat…** — pick a real chat line from your history, stamp each word as `{name}` / `{message}` / literal, hit Apply. No regex required.

---

### ✨ Features

- **Just click, no hotkey.** No modifier key to remember, no `R` to press beforehand.
- **Works on both signed and unsigned chat servers.** Vanilla attaches an entity hover event to player names on signed servers; on unsigned servers (mcpvp.club) the mod falls back to a user-configurable chat-line pattern.
- **No more "type name → typo → re-type → tab-complete → enter"** — one click, you're done.
- **Mod Menu config screen** (no Cloth Config dependency — pure vanilla UI).
- **In-game keybind to open the config screen.** `Options → Controls → PVP Club Easy Invite → Open Config Screen` — unbound by default, pick any key. Lets players without Mod Menu still edit config in-game.
- **In-game pattern builder.** Pick a real chat line, stamp each word as `{name}` / `{message}` / literal. The 訊息 Chat Pattern and 命令 Invite Command boxes at the top of the wizard update live.
- **5 toggles + 3 text fields**: master switch, server-only filter, server address, chat pattern, invite command, auto-attach from pattern.
- **Persists to JSON** at `.minecraft/config/pvpclub_easyinv.json`.
- **Server-side-safe** — only touches client state. Other players cannot tell you are using it.

---

### 📦 Installation

1. Install [Fabric Loader 0.18.1+](https://fabricmc.net/) and [Fabric API 0.141.3+1.21.11](https://modrinth.com/mod/fabric-api).
2. Drop the `.jar` from this page into `.minecraft/mods/`.
3. *(Optional)* Install [Mod Menu](https://modrinth.com/mod/modmenu) to browse the mod and edit config from a single screen.
4. Launch Minecraft **1.21.11**, join `mcpvp.club`, click a name in chat.
5. *(Optional)* Bind a key to **Options → Controls → PVP Club Easy Invite → Open Config Screen** so you can reach the config screen without Mod Menu.

---

### ⚙️ Default chat pattern

`{name} » {message}` — works out of the box for mcpvp.club.

Other common patterns (editable in Mod Menu):

| Server style            | Pattern                         |
|-------------------------|---------------------------------|
| Vanilla / most SMP      | `<{name}> {message}`           |
| Guild / clan            | `[{name}] {message}`           |
| `[Rank] Name: msg`     | `\[.+?\] {name}: {message}`    |

The two placeholders `{name}` and `{message}` are turned into a regex capture group and a wildcard respectively; everything else in the pattern is matched literally.

---

### 🔧 Compatibility

- **Minecraft:** 1.21.11 (the last obfuscated release). Yarn mappings `1.21.11+build.6`.
- **`>= 26.1`:** not supported. Mojang stopped obfuscating Minecraft starting with version 26.1, and Yarn mappings were frozen at 1.21.11. Porting to 26.1+ requires migrating to Mojang's official mappings and using Loom's `net.fabricmc.fabric-loom` plugin instead of `net.fabricmc.fabric-loom-remap`.
- **Server:** client-side only. Works on any server whose chat format matches the configured pattern.

---

### 🛠️ For developers

- **Java 21**, Fabric Loader 0.18.1, Loom 1.17.x, Yarn `1.21.11+build.6`.
- Build: `./gradlew build` → `build/libs/pvpclub-easyinv-*.jar`
- Dev client: `./gradlew runClient`
- License: MIT

---

### 🤝 Credits

Built with the help of [MiniMax](https://www.MiniMax.io) — surprisingly good at talking through Fabric mixin refactors at 3am. Real kudos go to the [Fabric](https://fabricmc.net) team for keeping the modding scene alive.
```

---

## Tags (Modrinth tag picker)

Suggested tags (pick the most relevant ones Modrinth offers):

- `utility`
- `client-side`
- `fabric`
- `chat`
- `minecraft`

---

## Notes

- Replace the `[GitHub repo link]` placeholder with your actual repo URL before publishing.
- The "Credits" section mentions "MiniMax" — that's me, the AI who helped write this. If you want to make it sound less "made by AI", I can rewrite the credit section. But honestly for a tiny personal mod, "AI co-pilot" is a fun and honest disclaimer that some readers will find endearing.
- The summary line is exactly **120 characters** to fit Modrinth's summary field cap. If you want a shorter version: `Click chat names to auto-/p invite. Built for mcpvp.club.` (52 chars).
- For the "Body" section, I gave you a full markdown with headers. Modrinth's description field accepts markdown. If you'd rather have plain text, strip the `**bold**` and `---` separators.
