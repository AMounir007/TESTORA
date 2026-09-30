# 11 - Continuous Integration

TESTORA ships CI definitions for GitHub Actions, GitLab CI and Jenkins. The GitHub workflow is the most complete.

## GitHub Actions (`.github/workflows/ci.yml`)

Runs on every push, every pull request and weekly (Monday 05:00 UTC).

| Step | Purpose |
|---|---|
| Set up Java 21 (Temurin) with Maven cache | Same JDK as the project target |
| Restore `.testora/` cache | Brings back execution history so Trends and flaky detection work |
| `mvn verify -Dgroups=smoke` | Compiles everything and runs the offline platform tests (no browser, device, application or AI) |
| Save `.testora/` cache | Keeps history for the next run (always, even on failure) |
| Upload `testora-report` | `report.html`, `report.jsonl`, `audit.jsonl`, evidence |
| Upload `surefire-reports` | Standard JUnit XML and text results |
| Weekly `dependency-report` job | Lists available dependency and plugin updates as an artifact |

The badge at the top of `README.md` shows whether the last run on the default branch passed.

## Dependabot (`.github/dependabot.yml`)

Weekly pull requests for Maven dependencies and GitHub Actions. Selenium and Appium updates are grouped into one PR, so you can check that they still work together. JUnit, RestAssured and AssertJ are grouped too. Review each PR: the build must be green before merging.

## Run the real (Web / API / Mobile) tests in CI

The default job runs only tests tagged `smoke` that need nothing external. To run browser tests, add a second job with a Selenium Grid service and your application URL:

```yaml
  web-tests:
    runs-on: ubuntu-latest
    services:
      selenium:
        image: selenium/standalone-chrome:latest
        ports: ["4444:4444"]
        options: --shm-size=2g
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21, cache: maven }
      - run: mvn -B -ntp test -Denv=qa -Dgroups=web -Dgrid.url=http://localhost:4444 -Dbase.url=${{ vars.QA_BASE_URL }}
        env:
          DEMO_PASSWORD: ${{ secrets.DEMO_PASSWORD }}
```

Store secrets under repository **Settings → Secrets and variables → Actions**. Never write them in the workflow file. Mobile tests need an Appium server and a device or emulator, which GitHub-hosted runners do not provide by default; use a device cloud or a self-hosted runner.

## History between runs

The cache key contains the run ID so every run saves a new copy, and `restore-keys` restores the latest one for the same branch. GitHub deletes caches not used for 7 days and limits total cache size, so history can disappear after long pauses. For long-term history, upload `.testora/history.jsonl` to durable storage instead.

## GitLab CI and Jenkins

`.gitlab-ci.yml` and `Jenkinsfile` run the same smoke command and archive `target/testora`. To keep history, add `.testora/` to GitLab `cache` paths, or archive it in Jenkins and copy it back from the last successful build.

## If the first run fails

The project was written without a build being available, so the first CI run may report compile errors or dependency conflicts. That is the purpose of the job. Open the run, read the Maven error, and fix it. For Selenium and Appium version conflicts, align `selenium.version` and `appium.version` in `pom.xml`.
