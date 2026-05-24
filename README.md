# Amagari Translation Tool

Amagari Translation Tool 是一个面向 Minecraft 1.21.1 Fabric 的翻译辅助模组。它可以从地图目录、远程服务器/LAN 主机或 ParaTranz 项目直接加载语言 JSON 到客户端翻译表，不需要额外打包或启用资源包；同时提供双语切换和源文显示，方便地图与模组翻译校对。

英文说明见 [README_EN.md](README_EN.md)。

## 功能概览

- **地图语言文件**：自动创建并读取 `amagari_translation_tool/lang`，语言文件格式与资源包 `assets/<namespace>/lang/<language>.json` 一致。
- **远程与局域网同步**：服务器或开放到局域网的主机会向安装本模组的加入者按需发送语言 manifest，客户端只下载缺失或 hash 变化的数据。
- **ParaTranz 拉取**：可在游戏内配置 Token、列出项目、按项目名拉取导出、立即应用语言 JSON，也可选择覆盖当前本地地图语言文件。
- **双语校对**：默认 `V` 切换目标语言/源语言；默认 `H` 显示源文辅助，包括物品侧边 tooltip、告示牌源文 HUD 和书本文本 `ⓘ` hover。
- **私有反馈与缓存清理**：命令反馈只发给执行者；远程语言缓存会自动清理 7 天未使用的数据，并且每个服务器/语言只保留最近 2 个 hash。

## 安装要求

- Minecraft 1.21.1
- Fabric Loader 0.16.14 或更高版本
- Fabric API
- Java 21

## 快速开始

### 使用地图语言文件

1. 安装本模组和 Fabric API。
2. 进入一次目标地图，模组会自动创建 `amagari_translation_tool/lang`。
3. 将语言 JSON 放入该目录，例如 `zh_cn.json` 或 `en_us.json`。
4. 在游戏内执行 `/amagari_lang reload` 重新加载，或执行 `/amagari_lang status` 查看加载状态。

常见路径：

```text
saves/<地图名>/amagari_translation_tool/lang/zh_cn.json
saves/<地图名>/amagari_translation_tool/lang/en_us.json
<服务端地图目录>/amagari_translation_tool/lang/zh_cn.json
<服务端地图目录>/amagari_translation_tool/lang/en_us.json
```

语言文件示例：

```json
{
	"item.minecraft.diamond": "地图自定义钻石名",
	"screen.example.title": "地图内标题"
}
```

### 使用 ParaTranz 拉取

1. 执行 `/amagari_lang paratranz config` 打开设置页面。
2. 填入 ParaTranz Token，并确认源语言、目标语言、是否拉取前触发导出、缓存数量和是否覆盖当前地图语言文件。
3. 执行 `/amagari_lang paratranz projects` 列出 Token 可访问的项目；聊天里的项目名可以点击直接拉取。
4. 执行 `/amagari_lang paratranz pull <项目名>` 按项目名导出、下载并应用语言 JSON。

示例：

```text
/amagari_lang paratranz pull Permafrost-i18n
```

## 加载规则

- 单人地图会合并当前客户端语言和 `en_us` 对应的地图语言文件。
- 远程服务器或 LAN 加入者会按需接收 `en_us` 和当前客户端语言；切换客户端语言后，可重新加入服务器或执行 `/amagari_lang pull`。
- 同一语言可以拆分为多个文件，例如 `zh_cn.items.json`、`zh_cn.ui.json`；匹配文件按文件名排序后加载。
- 翻译覆盖顺序从高到低为：本次会话应用的 ParaTranz 翻译、地图/远程同步语言文件、已启用资源包、原版语言表。
- ParaTranz 拉取结果如果包含 `*.world.block.*` 条目，会在客户端渲染时翻译匹配的固定告示牌字面量文本，适合 `Permafrost-i18n` 这类大厅告示牌，不会修改存档中的方块文本。
- 单个远程语言数据 payload 上限为 4 MiB；大型地图建议只保留实际需要的语言和命名空间。

## 命令

