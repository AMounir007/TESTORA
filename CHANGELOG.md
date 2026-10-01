# Changelog

All notable changes are documented here. The release workflow uses the section matching the version
(`## [x.y.z]`) as the GitHub Release notes; if none exists, notes are auto-generated.

## [1.0.0]

### Added
- Automatic release pipeline: every push to `main` bumps the patch version, updates `pom.xml`, tags (no `v` prefix),
  publishes a GitHub Release and triggers a JitPack build.
- JitPack support (`jitpack.yml`, sources and javadoc jars).
- Documentation: "How to start automating your application" (docs/01), code and security review (docs/13).

### Security
- Upgraded Appium java-client to 10.1.1, Jackson to 2.18.11, Logback to 1.5.19, AssertJ to 3.27.7.
- Pinned `commons-lang3` 3.18.0 and `logback-core` to fixed versions.

### Changed
- `groupId` is now `com.github.AMounir007` (matches JitPack coordinates).
