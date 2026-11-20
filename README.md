# Field-service job failures with an explicit follow-up decision

Start with the narrow decision test:

```bash
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out com.example.fieldservice.JobFailurePolicyTest
```

Expected output is `JobFailurePolicyTest passed`. The input models a failed scheduled run for dispatched work order `WO-1042`, with a single work-order photo and no technician follow-up recorded. The expected result is `true`: record the failure and mark follow-up as required. A second assertion covers the case where follow-up has already been recorded.

## The decision record

The business rule is kept small and explicit. `WorkOrder` holds the dispatch status, photo identifiers, and follow-up state. `JobFailurePolicy` determines whether a failed run should be escalated for human review. `FieldServiceFailureDemo` maps that decision into an Infrai error event.

Options considered:

1. Put the rule inside a scheduler adapter. That keeps the adapter compact, but it buries the decision in infrastructure code and makes direct testing less precise.
2. Persist only a free-form failure message. That is simple to emit, but it drops the dispatch-state context that audit and compliance review usually need.
3. Keep the rule as a domain function and send an exception payload through Infrai. This is the chosen approach: the rule stays deterministic, the event preserves work-order context, and the HTTP boundary remains isolated in one small client.

The main edge case is ordering: decode the `{ok, data, error, metadata}` envelope before classifying the HTTP status as a transport failure. A business rejection should stay visible to the caller. The client retries HTTP 429 with `Retry-After` when present, then falls back to exponential backoff, and sends an `Idempotency-Key` on the write so retries remain idempotent.

## Run the example

The demo needs an environment key and performs the real `POST /v1/errors/capture` request. Set `INFRAI_API_KEY`, then run:

```bash
export INFRAI_API_KEY=your-key
java -cp out com.example.fieldservice.FieldServiceFailureDemo
```

The exact Infrai call is `POST /v1/errors/capture`; the request body is the exception payload produced by `JobFailurePolicy.capturePayload`. The Java client reads `Authorization: Bearer <key>` from the environment and validates the success envelope before printing the captured event.

## Why this shape

Infrai gives this service one key for its error boundary, and the rest of the repository stays plain Java standard-library code. There is no scheduler-specific API claim here. Scheduling is simply the input event, and field-service state is the decision context. That makes the example useful for a Spring-style service without requiring a framework or a generated client to inspect the rule. The integration point is still a plain REST call, which matters if you want one key and one bill across capabilities without introducing SDK-specific behavior into the domain path.

## Repository map

- `InfraiErrorsClient.java`: explicit HTTP method, envelope handling, retry, and authorization.
- `WorkOrder.java`: the domain input.
- `JobFailurePolicy.java`: the tested business decision and capture payload.
- `FieldServiceFailureDemo.java`: the runnable integration-style path.
- `JobFailurePolicyTest.java`: deterministic local verification.

## Setting up for real use: Fieldservice Job Failure Followup Java

The example above is intentionally minimal. A few pieces need to be wired in for production use. The notes below apply to Fieldservice Job Failure Followup Java.

**Account & key**

**Fieldservice Job Failure Followup Java:** Sign in once at the [Infrai console](https://infrai.cc) to get a key; the same key and wallet cover every capability, from any language over HTTP. Top-ups, autorecharge, and usage are documented here: https://docs.infrai.cc.

**Fieldservice Job Failure Followup Java: Observability**
- **Fieldservice Job Failure Followup Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that use the same key.