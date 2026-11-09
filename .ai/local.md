# ReStudio fork rules

This repository is ReStudio, a fork of intellij-community. `AGENTS.md` imports this file. Upstream has no such file, so it causes no merge conflict.

- Keep the fork thin. The fork changes only branding and packaging. Language support goes into the separate plugin repository (`ReStudio-langs`).
- Put fork code into new files: `fork/`, `build/src/org/jetbrains/intellij/build/ReStudio*.kt`, `.github/workflows/fork-*.yml`, `.github/actions/fork-*`, `.github/README.md`, `.claude/skills/restudio-*`, and `ReStudio*` tests.
- Change a JetBrains file only when no extension point or override can do the job. Keep the change to a few lines and mark it with a `ReStudio:` comment.
- Add each changed JetBrains file to the table "Dokunulan upstream dosyaları" in `docs/upstream-sync.md`.
- `docs/plan.md` is the plan. Mark a task when you finish it, and write what you checked and what you did not check.
- `ReStudioBranding` holds the names, paths and URLs. `fork/branding/make-images.swift` makes the images. Run it again after a change to `fork/branding/*.svg`.
- `fork/bundled-plugins.tsv` lists the plugins that the IDE contains. Before a local installer build, run `fork/fetch-bundled-plugins.sh`.
- `fork/smoke-test.sh` starts an installed IDE and checks the bundled plugins. Run it after a change to the packaging.
- Publish a release from the Mac with the `restudio-release` skill, or with `.github/workflows/fork-release.yml`. Both use `fork/publish-release.sh`.
- Merge upstream only through `fork/upstream-sync.sh`. See `docs/upstream-sync.md`.
