# Address Lookup API

**Repository:** api-cp-crime-address-lookup

**Purpose:** This repository contains the OpenAPI specification, generated server interfaces/models, and contract-verification tests for the Address Lookup API — a Common Platform API that looks up and validates UK addresses (via OS Places) and returns them in the canonical CP address contract (`address1`-`address5`, `postcode`, `uprn`).

**This repository is a contract-only library.** It does **not** contain a runnable service: no controllers, no service layer, no OS Places HTTP client, no business logic. It publishes a jar of the generated request/response interfaces and models to GitHub Packages / Azure Artifacts, for a separate runtime service repository to depend on and implement.

For the full functional/non-functional design (validation rules, OS Places integration behaviour, auth, resilience, caching), see
`design-address-lookup-springboot.md` in the `cp-meta-arch` design-change repository.

---

## Quick links

* OpenAPI spec: `src/main/resources/openapi/address-lookup-api.openapi.yml`
* Generated sources: `build/generated/src/main/java` (populated by `openApiGenerate`, not committed)
* Main Gradle tasks:

    * `./gradlew openApiGenerate` — generate API interfaces & models from the OpenAPI spec
    * `./gradlew clean build` — full build (generate → format → compile → test → jacoco)
    * `./gradlew test` — run tests

---

## API summary (contract highlights)

* `GET /addresses/postcode` — search by `postcode` (required). Thin pass-through to OS Places' `/postcode` operation.
* `GET /addresses` — search by free-text `address` (required; may include a postcode inline for narrower relevance). Thin pass-through to OS Places' `/find` operation.
* `GET /addresses/find` — match a free-text `address` string against OS Places, with an optional `minMatch` score floor.

Each of the three maps 1:1 to one underlying OS Places operation and accepts only its own declared parameters (plus `include` on the two search endpoints) — a request carrying any other query parameter is rejected with `400`, not silently ignored.

All three endpoints return `200` with a `results` array (possibly empty — a zero-result search is not an error), `400` on invalid input, or `503` with a `DegradedResponse` when OS Places is unavailable/degraded/circuit-open.

See the spec file for full parameter, schema, and example detail.

---

## OpenAPI & generation notes

1. **Spec file name**: `address-lookup-api.openapi.yml` (under `src/main/resources/openapi`). Exactly one `*.openapi.yml` file must exist — `gradle/openapi.gradle` fails the build otherwise.
2. **Generator configuration**: OpenAPI Generator `spring`, interface-only, `useLombok: false` on generated models (avoids duplicate-constructor clashes) — see `gradle/openapi.gradle`. Lombok is for hand-written code only (`OpenAPIConfigurationLoader`).
3. **Regenerate** after editing the spec:

```bash
./gradlew clean openApiGenerate spotlessApply build
```

Spotless formats generated sources only; it runs automatically before `compileJava`.

---

## Building & testing

```bash
./gradlew clean build
```

Tests live under `src/test/java/uk/gov/hmcts/cp/openapi/model/al/` and cover three things, kept in sync whenever the spec changes:

* `GeneratedApiContractsExistTest` — asserts required/type/format/enum values directly against the parsed OpenAPI spec.
* `GeneratedModelContractsExistTest` — reflection checks that generated model classes and accessors exist, and enum values are as expected.
* `GeneratedObjectMappingTest` — Jackson (de)serialisation round-trips for each schema.

`API_SPEC_VERSION` (system property, defaults to `0.0.0`) controls the `info.version` the CI pipeline stamps into the spec at build time — see `docs/OPENAPI-SPEC-VERSIONING.md`.

---

## Contributing

When adding or changing an endpoint/schema: update the spec, then update all three test classes above together. Follow `.github/CONTRIBUTING.md` for PR and branching rules.

---

## License

This project is licensed under the [MIT License](LICENSE).
