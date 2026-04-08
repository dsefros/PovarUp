const test = require('node:test');
const assert = require('node:assert/strict');
const { db, resetDb } = require('./helpers/testContext');
const svc = require('../src/services/marketplaceService');

function prep() {
  resetDb();
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
