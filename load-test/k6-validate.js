/*
 * k6 load test for POST /api/v1/validate.
 *
 * Exercises a randomised mix of the three document types with a ~70/30 split of
 * valid to invalid values, drawn from pre-generated data in data/documents.json.
 * Requests are tagged by document type so results break down per type.
 *
 * Scenarios: smoke, average load, stress and spike.
 *
 * Run (see load-test/README.md for details):
 *   BASE_URL=http://localhost:8080 k6 run load-test/k6-validate.js
 *   BASE_URL=http://localhost:8080 k6 run --env SCENARIO=smoke load-test/k6-validate.js
 */
import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// Base URL from env; defaults to localhost for convenience.
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ENDPOINT = `${BASE_URL}/api/v1/validate`;

// Load pre-generated data once, shared across all VUs.
const dataset = new SharedArray('documents', function () {
  const raw = JSON.parse(open('./data/documents.json'));
  // Flatten the per-type groups into a single weighted pool.
  return [...raw.PAN, ...raw.CARD, ...raw.AADHAAR];
});

// Custom metric: fraction of responses whose isValid matched the expectation.
const contractErrors = new Rate('contract_errors');

// -- Scenario catalogue -----------------------------------------------------
const allScenarios = {
  smoke: {
    executor: 'constant-vus',
    vus: 1,
    duration: '30s',
    tags: { scenario: 'smoke' },
  },
  average: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m', target: 50 },
      { duration: '3m', target: 50 },
      { duration: '1m', target: 0 },
    ],
    tags: { scenario: 'average' },
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '2m', target: 100 },
      { duration: '2m', target: 300 },
      { duration: '2m', target: 350 },
      { duration: '2m', target: 0 },
    ],
    tags: { scenario: 'stress' },
  },
  spike: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '10s', target: 20 },
      { duration: '20s', target: 400 },
      { duration: '30s', target: 400 },
      { duration: '10s', target: 0 },
    ],
    tags: { scenario: 'spike' },
  },
};

// Select a single scenario via SCENARIO env var, or run smoke by default.
const selected = __ENV.SCENARIO;
const scenarios = selected
  ? { [selected]: allScenarios[selected] }
  : { smoke: allScenarios.smoke };

export const options = {
  scenarios,
  thresholds: {
    http_req_duration: ['p(95)<100'], // p95 latency under 100 ms
    http_req_failed: ['rate<0.01'], // HTTP error rate under 1%
    contract_errors: ['rate<0.01'], // response contract honoured
  },
};

export default function () {
  const item = dataset[Math.floor(Math.random() * dataset.length)];
  const payload = JSON.stringify({
    documentType: item.documentType,
    documentNumber: item.documentNumber,
  });

  // Tag per document type so metrics can be split by type.
  const typeTag = item.documentType === 'VID' ? 'AADHAAR' : item.documentType;
  const res = http.post(ENDPOINT, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { docType: typeTag },
  });

  let body = {};
  try {
    body = res.json();
  } catch (e) {
    body = {};
  }

  const contractOk = check(res, {
    'status is 200': (r) => r.status === 200,
    'has documentNumber': () => typeof body.documentNumber === 'string',
    'isValid is boolean': () => typeof body.isValid === 'boolean',
    'result matches expectation': () => body.isValid === item.expected,
  });

  contractErrors.add(!contractOk);
}

// Emit both a console summary and a JSON report artifact.
export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'load-test/summary.json': JSON.stringify(data, null, 2),
  };
}
