# PVP Club Easy Invite

A small Fabric client-side mod for **mcpvp.club**: click any player name in
chat to automatically send `/p invite <name>`. No more typing the command,
no more misspelling nicknames.

> **Minecraft 1.21.11** with **Fabric Loader** and **Fabric API**.
> Configurable via **Mod Menu** (`Mods → PVP Club Easy Invite → Config`)
> **or** an in-game keybind (`Options → Controls → PVP Club Easy Invite →
> Open Config Screen`) — Mod Menu is no longer required.

## What it does

You see a chat line like:

```
HARMANNSINGH » duel goat
```

…just **click** the player name. The mod attaches a `/p invite <name>`
click event to the name in advance, so vanilla's own click handler
fires the right command when you click it. No hotkeys, no record-then-click
dance — just click.

Two ways the name gets attached, in priority order:

### 1. Vanilla `HoverEvent$ShowEntity` (signed chat)

On servers that enable Mojang's chat signing (1.19.1+), vanilla Minecraft
attaches an entity hover event to every player-sender component. The mod
uses that as ground truth for "this is a player name", and attaches the
click event to that exact node — clicking the name fires the command,
clicking the rest of the line does nothing.

### 2. Chat-pattern fallback (unsigned chat — the mcpvp.club case)

Most anonymous PVP servers (including mcpvp.club) **don't** sign chat
messages. The player name is just plain text, with no hover or click
metadata. In that case the mod falls back to a user-configured pattern —
default `{name} » {message}`. The `{name}` placeholder becomes a regex
capture group; the click event is attached to the **first sibling** of
the chat line (which by Minecraft chat convention *is* the player name).
The pattern is configurable in Mod Menu (`<{name}> {message>`,
`[{name}] {message}`, etc.) — and you don't have to type the regex
yourself: there's a **Build chat pattern from recent chat…** wizard
that splits a real chat line into clickable tokens and lets you stamp
each one with `{name}` / `{message}` / literal.

## Features

- **Click-to-invite** (works for both signed and unsigned chat servers)
- **Two ways to open the config screen**:
  - **Mod Menu** — `Mods → PVP Club Easy Invite → Config` (requires Mod Menu)
  - **In-game keybind** — `Options → Controls → PVP Club Easy Invite →
    Open Config Screen`. **Unbound by default** so it never clashes
    with your existing habits; pick whatever key you like.
- **Mod Menu config screen** with the essentials:
  - **Mod Enabled** — master switch (default: ON)
  - **Only on this server** — restrict to a specific server address
    (default: ON, address: `mcpvp.club`)
  - **Server Address** — substring matched against the current
    server's address (case-insensitive)
  - **Chat Pattern** — template with `{name}` and `{message}`
    placeholders (default: `{name} » {message}`)
  - **Build chat pattern from recent chat…** — in-game wizard. Pick a
    real chat line from your history, click each word to stamp it
    as `{name}` / `{message}` / literal. The result fills the Chat Pattern
    box automatically.
  - **Invite Command** — what the mod sends when you click a name
    (default: `/p invite {name}`)
  - **Auto-attach from pattern** — fall back to the pattern when no
    hover event is present (default: ON)
- **Persists to JSON** at `.minecraft/config/pvpclub_easyinv.json`
- **Server-side-safe** — only touches client state, can't be detected
  by other players

## v1.4.0 changes

- **Open Config Screen keybind.** New keybind registered under the
  `PVP Club Easy Invite` category in `Options → Controls`. Bound to
  nothing by default so it never fires unexpectedly — pick a key
  you like. The keybind deliberately does **not** yank you out of
  screens that own text input (chat, anvil, sign, command block,
  book) so you don't lose what you were typing.
- **Mod Menu is now optional.** Players without Mod Menu can still
  reach the config screen via the keybind, and via direct JSON edits
  at `.minecraft/config/pvpclub_easyinv.json`.

## v1.3.0 changes

- **Removed the modifier-key toggle and the `R` "record" keybind.**
  Clicks always fire on chat lines when the mod is enabled and the
  player is on the configured server — one less thing to remember.
- **Redesigned the chat-pattern wizard.** The two result boxes
  (訊息 Chat pattern and 命令 Invite command) now sit at the
  **top** of the screen, so you see the answer before you start
  stamping. The tag selector and chat-line tokens live below.
- Old `requireModifierKey` / `modifierKey` JSON fields are silently
  ignored on load — the new config schema is a strict subset.

## Project layout

```
pvpclub_easyinv/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── src/main/
    ├── java/com/pvpclub/easyinv/
    │   ├── PvpClubEasyInv.java         # entry point
    │   ├── config/
    │   │   ├── ModConfig.java          # JSON-backed config + recent chat lines buffer
    │   │   └── ChatPattern.java        # {name}/{message} → compiled regex
    │   ├── gui/
    │   │   ├── ConfigScreen.java               # vanilla config screen for Mod Menu
    │   │   ├── ChatHistoryPickerScreen.java    # step 1: pick a real chat line
    │   │   └── ChatPatternBuilderScreen.java   # step 2: stamp words with {name}/{message}
    │   ├── integration/
    │   │   └── ModMenuIntegration.java # wires into Mod Menu
    │   ├── keybind/
    │   │   └── ModKeyBindings.java     # "Open Config Screen" keybind (v1.4.0+)
    │   └── mixin/client/
    │       ├── ChatHudAccessor.java    # @Invoker for addMessage (re-invoke without recursion)
    │       └── ChatHudMixin.java       # injects the ClickEvent, handles hover + pattern paths
    └── resources/
        ├── fabric.mod.json
        ├── pvpclub_easyinv.mixins.json
        └── assets/pvpclub_easyinv/
            ├── icon.png
            └── lang/en_us.lang         # keybind category & name (v1.4.0+)
```

