# Amagari Translation Tool

Amagari Translation Tool is a Fabric translation helper mod for Minecraft 26.1.

中文说明见 [README.md](README.md)。

## Requirements

- Minecraft 26.1
- Fabric Loader 0.19.2 or newer
- Fabric API
- Java 25

## Current Status

The project currently supports loading language files from a world directory into the client language table when entering singleplayer worlds, joining remote servers, or joining singleplayer worlds opened to LAN, without enabling a separate resource pack.

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
- World language files are applied as the final override layer, above vanilla and enabled resource-pack translations.
- A language can be split across multiple files, such as `zh_cn.items.json` and `zh_cn.ui.json`; matching files are loaded in filename order.
- After editing singleplayer files, run `/amagari_lang reload` to reload them, or `/amagari_lang status` to inspect the latest load result. These commands report from the client and should not trigger a server-side parse error.
- After editing remote server files, an operator can run `/amagari_lang push` to publish a new manifest. Online clients automatically request language data whose hash changed.
- Players can also run `/amagari_lang pull` to request a fresh manifest for their own language.
- Run `/amagari_lang help` to show a short description of every subcommand.
- `/amagari_lang` command feedback is visible only to the player who ran the command. Chinese clients receive Chinese feedback; other languages default to English.
- Remote servers and LAN hosts provide `en_us` plus each joining player's current client language on demand. If a player changes language after joining, reconnect or run `/amagari_lang pull`.
- The client cache lives under `.minecraft/amagari_translation_tool/lang_cache`, partitioned by a hash of the server address and storing gzip-compressed data. Cache hits and new downloads refresh the last-used timestamp; entries unused for 7 days are deleted automatically, and each server/language pair keeps at most the 2 most recent hashes.
- A single remote language data payload is capped at 4 MiB. Large maps should keep language files scoped to the languages and namespaces they actually use.

## Build

```powershell
.\gradlew-java25.bat build --stacktrace
```

The generated jar will be under `build/libs`.

## Development Docs

- `AGENTS.md`: repository-level development rules.
- `CHANGELOG.md`: user-visible change history.
- `docs/DEVELOPMENT.md`: development workflow.
- `docs/TESTING.md`: verification and manual test checklists.
- `docs/RELEASE.md`: release workflow.
- `docs/SESSION_HANDOFF.md`: cross-session handoff notes.
