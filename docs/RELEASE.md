# Amagari Translation Tool 发布流程

这是当前项目的轻量发布流程。默认先手动发布，不自动上传 GitHub Release。

## 什么时候 Bump 版本

准备给玩家使用新的 jar 时再 bump 版本。普通开发改动先记录到 `CHANGELOG.md` 的 `[未发布]`。

版本号位置：

```properties
gradle.properties
mod_version=0.1.0
```

建议格式：

```text
0.2.0
```

Minecraft 兼容范围不要写进 `mod_version`，继续由 `minecraft_dependency` 管理。

公开下载的 jar 文件名会保留 Minecraft 和加载器信息，例如：

```text
amagari-translation-tool-mc26.2-fabric-0.2.0.jar
```

## 发布前检查

1. 确认 `CHANGELOG.md` 的 `[未发布]` 已包含本次用户可见变更。
2. 将 `[未发布]` 内容移动到具体版本段落：

```md
## [0.2.0] - 2026-05-16
```

3. 更新 `gradle.properties`：

```properties
mod_version=0.2.0
```

4. 本地验证：

```powershell
git diff --check
.\gradlew-java25.bat build --stacktrace
```

5. 检查产物：

```text
build/libs/amagari-translation-tool-mc26.2-fabric-<version>.jar
```

## GitHub 发布

提交并打 tag：

```powershell
git add gradle.properties CHANGELOG.md
git commit -m "Prepare Amagari Translation Tool release <version>"
git tag v<version>
git push
git push --tags
```

在 GitHub Releases 中创建新版本：

- Tag: `v<version>`
- Title: `Amagari Translation Tool <version>`
- Notes: 使用 `CHANGELOG.md` 对应版本内容
- Artifact: 上传 `build/libs/amagari-translation-tool-mc26.2-fabric-<version>.jar`

## CI

GitHub Actions 会在 push 和 pull request 时运行：

```bash
./gradlew build --stacktrace
```

CI 通过只证明项目可以构建，不代表多人或客户端行为已经手动验证。
