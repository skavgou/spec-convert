# SpecConvert — Conversion Logic Notes

CNCF Serverless Workflow **0.8 → 1.0** | `src/main/java/com/specconvert/SpecConvert.java`

---

## Processing Pipeline

```
1. Read  →  2. Convert  →  3. Serialise  →  4. Write / stdout
```

The input file is parsed into a Jackson `JsonNode` tree (JSON or YAML). The `convert()` method builds a **new** output tree — nothing from the source is mutated. The output is serialised back to the same format as the input, inferred from the file extension.

---

## Top-level Structure

The 0.8 document is a flat object. The 1.0 document wraps everything inside two top-level keys: `document` and `do`.

**0.8 input**
```json
{
  "id": "helloworld",
  "version": "1.0",
  "specVersion": "0.8",
  "namespace": "default",
  "states": [ ... ]
}
```

**1.0 output**
```json
{
  "document": {
    "dsl": "1.0.0",
    "namespace": "default",
    "name": "helloworld",
    "version": "1.0"
  },
  "do": [ ... ]
}
```

---

## `document` Block — Field Mappings

| 0.8 field                          | 1.0 field            | Notes                                        |
|------------------------------------|----------------------|----------------------------------------------|
| `specVersion`                      | `document.dsl`       | Currently hard-coded to `"1.0.0"`            |
| `id`                               | `document.name`      | Direct copy                                  |
| `namespace`                        | `document.namespace` | Set to `"default"` if absent                 |
| `version`                          | `document.version`   | Direct value copy                            |

---

## `do` Block — State Conversion

The 0.8 `states` array becomes a 1.0 `do` array. Each state becomes a single-key object keyed by the state's `name`.

| 0.8 state type | 1.0 equivalent              | How                                                                                                       |
|----------------|-----------------------------|-----------------------------------------------------------------------------------------------------------|
| `"inject"`     | `{ set: { ...data } }`      | The state's `data` object is copied directly into a `set` wrapper.                                        |
| `"sleep"`      | `{ wait: { seconds: N } }`  | The `duration` ISO 8601 string (e.g. `PT5S`) is parsed into total seconds. Defaults to `PT0S` if absent. |
| `"switch"`     | `{ switch: [ ... ] }`       | Each `dataConditions` entry becomes a named case. EL expressions (`${ ... }`) are stripped and flagged, may need to be manually handled. Not complete. |
| `"callback"`   | `{ do: [ call, listen, switch ] }` | A three-step `do` task: (1) the outgoing `action` becomes a call task, (2) a `listen` task waits for the `eventRef` CloudEvent, (3) a `switch` task routes on the received event type. |

---

## State Conversion Examples

### `inject` → `set`

```json
// 0.8
{
  "name": "Hello State",
  "type": "inject",
  "data": { "result": "Hello World!" }
}

// 1.0
{
  "Hello State": {
    "set": { "result": "Hello World!" }
  }
}
```

### `sleep` → `wait`

```json
// 0.8
{
  "name": "SleepFiveSeconds",
  "type": "sleep",
  "duration": "PT5S"
}

// 1.0
{
  "SleepFiveSeconds": {
    "wait": { "seconds": 5 }
  }
}
```

### `switch` → `switch`

```json
// 0.8
{
  "name": "CheckApplicant",
  "type": "switch",
  "dataConditions": [
    { "name": "Applicant is adult", "condition": "${ fn:isAdult }", "transition": "ApproveApplication" }
  ],
  "defaultCondition": { "transition": "RejectApplication" }
}

// 1.0
{
  "CheckApplicant": {
    "switch": [
      {
        "applicantIsAdult": {
          "when": "fn:isAdult",
          "_warning": "EL expression 'fn:isAdult' requires manual translation to jq syntax.",
          "then": "ApproveApplication"
        }
      },
      {
        "default": { "then": "RejectApplication" }
      }
    ]
  }
}
```

Condition names are **camelCased** for use as YAML keys (e.g. `"Applicant is adult"` → `applicantIsAdult`).

---

### `callback` → `do[call + listen + switch]`

```json
// 0.8
{
  "name": "RequestVitals",
  "type": "callback",
  "action": {
    "name": "sendVitalsRequest",
    "functionRef": { "refName": "sendVitalsRequest", "arguments": { "patientId": "${ .patientId }" } }
  },
  "eventRef": "VitalsReceived",
  "transition": { "nextState": "ProcessVitals" }
}

// 1.0
{
  "RequestVitals": {
    "do": [
      {
        "sendVitalsRequest": {
          "call": "sendVitalsRequest",
          "with": { "patientId": "${ .patientId }" }
        }
      },
      {
        "RequestVitalsListen": {
          "listen": {
            "to": {
              "any": [{ "with": { "type": "com.hospital.vitals.received" } }]
            }
          }
        }
      },
      {
        "RequestVitalsRoute": {
          "switch": [
            {
              "callbackReceived": {
                "when": "${ .type == \"com.hospital.vitals.received\" }",
                "then": "ProcessVitals"
              }
            },
            {
              "default": { "then": "end" }
            }
          ]
        }
      }
    ]
  }
}
```

The outer `do` task preserves the sequential semantics of the original callback state:
- The call task fires first (outgoing trigger).
- The listen task blocks until the matching CloudEvent arrives.
- The switch task confirms the event type and routes to the transition target. The `default` branch handles any unexpected outcome by ending the flow segment.

If the callback state has no `action`, the call task step is omitted and the `do` contains only the listen and switch tasks.

---

## EL Expression Handling

0.8 conditions written as `${ ... }` (EL/JEXL expressions) are **not valid jq** in 1.0. The converter:

1. Strips the `${ }` wrapper from the `when` value.
2. Injects a `_warning` field into the output case.
3. Prints a `[WARN]` line to stderr.

These conditions require **manual translation** to jq syntax before the workflow will run correctly.

---

## ISO 8601 Duration Parsing (`sleep` states)

Durations are broken down with a regex into years, months, days, hours, minutes, and seconds, then summed to a total-seconds integer. Months are approximated as **30 days**; years as **365 days**.

| Input        | Output `seconds` |
|--------------|-----------------|
| `PT5S`       | `5`             |
| `PT1H30M`    | `5400`          |
| `P2DT3H4M`   | `183840`        |
