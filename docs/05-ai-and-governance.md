# 05 - AI and Governance

## Principles

1. AI is optional. With `ai.mode=offline` (the default) TESTORA never contacts any AI service.
2. Rules run first. AI is consulted only when rule-based confidence is below `ai.analysis.threshold`.
3. AI output is labelled `[AI-INFERENCE, not verified]` and is never treated as fact.
4. Prompts ask the model to separate FACT, INFERENCE, RECOMMENDATION and UNKNOWN, and to answer `UNKNOWN - Insufficient Evidence` when evidence is missing.
5. Secrets are masked before any prompt leaves the machine.
6. Every AI call is written to `target/testora/audit.jsonl`.

## AI modes

| `ai.mode` | Behaviour |
|---|---|
| `normal` | Gateway may call available providers when rules are unsure |
| `ai-unavailable` | Treated as no AI. Levels 1 and 2 only |
| `offline` (default) | No AI. Nothing is sent anywhere |

## AI Gateway

The framework talks only to `AiGateway`. It provides:

| Control | Setting |
|---|---|
| Timeout | `ai.timeout.seconds` |
| Prompt size limit | `ai.max.prompt.chars` |
| Call budget | `ai.max.calls` |
| Provider fallback | Providers sorted by `priority()`, lowest first. The next is tried if one fails |
| Audit | One record per call: purpose, provider, model, tokens, latency, outcome |

No provider ships with the framework. To connect one:

1. Implement `com.testora.ai.providers.AiProvider`:

   ```java
   public class MyProvider implements AiProvider {
       public String name() { return "my-provider"; }
       public boolean isAvailable() { return System.getenv("MY_AI_KEY") != null; }
       public int priority() { return 10; }   // cheaper or local models first
       public AiResponse complete(AiRequest request) throws Exception { /* call your service */ }
   }
   ```
2. Register it in `src/main/resources/META-INF/services/com.testora.ai.providers.AiProvider` (one class name per line).
3. Run with `-Dai.mode=normal`.

Do not enable a provider for production or customer data without your organization's approval.

## Failure classification (rules)

`FailureClassifier` maps a failure to one of:

`APPLICATION_DEFECT`, `AUTOMATION_DEFECT`, `ENVIRONMENT_ISSUE`, `SYNCHRONIZATION_ISSUE`, `TEST_DATA_ISSUE`, `INFRASTRUCTURE_ISSUE`, `AUTHENTICATION_ISSUE`, `UNKNOWN`.

Each result has a category, a rule confidence, the evidence sentence, and a **fingerprint**. Failures with the same category, exception type and normalized message share a fingerprint, so one root cause appears as one group in the report. Confidence is rule strength, not a statistical probability. Not every category can be produced by the current rules yet.

## Confidence policy

| Confidence | Default action |
|---|---|
| At or above `governance.auto.threshold` (0.85) | Automatic action allowed |
| Between review (0.60) and auto | Human review required |
| Below `governance.review.threshold` | Recommendation only |

Thresholds are configuration. Tune them for your organization.

## Controlled locator recovery

Disabled by default. Enable with `-Drecovery.enabled=true`.

When a wait times out and the element declared `.withFallback(...)` locators:

1. A fallback that matches exactly one visible element is a candidate (rule confidence 0.90).
2. The confidence policy decides: automatic, human review, or recommendation only.
3. The original failure, the candidate, the confidence, the evidence and the decision are written to `audit.jsonl`.
4. Automatic recovery counts in `recoveries` and appears in the report. Review cases are queued in the approval queue and the test still fails.

Recovery does **not** edit your code. Fix the locator after reviewing the audit record.

## Human approval

`ApprovalQueue.request(change, evidence, confidence)` records a `PENDING` approval in `audit.jsonl`. Changes to assertions, expected results, business rules, test logic or test deletion must always be made by a person. Nothing in TESTORA applies them automatically.

## Audit record format

Each line of `audit.jsonl` is JSON, for example:

```json
{"timestamp":"...","action":"LOCATOR_RECOVERY","element":"Login Button","originalLocator":"By.id: loginButton",
 "candidateLocator":"By.cssSelector: button[type=submit]","confidence":0.9,
 "evidence":"declared fallback matched exactly one visible element","decision":"AUTOMATIC","testId":"LoginTest.login"}
```