## How it works

In 1.21.x Mojang reworked how chat click events are routed through the
HUD: `ChatHud#mouseClicked` and `#getComponentStyleAt` were removed, and
`ClickEvent` / `HoverEvent` became sealed interfaces (concrete
implementations: `ClickEvent$RunCommand`, `HoverEvent$ShowEntity`, etc.).
Rather than chase the new click pipeline, we take the more robust route:

1. `mixin ChatHud#addMessage(Text, MessageSignatureData, MessageIndicator)`.
2. Before vanilla processes the line, we walk the `Text` tree.
3. **Hover event first**: any node whose `Style` carries
   `HoverEvent$ShowEntity` is a player name — attach
   `Style#withClickEvent(new ClickEvent$RunCommand(commandPattern))`.
4. **Pattern fallback**: if no hover event is found, the user's chat
   pattern is run against the whole line's text. If a name is
   extracted, attach the click event to the **first sibling** of the
   line — by Minecraft chat convention that sibling is the player name,
   so clicking it triggers the command without touching the rest of
   the line.
5. The re-invocation of `addMessage` goes through a `@Invoker` accessor
   so the mixin doesn't recurse into itself.

All chat rewriting rebuilds trees with **empty-string roots** so the
concatenated sibling string isn't duplicated when reconstructing.

## Build

You need **JDK 21+** installed. Then:

```bash
# first time only — generates gradle wrapper
gradle wrapper

# build the mod jar into build/libs/
./gradlew build
```

The output is at:

```
build/libs/pvpclub-easyinv-1.4.0.jar
```

## Run the dev client

```bash
./gradlew runClient
```

Loom downloads the Minecraft 1.21.11 client jar, Fabric Loader 0.18.1,
Fabric API 0.141.3+1.21.11, Mod Menu, and launches the dev client with
the mod loaded. You can iterate on config screens and re-launch quickly.

## Install

Drop the jar into your `.minecraft/mods/` folder together with
**Fabric Loader 0.18.1+** and **Fabric API 0.141.3+1.21.11**.
**Mod Menu is no longer required** — the config screen is reachable
via the keybind in `Options → Controls → PVP Club Easy Invite →
Open Config Screen`. (Mod Menu is still supported if you prefer
browsing your mods and their configs from a single screen.) Launch
the game, join mcpvp.club, click a name in chat — done.

## Config file

Lives at `.minecraft/config/pvpclub_easyinv.json`:

```json
{
  "enabled": true,
  "serverOnly": true,
  "serverAddress": "mcpvp.club",
  "chatPattern": "{name} » {message}",
  "autoAttachFromPattern": true,
  "commandPattern": "/p invite {name}"
}
```

`recentChatLines` (the rolling buffer used by the pattern wizard) is
in-memory only and intentionally not persisted.

## Chat pattern syntax

The pattern is a plain text template with two placeholders:

| Placeholder    | Meaning                                                |
|----------------|--------------------------------------------------------|
| `{name}`       | the player name (this is what gets extracted)          |
| `{message}`    | the rest of the chat line, ignored by the matcher      |

Everything else in the pattern is matched literally. Use `\{` or `\}` to
put a literal curly brace in your pattern.

Common patterns:

| Server style                  | Pattern                       |
|-------------------------------|-------------------------------|
| mcpvp.club                    | `{name} » {message}`          |
| Vanilla / most SMP            | `<{name}> {message}`          |
| Guild/clan                    | `[{name}] {message}`          |
| `[Rank] Name: msg`           | `\[.+?\] {name}: {message}`   |

### Don't want to type the pattern by hand?

Click **Build chat pattern from recent chat…** in the config screen:

1. The wizard shows the last 30 chat lines the mod has seen.
2. Pick the line that looks like a typical chat message on this server.
3. On the next screen, the line is split into clickable word-buttons.
4. Pick a tag at the top — **literal** / **{name}** / **{message}** —
   then click each word to stamp it.
5. The **訊息 Chat pattern** and **命令 Invite command** boxes at the
   top update live. Tweak them by hand if you like, then **Apply**.

## Compatibility notes

- **Minecraft version**: built and tested against **1.21.11** (the
  last obfuscated release). Loom 1.17.12 / Fabric Loader 0.18.1.
- **Yarn mappings 1.21.11**: `1.21.11+build.6`.
- **What about >= 26.1?** Mojang has officially **stopped obfuscating
  Minecraft starting with the 26.1 release**, and Yarn mappings are
  **frozen at 1.21.11**. To support 26.1+ the mod would have to be
  ported to the unobfuscated game (Loom's
  `net.fabricmc.fabric-loom` plugin + Mojang's official mappings).
  This source tree as-is will not load on 26.1+; that's a hard
  ecosystem-wide constraint, not something a single mod can opt out
  of. Plan a small fork when you want to cross that bridge.
- **Server-side**: this mod only modifies client behaviour. It is
  perfectly safe to use on any server. Other players cannot tell you
  are using it.
- **Self-invite / no-party edge cases**: if you have no party yet,
  `/p invite <name>` is interpreted by the server as "create a party
  and invite that player". If the player is offline, the server's
  normal rejection message will appear in chat as usual.

## License

MIT.