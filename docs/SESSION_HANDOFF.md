# Amagari Translation Tool 会话交接说明

新 Codex 会话接手 Amagari Translation Tool 时，先读本文件，再读 `AGENTS.md`、`README.md`、`README_EN.md`、`CHANGELOG.md`、`docs/DEVELOPMENT.md` 和 `docs/TESTING.md`。

## 正确项目

当前项目目录：

```text
E:\Minecraft_MOD\mod_init\amagari-translation-tool-26.1.2
```

## 稳定工作流

- 不自动打开 Minecraft 客户端，除非用户明确要求。
- 功能或行为改动必须同步更新 `CHANGELOG.md`。
- 命令、用法、限制、配置或玩家可见行为改变时，同步更新 `README.md` 和 `README_EN.md`。
- 多人、翻译 UI、客户端状态相关功能要同步检查或更新 `docs/TESTING.md`。
- 默认验证命令：

```powershell
git diff --check
.\gradlew-java25.bat build --stacktrace
```

- 完成功能后自查冗余、风险和遗留 helper。
- 只暂存属于本次任务的文件，不处理无关改动。

## 当前项目习惯

- Mod id 使用 `amagari_translation_tool`。
- 主入口类是 `com.amagari.translationtool.AmagariTranslationTool`。
- 当前目标为 Minecraft 26.1，Fabric API 依赖使用 `0.145.1+26.1`。
- `gradlew-java25.bat` 会优先使用本项目 `.gradle/local-jdks` 下的 Java 25；如果不存在，会复用本机已有的 Java 25。
- 翻译数据、配置、客户端 UI 和网络 payload 应保持边界清晰。

## 当前已知状态

- 当前功能包括地图语言文件自动目录创建、单人加载、远程服务器/LAN manifest 同步、客户端缓存清理、客户端 ParaTranz 拉取、`/amagari_lang` 命令和中英文执行者私有反馈。
- ParaTranz 命令包括 `/amagari_lang paratranz config`、`/amagari_lang paratranz projects` 和 `/amagari_lang paratranz pull <项目名>`；直接执行 `/amagari_lang paratranz` 只显示子命令帮助，项目名会作为可点击聊天组件执行对应 pull 命令。服务端命令树通过 `ParaTranzCommandPayload` 转发这些客户端动作，避免整合服把 `config` 当作项目名解析。配置位于 `.minecraft/config/amagari_lang/config.json`，字段包括 `paratranzApiToken`、`sourceLanguage`、`targetLanguage`、`triggerExport`、`maxCachedArtifacts` 和 `overwriteWorldLanguageFiles`，旧版 `.minecraft/config/amagari_translation_tool.json` 会自动迁移；开启覆盖后只会把目标语言写入当前本地地图的 `amagari_translation_tool/lang/<目标语言>.json`，并删除同语言旧分片文件。
- 朋友提交的 ParaTranz 新功能以客户端侧为主：按 Token 列出账号项目、按项目名导出/下载 artifact、应用语言 JSON、缓存 artifact，并在客户端渲染固定牌子文本时应用 `*.world.block.*` 翻译。
- 当前分支已通过：

```powershell
.\gradlew-java25.bat build --stacktrace
```

## 新会话建议开场

用户可以在新会话发送：

```text
请继续 Amagari Translation Tool 项目开发。
项目路径：E:\Minecraft_MOD\mod_init\amagari-translation-tool-26.1.2
请先阅读 docs/SESSION_HANDOFF.md 和 AGENTS.md。
不要自动打开客户端。
```
