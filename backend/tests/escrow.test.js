const test = require('node:test');
const assert = require('node:assert/strict');
const { buildTestRuntime } = require('./helpers/testContext');

function setupComplete(runtime) {
  const svc = runtime.service;
  const repo = runtime.repo;
  const app = svc.applyToShift('shift_1', 'worker_1');
  const asn = svc.offerAssignment(app.id, 'biz_1');
  svc.acceptAssignment(asn.id);
  repo.updateShiftTimes('shift_1', new Date(Date.now() - 10 * 60000).toISOString(), new Date(Date.now() + 3600_000).toISOString());
  svc.attendance(asn.id, 'check_in');
  svc.attendance(asn.id, 'check_out');
  return asn.id;
}

test('release creates payout', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const assignmentId = setupComplete(runtime);
    const payout = runtime.service.releasePayment(assignmentId);
    assert.equal(payout.assignment_id, assignmentId);
    assert.equal(runtime.repo.listPayoutsByWorkerId('worker_1').length, 1);
  } finally {
    cleanup();
  }
});

test('release is idempotent by assignment', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const assignmentId = setupComplete(runtime);
    const p1 = runtime.service.releasePayment(assignmentId);
    const p2 = runtime.service.releasePayment(assignmentId);
    assert.equal(p1.id, p2.id);
    assert.equal(runtime.repo.listPayoutsByWorkerId('worker_1').length, 1);
  } finally {
    cleanup();
  }
});
