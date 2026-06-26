# PVP Club Easy Invite

A small Fabric client-side mod for **mcpvp.club**: click any player name in
chat to automatically send `/p invite <name>`. No more typing the command,
no more misspelling nicknames.

> **Minecraft 1.21.11** with **Fabric Loader** and **Fabric API**.
> Mod Menu integration included — the mod shows up under
> `Mods → PVP Club Easy Invite → Config` and exposes a vanilla-styled
> config screen.

## What it does

You see a chat line like:

```
HARMANNSINGH » duel goat
```

…and the mod attaches a `/p invite <name>` click event to the player name
in advance, so vanilla's own click handler fires the right command when
you click it. Two ways the name gets attached, in priority order:

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
`[{name}] {message}`, etc.).

### Bonus: "Record Last Player" keybind (`R`)

Press **`R`** while a chat line is on screen and the mod will:

1. Take the most recent chat line.
2. Run the configured pattern over it.
3. Extract the player name.
4. Push it onto an in-memory stack.
5. The **next** chat line that comes in gets a click event attached
   to its entire text, set to `/p invite <recordedName>`.

Press R multiple times to stack up several names — successive clicks on
the next chat line invite them one by one (LIFO). Great for "I want to
duel all three of these guys".

## Features

- **Click-to-invite** (works for both signed and unsigned chat servers)
- **Mod Menu config screen** with six toggles + a press-to-bind key picker:
  - **Mod Enabled** — master switch (default: ON)
  - **Only on this server** — restrict to a specific server address
    (default: ON, address: `mcpvp.club`)
  - **Server Address** — substring matched against the current
    server's address (case-insensitive)
  - **Chat Pattern** — template with `{name}` and `{message}`
    placeholders (default: `{name} » {message}`)
  - **Auto-attach from pattern** — fall back to the pattern when no
    hover event is present (default: ON)
  - **Require Modifier Key** — gate the click behind a hotkey
    (default: ON)
  - **Modifier Key** — pick **any** key (default: `Unbound` — pick in
    the config screen, vanilla-controls-style press-to-bind UI)
- **Persists to JSON** at `.minecraft/config/pvpclub_easyinv.json`
- **Server-side-safe** — only touches client state, can't be detected
  by other players

## Project layout

```
pvpclub_easyinv/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── src/main/
    ├── java/com/pvpclub/easyinv/
    │   ├── PvpClubEasyInv.java         # entry point + R keybind
    │   ├── config/
    │   │   ├── ModConfig.java          # JSON-backed config + recordedPlayers stack
    │   │   ├── ModifierKey.java        # arbitrary-key + UNBOUND state
    │   │   └── ChatPattern.java        # {name}/{message} → compiled regex
    │   ├── gui/
    │   │   └── ConfigScreen.java       # vanilla config screen for Mod Menu
    │   ├── integration/
    │   │   └── ModMenuIntegration.java # wires into Mod Menu
    │   └── mixin/client/
    │       ├── ChatHudAccessor.java    # @Invoker for addMessage + @Accessor for messages
    │       └── ChatHudMixin.java       # injects the ClickEvent, handles pattern + record
    └── resources/
        ├── fabric.mod.json
        ├── pvpclub_easyinv.mixins.json
        └── assets/pvpclub_easyinv/icon.png
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
   `Style#withClickEvent(new ClickEvent$RunCommand("p invite <name>"))`.
4. **Pattern fallback**: if no hover event is found, the user's chat
   pattern is run against the whole line's text. If a name is
   extracted, attach the click event to the **first sibling** of the
   line — by Minecraft chat convention that sibling is the player name,
   so clicking it triggers the command without touching the rest of
   the line.
5. **Recorded-player stack**: if the user pressed `R` since the last
   chat line, pop one name and wrap the entire line with the click
   event (overriding any of the above, because recording is explicit).
6. The re-invocation of `addMessage` goes through a `@Invoker` accessor
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
build/libs/pvpclub-easyinv-1.2.0.jar
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
**Fabric Loader 0.18.1+** and **Fabric API 0.141.3+1.21.11** (and
**Mod Menu 10+** if you want the config screen). Launch the game, join
mcpvp.club, click a name in chat — done.

## Config file

Lives at `.minecraft/config/pvpclub_easyinv.json`:

```json
{
  "enabled": true,
  "serverOnly": true,
  "serverAddress": "mcpvp.club",
  "requireModifierKey": true,
  "modifierKey": -1,
  "chatPattern": "{name} » {message}",
  "autoAttachFromPattern": true
}
```

`modifierKey` is the GLFW key code as an integer; `-1` means Unbound
(no key chosen). `recordedPlayers` is in-memory only and intentionally
not persisted.

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

## Modifier key binding (vanilla controls style)

In the Mod Menu config screen, click the **Modifier Key** row once.
The button text changes to `Press a key…` and a centred prompt appears
above the buttons. Press **any** key — left Ctrl, F, K, a letter, whatever
you want. The binding is set. Press **Esc** while the screen is armed
to cancel.

The same row also accepts **mouse buttons** (left, right, middle, side
buttons) if you really want them as a modifier. (Most people won't.)

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
