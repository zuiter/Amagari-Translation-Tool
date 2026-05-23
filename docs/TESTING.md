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

- `build/libs/amagari-translation-tool-mc26.1-fabric-<version>.jar` exists.
- `fabric.mod.json` expands `${version}` and `${minecraft_dependency}` during resource processing.
- The main entrypoint class compiles successfully.

## World Language File Checklist

Use this checklist when manually testing world language files:

- Enter a world without an existing `amagari_translation_tool/lang` directory and confirm the mod creates it automatically.
- Create `saves/<world>/amagari_translation_tool/lang/<current_language>.json` with a known vanilla translation key override.
- Enter the singleplayer world and confirm the overridden text appears without enabling a resource pack.
- Edit the file, run `/amagari_lang reload`, and confirm the changed translation appears and no red server-side command parse error is shown.
- Run `/amagari_lang status` and confirm it reports the loaded file and entry count without a red server-side command parse error.
- Run `/amagari_lang help` and confirm it lists `help`, `reload`, `status`, `paratranz`, `pull`, and `push` for the executing player.
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

- Run `/amagari_lang paratranz` and confirm the chat feedback lists projects including `Permafrost-i18n`.
- Run `/amagari_lang paratranz Permafrost-i18n` and confirm it downloads, caches, and applies the project translations.
- Confirm `.minecraft/config/amagari_lang/config.json` exists and contains `paratranzApiToken`.
- Confirm `.minecraft/amagari_translation_tool/paratranz_cache/19173/` contains a downloaded artifact zip and metadata after a successful pull.
- Run `/amagari_lang status` and confirm it reports the world/remote language state plus ParaTranz project name/id, artifact id/time, loaded files, entries, active languages, and any failed files.
- Temporarily replace the token with an invalid value and confirm the error says the token was rejected without printing the token.
- Run `/amagari_lang paratranz <missing-project>` and confirm it shows a clean not-found or ambiguous-project message with suggestions when applicable.
- In a singleplayer integrated-server world, run `/amagari_lang paratranz`, `/amagari_lang paratranz Permafrost-i18n`, and `/amagari_lang status`; confirm chat feedback appears and no red Brigadier parse error is shown.

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
