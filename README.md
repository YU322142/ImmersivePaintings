# Immersive Paintings - Community Fork

[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Upstream](https://img.shields.io/badge/upstream-Luke100000%2FImmersivePaintings-lightgrey.svg)](https://github.com/Luke100000/ImmersivePaintings)

This is a community-maintained fork of
[Luke100000/ImmersivePaintings](https://github.com/Luke100000/ImmersivePaintings)
for Minecraft 1.21.11. It focuses on the local image upload and screenshot
browser workflow.

> This repository is not an official upstream release and is not endorsed or
> supported by the original author. Upstream project names and links are kept
> for attribution and compatibility reference.

## Changes in 0.7.12+1.21.11

- Targets paintings with the same crosshair-only lifecycle and distance
  settings used by MineAstr 0.6.24 sign overlays.
- Detects non-colliding painting entities even when Minecraft reports the
  backing block as the vanilla hit result.
- Requires MineAstr 0.6.24 on both client and server for image translation.

## Changes in 0.7.10+1.21.11

- Adds optional Fabric integration with MineAstr 0.6.24 and newer.
- Requests OCR translation only for the painting currently under the
  crosshair and only while MineAstr game translations are enabled.
- Compresses the painting to a bounded JPEG before MineAstr forwards it to
  the configured AstrBot multimodal provider.
- Caches results for the current level and reuses MineAstr's targeted
  world-space translation display.

## Changes in 0.7.9+1.21.11

- Opens the new-painting workflow directly when the player has upload
  permission.
- Adds a native image file picker, with Tiny File Dialogs as the primary
  implementation and an AWT fallback.
- Loads the screenshot library before building its controls, so the page count
  is correct when the screen first opens.
- Uses five screenshots per page consistently and keeps the final partial page
  visible.
- Correctly clamps and refreshes screenshot and painting-library pagination.
- Uses stable, unique texture identifiers for screenshot thumbnails to prevent
  entries from reusing the wrong preview.
- Improves English and Simplified Chinese interface text for local uploads.

See [changelog.md](changelog.md) for the upstream history and fork changes.

## Compatibility

| Component | Version |
| --- | --- |
| Minecraft | 1.21.11 |
| Java | 21 |
| Fabric Loader | 0.18.4 |
| Fabric API | 0.141.1+1.21.11 |
| NeoForge | 21.11.36-beta |
| MineAstr (optional, Fabric) | 0.6.24 or newer |

Both Fabric and NeoForge source sets are included.

### Optional MineAstr image translation

On Fabric, installing MineAstr 0.6.24 on both the client and server, together
with the matching AstrBot plugin, enables image translation for targeted
paintings when MineAstr's **Game translations** and floating translation
settings are on and its AstrBot bridge is connected. Immersive Paintings
sends a compressed JPEG through MineAstr's API; MineAstr and AstrBot determine
the configured translation provider and data handling. Disable **Game
translations** in MineAstr before entering a world to prevent painting images
from being submitted for translation.

MineAstr is not bundled into either mod JAR. The NeoForge build does not load
or call this integration.

## Building

Clone this fork, check out the 1.21.11 branch, and run:

    Windows:  .\gradlew.bat build
    Linux:    ./gradlew build

Built mod files are written to:

    fabric/build/libs/
    neoforge/build/libs/

This repository currently publishes source code and GitHub-generated source
archives. No user images or third-party screenshot collections are included.

## Upstream Resources

- Original source: <https://github.com/Luke100000/ImmersivePaintings>
- Official CurseForge page:
  <https://www.curseforge.com/minecraft/mc-mods/immersive-paintings>
- Official Modrinth page: <https://modrinth.com/mod/immersive-paintings>
- Configuration documentation:
  <https://github.com/Luke100000/ImmersivePaintings/wiki/Config>
- Modpack and datapack documentation:
  <https://github.com/Luke100000/ImmersivePaintings/wiki/Custom-Paintings>
- Upstream translations:
  <https://crowdin.com/project/immersive-collection>

Please report issues caused by this fork to this repository rather than to the
upstream project.

## License and Attribution

Immersive Paintings is originally authored by Luke100000. This fork retains the
upstream Git history and is distributed under the
[GNU General Public License v3.0](LICENSE), matching the upstream license.
Modified source files and the commit history identify the fork-specific changes
and their dates. Redistributions must continue to comply with GPL-3.0.

Bundled third-party components, their versions, source locations, and license
terms are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Complete
third-party license texts are retained in the [licenses](licenses) directory.

## 中文说明

这是基于
[Luke100000/ImmersivePaintings](https://github.com/Luke100000/ImmersivePaintings)
制作的社区维护分支，适配 Minecraft 1.21.11，并非原作者的官方版本。

0.7.9+1.21.11 主要修复上传图片界面：首次打开即可正确显示截图页数；
每页统一显示 5 张截图，最后不足 5 张时仍会显示；修复截图预览复用错误；
并加入本地图片选择器及中英文界面文本。源码继续使用 GPL-3.0 许可证，
未包含用户图片或第三方截图素材。
