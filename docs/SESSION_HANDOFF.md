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
.\gradlew.bat build --stacktrace
```

- 完成功能后自查冗余、风险和遗留 helper。
- 只暂存属于本次任务的文件，不处理无关改动。

## 当前项目习惯

- Mod id 使用 `amagari_translation_tool`。
- 主入口类是 `com.amagari.translationtool.AmagariTranslationTool`。
- 当前适配分支目标为 Minecraft 1.21.10，Fabric API 依赖使用 `0.138.4+1.21.10`，构建目标为 Java 21。
- 该分支使用 `.\gradlew.bat build --stacktrace` 构建；旧的 26.1.2 版本保留在 `main` 分支。
- 翻译数据、配置、客户端 UI 和网络 payload 应保持边界清晰。

## 当前已知状态

- 当前功能包括地图语言文件自动目录创建、单人加载、远程服务器/LAN manifest 同步、客户端缓存清理、`/amagari_lang` 命令和中英文执行者私有反馈。
- 当前适配分支已同步 1.21.10 的 Fabric 网络 payload、命令和客户端 mixin API。
- 当前适配分支已通过：

```powershell
.\gradlew.bat build --stacktrace
```

## 新会话建议开场

用户可以在新会话发送：

```text
请继续 Amagari Translation Tool 项目开发。
项目路径：E:\Minecraft_MOD\mod_init\amagari-translation-tool-26.1.2
请先阅读 docs/SESSION_HANDOFF.md 和 AGENTS.md。
不要自动打开客户端。
```