| 命令 | 作用 |
| --- | --- |
| `/amagari_lang help` | 显示所有子命令的简短说明。 |
| `/amagari_lang reload` | 重新加载当前单人地图语言文件。 |
| `/amagari_lang status` | 显示地图/远程语言状态和最近一次 ParaTranz 状态。 |
| `/amagari_lang pull` | 请求服务器或 LAN 主机重新发布执行者当前语言的 manifest。 |
| `/amagari_lang push` | 让服务器重新发布 manifest，通常由 OP 在修改服务端语言文件后执行。 |
| `/amagari_lang paratranz` | 显示 ParaTranz 子命令帮助。 |
| `/amagari_lang paratranz config` | 打开本机 ParaTranz 设置页面。 |
| `/amagari_lang paratranz projects` | 列出当前 API Token 可访问的 ParaTranz 项目。 |
| `/amagari_lang paratranz pull <项目名>` | 按项目名导出、下载并应用 ParaTranz 项目语言 JSON。 |

命令反馈只会发送给执行者本人。客户端语言为中文时显示中文反馈，其他语言默认显示英文反馈。

## ParaTranz 细节

- 配置文件位于 `.minecraft/config/amagari_lang/config.json`。
- 字段包括 `paratranzApiToken`、`sourceLanguage`、`targetLanguage`、`triggerExport`、`maxCachedArtifacts` 和 `overwriteWorldLanguageFiles`。
- 旧版 `.minecraft/config/amagari_translation_tool.json` 会自动迁移到新路径。
- 已保存 Token 不会在设置页面回显；Token 输入框留空保存会保留旧 Token，勾选“清除 token”才会移除。
- 下载缓存位于 `.minecraft/amagari_translation_tool/paratranz_cache/<projectId>/`。断开世界只会清理本次会话内的激活状态，不会删除全局缓存。
- 勾选“覆盖当前地图语言文件”后，目标语言会写入当前本地地图的 `amagari_translation_tool/lang/<目标语言>.json`，并删除同语言旧分片文件，例如 `zh_cn.items.json`。如果当前没有可写入的本地地图目录，则跳过覆盖，只应用到本次客户端会话。
- `/amagari_lang paratranz pull <项目名>` 支持项目名补全；补全来源是当前 Token 可访问的项目。

## 双语校对

- 默认按 `V` 可在 ParaTranz 目标语言和配置的源语言之间切换。切到源语言时会临时暂停 ParaTranz 覆盖，让客户端回到源语言资源和地图源语言文件。
- 默认按 `H` 可开启源文显示。
- 物品会在原 tooltip 旁显示独立的源语言 tooltip，并尽量按原 tooltip 的可翻译行显示源文。
- 看向带有 ParaTranz `*.world.block.*` 翻译的告示牌时，准星附近会显示源文本 HUD；普通方块名不会作为 HUD 显示。
- 书本中的可翻译文本会在后面附加 `ⓘ`，鼠标悬停 `ⓘ` 可查看源文。
- 源文显示优先读取当前已启用的 Minecraft、资源包和模组自身的源语言资源，例如 `assets/*/lang/en_us.json`；找不到 key 时，才回退到本次会话已拉取的 ParaTranz 源语言条目。
- 物品 tooltip 和书本文本必须保留可翻译 key 才能反查源文。如果内容已经被模组或地图写成纯字面量文本，客户端无法自动恢复原始源文。

## 远程服务器和 LAN

- 服务端和客户端都安装本模组时，服务器会先发送语言 manifest，客户端只在本地缓存缺失或 hash 变化时下载 gzip 压缩语言数据。
- 开放到局域网时，主机本人继续读取本地地图目录，加入者通过 manifest/cache 下载语言数据。
- 客户端远程语言缓存位于 `.minecraft/amagari_translation_tool/lang_cache`，按服务器地址 hash 分目录保存。
- 缓存命中或写入新缓存会刷新使用时间；7 天未使用的缓存会自动删除，同一服务器同一语言最多保留最近 2 个 hash。

## 构建

```powershell
.\gradlew-java21.bat build --stacktrace
```

生成的 jar 位于 `build/libs`。

## 开发文档

- `AGENTS.md`：仓库级开发规则。
- `CHANGELOG.md`：用户可见变更记录。
- `docs/DEVELOPMENT.md`：开发工作流。
- `docs/TESTING.md`：验证和手动测试清单。
- `docs/RELEASE.md`：发布流程。
- `docs/SESSION_HANDOFF.md`：跨会话交接信息。
