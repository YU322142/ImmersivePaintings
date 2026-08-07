# Immersive Paintings - Community Fork

[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Upstream](https://img.shields.io/badge/upstream-Luke100000%2FImmersivePaintings-lightgrey.svg)](https://github.com/Luke100000/ImmersivePaintings)

This is a community-maintained NeoForge port based on the upstream Minecraft
1.21.1 branch of
[Luke100000/ImmersivePaintings](https://github.com/Luke100000/ImmersivePaintings).
It carries the common and NeoForge changes developed in this fork for the
Minecraft 1.21.11 release line.

> This repository is not an official upstream release and is not endorsed or
> supported by the original author. Upstream project names and links are kept
> for attribution and compatibility reference.

## Changes in 0.7.12+1.21.1

- Opens the new-painting workflow directly when the player has upload
  permission.
- Adds a native local image picker, using Tiny File Dialogs with an AWT
  fallback, while retaining URL and drag-and-drop loading.
- Loads and sorts screenshots before controls are built, so the initial page
  indicator is correct.
- Uses five screenshots per page consistently, preserves the final partial
  page, and prevents thumbnail texture identifiers from colliding.
- Correctly clamps and refreshes screenshot and painting-library pagination;
  painting filters no longer leak between screens or libraries.
- Removes the bundled example paintings from the NeoForge JAR without
  affecting player uploads or external data packs.
- Makes newly uploaded paintings visible by default and adds a three-step
  confirmation before an operator deletes every painting by an author.
- Retains the public full-resolution client cache accessor introduced for
  compatibility integrations.
- Includes third-party notices and the complete TwelveMonkeys BSD-3-Clause
  license in source and binary distributions.

See [changelog.md](changelog.md) for upstream history and fork changes.

## Compatibility

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| NeoForge | 21.1.154 |
| Fzzy Config | 0.7.0+1.21 |

This branch and its release artifact target NeoForge. MineAstr 0.6.24 image
translation is a Fabric-only integration in the 1.21.11 source line; it is not
loaded or called by this NeoForge build, and no MineAstr code or JAR is bundled.

## Building

Clone this fork, check out `1.21.1-neoforge`, and run:

    Windows:  .\gradlew.bat :neoforge:build
    Linux:    ./gradlew :neoforge:build

The built mod is written to `neoforge/build/libs/`. No user images or
third-party screenshot collections are included.

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
Modified source files and the commit history identify fork-specific changes.
Redistributions must continue to comply with GPL-3.0.

Bundled third-party components, versions, source locations, and license terms
are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Complete
third-party license texts are retained in the [licenses](licenses) directory.

## 中文说明

这是基于上游 Minecraft 1.21.1 分支制作的 NeoForge 社区移植版，并非原作者
的官方发布。`0.7.12+1.21.1` 将本分支在 1.21.11 上完成的通用上传界面、
截图分页、筛选状态、删除确认、默认可见性和许可证改动迁移到 1.21.1；
每页固定显示 5 张截图，最后不足 5 张仍会正常显示，首次打开也会立即得到
正确页数。MineAstr 图像翻译桥接只存在于 1.21.11 的 Fabric 构建，本
NeoForge JAR 不加载或捆绑 MineAstr。
