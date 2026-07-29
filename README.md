# Codacy Metrics cloc

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/11ebf162681e4d45beb0a975926ac34b)](https://www.codacy.com/gh/codacy/codacy-metrics-cloc?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/codacy-metrics-cloc&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/11ebf162681e4d45beb0a975926ac34b)](https://www.codacy.com/gh/codacy/codacy-metrics-cloc?utm_source=github.com&utm_medium=referral&utm_content=codacy/codacy-metrics-cloc&utm_campaign=Badge_Coverage)
[![CircleCI](https://circleci.com/gh/codacy/codacy-metrics-cloc.svg?style=svg)](https://circleci.com/gh/codacy/codacy-metrics-cloc)
[![Docker Version](https://images.microbadger.com/badges/version/codacy/codacy-metrics-cloc.svg)](https://microbadger.com/images/codacy/codacy-metrics-cloc "Get your own version badge on microbadger.com")

This is the docker engine we use at Codacy to have [cloc](hhttps://github.com/AlDanial/cloc/) support.

## Usage

You can create the docker by doing:

```bash
sbt stage
docker build -t codacy-metrics-cloc .
```

The docker is ran with the following command:

```bash
docker run -it -v $srcDir:/src  <DOCKER_NAME>:<DOCKER_VERSION>
docker run -it -v $PWD/src/test/resources:/src codacy/codacy-metrics-cloc:latest
```

## Agent Playbook: Updating This Repository End-to-End

This section is written for an AI coding agent (or a human) tasked with updating this repo — most commonly bumping the wrapped `cloc` version, but also base image / orb / Scala-seed bumps. Follow it top to bottom; it tells you what to change, how to test locally, and how to interpret CI so you can iterate on failures without guessing.

### 1. What this repository is

This is a Codacy **metrics engine**, not a pattern/linting engine. It is a thin Scala wrapper (`src/main/scala/codacy/metrics/Cloc.scala` and `Metrics.scala`, built on the `codacy-metrics-scala-seed` library) that packages [cloc](https://github.com/AlDanial/cloc) (a Perl tool that Counts Lines of Code) as a Docker image Codacy's platform runs against a customer's source code. `Cloc.scala` shells out to `cloc --json --by-file --skip-uniqueness <targets>` and parses the resulting JSON into `FileMetrics(filename, loc, cloc)` (lines of code / lines of comments) per file — there are no "patterns", severities, or rule parameters involved anywhere in this engine.

Consequently:

- **`docs/patterns.json` does not exist in this repository**, and there is no `docs/description/` directory and no `DocGenerator`. Metrics tools don't have a pattern catalog to generate or maintain.
- The only thing under `docs/` is `docs/tests/` — plain source fixtures (`example.py`, `example.rb`, `example.go`) used as test input. Each file's first line is an annotation comment consumed by `codacy-plugins-test`'s metrics test mode, e.g. `# #Metrics: {"loc": 42, "cloc": 25}`, which the test harness compares against what the built image actually reports for that file. If a `cloc` version bump changes how lines are counted for a given language, these expected numbers may need to be updated.
- `cloc` itself is not vendored as a JVM/Scala dependency — it's consumed as an **npm package** (`node_modules/cloc`, which is a Node wrapper that ships the upstream Perl `cloc` script) and invoked as an external process at runtime, so its Perl interpreter and the `perl` package in the Docker image are also part of the runtime contract.

### 2. Files that encode versions — check all of these on every update

| File | What it controls | What to check |
|---|---|---|
| `package.json` → `dependencies.cloc` | The `cloc` (Perl tool) version bundled into the image | Bump to the target npm-published `cloc` version; note the npm package's versions look like `2.8.0` or `2.6.0-cloc` — confirm the target version actually exists on the npm registry, since the two schemes have coexisted here before. |
| `package-lock.json` | The exact resolved/locked `cloc` version and its integrity hash | **This file was found out of sync with `package.json` in this repository at investigation time** (`package.json` pinned `^2.6.0-cloc`, but `package-lock.json` still resolved `2.8.0`). Always regenerate it with `npm install` after changing `package.json` and commit the result — do not hand-edit the hash. |
| `Dockerfile` → `ARG BASE_IMAGE` | Alpine base image | Bump when doing a base-image update; also carries the `openjdk17`/`perl`/`npm`/`bash` `apk add` line — bump the JDK major version here too if the Scala seed or sbt plugins require a newer JDK. |
| `build.sbt` → `codacy-metrics-scala-seed` version | The engine framework / `MetricsTool`/`DockerMetrics` API surface | Bump when a seed update is scoped; check the seed's own changelog for breaking API changes to `Cloc.scala`. |
| `project/build.properties` → `sbt.version` | sbt itself | Bump alongside `codacy-sbt-plugin` when required by that plugin's own minimum sbt version. |
| `project/plugins.sbt` → `codacy-sbt-plugin`, `sbt-native-packager`, `sbt-native-image` | Build/packaging plugins | Bump together; note history shows these have been bumped and then selectively **downgraded** again (e.g. `sbt-native-packager` was bumped to `1.11.1` then downgraded back to `1.9.6` in a follow-up commit) — treat plugin bumps as needing a real local `sbt stage` verification, not a blind version swap. |
| `.circleci/config.yml` → `codacy/base` orb | Shared CircleCI steps (checkout, sbt build/test, docker publish, tag) | Check the latest published version. |
| `.circleci/config.yml` → `codacy/plugins-test` orb | Runs `codacy-plugins-test` in CI, with `run_metrics_tests: true` and `run_json_tests`/`run_pattern_tests` set to `false` (this is a metrics-only engine) | Bump the orb version; do not flip the test-mode flags — this engine only ever runs metrics tests. |

### 3. Step-by-step update procedure

1. **Bump the version(s)** as scoped by the task, in every file listed above that applies.
2. **Regenerate `package-lock.json`** with `npm install` whenever `package.json`'s `cloc` version changed, and check in the result — there is no separate docs generator to run in this repo (no `patterns.json`/`DocGenerator`, unlike pattern-based Codacy engines).
3. **Compile, format, and stage** with sbt: `sbt test:scalafmt scalafmt scalafmtSbt stage` (this is exactly what CI runs).
4. **Build the Docker image**: `docker build -t codacy-metrics-cloc .` (or `docker build -t $CIRCLE_PROJECT_REPONAME:latest .` to match CI's tag).
5. **Sanity-check the image runs and calls the right `cloc`**: `docker run -it -v $PWD/docs/tests:/src codacy/codacy-metrics-cloc:latest` and confirm it emits JSON metrics for the fixture files, and that `docker run --rm --entrypoint sh codacy-metrics-cloc -c 'cloc --version'` reports the version you just bumped to.
6. **Run `codacy-plugins-test` locally** before pushing — clone https://github.com/codacy/codacy-plugins-test and run its metrics-mode `DockerTest` command against your local image tag (mirror the CircleCI job, which only runs metrics tests: `run_metrics_tests: true`, `run_json_tests: false`, `run_pattern_tests: false`). If a `cloc` version bump changed how any language's lines are counted, update the expected `#Metrics: {...}` annotation at the top of the affected file(s) under `docs/tests/` to match the new, verified-correct output — do not just change numbers until the test passes; confirm the new counts are actually correct for that version of `cloc`.
7. **Iterate on failures**, re-running only the relevant test command after each fix.
8. **Commit** the version bump(s), the regenerated `package-lock.json`, and any updated `docs/tests/*` fixture annotations together in one change.
9. **Push and open a PR.**
10. **Poll the PR's real CI checks until they all pass — local validation is NOT the finish line.** After every push, run `gh pr checks <pr-url>` and keep re-polling (short sleep while any check is `pending`) until all checks finish. If a check fails, fetch its actual log (don't guess), find the true root cause, fix it, push again (never `--no-verify`, never force-push), and re-poll. Repeat until every check is green. **The CI environment's toolchain can differ from your local one**, so a clean local run does not guarantee CI passes. Only stop iterating when every check passes, or you hit a genuine product/infra decision that needs a human.

### 4. Common failure modes and fixes

| Symptom | Likely cause | Fix |
|---|---|---|
| `package-lock.json` still resolves the old `cloc` version after editing `package.json` | `npm install` wasn't re-run, or was run without network/registry access | Re-run `npm install` and verify both the `dependencies` and `packages[""].dependencies` blocks in the lockfile show the new version before committing. |
| `codacy-plugins-test` metrics assertions fail on one or two languages only | The new `cloc` release changed how it counts lines/comments for those specific languages (this genuinely happens between `cloc` releases) | Re-verify the correct counts by hand (e.g. run `cloc` locally against the fixture) and update the `#Metrics: {...}` header comment in the corresponding `docs/tests/example.*` file, rather than assuming the test fixture is wrong. |
| `sbt stage` or the Docker build fails after a plugins.sbt/build.properties bump | A newer `sbt-native-packager`/`codacy-sbt-plugin` version pulled in an incompatible sbt or JDK requirement | Check that plugin's changelog; this repo has previously resolved this by downgrading `sbt-native-packager` again rather than chasing the newest release. |

### 5. Definition of done

- Version bump(s) reflected in all files listed in section 2 that apply (`package.json`, `package-lock.json`, `Dockerfile`, `build.sbt`, `project/build.properties`, `project/plugins.sbt`, `.circleci/config.yml` as applicable).
- Local `sbt test:scalafmt scalafmt scalafmtSbt stage` and Docker build succeed.
- The built image's bundled `cloc --version` matches the target version.
- `codacy-plugins-test` metrics-mode tests pass locally against the freshly built image, with any fixture annotations in `docs/tests/` updated and re-verified if the bump changed counting behavior.
- **After pushing and opening/updating the PR, every CI check on it is green.** Poll `gh pr checks <pr-url>` and iterate on any failure until all pass.

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
