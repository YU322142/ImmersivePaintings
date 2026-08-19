# Immersive Paintings 0.7.15 NeoForge 1.21.1 社区移植版

[English](README.md) · [变更日志](changelog.md) · [第三方声明](THIRD_PARTY_NOTICES.md)

这是基于 [Luke100000/ImmersivePaintings](https://github.com/Luke100000/ImmersivePaintings) 维护的 NeoForge 社区移植版，不是原作者的官方发布。

| 项目 | 要求 |
| --- | --- |
| Immersive Paintings | `0.7.15+1.21.1` |
| Minecraft | `1.21.1` |
| Java | `21` |
| NeoForge | `21.1.154` 或更高 |
| Fzzy Config | `0.7.0+1.21` |
| 可选图片翻译 | MineAstr `0.6.29` |

## 功能边界

- 上传并展示玩家画作。
- 从本地文件、截图、URL、拖放输入和服务器画作库选择图片。
- 通过模组自己的服务器缓存和 SavedData 保存画作。
- 保留 NeoForge 迁移中的画框朝向与旋转兼容修复。
- 可选地把当前准星指向的画作交给 MineAstr 翻译。

源码仓库不包含玩家图片、服务器画作数据库或客户端翻译缓存。

## 安装

客户端与服务端安装同一个文件：

```text
immersive_paintings-neoforge-1.21.1-0.7.15.jar
```

不要与旧版本并存。服务端负责画作索引与上传图片，客户端负责编辑界面、图片选择、渲染和可选翻译请求。

## MineAstr 图片翻译

图片翻译要求客户端与服务端都安装 MineAstr 0.6.29。

```text
准星目标画作
  → 沉浸画框完整图片缓存
  → 有大小上限的 JPEG 编码
  → MineAstr 公共图片翻译 API
  → AstrBot 桥接
  → MineAstr 目标 HUD
```

沉浸画框不会直接连接 AstrBot，也不会把画作所有权交给 MineAstr；只会提交用于翻译的受限编码副本。

显示规则：

- 只有准星仍指向同一画作时才显示译文。
- 移开准星、打开界面、隐藏 HUD、切换世界或画作失效时立即清理。
- 目标 HUD 默认只显示译文，不重复原文。
- 即使画作关闭碰撞，也可通过兼容目标检测识别。

关闭 MineAstr 的游戏翻译即可阻止画作图片被提交翻译。

## 数据归属

| 数据 | 归属 | 更新原则 |
| --- | --- | --- |
| 模组 JAR | 客户端与服务端 | 两端配对更新 |
| 画作索引 | 服务端世界 SavedData | 随世界备份，普通 OTA 不覆盖 |
| 上传图片缓存 | 服务端运行目录 | 更新时必须保留 |
| 客户端图片缓存 | 客户端运行目录 | 可按需重建 |
| 翻译缓存 | 客户端运行目录 | 私有缓存，不参与分发 |

## 快速排错

| 现象 | 优先检查 |
| --- | --- |
| 编辑界面打不开 | 客户端/服务端版本是否一致、是否有上传权限 |
| 上传后画作不可见 | 是否混装旧 JAR、服务端缓存是否存在、可见性元数据 |
| 画作正常但不翻译 | 客户端 MineAstr 0.6.29、游戏翻译开关、AstrBot 图片能力 |
| 移开准星仍显示 | 客户端是否残留旧 MineAstr 或旧画框 JAR |
| 一直等待图片 | 完整分辨率图片是否已经到达客户端缓存 |
| 旧画作朝向错误 | 两端是否都使用本社区迁移版 |

## 构建

```powershell
.\gradlew.bat :neoforge:clean :neoforge:test :neoforge:build --no-daemon
```

产物位于 `neoforge/build/libs/`。

## 上游与许可证

- 上游源码：[Luke100000/ImmersivePaintings](https://github.com/Luke100000/ImmersivePaintings)
- 上游 Modrinth：[Immersive Paintings](https://modrinth.com/mod/immersive-paintings)
- 上游 CurseForge：[Immersive Paintings](https://www.curseforge.com/minecraft/mc-mods/immersive-paintings)

本分支保留上游 Git 历史，并依照 [GPL-3.0](LICENSE) 分发。移植改动见 Git 历史与 [变更日志](changelog.md)。由本移植版引起的问题请提交到本仓库，不要打扰上游作者。
