# 更新日志

Amagari Translation Tool 的重要更改都会记录在这里。

本项目采用轻量级 Keep a Changelog 风格。尚未发布的改动写入 `[未发布]`，发布版本后再归档到对应版本。

## [未发布]

### 新增

- 新增单人地图目录语言文件加载：`amagari_translation_tool/lang/<语言>.json` 会在进入地图后合并到客户端语言表，无需启用资源包。
- 新增远程服务器地图语言文件同步：装有本模组的服务端会在玩家加入时发布 `en_us` 和玩家当前语言的 manifest，客户端按 hash 下载缺失或变更的压缩语言数据，无需资源包。
- 新增远程语言缓存：客户端会将压缩语言数据缓存到 `.minecraft/amagari_translation_tool/lang_cache`，重复加入同一服务器时优先复用缓存以减少下载流量。
- 新增 `/amagari_lang reload`、`/amagari_lang status`、`/amagari_lang pull` 和 `/amagari_lang push` 命令，用于重新加载地图语言文件、查看加载结果、主动拉取最新 manifest，以及让远程服务器向在线玩家发布最新 manifest。
- 新建面向 Minecraft 26.1.2 的 Fabric 模组骨架，模组名为 `Amagari Translation Tool`，mod id 为 `amagari_translation_tool`。
- 新增 Java 25 构建脚本、Gradle/Fabric Loom 配置和基础主入口类。
- 新增项目工作流文档、测试清单、发布说明和 GitHub Actions 构建流程。
