# Amagari Translation Tool

Amagari Translation Tool is a Fabric translation helper mod for Minecraft 1.21.4.

中文说明见 [README.md](README.md)。

## Requirements

- Minecraft 1.21.4
- Fabric Loader 0.16.14 or newer
- Fabric API
- Java 21

## Current Status

The project currently supports loading language files from a world directory into the client language table when entering singleplayer worlds, joining remote servers, or joining singleplayer worlds opened to LAN, without enabling a separate resource pack. The client can also pull a ParaTranz project export directly and apply its language JSON files as a global override layer.

## World Language Files

The mod automatically creates `amagari_translation_tool/lang` inside the world save directory; place language files in that directory after it appears:

```text
saves/<world name>/amagari_translation_tool/lang/zh_cn.json
saves/<world name>/amagari_translation_tool/lang/en_us.json
<server world directory>/amagari_translation_tool/lang/zh_cn.json
<server world directory>/amagari_translation_tool/lang/en_us.json
```

The file format matches resource-pack language files at `assets/<namespace>/lang/<language>.json`:

```json
{
	"item.minecraft.diamond": "World-specific diamond name",
	"screen.example.title": "World screen title"
}
```

Loading rules:

- After entering a singleplayer world, the mod merges world language files for the current language and `en_us`.
- If `amagari_translation_tool/lang` does not exist yet, the mod creates it the first time it checks world language files.
- After joining a remote server or a singleplayer world opened to LAN, if both the server/host and client have this mod installed, the server first sends a manifest for `en_us` and the player's current client language. The client downloads compressed language data only when its local cache is missing or the hash changed.
- When a singleplayer world is opened to LAN, the host player still reads local world files directly; joining LAN players use manifest/cache downloads.
- World language files override vanilla and enabled resource-pack translations. ParaTranz translations are applied after local world files and remote sync files as the final override layer.
- A language can be split across multiple files, such as `zh_cn.items.json` and `zh_cn.ui.json`; matching files are loaded in filename order.
- After editing singleplayer files, run `/amagari_lang reload` to reload them, or `/amagari_lang status` to inspect the latest load result. These commands report from the client and should not trigger a server-side parse error.
- After editing remote server files, an operator can run `/amagari_lang push` to publish a new manifest. Online clients automatically request language data whose hash changed.
- Players can also run `/amagari_lang pull` to request a fresh manifest for their own language.
- Run `/amagari_lang help` to show a short description of every subcommand.
- Run `/amagari_lang paratranz config` to open the local ParaTranz settings screen. Existing tokens are not echoed; saving an empty field keeps the old token, and `Clear token` removes it. The screen also configures source language, target language, export triggering, how many artifact zips to cache per project, and whether a successful pull overwrites the current local world's language file.
- When `Overwrite current world language files` is enabled, a successful ParaTranz pull writes the target language to the current local world's `amagari_translation_tool/lang/<target>.json` and removes older split files for that same language, such as `zh_cn.items.json`. If no writable local world directory is active, overwrite is skipped and translations are applied only to the current client session.
- Run `/amagari_lang paratranz` to show ParaTranz subcommand help. Run `/amagari_lang paratranz projects` to list ParaTranz projects associated with the current API token. Project names in the list are clickable and run the matching pull command.
- Run `/amagari_lang paratranz pull <project>` to match a ParaTranz project by name, export it, download it, and apply its language JSON immediately; for example, `/amagari_lang paratranz pull Permafrost-i18n`.
- In integrated-server singleplayer, LAN, or remote servers, ParaTranz subcommands pass through the server command tree and are forwarded back to the executing client to open the settings screen or run the pull. Feedback still appears only to the executor.
- The ParaTranz project argument offers tab completions from the projects visible to the configured API token.
- When an applied ParaTranz export includes `*.world.block.*` entries, matching literal sign lines are translated on the client while rendering. This covers fixed `Permafrost-i18n` lobby signs without modifying the saved world.
- Run `/amagari_lang status` to show both the world/remote language status and the latest ParaTranz pull status, including project, artifact, file count, entry count, languages, and errors.
- `/amagari_lang` command feedback is visible only to the player who ran the command. Chinese clients receive Chinese feedback; other languages default to English.
- Remote servers and LAN hosts provide `en_us` plus each joining player's current client language on demand. If a player changes language after joining, reconnect or run `/amagari_lang pull`.
- The client cache lives under `.minecraft/amagari_translation_tool/lang_cache`, partitioned by a hash of the server address and storing gzip-compressed data. Cache hits and new downloads refresh the last-used timestamp; entries unused for 7 days are deleted automatically, and each server/language pair keeps at most the 2 most recent hashes.
- The ParaTranz config file lives at `.minecraft/config/amagari_lang/config.json` with `paratranzApiToken`, `sourceLanguage`, `targetLanguage`, `triggerExport`, `maxCachedArtifacts`, and `overwriteWorldLanguageFiles` fields. The legacy `.minecraft/config/amagari_translation_tool.json` path migrates automatically, and logs/errors do not print the token.
- Downloaded ParaTranz exports are cached under `.minecraft/amagari_translation_tool/paratranz_cache/<projectId>/`. Disconnecting from a world clears only the active in-memory state, not the global cache.
- Press `V` by default to switch between the ParaTranz target language and the configured source language. Source-language mode temporarily pauses the ParaTranz override so the client falls back to source-language resources and any matching world source-language files. Fixed sign lines backed by `*.world.block.*` entries prefer the current ParaTranz pull, and also use matching current-world or remote-synced language files after an export has been overwritten into `amagari_translation_tool/lang`.
- Press `H` by default to toggle source text helpers: item tooltips add the source name, signs with matching ParaTranz `*.world.block.*` literal translations show source text near the crosshair, and translatable book text gets a hoverable `ⓘ` marker. Ordinary block names are not shown as crosshair HUD text.
- Source display first reads the enabled Minecraft, resource-pack, and mod source-language resources, such as `assets/*/lang/en_us.json`. It falls back to source-language entries from the ParaTranz project pulled in the current session only when the resource language table does not contain the key. `V` client-language switching does not require a pulled project, but ParaTranz fixed-sign source/target switching needs matching `*.world.block.*` entries in the current session pull, current-world language files, or remote-synced language files.
- A single remote language data payload is capped at 4 MiB. Large maps should keep language files scoped to the languages and namespaces they actually use.

## Build

```powershell
.\gradlew-java21.bat build --stacktrace
```

The generated jar will be under `build/libs`.

## Development Docs

- `AGENTS.md`: repository-level development rules.
- `CHANGELOG.md`: user-visible change history.
- `docs/DEVELOPMENT.md`: development workflow.
- `docs/TESTING.md`: verification and manual test checklists.
- `docs/RELEASE.md`: release workflow.
- `docs/SESSION_HANDOFF.md`: cross-session handoff notes.
