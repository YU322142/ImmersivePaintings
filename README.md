# Immersive Paintings - Upload UI Fix Fork

[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Upstream](https://img.shields.io/badge/upstream-Luke100000%2FImmersivePaintings-lightgrey.svg)](https://github.com/Luke100000/ImmersivePaintings)

This is a community-maintained fork of
[Luke100000/ImmersivePaintings](https://github.com/Luke100000/ImmersivePaintings)
for Minecraft 1.21.11. It focuses on the local image upload and screenshot
browser workflow.

> This repository is not an official upstream release and is not endorsed or
> supported by the original author. Upstream project names and links are kept
> for attribution and compatibility reference.

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

Both Fabric and NeoForge source sets are included.

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

## 中文说明

这是基于
[Luke100000/ImmersivePaintings](https://github.com/Luke100000/ImmersivePaintings)
制作的社区维护分支，适配 Minecraft 1.21.11，并非原作者的官方版本。

0.7.9+1.21.11 主要修复上传图片界面：首次打开即可正确显示截图页数；
每页统一显示 5 张截图，最后不足 5 张时仍会显示；修复截图预览复用错误；
并加入本地图片选择器及中英文界面文本。源码继续使用 GPL-3.0 许可证，
未包含用户图片或第三方截图素材。
