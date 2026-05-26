# Amagari Translation Tool

Amagari Translation Tool is a Fabric translation helper mod for Minecraft 1.21.10. It loads language JSON files from world saves, remote servers/LAN hosts, or ParaTranz exports directly into the client language table without requiring a separate resource pack. It also provides bilingual switching and source-text helpers for translation review.

中文说明见 [README.md](README.md)。

## Feature Overview

- **World language files**: automatically creates and reads `amagari_translation_tool/lang`; files use the same format as resource-pack language files at `assets/<namespace>/lang/<language>.json`.
- **Remote and LAN sync**: servers and LAN hosts send language manifests to joining clients with the mod installed; clients download only missing or changed data.
- **ParaTranz pull**: configure a token in-game, list projects, pull by project name, apply language JSON immediately, and optionally overwrite the current local world's language file.
- **Bilingual review**: press `V` to switch between target/source languages; press `H` to show source helpers for item tooltips, signs, and book text.
- **Private feedback and cache cleanup**: command feedback is visible only to the executor; remote language cache entries unused for 7 days are deleted, and each server/language keeps at most the 2 newest hashes.

## Requirements

- Minecraft 1.21.10
- Fabric Loader 0.16.14 or newer
- Fabric API
- Java 21

## Quick Start

### World Language Files

1. Install this mod and Fabric API.
2. Enter the target world once. The mod creates `amagari_translation_tool/lang` automatically.
3. Place language JSON files in that directory, such as `zh_cn.json` or `en_us.json`.
4. Run `/amagari_lang reload` in-game to reload, or `/amagari_lang status` to inspect the load status.

Common paths:

```text
saves/<world name>/amagari_translation_tool/lang/zh_cn.json
saves/<world name>/amagari_translation_tool/lang/en_us.json
<server world directory>/amagari_translation_tool/lang/zh_cn.json
<server world directory>/amagari_translation_tool/lang/en_us.json
```

Language file example:

```json
{
	"item.minecraft.diamond": "World-specific diamond name",
	"screen.example.title": "World screen title"
}
```

### ParaTranz Pull

1. Run `/amagari_lang paratranz config` to open the settings screen.
2. Enter your ParaTranz token and confirm source language, target language, export triggering, cache count, and whether successful pulls should overwrite the current world language file.
3. Run `/amagari_lang paratranz projects` to list projects visible to the token. Project names in chat are clickable.
4. Run `/amagari_lang paratranz pull <project>` to export, download, and apply a project by name.

Example:

```text
/amagari_lang paratranz pull <project-name>
```

## Loading Rules

- Singleplayer worlds merge world language files for the current client language and `en_us`.
- Remote servers or LAN joining clients receive `en_us` plus the current client language on demand. If the player changes language after joining, reconnect or run `/amagari_lang pull`.
- A language can be split across multiple files, such as `zh_cn.items.json` and `zh_cn.ui.json`; matching files are loaded in filename order.
- Translation override order, from highest to lowest, is: the active ParaTranz pull, world/remote-synced language files, enabled resource packs, vanilla language files.
- When an applied ParaTranz export contains `*.world.block.*` entries, matching literal sign lines are translated client-side during rendering. This is useful for fixed map text such as lobby notices, quest hints, and rule boards. It changes only the client display and does not modify saved block text.
- A single remote language data payload is capped at 4 MiB. Large maps should keep language files scoped to the languages and namespaces they actually use.

## Commands

| Command | Purpose |
| --- | --- |
| `/amagari_lang help` | Show a short description of every subcommand. |
| `/amagari_lang reload` | Reload current singleplayer world language files. |
| `/amagari_lang status` | Show world/remote language status and the latest ParaTranz status. |
| `/amagari_lang pull` | Ask the server or LAN host to publish a fresh manifest for the executor's current language. |
| `/amagari_lang push` | Ask the server to publish a new manifest, usually after an operator edits server-side language files. |
| `/amagari_lang paratranz` | Show ParaTranz subcommand help. |
| `/amagari_lang paratranz config` | Open the local ParaTranz settings screen. |
| `/amagari_lang paratranz projects` | List ParaTranz projects visible to the current API token. |
| `/amagari_lang paratranz pull <project>` | Export, download, and apply a ParaTranz project by name. |

Command feedback is visible only to the player who ran the command. Chinese clients receive Chinese feedback; other languages default to English.

## ParaTranz Details

- Config file: `.minecraft/config/amagari_lang/config.json`.
- Fields: `paratranzApiToken`, `sourceLanguage`, `targetLanguage`, `triggerExport`, `maxCachedArtifacts`, and `overwriteWorldLanguageFiles`.
- The legacy `.minecraft/config/amagari_translation_tool.json` path migrates automatically.
- Saved tokens are not echoed on the settings screen. Saving an empty token field keeps the old token; selecting `Clear token` removes it.
- Download cache: `.minecraft/amagari_translation_tool/paratranz_cache/<projectId>/`. Disconnecting from a world clears only active in-memory state, not the global cache.
- When `Overwrite current world language files` is enabled, the target language is written to the current local world's `amagari_translation_tool/lang/<target>.json`, and older split files for the same language, such as `zh_cn.items.json`, are removed. If no writable local world directory is active, overwrite is skipped and translations apply only to the current client session.
- `/amagari_lang paratranz pull <project>` supports project-name completions from the projects visible to the configured token.

## Bilingual Review

- Press `V` by default to switch between the ParaTranz target language and the configured source language. Source-language mode temporarily pauses the ParaTranz override so the client falls back to source-language resources and world source-language files.
- Press `H` by default to toggle source text helpers.
- Items render a separate source-language tooltip beside the original tooltip, try to show source text for each translatable tooltip line, and preserve the original component colors, italics, and argument styles where possible.
- Signs with matching ParaTranz `*.world.block.*` literal translations show source text near the crosshair. Ordinary block names are not shown as crosshair HUD text.
- Translatable book text gets a hoverable `ⓘ` marker after the translated text; hovering the marker shows the source text on a light background close to the vanilla book page.
- Source display, including the sign source HUD, first reads enabled Minecraft, resource-pack, and mod source-language resources, such as `assets/*/lang/en_us.json`. It falls back to source-language entries from the ParaTranz project pulled in the current session only when the resource language table does not contain the key.
- Item tooltips and book text must preserve translatable keys for source lookup. If a mod or map has already converted the content into plain literal text, the client cannot reconstruct the original source text automatically. Language JSON does not carry color data, so item source text can only inherit colors and layout styles already present on the original components; book source boxes use a fixed page-like style for readability.

## Remote Servers And LAN

- When both server and client have this mod installed, the server sends a language manifest first. Clients download gzip-compressed language data only when their local cache is missing or the hash changed.
- When a singleplayer world is opened to LAN, the host player still reads local world files directly; joining LAN players use manifest/cache downloads.
- Remote language cache path: `.minecraft/amagari_translation_tool/lang_cache`, partitioned by a hash of the server address.
- Cache hits and new downloads refresh the last-used timestamp. Entries unused for 7 days are deleted automatically, and each server/language pair keeps at most the 2 most recent hashes.
