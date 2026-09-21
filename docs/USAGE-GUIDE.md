# Address Lookup API — Usage Guide

**Audience:** any CP engineering team that needs validated UK addresses and wants to integrate
against this API without reading the service's source code.

**Source of truth:** `src/main/resources/openapi/address-lookup-api.openapi.yml`. This guide is a
narrative walkthrough of that spec plus the operational facts a caller needs that don't fit inside
an OpenAPI document (deployment topology, caching, timeouts). If anything here ever disagrees with
the spec, the spec wins — file an issue against this guide.

This repository is a **contract-only library** — spec, generated interfaces/models, and
contract-verification tests. The runtime service that actually implements this contract (calls OS
Places, applies the policies described below) is built and deployed from a separate repository.
Everything in this guide describes the contract that runtime service is required to honour.

---

## 1. Operations

Three operations, each mapping 1:1 to a single OS Places backend operation:

| Operation | Method & path | Required params | Optional params |
|---|---|---|---|
| Search by postcode | `GET /addresses/postcode` | `postcode` | `include` |
| Search by free text | `GET /addresses` | `address` | `include` |
| Match / validate | `GET /addresses/find` | `address` | `minMatch` |

**None of the three passes through OS Places' full parameter set.** OS Places itself accepts
several other query parameters on its `/postcode` and `/find` operations (`dataset`, `bbox`,
`minmatch`, `matchprecision`, `lr`, `output_srs`, `fq`, `offset`, `maxresults`) — this API
deliberately exposes only what's listed above. **Every operation rejects any query parameter
outside its own declared set with `400`**, before any upstream call is made — it does not
silently ignore parameters it doesn't recognise. If you send `bbox` to `/addresses/postcode`
expecting a geographic filter, you get a `400`, not a request that quietly ignored `bbox`.

### 1.1 `GET /addresses/postcode`

| Param | Type | Required | Constraints | Notes |
|---|---|---|---|---|
| `postcode` | string | yes | max length 10 | Full or partial UK postcode — OS Places handles partial-postcode matching natively. |
| `include` | string enum | no | `dpa` | Nests the raw OS Places DPA record on each candidate. |

### 1.2 `GET /addresses`

| Param | Type | Required | Constraints | Notes |
|---|---|---|---|---|
| `address` | string | yes | max length 200 | Free text, forwarded to OS Places as-is. May include a postcode inline (e.g. `"10 Downing Street SW1A 1AA"`) to improve relevance — **this is not a strict postcode filter**; OS Places' free-text matching can still return a candidate whose postcode differs if its other terms score highly enough. |
| `include` | string enum | no | `dpa` | Same as above. |

There is no server-side combining logic on this operation — whatever you put in `address` is what
OS Places searches on.

### 1.3 `GET /addresses/find`

| Param | Type | Required | Constraints | Notes |
|---|---|---|---|---|
| `address` | string | yes | max length 200 | Free-text address string to resolve/validate. |
| `minMatch` | number | no | `0.1`-`1.0` inclusive | Minimum OS Places match score a candidate must reach to be returned. Mirrors OS Places' own `minmatch` constraint — note `0` is **not** a valid value. |

Use this operation, not `GET /addresses`, when you need a match-score floor.

### `dataset` is fixed, not client-selectable

**All three operations always query OS Places with `dataset=DPA`.** You cannot ask for `LPI` or
`DPA,LPI`. Consequence: **LPI-only records — addresses that exist in OS Places' LPI dataset but
have no corresponding DPA entry — are never returned by this API**, on any operation. If an
address you expect to see doesn't appear in results, this is worth checking before assuming a bug.

### SJP folding is not currently supported

An earlier design draft specified a `variant=sjp` parameter that would fold each candidate to a
4-line/32-character format. **This was removed from the contract as an unconfirmed requirement and
is not present in the current spec.** There is no `variant` parameter on any operation. If your
integration needs this, raise it as a fresh requirement — don't assume it exists.

---

## 2. The canonical `Address` schema

Every operation returns the same wrapper and candidate shape:

```json
{
  "results": [
    {
      "line1": "10",
      "line2": "Downing Street",
      "postcode": "SW1A 1AA",
      "uprn": "10033544886"
    }
  ]
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `line1` | string | yes | Primary address line. Passed through from OS Places as-is — no length restriction imposed by this contract. |
| `line2`-`line4` | string | no | Additional address lines, omitted when not applicable. |
| `line5` | string | no | Fifth address line, omitted when not applicable. |
| `postcode` | string | yes | As returned by OS Places — no length or format restriction imposed by this contract. |
| `uprn` | string | yes | OS Places Unique Property Reference Number. Pattern `^[0-9]{1,12}$`. |
| `match` | number | no | OS Places match score (0-1). **Only populated by `/addresses/find`** — never present on `/addresses/postcode` or `/addresses` results. |
| `dpa` | object | no | Raw OS Places DPA record. **Only present when `include=dpa` was requested** — key material is always stripped from it regardless. |

`AddressCandidate` has `additionalProperties: false` — don't build a client that expects fields
beyond the ones listed here.

---

## 3. Response classes

Every operation can return exactly these response shapes. There is no response type that exists
only in service code and not here.

### 200 — success (with or without results)

A zero-result search or match is **not an error** — it's a normal `200` with `"results": []`.
Don't treat an empty array as a failure condition in your integration.

```json
{ "results": [] }
```

### 400 — invalid request

Covers three distinct causes, distinguishable by the `message` field (not by a separate error
code — `error` is just the HTTP status as a string):

1. A required parameter is missing (`postcode` on `/addresses/postcode`, `address` on the other two).
2. A parameter exceeds its declared length/range (e.g. `address` over 200 chars, `minMatch` outside `0.1`-`1.0`).
3. **The request carries a query parameter this operation doesn't declare** (e.g. `bbox`, `minmatch` on `/addresses/postcode`) — rejected before any upstream call, per §1.

```json
{
  "error": "400",
  "message": "'postcode' is required.",
  "timestamp": "2025-01-01T11:11:11Z",
  "traceId": "a1b2c3d4e5f6g7h8"
}
```

### 503 — degraded

Returned when OS Places couldn't serve the request, or the circuit breaker is already open. Never
returned for zero-result searches. `reason` is one of:

| `reason` | Meaning |
|---|---|
| `upstream-timeout` | OS Places did not respond within the 10s budget (see §5). |
| `upstream-server-error` | OS Places returned a 5xx. |
| `upstream-rate-limit` | OS Places rate limit exceeded. `retryAfterSeconds` may be populated. |
| `upstream-auth` | The service's OS Places API key was rejected. |
| `upstream-contract` | OS Places returned a response this service couldn't parse (early-warning signal for OS Places API drift). |
| `circuit-open` | The circuit breaker was already open — no upstream call was even attempted. |

```json
{ "degraded": true, "reason": "upstream-rate-limit", "retryAfterSeconds": 30 }
```

### 401 — not part of this API's own contract

**None of this API's three operations declares a `401` response**, and there's no authentication
scheme defined on this API at all. If you receive a `401`, it came from the **platform gateway**
in front of the service, rejecting an unauthenticated request before it ever reached this API —
not from the address-lookup logic itself. See §4 for the call path this implies.

---

## 4. Consumption paths

There are two distinct ways to reach this service, and no API-Management-style subscription key
in either:

- **Browser, same-origin.** The reference consumer (a CP case UI) calls `/api/address-lookup/...`
  same-origin; the platform (IDAM) gateway sits in front, authenticates the request, and injects a
  `CJSCPPUID` header before routing in-cluster to the service. A missing/invalid session here is
  what produces the `401` described in §3 — it never reaches this API's own logic.
- **In-cluster callers.** Another in-mesh service can call the address-lookup service directly,
  the same way it would call any other in-cluster HTTP service. There is **no APIM layer and no
  subscription key** in this architecture — that's a deliberate change from the superseded
  APIM-hosted design. A direct in-cluster caller bypasses the gateway's authentication entirely, so
  `CJSCPPUID` may be absent; the service is designed to still serve such requests (logged without
  it), not reject them outright — this API defines no authorization of its own.

---

## 5. Operational facts

⚠️ **These describe the designed behaviour of the runtime service per
`design-address-lookup-springboot.md`. The runtime service is a separate, not-yet-built repository
— nothing here is verifiable by running code in *this* repository.** They're recorded here so a
consumer doesn't have to track down that document to plan an integration.

| Fact | Value |
|---|---|
| Upstream timeout | 10 seconds, total budget per OS Places call. Exceeding it produces `upstream-timeout` (§3). |
| Response cache | In-process (Caffeine), TTL 10 minutes, keyed on normalised request parameters. Per-replica — a cold cache on one pod doesn't imply a warm cache on another. |
| Per-environment OS backend | STE / DEV / NFT: an in-cluster WireMock stub with a fixture corpus — **no real OS Places egress, no API key needed**. SIT / PRP / PRD: the real `https://api.os.uk`, with a live key. Don't expect real-world OS Places data in lower environments. |
| Rate limiting | Not implemented in the current phase. The only backpressure today is OS Places' own platform caps, surfaced as `upstream-rate-limit`. |
| Auditing / bespoke metrics | Not implemented in the current phase (deferred). |

