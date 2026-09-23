# Thaumory

A Minecraft magic mod inspired by Thaumcraft, especially TC2.

In Thaumory, everything in the world is made of **aspects**. Melt items in a crucible to extract them as Essentia, draw magic circles around a Core, and feed the circles to reshape the world. You discover how it all works by scanning and experimenting. Careless magic leaves **Flux** behind.

> **Status:** early development. Nothing is playable yet. See [docs/plan.md](docs/plan.md) for progress.

## Features (planned)

- **Aspects**: 6 primal aspects in three opposing pairs (Ignis/Aqua, Aer/Terra, Vita/Mors) and 12 compound aspects. Every item has an aspect composition, taken from data or estimated from its recipes.
- **Magic circles**: a Core takes three runes. The first two choose the effect and the third sets its parameter, for example `Arcanum + Aer + Terra` teleports you to other circles on the Terra channel. Sigils drawn in rings around the Core set the range, power, efficiency, and stability.
- **Research by discovery**: scan items to learn their aspects. Your book records every circle you have tried, whether it worked or not, and gives you hints as you progress.
- **Arcane engineering**: move Essentia by hand, then through pipes, and later wirelessly.
- **Infusion**: aim a circle at an item to bind its effect into gear, scrolls, charms, or utility blocks.
- **Potion brewing**: each aspect pushes your brew across a hexagonal map, in the style of Potion Craft.
- **Flux**: mistakes pollute the chunk you are in. Left alone, it gets worse in stages, from harmless particles up to spawning mobs and circles misfiring.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5+, Fabric API, Architectury API 22.0.2+
- Java 25

NeoForge support is planned.

## Building

```sh
./gradlew build
```

The mod jar is written to `fabric/build/libs/`.

## For addon developers

Thaumory will ship a separate `thaumory-api` artifact. You will be able to add aspects, circle effects, sigils, and Essentia storage through it, and to listen for circle, Flux, and infusion events. It is not published yet.

## Documentation

- [Requirements](docs/requirements.md) (Japanese)
- [Implementation plan and progress](docs/plan.md) (Japanese)

## License

- Mod code: MPL-2.0
- `thaumory-api`: MIT

(The repository still has the initial MIT `LICENSE.txt`. It will be replaced when the license switch lands.)
