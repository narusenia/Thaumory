# AGENTS.md

Guidance for coding agents working on Thaumory.

## Project

Thaumory is a Minecraft magic mod inspired by Thaumcraft (especially TC2). Everything is built on one aspect system: items melt into aspects (Essentia), which fuel magic circles, infusion, and potion brewing. Mistakes produce Flux.

- Minecraft 26.3, Java 25, Architectury (`dev.architectury.loom-no-remap`)
- Fabric is the primary loader. NeoForge is planned for M4 — do not add it earlier.
- Package root: `one.nxeu.thaumory`, mod id `thaumory`

## Source of truth

Read these before starting any task:

1. `docs/requirements.md` — every design decision. Do not invent mechanics that contradict it. If a task needs a decision the document does not cover, stop and ask.
2. `docs/plan.md` — task list and progress. Work on tasks by ID (e.g. `M1-4`) and respect the `依存` (dependency) column.

When you finish a task, update `docs/plan.md` in the same commit: set its status to `[x]` and update the summary table. Use `[~]` for work in progress and `[-]` with a reason for dropped tasks. Never renumber task IDs.

If requirements change during implementation, update `docs/requirements.md` first, then adjust the plan.

## Layout

```
api/      thaumory-api: public extension points (MIT). Stable API only.
common/   Loader-agnostic mod logic.
fabric/   Fabric entrypoints and Fabric-specific implementations.
docs/     requirements.md, plan.md
```

`api/` is added by task M0-3. Until then, treat it as planned.

## Architecture rules

- **Data vs. code.** Numbers and mappings (item aspects, circle recipes, costs, Flux thresholds, potion map positions, research tree) live in datapack JSON and must reload with `/reload`. Behavior (what an effect does) lives in code.
- **Dogfood the API.** Built-in aspects, circle effects, and sigils register through `thaumory-api`, the same way addons would.
- **Keep logic testable.** Aspect math, recipe-based aspect estimation, circle validation, instability, Flux accumulation/decay, and potion path math belong in plain Java classes with no Minecraft dependencies, covered by JUnit tests. Minecraft-facing code is a thin adapter over them.
- **Performance.** Circle scans, pipe networks, and Flux processing run on intervals, not every tick. Pipe networks are computed per network and rebuilt only on change.
- **Loader-specific APIs.** Fabric Data Attachment and Transfer API are used through small interfaces in `common` so NeoForge can implement them in M4.
- **Stability labels.** API extension points 4–6 (infusion effects, potion map, research entries) are experimental until M4. Mark them clearly.

## Localization

Every user-facing string needs both `en_us` and `ja_jp` entries. Generate lang files through datagen where possible.

## Commands

```sh
./gradlew build          # build all subprojects
./gradlew test           # unit tests
./gradlew :fabric:runClient
./gradlew :fabric:runServer
```

GameTest and datagen tasks are set up in M0-5 and M0-6.

## Git

- Conventional Commits, single line, no body: `feat: add aspect registry`, `fix: crucible overflow flux`, `docs: update plan progress`
- Name concrete things. Never use process words such as "phase 1", "step 2", or "review fixes" in commits or branch names.
- One logical change per commit.
- Never commit secrets. Keep them in `.env` (git-ignored).

## Licensing

Mod code is MPL-2.0 and `thaumory-api` is MIT (the switch is task M0-2). Do not copy code or assets from Thaumcraft or other mods.
