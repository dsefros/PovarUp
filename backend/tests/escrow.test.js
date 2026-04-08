const test = require('node:test');
const assert = require('node:assert/strict');
const { db, resetDb } = require('./helpers/testContext');
const svc = require('../src/services/marketplaceService');

function setupComplete() {
  resetDb();
  const app = svc.applyToShift('shift_1', 'worker_1');
  const asn = svc.offerAssignment(app.id, 'biz_1');
  svc.acceptAssignment(asn.id);
  const shift = db.shifts.find((s) => s.id === 'shift_1');
  shift.start_at = new Date(Date.now() - 10 * 60000).toISOString();
  shift.end_at = new Date(Date.now() + 3600_000).toISOString();
  svc.attendance(asn.id, 'check_in');
  svc.attendance(asn.id, 'check_out');
  return asn.id;
}

test('release creates payout', () => {
  const assignmentId = setupComplete();
  const payout = svc.releasePayment(assignmentId);
  assert.equal(payout.assignment_id, assignmentId);
  assert.equal(db.payouts.length, 1);
});

test('release is idempotent by assignment', () => {
  const assignmentId = setupComplete();
  const p1 = svc.releasePayment(assignmentId);
  const p2 = svc.releasePayment(assignmentId);
  assert.equal(p1.id, p2.id);
  assert.equal(db.payouts.length, 1);
});
