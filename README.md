# Immersive Paintings 0.7.15 for NeoForge 1.21.1

[中文说明](README.zh-CN.md) · [Changelog](changelog.md) · [Third-party notices](THIRD_PARTY_NOTICES.md)

This repository is a community-maintained NeoForge port of [Luke100000/ImmersivePaintings](https://github.com/Luke100000/ImmersivePaintings). It is not an official upstream release.

| Component | Requirement |
| --- | --- |
| Immersive Paintings | `0.7.15+1.21.1` |
| Minecraft | `1.21.1` |
| Java | `21` |
| NeoForge | `21.1.154` or newer |
| Fzzy Config | `0.7.0+1.21` |
| Optional translation | MineAstr `0.6.29` |

## Features

- Upload and display player-provided paintings.
- Browse local screenshots, URLs, drag-and-drop input, and the server painting library.
- Keep uploaded images in the server painting cache and index them through the mod's SavedData.
- Preserve the NeoForge migration fixes for painting direction and rotation.
- Optionally translate painting images through MineAstr while the painting is targeted.

No player images or server painting databases are stored in this source repository.

## Installation

Install the same Immersive Paintings JAR on client and server:

```text
immersive_paintings-neoforge-1.21.1-0.7.15.jar
```

Do not keep an older Immersive Paintings JAR beside it. The server owns painting metadata and uploaded images; the client owns editing screens, image selection, rendering, and optional translation requests.

## MineAstr image translation

Optional image translation requires MineAstr 0.6.29 on both client and server.

```text
Target painting
  → Immersive Paintings full-image client cache
  → bounded JPEG encoding
  → MineAstr public image-translation API
  → AstrBot bridge
  → MineAstr target HUD
```

The integration does not connect directly to AstrBot and does not move the original painting into MineAstr. It only submits a bounded encoded copy for translation.

Display lifecycle:

- The translation is shown only while the matching painting remains targeted.
- Looking away, opening a screen, hiding the HUD, changing world, or invalidating the painting clears it immediately.
- The target HUD displays translated text only by default.
- Paintings with collision disabled are still detected through the compatibility targeting path.

Disable MineAstr game translations to prevent painting images from being submitted.

## Data ownership

| Data | Owner | Deployment guidance |
| --- | --- | --- |
| Mod JAR | client and server | distribute the paired version |
| Painting index | server world SavedData | back up with the world; never overwrite by ordinary OTA |
| Uploaded image cache | server runtime | preserve across updates |
| Client image cache | client runtime | disposable and rebuilt on demand |
| Translation cache | client runtime | private cache; do not distribute |

## Troubleshooting

| Symptom | Check first |
| --- | --- |
| Editor does not open | client/server version parity and upload permission |
| Uploaded painting is invisible | duplicate JARs, image cache presence, and painting visibility metadata |
| Painting appears but translation does not | MineAstr 0.6.29 on the client, game translations, and AstrBot image support |
| Translation remains after looking away | stale Immersive Paintings or MineAstr client JAR |
| Request repeatedly waits for an image | whether the full-resolution image reached the client cache |
| Existing painting has the wrong orientation | verify that this fork's migration JAR is installed on both sides |

## Build

```powershell
.\gradlew.bat :neoforge:clean :neoforge:test :neoforge:build --no-daemon
```

The artifact is written to `neoforge/build/libs/`.

## Upstream and license

- Upstream source: [Luke100000/ImmersivePaintings](https://github.com/Luke100000/ImmersivePaintings)
- Upstream Modrinth page: [Immersive Paintings](https://modrinth.com/mod/immersive-paintings)
- Upstream CurseForge page: [Immersive Paintings](https://www.curseforge.com/minecraft/mc-mods/immersive-paintings)

This fork retains upstream history and is distributed under [GPL-3.0](LICENSE). Fork-specific changes are recorded in the Git history and [changelog](changelog.md). Report issues caused by this port to this repository rather than to upstream.
