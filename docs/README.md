# Betafront documentation

Betafront lets Minecraft Beta mods use Wavefront `.obj` files as block models. It comes in two builds that read the same model JSON:

- **BTA build**: Better Than Adventure 8.0.1, on HalpLibe.
- **StationAPI build**: regular Babric for Beta 1.7.3, on StationAPI.

The model JSON follows NeoForge's `neoforge:obj` loader, so most NeoForge OBJ models carry over with few edits.

## Guides

1. [Installing Betafront](installing.md): what players and mod developers need for each build.
2. [Adding an OBJ block model](adding-models.md): file locations, the model JSON, and the OBJ and MTL features Betafront reads.
3. [Worked example](example.md): the two bundled test blocks, file by file, and how to try them in the dev client.
4. [Known limitations](limitations.md): what doesn't work yet.

For building Betafront itself, see the [main README](../README.md#building).
