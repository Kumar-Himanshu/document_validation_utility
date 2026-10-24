# Load testing

Load tests for `POST /api/v1/validate`. The preferred tool is [k6](https://k6.io);
an optional JMeter plan is provided as an alternative.

## Data

`data/documents.json` and `data/documents.csv` hold pre-generated values:

- A randomised mix of PAN, CARD and AADHAAR/VID.
- About 70% valid and 30% invalid per type.
- Each record carries an `expected` flag so the test can assert the response's
  `isValid` matches, not just that a 200 came back.

The data is generated deterministically and every record's `expected` flag was
cross-checked against reference implementations of the PAN rules, Luhn and
Verhoeff, so the valid/invalid split is accurate.

## k6 (preferred)

Install k6 (`brew install k6`, or see the k6 docs), start the service, then run:

```bash
# Smoke test (default scenario): 1 VU for 30s
BASE_URL=http://localhost:8080 k6 run load-test/k6-validate.js

# Pick a specific scenario: smoke | average | stress | spike
BASE_URL=http://localhost:8080 k6 run --env SCENARIO=average load-test/k6-validate.js
BASE_URL=http://localhost:8080 k6 run --env SCENARIO=stress  load-test/k6-validate.js
BASE_URL=http://localhost:8080 k6 run --env SCENARIO=spike   load-test/k6-validate.js
```

The base URL is read from the `BASE_URL` environment variable (defaults to
`http://localhost:8080`).

### Scenarios

| Scenario | Profile                                   |
|----------|-------------------------------------------|
| smoke    | 1 VU, 30s                                 |
| average  | ramp to 50 VUs, hold 3m, ramp down (~5m)  |
| stress   | ramp to 350 VUs over ~8m                  |
| spike    | burst to 400 VUs, hold 30s, drop          |

### Thresholds (the run fails if any is breached)

- `http_req_duration` p95 < 100 ms
- `http_req_failed` rate < 1%
- `contract_errors` rate < 1% — every response must contain a string
  `documentNumber` and a boolean `isValid`, and `isValid` must match the
  record's expectation.

### Per-type breakdown

Each request is tagged with `docType` (PAN, CARD, AADHAAR). Break results down
per type in the k6 output, or when streaming to a time-series backend, by
filtering on the `docType` tag. The `scenario` tag identifies which profile ran.

### Report output

`handleSummary` writes `load-test/summary.json` (full metrics) and prints a
console summary. For an HTML report, pipe the JSON through a converter such as
`k6-html-reporter`, or stream metrics to Grafana/InfluxDB with
`k6 run --out influxdb=...`.

## JMeter (optional)

```bash
jmeter -n -t load-test/jmeter-validate.jmx -l load-test/jmeter-results.jtl \
  -Jhost=localhost -Jport=8080 -Jthreads=50 -Jrampup=60 -Jduration=300 \
  -Jdatafile=load-test/data/documents.csv
```

Properties: `host`, `port`, `threads`, `rampup`, `duration`, `datafile`. The
plan asserts HTTP 200 and that `$.isValid` equals the CSV `expected` column.

## Running in CI

1. Start the service (`java -jar target/*.jar` or `docker compose up -d`) and
   wait for `GET /actuator/health/readiness` to report `UP`.
2. Run the smoke scenario as a gate:

   ```bash
   BASE_URL=http://service:8080 k6 run --env SCENARIO=smoke load-test/k6-validate.js
   ```

   k6 exits non-zero if any threshold is breached, failing the CI job.
3. Archive `load-test/summary.json` as a build artifact.

Run `average`, `stress` and `spike` on a schedule (e.g. nightly) rather than on
every commit, since they take several minutes each.
