# 更新日志

Amagari Translation Tool 的重要更改都会记录在这里。

本项目采用轻量级 Keep a Changelog 风格。尚未发布的改动写入 `[未发布]`，发布版本后再归档到对应版本。

## [未发布]

### 新增

- 客户端远程语言缓存会自动清理：7 天未使用的缓存文件会被删除，同一服务器同一语言最多保留最近 2 个 hash。

### 变更

- 新增面向 Minecraft 1.21.11 Fabric 的适配分支：构建目标改为 Java 21，依赖更新为 Fabric API 0.141.4+1.21.11、Fabric Loader 0.19.2，并同步 1.21.11 的 Fabric 网络 payload、命令和客户端 mixin API。
- 远程语言数据改为分片发送：单个语言保留 4 MiB 压缩数据总上限，但每个网络 payload 最多发送 512 KiB，以适配 Minecraft 1.21.11 的普通自定义 payload 注册方式。

### 修复

- 修复 Minecraft 1.21.11 分支客户端 mixin 配置仍要求 Java 25，导致 Java 21 启动时 Fabric Loader 报错的问题。

## [0.0.1] - 2026-05-17

### 新增

- 新建面向 Minecraft 26.1.2 的 Fabric 模组骨架，模组名为 `Amagari Translation Tool`，mod id 为 `amagari_translation_tool`。
- 新增 Java 25 构建脚本、Gradle/Fabric Loom 配置和基础主入口类。
- 新增项目工作流文档、测试清单、发布说明和 GitHub Actions 构建流程。
- 新增单人地图目录语言文件加载：`amagari_translation_tool/lang/<语言>.json` 会在进入地图后合并到客户端语言表，无需启用资源包。
- 新增远程服务器和开放到局域网单人世界的地图语言文件同步：装有本模组的服务端或主机会在玩家加入时发布 `en_us` 和玩家当前语言的 manifest，客户端按 hash 下载缺失或变更的压缩语言数据，无需资源包。
- 新增远程语言缓存：客户端会将压缩语言数据缓存到 `.minecraft/amagari_translation_tool/lang_cache`，重复加入同一服务器时优先复用缓存以减少下载流量。
- 新增 `/amagari_lang reload`、`/amagari_lang status`、`/amagari_lang pull` 和 `/amagari_lang push` 命令，用于重新加载地图语言文件、查看加载结果、主动拉取最新 manifest，以及让远程服务器向在线玩家发布最新 manifest。
- 地图语言目录 `amagari_translation_tool/lang` 会在模组首次检查地图语言文件时自动创建，玩家和服主无需手动创建完整目录路径。
- `/amagari_lang` 命令反馈会按执行者客户端语言显示中文或英文，并且只发送给执行者本人。
- 新增 `/amagari_lang help` 子命令，用于查看各个地图语言命令的简短说明。
- 新增 32x32 模组封面图标，并按 Fabric API 的资源格式使用 `assets/amagari_translation_tool/icon.png` 作为元数据图标路径。

### 修复

- 修复单人世界中执行 `/amagari_lang reload` 或 `/amagari_lang status` 后仍被集成服务器解析并显示红色命令参数错误的问题。
- 修复 `/amagari_lang push` 成功反馈会广播给其他管理员的问题。
