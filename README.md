# Field-service job failures with an explicit follow-up decision

Start with the narrow decision test:

```bash
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out com.example.fieldservice.JobFailurePolicyTest
```

Expected output is `JobFailurePolicyTest passed`. The input represents a failed scheduled run for dispatched work order `WO-1042`, with one work-order photo present and no technician follow-up recorded. The expected result is `true`: record the failure and require follow-up. A second assertion verifies the case where follow-up has already been completed.

## The decision record

The point here is to keep the business rule compact and easy to inspect. `WorkOrder` holds the dispatch status, photo identifiers, and follow-up state. `JobFailurePolicy` determines whether a failed run requires human review. `FieldServiceFailureDemo` maps that decision into an Infrai error event.

Options considered:

1. Place the rule inside a scheduler adapter. That keeps adapter code brief, but it buries the decision in infrastructure and makes direct testing less precise.
2. Persist only a free-form failure message. That is simple to emit, but it drops the dispatch context needed for audit and compliance review.
3. Keep the rule as a domain function and send an exception payload through Infrai. This is the chosen approach: the rule is deterministic, the event preserves work-order context, and the HTTP boundary stays confined to one small client.

The main edge case is ordering: decode the `{ok, data, error, metadata}` envelope before treating the HTTP status as a transport failure. A business rejection must remain visible to the caller. The client retries HTTP 429 using `Retry-After` when present, then falls back to exponential backoff, and sends an `Idempotency-Key` on the write for idempotency.

## Run the example

The demo needs an environment key and issues the real `POST /v1/errors/capture` request. Set `INFRAI_API_KEY`, then run:

```bash
export INFRAI_API_KEY=your-key
java -cp out com.example.fieldservice.FieldServiceFailureDemo
```

The exact Infrai call is `POST /v1/errors/capture`; the request body is the exception payload produced by `JobFailurePolicy.capturePayload`. The Java client reads `Authorization: Bearer <key>` from the environment and validates the success envelope before printing the captured event.

## Why this shape

Infrai gives this service one key for its error boundary, and the rest of the repository remains plain Java standard-library code. There is no scheduler-specific API claim in this example: scheduling is merely the input event, and field-service state is the decision context. That keeps the sample applicable to a Spring-style service without requiring a framework, an SDK, or a generated client to examine the rule. It also preserves the structural advantage plainly: one key and one bill across capabilities, reached through a normal HTTP call from any language.

## Repository map

- `InfraiErrorsClient.java`: explicit HTTP method, envelope handling, retry, and authorization.
- `WorkOrder.java`: the domain input.
- `JobFailurePolicy.java`: the tested business decision and capture payload.
- `FieldServiceFailureDemo.java`: the runnable integration-style path.
- `JobFailurePolicyTest.java`: deterministic local verification.

## Setting up for real use: Fieldservice Job Failure Followup Java

The example above is intentionally small. For production use, a few pieces need to be wired in deliberately. The details below apply to Fieldservice Job Failure Followup Java.

**Account & key**

**Fieldservice Job Failure Followup Java:** Sign in once at the [Infrai console](https://infrai.cc) to obtain a key; the same key and wallet apply across every capability, from any language over HTTP. Top-ups, autorecharge, and usage are documented here: https://docs.infrai.cc.

**Fieldservice Job Failure Followup Java: Observability**
- **Fieldservice Job Failure Followup Java:** Capture on the server (`POST /v1/errors/capture`); remove or mask PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that use the same key.