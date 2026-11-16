# Installing Betafront

Betafront is client side: it only changes how blocks are drawn. Pick the jar that matches your game.

## For players

### Better Than Adventure

| You need | Version |
| --- | --- |
| Better Than Adventure | 8.0.1 |
| Fabric Loader (BTA fork) | 0.18.4-bta.11 or newer (comes with the BTA Babric instance) |
| [HalpLibe](https://github.com/Turnip-Labs/bta-halplibe) | 6.2.0+8.0.1 or newer |
| Java | 17 or newer |

Put `betafront-bta-<version>+8.0.1.jar` and HalpLibe in the instance's `mods` folder. [ModMenu](https://github.com/Turnip-Labs/ModMenu) is optional.

### Beta 1.7.3 with StationAPI

| You need | Version |
| --- | --- |
| Minecraft | Beta 1.7.3 |
| [Babric](https://github.com/babric/prism-instance) | Fabric Loader 0.19.3 or newer |
| [StationAPI](https://modrinth.com/mod/stationapi) | 2.0.0-alpha.6.5 or newer |
| Java | 17 or newer |

Put `betafront-stationapi-<version>.jar` and StationAPI in the instance's `mods` folder.

## For mod developers

Betafront isn't published to a Maven repository yet. Build the jar (`./gradlew buildAll`, see the [main README](../README.md#building)) or download it, and put it in your mod project, for example in `libs/`.

### BTA mod

You call `ObjModels.block(...)` from code, so Betafront is a compile dependency:

```kotlin
// build.gradle.kts
dependencies {
    implementation(files("libs/betafront-bta-0.1.0-alpha.1+8.0.1.jar"))
}
```

### StationAPI mod

You only write JSON files, so Betafront just has to be present at runtime. In the dev client:

```groovy
// build.gradle
dependencies {
    modLocalRuntime files("libs/betafront-stationapi-0.1.0.jar")
}
```

### Declare the dependency

Add Betafront to your `fabric.mod.json`, so players get a clear message when it's missing:

```json
"depends": {
  "betafront": "*"
}
```
