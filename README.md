# Amagari Translation Tool

Amagari Translation Tool 是一个面向 Minecraft 26.1 Fabric 的翻译辅助模组。

英文说明见 [README_EN.md](README_EN.md)。

## 安装要求

- Minecraft 26.1
- Fabric Loader 0.19.2 或更高版本
- Fabric API
- Java 25

## 当前状态

项目当前支持在地图目录中放置语言文件，并在进入地图、加入远程服务器或加入开放到局域网的单人世界时自动加载到客户端语言表，无需额外启用资源包。客户端还可以直接从 ParaTranz 拉取项目导出，将下载到的语言 JSON 作为全局覆盖层应用。

## 地图语言文件

模组会自动创建地图存档目录下的 `amagari_translation_tool/lang`；创建后将语言文件放入该目录：

```text
saves/<地图名>/amagari_translation_tool/lang/zh_cn.json
saves/<地图名>/amagari_translation_tool/lang/en_us.json
<服务端地图目录>/amagari_translation_tool/lang/zh_cn.json
<服务端地图目录>/amagari_translation_tool/lang/en_us.json
```

语言文件格式与资源包的 `assets/<namespace>/lang/<语言>.json` 一致：

```json
{
	"item.minecraft.diamond": "地图自定义钻石名",
	"screen.example.title": "地图内标题"
}
```

加载规则：

- 进入单人地图后，本模组会合并当前语言和 `en_us` 对应的地图语言文件。
- 如果地图目录中还没有 `amagari_translation_tool/lang`，模组会在首次检查地图语言文件时自动创建。
- 加入远程服务器或开放到局域网的单人世界后，如果服务端和客户端都安装了本模组，服务端会先发送 `en_us` 和玩家当前客户端语言的 manifest，客户端只在本地缓存缺失或 hash 变化时下载压缩后的语言数据。
- 开放到局域网时，主机本人继续直接读取本地地图目录，其他加入者通过 manifest/cache 下载语言数据。
- 地图语言文件优先级高于原版和已启用资源包中的同名翻译键；ParaTranz 拉取的翻译作为最后一层覆盖，优先级高于本地地图文件和远程同步文件。
- 同一语言可拆分为多个文件，例如 `zh_cn.items.json`、`zh_cn.ui.json`，文件按文件名排序后加载。
- 修改单人地图文件后可执行 `/amagari_lang reload` 重新加载，执行 `/amagari_lang status` 查看最近一次加载结果；这两个命令会在客户端显示结果，不应再触发服务端命令解析报错。
- 修改远程服务器文件后，OP 可执行 `/amagari_lang push` 让服务器重新发布 manifest；在线客户端会按 hash 自动请求变化的语言数据。
- 玩家也可执行 `/amagari_lang pull` 主动请求服务器重新发布自己的语言 manifest。
- 执行 `/amagari_lang help` 可查看所有子命令的简短说明。
- 执行 `/amagari_lang paratranz` 可列出当前 API Token 关联的 ParaTranz 项目。
- 执行 `/amagari_lang paratranz <项目名>` 可按项目名匹配 ParaTranz 项目，导出、下载并立即应用语言 JSON；例如 `/amagari_lang paratranz Permafrost-i18n`。
- 执行 `/amagari_lang status` 会同时显示地图/远程语言状态和最近一次 ParaTranz 拉取状态，包括项目、成品、文件数、条目数、语言和错误信息。
- `/amagari_lang` 命令反馈只会发送给执行者本人；客户端语言为中文时显示中文反馈，其他语言默认显示英文反馈。
- 远程服务器和开放到局域网的主机为每名加入者按需提供 `en_us` 和其当前客户端语言；玩家切换语言后可重新加入服务器，或执行 `/amagari_lang pull`。
- 客户端缓存位于 `.minecraft/amagari_translation_tool/lang_cache`，按服务器地址 hash 分目录保存 gzip 压缩数据；命中缓存或写入新缓存时会刷新使用时间，7 天未使用的缓存会自动删除，同一服务器同一语言最多保留最近 2 个 hash。
- ParaTranz 配置文件位于 `.minecraft/config/amagari_lang/config.json`，字段为 `paratranzApiToken`；默认生成空白 Token 占位，请填入自己的 API Token；日志和错误信息不会打印 Token。
- ParaTranz 下载缓存位于 `.minecraft/amagari_translation_tool/paratranz_cache/<projectId>/`，断开世界只会清理本次会话内的激活状态，不会删除全局缓存。
- 单个远程语言数据 payload 上限为 4 MiB；大型地图建议按实际使用语言和命名空间控制语言文件规模。

## 构建

```powershell
.\gradlew-java25.bat build --stacktrace
```

生成的 jar 位于 `build/libs`。

## 开发文档

- `AGENTS.md`：仓库级开发规则。
- `CHANGELOG.md`：用户可见变更记录。
- `docs/DEVELOPMENT.md`：开发工作流。
- `docs/TESTING.md`：验证和手动测试清单。
- `docs/RELEASE.md`：发布流程。
- `docs/SESSION_HANDOFF.md`：跨会话交接信息。
