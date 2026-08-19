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

## Changes in 0.7.15+1.21.1

- Updates the optional integration baseline to MineAstr NeoForge 0.6.29.
- Requests the full painting image through the existing client cache and logs a bounded waiting state while it arrives.
- Keeps translation display tied to the currently targeted painting and removes it immediately when the target, screen, world, or HUD state changes.
- Uses MineAstr's translated-only Create-style HUD instead of rendering duplicated original text in world space.

## Changes in 0.7.14+1.21.1

- Ports the complete MineAstr painting-image translation integration to
  NeoForge 1.21.1 and MineAstr NeoForge 0.6.28 or newer.
- Detects the painting under the crosshair, including non-colliding paintings
  whose backing block owns Minecraft's normal hit result.
- Compresses the full painting to a bounded JPEG and submits it through
  MineAstr's public image-translation API.
- Caches translations persistently by the server painting hash and language,
  sharing one result between motive, full-image, and thumbnail aliases.
- Displays translated text beside the targeted painting through MineAstr's
  public world-space display API.

## Changes in 0.7.13+1.21.1

- Fixes NeoForge gameplay behavior drifting from the Fabric implementation:
  crouching right-click is reserved for compatibility interactions, while a
  normal right-click opens the painting editor.
- Rebuilds the NeoForge artifact from the maintained source branch instead of
  the stale `0.7.8-migration.2` package.
- Verifies that newly uploaded paintings are visible by default.
- Verifies that the 189 bundled sample-art resources are absent from the final
  NeoForge JAR while player uploads and external data packs remain supported.

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

This branch and its release artifact target NeoForge. Optional painting-image
translation requires MineAstr NeoForge 0.6.29 or newer on both client and
server, together with its configured AstrBot bridge. MineAstr is not bundled.
Disable MineAstr's game translations to prevent painting images from being
submitted for translation.

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
正确页数。0.7.15 使用 MineAstr NeoForge 0.6.29：准星指向画作时通过既有
图片缓存请求翻译，只在当前目标仍有效时显示译文；移开准星、打开界面、隐藏 HUD
或切换世界会立即清理显示。画面默认只显示译文，不再重复原文；客户端和服务器应
使用配套版本，客户端是实际图片编码、请求和显示的一侧。

### 中文安装说明

- 客户端与服务端均替换为 `immersive_paintings-neoforge-1.21.1-0.7.15.jar`，不要与旧版并存。
- 客户端与服务端安装 MineAstr 0.6.29；只升级服务器不能补齐客户端显示 API。
- 玩家上传的图片、服务器画作索引和客户端翻译缓存都属于运行数据，不应提交到源码仓库或随普通模组更新包分发。
- 图片仍由沉浸画框原有缓存/网络链路取得，兼容层只把压缩后的图像交给 MineAstr，不直接连接 AstrBot。
