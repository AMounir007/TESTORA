# Changelog

All notable changes to TESTORA are documented here.

**How releases use this file:** write your changes under `## [Unreleased]` before you push.
On every push the release workflow turns that section into the GitHub Release notes (what followers see in their feed),
renames it to `## [x.y.z] - date`, and adds a fresh empty template on top. Empty headings are dropped automatically.
If `[Unreleased]` is empty, notes are built from commit messages, so use Conventional Commits:
`feat: ...`, `fix: ...`, `security: ...`, `perf: ...`, `docs: ...`, `ci: ...` (add `!` for breaking changes, e.g. `feat!: ...`).

## [Unreleased]

### Overview
<!-- One sentence for users: what this release gives them. It becomes the release title in the GitHub feed. -->

### Added
<!-- New capabilities. Say what the user can now do. -->

### Fixed
<!-- Bugs fixed. Say what was wrong and what users see now. -->

### Security
<!-- Vulnerabilities fixed: dependency, old -> new version, CVE/GHSA id, risk removed. -->

### Changed
<!-- Behaviour or configuration changes. -->

### Breaking
<!-- Anything that needs users to change code or config, with the migration step. -->

### Why it matters
<!-- How this helps testers and teams (less flakiness, faster runs, safer evidence...). -->

## [1.0.0] - 2026-10-01

### Overview
Versioned releases on JitPack and security-hardened dependencies

### Added
- **Use TESTORA as a dependency** from any Maven or Gradle project through JitPack (sources and javadoc included).
- **Automatic releases:** every push gets a version number, a tag, release notes and a JitPack build, with no manual steps.
- **New guide:** "How to start automating your application" (docs/01), a step-by-step path from first test to CI.
- **Code and security review** of the platform (docs/13).

### Security
- Appium java-client 9.3.0 → 10.1.1: blocks redirection of session traffic through `directConnect` (GHSA-28f5-38xr-jh2w).
- Jackson 2.18.2 → 2.18.11: fixes several deserialization and denial-of-service issues.
- Logback 1.5.12 → 1.5.19 (classic and core): fixes expression-injection and code-execution issues in configuration processing.
- AssertJ 3.26.3 → 3.27.7: fixes XXE in `isXmlEqualTo`.
- commons-lang3 pinned to 3.18.0: fixes uncontrolled recursion on long inputs.

### Changed
- Maven coordinates are now `com.github.AMounir007:TESTORA` to match JitPack.

### Why it matters
- Teams pin an exact, traceable TESTORA version instead of copying source, and test runs no longer pull in libraries with known vulnerabilities.