---

## 6. Worked examples

### Search by postcode

```
GET /addresses/postcode?postcode=SW1A%201AA
```
```json
{ "results": [ { "line1": "10", "line2": "Downing Street", "postcode": "SW1A 1AA", "uprn": "10033544886" } ] }
```

### Search by free text, narrowed by an inline postcode

```
GET /addresses?address=10%20Downing%20Street%20SW1A%201AA
```
Same response shape as above — remember this is relevance-improving, not a guaranteed filter (§1.2).

### Match / validate with a score floor

```
GET /addresses/find?address=10%20Downing%20Street%2C%20London%20SW1A%201AA&minMatch=0.7
```
```json
{ "results": [ { "line1": "10", "line2": "Downing Street", "postcode": "SW1A 1AA", "uprn": "10033544886", "match": 0.95 } ] }
```

### No results (not an error)

```
GET /addresses/postcode?postcode=ZZ99%209ZZ
```
```json
{ "results": [] }
```

### Unrecognised parameter

```
GET /addresses/postcode?postcode=SW1A%201AA&bbox=1,2,3,4
```
```json
{ "error": "400", "message": "Unsupported parameter 'bbox'.", "timestamp": "...", "traceId": "..." }
```

### Upstream degraded

```
GET /addresses/find?address=10+Downing+Street
```
```json
{ "degraded": true, "reason": "upstream-timeout" }
```

---

## 7. How this contract is validated

- `gradle/openapi.gradle`'s `openApiGenerate` task runs with `validateSpec: true`, so the spec is
  validated structurally on every build (`compileJava` depends on `openApiGenerate` — this isn't
  an opt-in step).
- CI (`.github/workflows/lint-openapi.yml`) runs Spectral against `.spectral.yml` on every PR, and
  separately bans any internal HMCTS URL (`cjscp.org.uk`, `service.gov.uk`, `justice.gov.uk`,
  `hmcts.net`, `ejudiciary.net`) appearing anywhere in the spec.
- The three test classes under `src/test/java/uk/gov/hmcts/cp/openapi/model/al/` assert the
  parsed spec's shape, the generated model classes, and JSON (de)serialisation directly — see
  `README.md` for what each one covers.
- Versioning follows `docs/API-VERSIONING-STRATEGY.md` (media-type/SemVer) and
  `docs/OPENAPI-SPEC-VERSIONING.md` (how `info.version` is stamped at build/release time).

---

## 8. Known gaps (don't build against these as if they exist)

- **SJP folding / `variant` parameter** — not in the current contract (§1).
- **Per-user rate limiting, bespoke outcome metrics, audit logging** — deferred; see §5.
- **Global request quota** — no mechanism designed yet; OS Places' own platform caps are the only
  guard today.
