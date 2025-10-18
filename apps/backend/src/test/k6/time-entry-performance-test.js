import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics for time entry performance
export let errorRate = new Rate('errors');
export let timeEntryCreationLatency = new Trend('time_entry_creation_latency');

// Test configuration
export let options = {
  stages: [
    { duration: '2m', target: 10 }, // Ramp up to 10 users
    { duration: '5m', target: 10 }, // Stay at 10 users
    { duration: '2m', target: 50 }, // Ramp up to 50 users
    { duration: '5m', target: 50 }, // Stay at 50 users
    { duration: '2m', target: 100 }, // Ramp up to 100 users
    { duration: '5m', target: 100 }, // Stay at 100 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // p95 must be under 200ms
    http_req_failed: ['rate<0.1'], // Error rate must be below 10%
    time_entry_creation_latency: ['p(95)<200'], // Custom metric threshold
    errors: ['rate<0.1'], // Custom error rate threshold
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test data generators
function generateTimeEntry() {
  const durations = [15, 30, 45, 60, 90, 120];
  const descriptions = [
    'Investigated incident root cause',
    'Fixed bug in authentication service',
    'Updated documentation',
    'Attended team standup',
    'Code review for PR #123',
    'Deployed hotfix to production',
    'Resolved customer ticket',
    'Database performance optimization',
  ];
  const incidentIds = ['inc-001', 'inc-002', 'inc-003', 'inc-004', 'inc-005'];
  const taskIds = ['task-001', 'task-002', 'task-003', 'task-004', 'task-005'];

  const targetEntities = [];
  // Randomly add 1-3 target entities
  const numTargets = Math.floor(Math.random() * 3) + 1;

  if (Math.random() > 0.5) {
    targetEntities.push({
      type: 'INCIDENT',
      entityId: incidentIds[Math.floor(Math.random() * incidentIds.length)],
      entityTitle: `Incident ${Math.floor(Math.random() * 1000)}`,
    });
  }

  if (Math.random() > 0.5) {
    targetEntities.push({
      type: 'TASK',
      entityId: taskIds[Math.floor(Math.random() * taskIds.length)],
      entityTitle: `Task ${Math.floor(Math.random() * 1000)}`,
    });
  }

  return {
    durationMinutes: durations[Math.floor(Math.random() * durations.length)],
    description: descriptions[Math.floor(Math.random() * descriptions.length)],
    occurredAt: new Date(Date.now() - Math.random() * 86400000).toISOString(), // Random time in last 24h
    targetEntities: targetEntities,
  };
}

export default function () {
  // Generate random time entry data
  const timeEntry = generateTimeEntry();

  // Measure the creation latency
  let startTime = new Date().getTime();

  let response = http.post(`${BASE_URL}/api/v1/time-entries`, JSON.stringify(timeEntry), {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${__ENV.API_TOKEN || 'test-token'}`,
    },
  });

  let endTime = new Date().getTime();
  let latency = endTime - startTime;

  // Record custom metric
  timeEntryCreationLatency.add(latency);

  // Validate response
  let success = check(response, {
    'time entry created - status is 202': (r) => r.status === 202,
    'time entry created - has tracking ID': (r) => r.json('trackingId') !== undefined,
    'time entry created - has time entry IDs': (r) => Array.isArray(r.json('timeEntryIds')),
    'time entry created - response time < 200ms': (r) => r.timings.duration < 200,
  });

  errorRate.add(!success);

  // Log detailed metrics for debugging
  if (!success) {
    console.log(`Failed request: ${response.status} - ${response.body}`);
  }

  // Random sleep between requests (1-3 seconds)
  sleep(Math.random() * 2 + 1);
}

export function handleSummary(data) {
  console.log('\n=== Time Entry Performance Test Summary ===');

  // Check if p95 latency meets requirement
  const p95Latency = data.metrics.http_req_duration['p(95)'];
  const meetsLatencyRequirement = p95Latency < 200;

  console.log(`P95 Response Time: ${p95Latency.toFixed(2)}ms`);
  console.log(`Latency Requirement (≤200ms): ${meetsLatencyRequirement ? '✅ PASS' : '❌ FAIL'}`);

  // Check error rate
  const errorRateValue = data.metrics.http_req_failed.rate * 100;
  const meetsErrorRequirement = errorRateValue < 10;

  console.log(`Error Rate: ${errorRateValue.toFixed(2)}%`);
  console.log(`Error Requirement (<10%): ${meetsErrorRequirement ? '✅ PASS' : '❌ FAIL'}`);

  // Overall test result
  const testPassed = meetsLatencyRequirement && meetsErrorRequirement;
  console.log(`\nOverall Test Result: ${testPassed ? '✅ PASS' : '❌ FAIL'}`);

  if (!testPassed) {
    console.log('\n❌ Performance requirements not met:');
    if (!meetsLatencyRequirement) {
      console.log(`   - P95 latency ${p95Latency.toFixed(2)}ms exceeds 200ms limit`);
    }
    if (!meetsErrorRequirement) {
      console.log(`   - Error rate ${errorRateValue.toFixed(2)}% exceeds 10% limit`);
    }
  } else {
    console.log('\n✅ All performance requirements met!');
  }

  return {
    time_entry_performance_test: testPassed ? 'pass' : 'fail',
    p95_latency_ms: p95Latency,
    error_rate_percent: errorRateValue,
    meets_latency_requirement: meetsLatencyRequirement,
    meets_error_requirement: meetsErrorRequirement,
  };
}