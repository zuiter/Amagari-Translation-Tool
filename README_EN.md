# Amagari Translation Tool

Amagari Translation Tool is a Fabric translation helper mod for Minecraft 26.1.2.

中文说明见 [README.md](README.md)。

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2 or newer
- Fabric API
- Java 25

## Current Status

The project currently supports loading language files from a world directory into the client language table when entering singleplayer worlds or joining remote servers, without enabling a separate resource pack.

## World Language Files

Place language files under `amagari_translation_tool/lang` inside the world save directory:

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
- After joining a remote server, if both the server and client have this mod installed, the server first sends a manifest for `en_us` and the player's current client language. The client downloads compressed language data only when its local cache is missing or the hash changed.
- World language files are applied as the final override layer, above vanilla and enabled resource-pack translations.
- A language can be split across multiple files, such as `zh_cn.items.json` and `zh_cn.ui.json`; matching files are loaded in filename order.
- After editing singleplayer files, run `/amagari_lang reload` on the client to reload them, or `/amagari_lang status` to inspect the latest load result.
- After editing remote server files, an operator can run `/amagari_lang push` to publish a new manifest. Online clients automatically request language data whose hash changed.
- Players can also run `/amagari_lang pull` to request a fresh manifest for their own language.
- Remote servers provide `en_us` plus each player's current client language on demand. If a player changes language after joining, reconnect or run `/amagari_lang pull`.
- The client cache lives under `.minecraft/amagari_translation_tool/lang_cache`, partitioned by a hash of the server address and storing gzip-compressed data.
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
