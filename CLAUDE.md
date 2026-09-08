# api-cp-crime-address-lookup

## What this repo is

**A contract-only OpenAPI library.** It contains the Address Lookup API's OpenAPI spec,
openapi-generator-produced server interfaces/models, and contract-verification tests — published as
a jar to GitHub Packages / Azure Artifacts. Same shape as
`api-cp-crime-caseadmin-case-document-knowledge`.

## What NOT to do

- **Do not add `@SpringBootApplication`, `@RestController`, a service layer, an HTTP client, or any
  business logic here.** This is a library, not a runtime service. The full Spring Boot service
  that implements this contract (OS Places integration, caching, resilience, auth) is designed in
  `design-address-lookup-springboot.md` (`cp-meta-arch` repo) and belongs in a **separate** service
  repository — see that document's "Implementation note" for the current split.
- Never commit `build/generated/`.
- Never set `useLombok: true` in `gradle/openapi.gradle`'s generator config — it clashes with the
  generator's own constructors.
- Never hand-edit `info.version: 0.0.0` in the spec — CI stamps the real version at build time.
- Exactly one `*.openapi.yml` file may exist under `src/main/resources/openapi/` — the Gradle build
  fails otherwise.

## Adding or changing an endpoint/schema

1. Edit `src/main/resources/openapi/address-lookup-api.openapi.yml`.
2. Update all three test classes together, under
   `src/test/java/uk/gov/hmcts/cp/openapi/model/al/`:
   - `GeneratedApiContractsExistTest` — assertions against the parsed spec (required/type/format/enum).
   - `GeneratedModelContractsExistTest` — reflection checks on generated classes/accessors/enums.
   - `GeneratedObjectMappingTest` — Jackson round-trip tests.
3. `./gradlew clean build` — must pass (compiles generated code, formats it via Spotless, runs tests,
   jacoco). Run `./gradlew pmdMain` explicitly too (it's excluded from the default `build`/`check`
   lifecycle in this repo's `gradle/pmd.gradle`, but CI's `code-analysis.yml` runs it).
