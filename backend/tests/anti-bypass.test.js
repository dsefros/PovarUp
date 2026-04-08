const test = require('node:test');
const assert = require('node:assert/strict');
const { buildTestRuntime } = require('./helpers/testContext');

function prep(runtime) {
  const svc = runtime.service;
  const app = svc.applyToShift('shift_1', 'worker_1');
  const asn = svc.offerAssignment(app.id, 'biz_1');
  svc.acceptAssignment(asn.id);
  return asn.id;
}

test('chat message with phone creates violation flag', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const assignmentId = prep(runtime);
    runtime.service.sendMessage(assignmentId, 'worker_1', 'Call me at +1 555 111 2233');
    assert.equal(runtime.repo.listViolationFlags().length, 1);
  } finally {
    cleanup();
  }
});
