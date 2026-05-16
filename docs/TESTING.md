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

- Create `saves/<world>/amagari_translation_tool/lang/<current_language>.json` with a known vanilla translation key override.
- Enter the singleplayer world and confirm the overridden text appears without enabling a resource pack.
- Edit the file, run `/amagari_lang reload`, and confirm the changed translation appears.
- Run `/amagari_lang status` and confirm it reports the loaded file and entry count.
- Add a malformed JSON file and confirm the client keeps running while the mod logs/skips the bad file.
- Leave the world and enter a different world without language files; confirm the previous world's overrides no longer apply after reload.

## Remote Server Language Sync Checklist

Use this checklist when manually testing remote server language delivery:

- Install the mod on both the dedicated server and the client.
- Create `<server world directory>/amagari_translation_tool/lang/en_us.json` and `<server world directory>/amagari_translation_tool/lang/<client_language>.json` with known vanilla translation key overrides.
- Join the remote server and confirm the overridden text appears without enabling a resource pack.
- Run `/amagari_lang status` on the client and confirm it reports remote world language entries.
- Confirm `.minecraft/amagari_translation_tool/lang_cache` contains cached language data after the first download.
- Rejoin the same server without changing files and confirm `/amagari_lang status` reports cached manifest handling without requiring a fresh data download.
- Edit a server-side language file, run `/amagari_lang push` as an operator, and confirm the connected client requests and receives only the changed language data.
- Run `/amagari_lang pull` as a normal player and confirm the server publishes a fresh manifest for that player.
- Join with a client that does not have this mod installed and confirm the server keeps running without trying to send unsupported payloads.
- Change the client's language, reconnect or run `/amagari_lang pull`, and confirm the server offers the newly selected language plus `en_us`.
- Add a malformed JSON file on the server and confirm the server logs/skips it while still sending valid files.

Do not mark remote server sync as manually tested unless an actual dedicated or LAN server/client pair was used.
