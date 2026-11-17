# Betafront: notes for Claude

Read [KNOWLEDGE.md](KNOWLEDGE.md) before changing anything. It has the layout, build commands, verified BTA 8.0.1 API names, pitfalls and the current state.

The short version:

- Three Gradle builds: `core/` (pure Java parser and tests), `bta/` (main focus: BTA 8.0.1 + HalpLibe), `stationapi/` (Babric b1.7.3). The root folder is a composite build for IntelliJ: `./gradlew buildAll`, `runClientBta`, `runClientBabric`, `testCore`.
- Cloud sessions can't build the mods (network blocks the game and maven hosts). They can run the core tests with javac + a JUnit jar, and edit code and docs. Full builds need a developer machine, and agents can't see the game: never claim something looks right in game unless a person checked it.
- Other agents may be working on the repo too: pull before you start and before you commit.
- Keep NeoForge-derived files' LGPL headers. Keep the README's AI disclaimer and dependency tables.
- Keep `core/` free of game code. Update CHANGELOG.md, ROADMAP.md and KNOWLEDGE.md when what they say changes.
