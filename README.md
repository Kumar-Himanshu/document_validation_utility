# Document Validation Utility (PAN, Card, Aadhaar/VID)

A stateless Spring Boot service that performs **offline** format and checksum
validation of identity and financial identifiers. It validates three document
types today — PAN, credit/debit card (Luhn) and Aadhaar/VID (Verhoeff) — behind
a single API, and is designed so new types can be added by writing one new
validator class, with no change to the API contract.

> This utility checks **format and checksum integrity only**. A `true` result
> does **not** prove that a document exists, is active, or belongs to anyone. It
> does not contact the Income Tax Department, UIDAI or any card issuer.

## Contents

- [API](#api)
- [Architecture](#architecture)
- [SOLID design](#solid-design)
- [How to run](#how-to-run)
- [How to test](#how-to-test)
- [Coverage gate](#coverage-gate)
- [Adding a new document type](#adding-a-new-document-type)
- [Production-readiness features](#production-readiness-features)
- [Load testing](#load-testing)
- [Assumptions](#assumptions)
- [BRD gaps and risks](#brd-gaps-and-risks)

## API

### `POST /api/v1/validate`

Request:

```json
{ "documentType": "PAN", "documentNumber": "ABCPE1234F" }
```

Response (identical structure for every type):

```json
{ "documentNumber": "ABCPE1234F", "isValid": true }
```

- `documentType` is case-insensitive. `VID` is an alias routed to the
  Aadhaar/VID (Verhoeff) validator.
- `documentNumber` is echoed back exactly as received.
- A value that simply fails validation returns **HTTP 200** with
  `isValid: false` — this is **not** an error.

### `GET /api/v1/validate/types`

Returns the list of supported canonical types, e.g. `["AADHAAR","CARD","PAN"]`.

### Error responses

| Scenario                                   | Status | Body                                            |
|--------------------------------------------|--------|-------------------------------------------------|
| Blank/missing `documentNumber`             | 400    | Structured error (see below)                    |
| Missing/blank/unsupported `documentType`   | 400    | Message lists supported types                   |
| Missing/malformed request body             | 400    | Structured error                                |
| Unexpected failure                         | 500    | Generic message, no sensitive data              |

Error body shape:

```json
{
  "timestamp": "2026-10-24T10:15:30.123+05:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Unsupported documentType. Supported types: PAN, CARD, AADHAAR, VID",
  "path": "/api/v1/validate"
}
```

### Validation rules

- **PAN** — trimmed and upper-cased, then matched against
  `^[A-Z]{3}[PCHFATBGLJ][A-Z][0-9]{4}[A-Z]$`; the 4-digit numeric block must not
  be `0000`. The 10th character is only checked for being a letter (the check-
  letter formula is not public and is deliberately not recomputed).
- **Card** — spaces and hyphens stripped, must then be 13–19 digits, and must
  pass the Luhn (mod-10) checksum.
- **Aadhaar/VID** — spaces and hyphens stripped, must then be 12 (Aadhaar) or
  16 (VID) digits, must not start with `0` or `1`, and must pass the Verhoeff
  checksum.

### API documentation

With the service running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Architecture

```
controller   ValidationController          HTTP only; delegates to the service
service      DocumentValidationService     orchestrates: parse type, route, shape response
             ValidatorRegistry             auto-collects all DocumentValidator beans
validator    DocumentValidator (interface) one implementation per type
  pan        PanValidator                  regex + non-0000 numeric block
  card       CardValidator                 length + Luhn (delegates to algorithm)
  aadhaar    AadhaarVidValidator           length + first-digit + Verhoeff
algorithm    ChecksumAlgorithm (interface) reusable, document-agnostic checksums
             LuhnAlgorithm, VerhoeffAlgorithm
dto          DocumentType (enum), ValidationRequest/Response, ErrorResponse
exception    GlobalExceptionHandler, UnsupportedDocumentTypeException
config       CorrelationIdFilter (MDC), OpenApiConfig
util         SensitiveDataMasker
```

Request flow: `Controller → Service → ValidatorRegistry → DocumentValidator →
ChecksumAlgorithm`. The checksum algorithms are independent, reusable classes,
so the core validation logic can be used as a library without the web layer.

## SOLID design

- **Single Responsibility** — the controller handles HTTP, the service
  orchestrates, each validator validates one type, and the Luhn/Verhoeff
  algorithms live in their own classes.
- **Open/Closed** — a new type needs only a new `DocumentValidator` bean; Spring
  injects every such bean into `ValidatorRegistry` automatically. The
  controller, service and existing validators are untouched. `ValidatorRegistryTest`
  proves a new dummy validator is picked up with no changes to existing code.
- **Liskov** — all validators implement `DocumentValidator` and are fully
  substitutable; none throws for ordinary invalid input.
- **Interface Segregation** — `ChecksumAlgorithm` (checksum only) is separate
  from `DocumentValidator` (document validation).
- **Dependency Inversion** — the controller depends on the
  `DocumentValidationService` abstraction and the service on the
  `DocumentValidator` abstraction; everything is wired with constructor injection.

## How to run

Requires a JDK and Maven (the project ships a Maven wrapper, `./mvnw`).

> **Java version note.** The BRD requests Java 21. The build host for this
> project had only Java 17 installed, so the Maven build targets **Java 17** to
> keep `mvn clean verify` green locally. The source uses no Java-21-only
> features. The **Dockerfile builds and runs on Java 21** as the BRD requests;
> because the bytecode target is 17, the artifact runs on either runtime. To
> target 21 for the local build as well, set `<java.version>21</java.version>`
> and `<maven.compiler.release>21</maven.compiler.release>` in `pom.xml` with a
> JDK 21 on `PATH`.

### Locally

```bash
./mvnw spring-boot:run                       # uses the default (dev) profile
# or
./mvnw clean package
java -jar target/document-validation-utility-1.0.0.jar

# choose a profile
SPRING_PROFILES_ACTIVE=prod java -jar target/document-validation-utility-1.0.0.jar
```

Quick check:

```bash
curl -s -X POST http://localhost:8080/api/v1/validate \
  -H 'Content-Type: application/json' \
  -d '{"documentType":"PAN","documentNumber":"ABCPE1234F"}'
# => {"documentNumber":"ABCPE1234F","isValid":true}
```

### With Docker

```bash
docker compose up --build
```

The image is multi-stage, runs as a non-root user, and ships a container
health check against the actuator liveness probe.

## How to test

```bash
./mvnw test          # run all unit, slice and integration tests
./mvnw clean verify  # tests + coverage gate (used in CI)
```

The suite covers:

- Unit tests for each algorithm (`LuhnAlgorithmTest`, `VerhoeffAlgorithmTest`)
  and validator, including valid/invalid values, boundary lengths,
  null/empty/blank, whitespace, lowercase PAN, separators in card and Aadhaar
  numbers, a single-digit error, an adjacent transposition, and the
  "starts with 0 or 1" Aadhaar case.
- Known-good vectors (e.g. the Luhn number `4111111111111111`). Verhoeff vectors
  are generated by the algorithm's own check-digit generator in a test helper
  (`VerhoeffTestSupport`), then mutated to assert that errors are caught.
- Parameterised PAN tests covering **every** allowed 4th character and several
  disallowed ones.
- Service routing tests with mocked validators (`DefaultDocumentValidationServiceTest`).
- `@WebMvcTest` controller tests for the 200, 400 and 500 paths and the response
  contract (`ValidationControllerTest`).
- A registry test demonstrating Open/Closed (`ValidatorRegistryTest`).
- An end-to-end `@SpringBootTest` for all three types (`ValidationIntegrationTest`)
  and a test covering all 15 BRD section-4 scenarios (`BrdScenariosTest`).

Test names follow the convention
`shouldReturnFalse_whenPanHasInvalidCategoryCharacter`.

## Coverage gate

`mvn clean verify` enforces a JaCoCo rule that **fails the build below 90% line
coverage** on the `validator` and `algorithm` packages. Current coverage for
those packages is 100%. The HTML report is written to
`target/site/jacoco/index.html`.

## Adding a new document type

Open/Closed in practice — to add, say, GSTIN:

1. Add `GSTIN` to the `DocumentType` enum (add an alias in `parse(...)` if needed).
2. Create `validator/gstin/GstinValidator` implementing `DocumentValidator`,
   annotated `@Component`, returning `DocumentType.GSTIN` from `type()`. Reuse an
   existing `ChecksumAlgorithm` or add a new one if the type needs its own.
3. Done. Spring injects the new bean into `ValidatorRegistry` automatically;
   `GET /api/v1/validate/types` lists it, and `POST /api/v1/validate` routes to
   it. No change to the controller, service or existing validators.

## Production-readiness features

- **Bean Validation** on the request DTO plus a global `@RestControllerAdvice`
  (`GlobalExceptionHandler`) producing structured error bodies.
- **Log masking** — PAN, card and Aadhaar/VID values are only ever logged via
  `SensitiveDataMasker`, which reveals only the last 4 characters. Values are
  never persisted.
- **Stateless and thread-safe** — validators and algorithms hold no mutable
  state; Verhoeff tables are immutable constants.
- **Correlation ID** — `CorrelationIdFilter` puts a request ID into the SLF4J
  MDC (reusing an inbound `X-Correlation-Id` if present) and echoes it in the
  response header and every log line.
- **Actuator** — health plus liveness/readiness probes at
  `/actuator/health/{liveness,readiness}`.
- **Metrics** — Micrometer timer `document.validation` tagged by `type` and
  `result` captures request count and latency per document type; Prometheus
  scrape endpoint at `/actuator/prometheus`.
- **Externalised config** — `application.yml` with `dev` and `prod` profiles.
- **Request body size limit** — capped at the connector and codec level; all
  input is treated as untrusted, and `documentNumber` is length-bounded.
- **OpenAPI/Swagger** with request and response examples.
- **Docker** — multi-stage, non-root Dockerfile plus `docker-compose.yml`.

## Load testing

See [`load-test/README.md`](load-test/README.md). A k6 script
(`load-test/k6-validate.js`, preferred) and an optional JMeter plan drive a
randomised ~70/30 valid/invalid mix across the three types from pre-generated
data, with smoke/average/stress/spike scenarios, per-type tagging, thresholds
(p95 < 100 ms, error rate < 1%, response-contract checks) and a JSON summary.

## Assumptions

These resolve points the BRD left open or implicit:

1. **Java version.** Built against Java 17 (the only JDK on the build host);
   Docker builds/runs on Java 21 as requested. See the run note above. No
   Java-21-only language features are used.
2. **PAN 10th character.** Only checked for being a letter, per the BRD (the
   check-letter formula is not public).
3. **PAN 5th character.** Not validated beyond being `A–Z`; the BRD states the
   surname linkage cannot be verified without personal data.
4. **Card validation scope.** Luhn + 13–19 digit length only. No BIN/network
   (Visa, Mastercard, RuPay) checks, as the BRD places these out of scope.
5. **Aadhaar/VID length.** Exactly 12 (Aadhaar) or 16 (VID); any other length is
   invalid. First digit must not be `0` or `1`.
6. **Separators.** Only spaces and hyphens are stripped from card and
   Aadhaar/VID input. Any other non-digit character makes the value invalid.
7. **`documentNumber` echo.** Returned exactly as received (including original
   case and separators), per the BRD. Callers must use secure channels.
8. **`documentNumber` length cap.** Bounded at 64 characters as an untrusted-
   input safeguard — comfortably above the longest valid value (19-digit card).
9. **`VID` is an alias, not a separate type.** It maps to the Aadhaar/VID
   (Verhoeff) validator. `/types` therefore lists `AADHAAR`, and `VID` is
   accepted on input and named in error messages as a supported token.
10. **Transport security.** HTTPS termination is assumed to be handled by the
    deployment environment (ingress/load balancer); the service speaks HTTP in-
    cluster. No values are stored.

## BRD gaps and risks

- **No authentication/authorization.** The BRD does not specify auth for the
  API. It is currently open. Add an API gateway, mTLS or token auth before
  exposing it beyond a trusted network.
- **No rate limiting.** Not in the BRD. Because validation is cheap and
  stateless, a flood could still consume resources; consider gateway-level rate
  limiting in production.
- **"Valid" can be misread as proof of identity.** Mitigated by documentation
  (this README and the API description), but worth reinforcing with API
  consumers — Luhn/Verhoeff catch typos, not forgeries.
- **Echoing the input back** returns potentially sensitive data to the caller.
  This follows the BRD, but combined with the lack of enforced HTTPS in-process
  it relies on the deployment to secure the channel.
- **Rule volatility.** PAN categories or length rules can change. They live in
  constants/regex in one validator each, so updates are localised, but there is
  no external config to change them without a redeploy.
```
