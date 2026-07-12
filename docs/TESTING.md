# Amagari Translation Tool Testing Guide

Use this file for manual checks that are hard to automate in a Minecraft mod.

## Default Verification

Run after normal code changes:

```powershell
git diff --check
.\gradlew-java25.bat build --stacktrace
```

This proves formatting and compilation, not multiplayer runtime behavior.

## Client Launch Policy

Only launch Minecraft clients when explicitly requested.

Example command when manual client testing is requested:

```powershell
.\gradlew-java25.bat runClient
```

## Current Scaffold Checklist

Expected after a clean build:

- `build/libs/amagari-translation-tool-mc26.1.2-fabric-<version>.jar` exists.
- `fabric.mod.json` expands `${version}` and `${minecraft_dependency}` during resource processing.
- The main entrypoint class compiles successfully.

## World Language File Checklist

Use this checklist when manually testing world language files:

- Enter a world without an existing `amagari_translation_tool/lang` directory and confirm the mod creates it automatically.
- Create `saves/<world>/amagari_translation_tool/lang/<current_language>.json` with a known vanilla translation key override.
- Enter the singleplayer world and confirm the overridden text appears without enabling a resource pack.
- Edit the file, run `/amagari_lang reload`, and confirm the changed translation appears and no red server-side command parse error is shown.
- Run `/amagari_lang status` and confirm it reports the loaded file and entry count without a red server-side command parse error.
- Run `/amagari_lang help` and confirm it lists `help`, `reload`, `status`, `pull`, and `push` only for the executing player.
- Switch the client language between Chinese and English, then confirm `/amagari_lang help`, `/amagari_lang reload`, and `/amagari_lang status` feedback follows the executing client's language.
- Add a malformed JSON file and confirm the client keeps running while the mod logs/skips the bad file.
- Leave the world and enter a different world without language files; confirm the previous world's overrides no longer apply after reload.

## Remote Server Language Sync Checklist

Use this checklist when manually testing remote server language delivery:

- Install the mod on both the dedicated server and the client.
- Start or join once without an existing `<server world directory>/amagari_translation_tool/lang` directory and confirm the mod creates it automatically.
- Create `<server world directory>/amagari_translation_tool/lang/en_us.json` and `<server world directory>/amagari_translation_tool/lang/<client_language>.json` with known vanilla translation key overrides.
- Join the remote server and confirm the overridden text appears without enabling a resource pack.
- Run `/amagari_lang status` on the client and confirm it reports remote world language entries.
- Confirm `.minecraft/amagari_translation_tool/lang_cache` contains cached language data after the first download.
- Rejoin the same server without changing files and confirm `/amagari_lang status` reports cached manifest handling without requiring a fresh data download.
- Create or age old cache files in `.minecraft/amagari_translation_tool/lang_cache`, then trigger a manifest load/download and confirm files unused for more than 7 days are removed.
- Create more than two cached hashes for the same server/language, then trigger a manifest load/download and confirm only the two most recently used hashes remain.
- Edit a server-side language file, run `/amagari_lang push` as an operator, and confirm the connected client requests and receives only the changed language data.
- Run `/amagari_lang pull` as a normal player and confirm the server publishes a fresh manifest for that player.
- Confirm `/amagari_lang pull` and `/amagari_lang push` feedback is visible only to the executing player and follows that player's client language.
- Join with a client that does not have this mod installed and confirm the server keeps running without trying to send unsupported payloads.
- Change the client's language, reconnect or run `/amagari_lang pull`, and confirm the server offers the newly selected language plus `en_us`.
- Add a malformed JSON file on the server and confirm the server logs/skips it while still sending valid files.

## ParaTranz Pull Checklist

Use this checklist when manually testing ParaTranz downloads:

- Run `/amagari_lang paratranz config` and confirm the ParaTranz settings screen opens, preserves an existing token when the field is left blank, clears the token only when `Clear token` is selected, and persists source language, target language, trigger export, cached artifact count, and the current-world overwrite checkbox.
- Run `/amagari_lang paratranz` and confirm it shows ParaTranz subcommand help without requesting the project list.
- Run `/amagari_lang paratranz projects`; confirm it lists projects visible to the configured token, and that clicking a project name runs the matching `/amagari_lang paratranz pull <project>` command.
- Type `/amagari_lang paratranz pull ` and confirm tab/completion suggestions include project names from the configured API token.
- Run `/amagari_lang paratranz pull <project>` with a visible test project and confirm it downloads, caches, and applies the project translations.
- Enable `Overwrite current world language files`, pull a project in a local singleplayer world, and confirm `saves/<world>/amagari_translation_tool/lang/<target_language>.json` is rewritten while older split files for the same language are removed.
- Join a remote server with overwrite enabled, pull a project, and confirm the chat reports that no writable local world directory is active while the session-only ParaTranz translations still apply.
- In a test map with fixed literal signs covered by `*.world.block.*` entries, confirm those signs render with their pulled target-language translations after apply.
- Confirm `.minecraft/config/amagari_lang/config.json` exists and contains `paratranzApiToken`; if `.minecraft/config/amagari_translation_tool.json` exists first, confirm it migrates into the nested config path.
- Confirm `.minecraft/amagari_translation_tool/paratranz_cache/19173/` contains a downloaded artifact zip and metadata after a successful pull.
- Set `maxCachedArtifacts=1`, pull the same project twice when two artifact zips are available, and confirm only the newest `artifact-*.zip` remains for that project.
- Run `/amagari_lang status` and confirm it reports the world/remote language state plus ParaTranz project name/id, artifact id/time, loaded files, entries, active languages, and any failed files.
- Temporarily replace the token with an invalid value and confirm the error says the token was rejected without printing the token.
- Run `/amagari_lang paratranz pull <missing-project>` and confirm it shows a clean not-found or ambiguous-project message with suggestions when applicable.
- In a singleplayer integrated-server world, run `/amagari_lang paratranz config`, `/amagari_lang paratranz projects`, `/amagari_lang paratranz pull <project>`, and `/amagari_lang status`; confirm the config screen opens, chat feedback appears, and no red Brigadier parse error is shown.
- After a successful ParaTranz pull with `Trigger export` and `Overwrite current world language files` enabled, stay in the current world and confirm fixed signs backed by `*.world.block.*` entries refresh to the target text without rejoining.
- After a successful ParaTranz pull, press `V` and confirm the client switches to the configured source language, including fixed signs backed by `*.world.block.*` literal sign entries returning from target-language text to source text; press `V` again and confirm it returns to the configured target language with ParaTranz translations active.
- Restart the client after overwriting a ParaTranz pull into the current world's `amagari_translation_tool/lang/<target>.json`, then press `V` and confirm the same fixed signs can still switch between target text and source text from the saved world language file.
- Press `H` with `sourceLanguage=en_us` and confirm vanilla/modded items show a separate source-language tooltip beside the original tooltip, using the real English text from enabled Minecraft/resource-pack/mod language resources for the item name and each translatable tooltip line even when the pulled ParaTranz `en_us` export contains translated target text. Confirm the source text is not appended as a normal line under the original tooltip, and that colored or italic source lines preserve the original tooltip component style where possible.
- After a successful ParaTranz pull, press `H` and confirm the side source-language item tooltip can still fall back to source-language helper text from the pulled project when the key does not exist in enabled resource language files.
- With `H` enabled, point at a sign that has a matching ParaTranz `*.world.block.*` literal translation and confirm a compact HUD appears near the crosshair with the source lines; point at an ordinary block and confirm no block-name HUD appears.
- With local world `en_us.json` and target-language files both containing the same `*.world.block.*` key, confirm the `H` source HUD shows the `en_us` value rather than a humanized key name.
- With only local world `<target>.json` present after a ParaTranz overwrite, keep the original map resource pack enabled and confirm the `H` sign source HUD uses `assets/*/lang/<source>.json` source text rather than a humanized `*.world.block.*` key name.
- With `H` enabled, open a written book containing translatable text from the pulled project and confirm a hoverable `ⓘ` marker appears after translated text; hovering the marker should show the source text in a readable light book-page-style box rather than the default dark tooltip.
- With `H` enabled, open a map-provided book whose translatable text already has a hover tooltip and click action. Confirm hovering/clicking the original text still uses the map interaction, hovering `ⓘ` shows only the ATT source box, and clicking `ⓘ` does not run the map action.
- In Minecraft 26.1.2, enter a singleplayer world after installing the mod and confirm the client reaches the world without disconnecting with a network protocol error while `BookViewScreen` is loaded.
- Press `H` before pulling a ParaTranz project and confirm source display toggles cleanly; press `V` before pulling and confirm it still switches between configured source/target client languages.

## Open To LAN Language Sync Checklist

Use this checklist when manually testing a singleplayer world opened to LAN:

- Install the mod on the host client and the joining LAN client.
- Create `saves/<world>/amagari_translation_tool/lang/en_us.json` and `saves/<world>/amagari_translation_tool/lang/<joining_client_language>.json` on the host world with known vanilla translation key overrides.
- Open the singleplayer world to LAN and join from another client.
- Confirm the host still reads local world language files directly.
- Confirm the joining LAN client receives the language manifest, downloads missing cached data, and shows the overridden text without enabling a resource pack.
- Run `/amagari_lang pull` from the joining LAN client and confirm it can request a fresh manifest from the host.
- Edit a host-side language file, run `/amagari_lang push` as the host/operator, and confirm the joining LAN client receives only changed language data.

Do not mark remote or LAN sync as manually tested unless an actual dedicated server or LAN host/client pair was used.
