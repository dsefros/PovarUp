const test = require('node:test');
const assert = require('node:assert/strict');
const { buildTestRuntime } = require('./helpers/testContext');

test('assignment lifecycle from apply to rated', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const svc = runtime.service;
    const repo = runtime.repo;
    const app = svc.applyToShift('shift_1', 'worker_1');
    const asn = svc.offerAssignment(app.id, 'biz_1');
    svc.acceptAssignment(asn.id);
    const shift = repo.findShiftById('shift_1');
    repo.updateShiftTimes(shift.id, new Date(Date.now() - 10 * 60000).toISOString(), new Date(Date.now() + 2 * 3600_000).toISOString());
    svc.attendance(asn.id, 'check_in');
    svc.attendance(asn.id, 'check_out');
    svc.addRating(asn.id, 'worker', 5, 'great');
    svc.addRating(asn.id, 'business', 5, 'great');
    const done = repo.findAssignmentById(asn.id);
    assert.equal(done.status, 'completed_rated');
  } finally {
    cleanup();
  }
});

test('cannot accept assignment unless offered', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    assert.throws(() => runtime.service.acceptAssignment('asn_missing'), /assignment not found/);
  } finally {
    cleanup();
  }
});

test('duplicate rating from same role is rejected', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const svc = runtime.service;
    const app = svc.applyToShift('shift_1', 'worker_1');
    const asn = svc.offerAssignment(app.id, 'biz_1');
    svc.acceptAssignment(asn.id);
    svc.addRating(asn.id, 'worker', 5, 'great');
    assert.throws(() => svc.addRating(asn.id, 'worker', 4, 'dup'), /already rated/);
  } finally {
    cleanup();
  }
});
