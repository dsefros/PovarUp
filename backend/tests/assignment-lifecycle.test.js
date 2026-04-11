const test = require('node:test');
const assert = require('node:assert/strict');
const { buildTestRuntime } = require('./helpers/testContext');

test('assignment lifecycle from apply to completion', () => {
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
    const done = repo.findAssignmentById(asn.id);
    assert.equal(done.status, 'completed');
  } finally {
    cleanup();
  }
});

test('cannot accept assignment unless assigned', () => {
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

test('invalid duplicate lifecycle actions are blocked', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const svc = runtime.service;
    const repo = runtime.repo;
    const app = svc.applyToShift('shift_1', 'worker_1');
    const asn = svc.offerAssignment(app.id, 'biz_1');
    svc.acceptAssignment(asn.id);
    assert.equal(svc.acceptAssignment(asn.id).status, 'assigned');
    const shift = repo.findShiftById('shift_1');
    repo.updateShiftTimes(shift.id, new Date(Date.now() - 10 * 60000).toISOString(), new Date(Date.now() + 2 * 3600_000).toISOString());
    svc.attendance(asn.id, 'check_in');
    assert.throws(() => svc.attendance(asn.id, 'check_in'), /must be assigned to check in/);
    svc.attendance(asn.id, 'check_out');
    assert.throws(() => svc.attendance(asn.id, 'check_out'), /must be in progress to check out/);
  } finally {
    cleanup();
  }
});

test('completed assignments cannot be cancelled', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const svc = runtime.service;
    const repo = runtime.repo;
    const app = svc.applyToShift('shift_1', 'worker_1');
    const asn = svc.offerAssignment(app.id, 'biz_1');
    repo.updateShiftTimes('shift_1', new Date(Date.now() - 10 * 60000).toISOString(), new Date(Date.now() + 2 * 3600_000).toISOString());
    svc.attendance(asn.id, 'check_in');
    svc.attendance(asn.id, 'check_out');
    assert.throws(() => svc.cancelAssignment(asn.id), /invalid assignment transition/);
  } finally {
    cleanup();
  }
});
