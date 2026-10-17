# Paper Game Standalone (Minecraft 1.21 / Fabric)

A separate Fabric client mod project with a configurable 1–9 item mapping, settings screen, HUD, Host/Player state toggle, nearby lever-state detection, and a rendering hook for stacks with custom names `1` through `9`.

## Build

Requires Java 21 and Gradle 8.12.1 or newer compatible with Loom 1.9.2:

```sh
gradle build
```

The jar is created in `build/libs/`. GitHub Actions builds it and uploads the `paper-game-mod` artifact.

## Controls

- `O`: toggle the HUD.
- `P`: open Paper Game settings.
- `K`: manually toggle Host/Player state.

The settings screen cycles each number through common example items. The full IDs are saved in `config/papergame.json` and can be edited there.

## Important fidelity note

This is a standalone reconstruction based on the available compiled BetterTOTEM JAR and conversation details. It is not guaranteed to be byte-for-byte or behaviour-for-behaviour identical to the original. In particular, the original's complete number-selection rules and every rendering path require in-game verification. Lever detection currently watches for any powered lever within six blocks of the player and maps powered to Host / unpowered to Player; adjust this if the original uses a specific lever or screen context.
