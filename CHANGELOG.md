# 更新日志

- Keep map-provided book hover/click actions on the original text while isolating ATT source proofreading to the appended `ⓘ` marker.
- Fix the keybind category label on Minecraft versions that use registered key mapping categories.
- Render book source-text hover boxes with a light book-page-style background for better proofreading readability.
- Fix source-display HUD for local world sign translations so source text can come from the world's source-language file instead of falling back to a humanized translation key.
- Fix `/amagari_lang paratranz` project-name completion and suppress the integrated-server parse error for the client-only ParaTranz command.
- Change ParaTranz commands to `/amagari_lang paratranz projects`, `/amagari_lang paratranz config`, and `/amagari_lang paratranz pull <project>`, with bare `/amagari_lang paratranz` showing subcommand help.
- Make ParaTranz project-list entries clickable so players can pull a listed project directly from chat.
- Mirror the ParaTranz subcommand tree on the server side and forward it to the client, so integrated-server parsing no longer treats `config` as a project name.
- Fix ParaTranz artifact parsing when `/projects/{id}/artifacts` returns a single artifact object instead of an array.
- Apply ParaTranz `*.world.block.*` entries to matching literal sign lines during client rendering, so fixed map signs can display pulled translations without editing the save.
- Add bilingual client controls: `V` temporarily switches to the configured source language without ParaTranz override, and `H` toggles source-language helper text for item tooltips, crosshair block/sign HUD text, and translatable book text.

Amagari Translation Tool 的重要更改都会记录在这里。

本项目采用轻量级 Keep a Changelog 风格。尚未发布的改动写入 `[未发布]`，发布版本后再归档到对应版本。

## [未发布]

### 修复

- 修复地图书本文字自带 hover/click 交互时与 ATT 源文校对框重叠或误触的问题；地图提示与点击继续由正文触发，ATT 源文框只由追加的 `ⓘ` 标记触发。
- 修复 1.21.10 客户端加载时 `GuiGraphicsMixin` 仍注入旧版 `renderTooltip(ItemStack)` 入口导致 Mixin 应用失败的问题。
- 修复双语源语言模式下 ParaTranz 纯文本告示牌渲染没有立即切回源文的问题；按 `V` 切到源语言后，模组会复用拉取结果建立反向索引，把已渲染成目标语言的固定大厅告示牌还原为源文本。
- 调整双语源文显示的来源优先级：`H` 现在优先读取当前 Minecraft、资源包和模组自身的真实源语言资源，只有找不到 key 时才回退到 ParaTranz 拉取结果。
- 修复 `H` 的告示牌源文 HUD 在本地世界只保存目标语言 `zh_cn.json` 时没有读取资源包 `en_us.json` 的问题，避免把 `*.world.block.*` key 人工拆成英文词组。
- 调整 `H` 的准星 HUD：普通方块名不再显示为源文提示，只有带有 ParaTranz `*.world.block.*` 字面量翻译的告示牌会显示对应源文本。
- 修复 ParaTranz 配置刷新后固定告示牌字面量索引可能未同步重建的问题，降低 `V` 切换源语言后告示牌仍保留目标语言文本的概率。
- 告示牌双语切换现在也会读取当前地图或远程同步语言文件中的 `*.world.block.*` 条目；即使 ParaTranz 拉取结果已经覆盖到 `amagari_translation_tool/lang` 并重启游戏，也能继续用于固定告示牌的源文/译文切换。
- 修复 ParaTranz 拉取并覆盖当前地图语言文件后，固定告示牌仍可能需要重进世界才刷新文本的问题；已知目标文本也会触发新的告示牌渲染文本对象，避免复用旧 `SignText` 渲染缓存。
- 修复 `H` 双语源文显示的物品提示形态：容器/背包物品现在会在原物品 tooltip 旁显示独立源语言 tooltip，并按原 tooltip 的可翻译行显示源文，不再只显示物品名或追加到原 tooltip 下方。
- 修复书本源文 hover 的自查问题：`H` 开关和源语言表刷新会使已打开书页重新拆行，书本文本源文解析也复用物品 tooltip 的参数化源文解析。
- 优化物品和书本的双语源文显示：源文本会尽量沿用原可翻译组件的颜色、斜体和参数组件样式，不再统一强制为灰色斜体。

## [0.0.3] - 2026-05-24

### 新增

- 新增客户端 ParaTranz 拉取：`/amagari_lang paratranz projects` 可列出 API Token 关联项目，`/amagari_lang paratranz pull <项目名>` 可导出、下载并应用项目语言 JSON。
- 新增 `/amagari_lang paratranz config` 游戏内设置页面，可在客户端本地保存、保留或清除 ParaTranz API Token，并配置源语言、目标语言、拉取前触发导出、artifact 缓存保留数量，以及是否在拉取成功后覆盖当前本地地图语言文件。
- 新增双语辅助：默认按 `V` 可在目标语言和配置的源语言之间切换；默认按 `H` 可显示源文辅助，物品 tooltip 会追加源语言名称，看向方块/告示牌会显示源文 HUD，书本中的可翻译文本会附加源文 hover 标记。
- 新增 `.minecraft/config/amagari_lang/config.json` 配置文件，包含 `paratranzApiToken`、`sourceLanguage`、`targetLanguage`、`triggerExport`、`maxCachedArtifacts` 和 `overwriteWorldLanguageFiles` 字段，并会迁移旧版 `.minecraft/config/amagari_translation_tool.json`；ParaTranz 下载缓存写入 `.minecraft/amagari_translation_tool/paratranz_cache/<projectId>/`。
- 勾选“覆盖当前地图语言文件”后，ParaTranz 会把目标语言写入当前本地地图的 `amagari_translation_tool/lang/<目标语言>.json`，并删除同语言旧分片文件。
- `/amagari_lang status` 现在会同时显示地图/远程语言状态和 ParaTranz 最近一次列表、下载、应用或失败状态。
- 新增轻量级 `runUnitChecks` 验证任务，覆盖 ParaTranz 项目匹配、API JSON 解析、zip 语言识别、坏 JSON 跳过和状态文案。
- 客户端远程语言缓存会自动清理：7 天未使用的缓存文件会被删除，同一服务器同一语言最多保留最近 2 个 hash。

### 修复

- 覆盖当前地图语言文件时会在异常路径清理临时 `.tmp` 文件，避免失败写入留下冗余文件。
- 优化 ParaTranz 设置页布局：清除 token 与第二行设置对齐，拉取前触发导出和覆盖当前地图语言文件位于同一行，保存/取消按钮居中显示。
- `/amagari_lang paratranz` 现在只显示 ParaTranz 子命令帮助，不再和 `/amagari_lang paratranz projects` 重复拉取项目列表。

## [0.0.1] - 2026-05-17

### 新增

- 新建面向 Minecraft 1.21.10 的 Fabric 模组骨架，模组名为 `Amagari Translation Tool`，mod id 为 `amagari_translation_tool`。
- 新增 Java 21 构建脚本、Gradle/Fabric Loom 配置和基础主入口类。
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
