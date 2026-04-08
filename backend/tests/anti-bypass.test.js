const test = require('node:test');
const assert = require('node:assert/strict');
const { db, seed } = require('../src/domain/store');
const svc = require('../src/services/marketplaceService');

function prep() {
  Object.keys(db).forEach((k) => (db[k].length = 0));
  seed();
  const app = svc.applyToShift('shift_1', 'worker_1');
  const asn = svc.offerAssignment(app.id, 'biz_1');
  svc.acceptAssignment(asn.id);
  return asn.id;
}

test('chat message with phone creates violation flag', () => {
  const assignmentId = prep();
  svc.sendMessage(assignmentId, 'worker_1', 'Call me at +1 555 111 2233');
  assert.equal(db.violation_flags.length, 1);
});
