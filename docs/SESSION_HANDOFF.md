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
- 当前适配分支目标为 Minecraft 26.1，Fabric API 依赖使用 `0.145.1+26.1`。
- `gradlew-java25.bat` 会优先使用本项目 `.gradle/local-jdks` 下的 Java 25；如果不存在，会复用同级 `mapsociety-template-26.1` 中已下载的 Java 25。
- 翻译数据、配置、客户端 UI 和网络 payload 应保持边界清晰。

## 当前已知状态

- 项目是新建的可构建 Fabric 模组骨架，尚未初始化 Git 仓库。
- 当前尚未实现玩家可见的翻译功能。
- 当前已同步 MapSociety 风格的开发工作流文档、测试清单、发布流程和 GitHub Actions 构建流程。
- 该骨架已通过：

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
