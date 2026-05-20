# Amagari Translation Tool Development Guide

This guide keeps project workflow inside the repository instead of relying only on chat memory.

## Default Workflow

1. Understand the requested translation, command, UI, or gameplay behavior.
2. Identify the smallest relevant files with `rg` or narrow diffs.
3. Keep common initialization, translation data, config, client UI state, payloads, and mixins in clear ownership boundaries as they are introduced.
4. Update player-facing documentation when usage changes.
5. Update `CHANGELOG.md` for every completed feature or behavior change.
6. Verify with `git diff --check` and `.\gradlew.bat build --stacktrace`.

## Project Conventions

- Mod id: `amagari_translation_tool`.
- Main entrypoint: `com.amagari.translationtool.AmagariTranslationTool`.
- Keep translation-specific logic under `com.amagari.translationtool` packages.
- Client-only features must reset on disconnect. If coordinates, screen state, or dimension state are involved, also reset on world changes.
- Visual-only or UI-only features must document that they do not change real server state when applicable.
- Translation data should be represented with structured formats and parsers rather than ad hoc string manipulation where practical.

## Subagents And Skills

Use subagents and skills only when they provide clear value.

- Good subagent tasks: code risk review, README/CHANGELOG consistency, focused file/symbol mapping.
- Poor subagent tasks: vague whole-repo review, critical-path implementation, tasks needing the full chat history.
- Give subagents narrow prompts, read-only scope by default, and bounded timeouts.
- Close completed, failed, or timed-out subagents.

Skill routing:

- Use `analyze` for read-only investigation.
- Use `code-review` for explicit reviews.
- Use cleanup/refactor skills only for cleanup-focused requests.
- Skip skills that are unsupported, overkill, or likely to add noise.

## Git Notes

- The worktree may already contain user changes.
- Do not reset, checkout, or discard files unless explicitly asked.
- Stage only files that belong to the requested project change.
- If committing, follow the repository's lore-style commit message convention when available.

## Common Risks

- Translation features may touch user-authored text; avoid destructive rewrites and preserve formatting where possible.
- Client UI and translation previews depend on the client mod.
- Network payloads should be versioned carefully if translation state later crosses server/client boundaries.
- Do not auto-launch the Minecraft client unless the user asks.
