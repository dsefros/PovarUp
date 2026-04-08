const test = require('node:test');
const assert = require('node:assert/strict');
const { db, seed } = require('../src/domain/store');
const svc = require('../src/services/marketplaceService');

function reset() {
  Object.keys(db).forEach((k) => (db[k].length = 0));
  seed();
}

test('assignment lifecycle from apply to rated', () => {
  reset();
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
