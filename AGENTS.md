# Amagari Translation Tool Agent Guide

This file is the repository-level operating guide for coding agents working on Amagari Translation Tool.
It should evolve with the project. Update it when a rule becomes stale, too strict, or incomplete.

## Core Rules

- At the start of a new Codex session, read `docs/SESSION_HANDOFF.md` before making changes.
- Do not auto-launch the Minecraft client unless the user explicitly asks.
- Preserve existing user changes and staged files. Do not revert unrelated work.
- Every feature or behavior change must update `CHANGELOG.md`.
- Update `README.md` and `README_EN.md` when commands, usage, limitations, config, or player-visible behavior change.
- Prefer small, reviewable changes that follow existing project patterns.

## Architecture Boundaries

- Common initialization belongs in `com.amagari.translationtool.AmagariTranslationTool`.
- Keep translation data, command handling, config, and client-only UI/rendering code in clear ownership boundaries as they are introduced.
- Server-to-client visual or UI behavior should use typed Fabric payloads registered in common/client initialization.
- Client-only state must reset on disconnect or world changes when state can leak.
- Mixins should be minimal, side-specific, and registered in the correct mixin config.
- Keep server gameplay state and client-only presentation state clearly separated.

## Post-Feature Workflow

After a feature is implemented:

1. Self-review the changed code for risk, redundancy, and stale helpers.
2. Check `README.md`, `README_EN.md`, and `CHANGELOG.md`.
3. Run `git diff --check`.
4. Run `.\gradlew-java25.bat build --stacktrace`.
5. Stage only files that belong to the completed change when the user has asked to keep git ready.

Use subagents only for narrow, independent review or documentation checks. Close them after completion, failure, or timeout.

## Verification

- Default build command: `.\gradlew-java25.bat build --stacktrace`.
- Do not claim runtime behavior was manually tested unless a Minecraft client/server test was actually performed.
- For multiplayer, translation UI, or client-state features, add or update a short manual test checklist in `docs/TESTING.md`.

## CI And Release

- GitHub Actions build config lives at `.github/workflows/build.yml`.
- CI should use Java 25 and run `./gradlew build --stacktrace`.
- Release instructions live in `docs/RELEASE.md`.
- Keep unreleased changes in `CHANGELOG.md` until a version bump.
- On release, update `gradle.properties`, archive the changelog entry under the released version, build locally, tag the release, and upload the jar manually unless the user asks for automation.

## Documentation Split

- `README.md`: Chinese player/server-owner documentation.
- `README_EN.md`: English player/server-owner documentation.
- `CHANGELOG.md`: user-visible release notes.
- `docs/DEVELOPMENT.md`: developer and agent workflow.
- `docs/RELEASE.md`: lightweight version bump and GitHub Release process.
- `docs/SESSION_HANDOFF.md`: cross-session continuity notes and current project path.
- `docs/TESTING.md`: manual verification checklists.
