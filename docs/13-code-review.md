# Code Review & Security Review

## Scope
Java 21 / Maven test framework (web, mobile, API, AI diagnosis, governance, reporting).

## Findings
| # | Area | Finding | Status |
|---|------|---------|--------|
| 1 | Dependencies | Known CVEs: `appium java-client` 9.3.0, `jackson-databind` 2.18.2, `assertj-core` 3.26.3 | Fixed (10.1.1 / 2.18.11 / 3.27.7) |
| 2 | Secrets | No hard-coded credentials found; `TestoraConfig` reads secrets from env and fails fast if missing | OK |
| 3 | Logging | `Masker` redacts `password=` style values before AI/log output (covered by `PlatformTest`) | OK |
| 4 | Deserialization | Jackson used without default typing; YAML read into `Map` only | OK |
| 5 | File paths | Output paths are fixed under `target/`; `history.file` and OpenAPI location come from config (trusted input) | Note |
| 6 | Network | Appium default URL is plain `http://127.0.0.1` (local only); Preflight uses 5s timeouts | OK |
| 7 | Transitive deps | `logback-core`, `commons-lang3`, `opentelemetry-api`, `rhino` flagged by IDE | Open – add overrides after `mvn dependency:tree` |
| 8 | CI | Release workflow uses least-privilege `contents: write` only in the release job | OK |

## Recommendations
- Run `mvn clean verify` after the Appium 10 upgrade and fix any API changes.
- Enable Dependabot and GitHub secret scanning.
- Keep `appium.url` and AI provider endpoints HTTPS when remote.
- Validate `history.file` stays within the workspace if config becomes user-supplied.
