const test = require('node:test');
const assert = require('node:assert/strict');
const { db, resetDb } = require('./helpers/testContext');
const svc = require('../src/services/marketplaceService');

test('assignment lifecycle from apply to rated', () => {
  resetDb();
  const app = svc.applyToShift('shift_1', 'worker_1');
  const asn = svc.offerAssignment(app.id, 'biz_1');
  svc.acceptAssignment(asn.id);
  const shift = db.shifts.find((s) => s.id === 'shift_1');
  shift.start_at = new Date(Date.now() - 10 * 60000).toISOString();
  shift.end_at = new Date(Date.now() + 2 * 3600_000).toISOString();
  svc.attendance(asn.id, 'check_in');
  svc.attendance(asn.id, 'check_out');
  svc.addRating(asn.id, 'worker', 5, 'great');
  svc.addRating(asn.id, 'business', 5, 'great');
  const done = db.assignments.find((a) => a.id === asn.id);
  assert.equal(done.status, 'completed_rated');
});

test('cannot accept assignment unless offered', () => {
  resetDb();
  assert.throws(() => svc.acceptAssignment('asn_missing'), /assignment not found/);
});

test('duplicate rating from same role is rejected', () => {
  resetDb();
  const app = svc.applyToShift('shift_1', 'worker_1');
  const asn = svc.offerAssignment(app.id, 'biz_1');
  svc.acceptAssignment(asn.id);
  svc.addRating(asn.id, 'worker', 5, 'great');
  assert.throws(() => svc.addRating(asn.id, 'worker', 4, 'dup'), /already rated/);
});
