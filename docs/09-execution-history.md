# 09 - Execution History and Trends

TESTORA records every test result and uses it to answer: *Is this test flaky? Is this failure new? Did this test pass only because of retries?*

## How it works

1. After each test, `TestoraExtension` appends one line to the history file (default `.testora/history.jsonl`): run ID, test ID, status, duration, retries, recoveries, failure category and fingerprint. No secrets or test data are stored.
2. At the end of the run, `report.html` gets a **Trends** section built from all stored runs.
3. The file is outside `target/`, so `mvn clean` does not delete it.

## What the Trends section shows

| Section | Rule |
|---|---|
| Flaky tests | Mixed pass and fail inside the window, failure rate at or above the threshold, and at least the minimum number of runs |
| Broken tests | Failed in every run (at least 3). These are broken or real defects, not flaky |
| Passed only with retries/recovery | Tests in the current run that passed but needed help. Review them |
| New failure fingerprints | Failure causes not seen in earlier runs |
| Recurring failure fingerprints | Failure causes already seen in earlier runs |

These are facts about past results. They do not prove a cause.

## Settings

| Key | Default | Meaning |
|---|---|---|
| `history.enabled` | `true` | Turn recording off with `false` |
| `history.file` | `.testora/history.jsonl` | Where history is stored |
| `history.window` | `20` | How many recent runs per test are considered |
| `history.flaky.threshold` | `0.10` | Minimum failure rate to call a mixed-result test flaky |
| `history.min.runs` | `5` | Minimum runs before flakiness is reported |

Flakiness results are only meaningful after roughly 10 or more runs per test.

## Ranked run list

`TrendAnalyzer.rankByRecentFailures(impactedTests, budget)` returns tests ordered by recent failure rate, with a reason for each. Impacted tests (for example from `KnowledgeGraph.affectedBy`) come first. Use an empty set if you have no impact data. It is a library call; it is not yet a command-line switch.

## Keeping history in CI

CI agents start clean, so history is lost unless you keep the file. Examples:

- **GitHub Actions:** the provided workflow already restores and saves `.testora/` with the cache action (see [11](11-continuous-integration.md)).
- **GitLab CI:** add `.testora/` to `cache` paths.
- **Jenkins:** archive it and copy it back from the last successful build.

Do not commit it to Git in a shared repository unless your team agrees, because every run changes it.
