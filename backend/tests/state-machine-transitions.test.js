const test = require('node:test');
const assert = require('node:assert/strict');
const { buildTestRuntime } = require('./helpers/testContext');

test('explicit transitions enforce state machine and idempotency', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const svc = runtime.service;
    const created = svc.createShift({ businessId: 'biz_1', locationId: 'loc_1', title: 'Prep', startAt: new Date().toISOString(), endAt: new Date(Date.now()+3600000).toISOString(), payRateCents: 1000 });
    assert.equal(created.status, 'draft');
    assert.equal(svc.updateShiftStatus(created.id, 'published').status, 'published');
    assert.equal(svc.updateShiftStatus(created.id, 'published').status, 'published');
    assert.throws(() => svc.updateShiftStatus(created.id, 'draft'), /invalid shift transition/);

    const app = svc.applyToShift('shift_1', 'worker_1');
    assert.equal(svc.rejectApplication(app.id).status, 'rejected');
    assert.throws(() => svc.withdrawApplication(app.id), /invalid application transition/);

    const app2 = svc.applyToShift('shift_1', 'worker_2');
    const asn = svc.offerAssignment(app2.id, 'biz_1');
    assert.equal(asn.status, 'assigned');
    assert.equal(svc.cancelAssignment(asn.id).status, 'cancelled');
    assert.equal(svc.cancelAssignment(asn.id).status, 'cancelled');
    assert.throws(() => svc.attendance(asn.id, 'check_in'), /assigned to check in/);

    const shift2 = svc.createShift({ businessId: 'biz_1', locationId: 'loc_1', title: 'Prep 2', startAt: new Date().toISOString(), endAt: new Date(Date.now()+3600000).toISOString(), payRateCents: 1000 });
    svc.updateShiftStatus(shift2.id, 'published');
    const app3 = svc.applyToShift(shift2.id, 'worker_1');
    const asn2 = svc.offerAssignment(app3.id, 'biz_1');
    runtime.repo.updateShiftTimes(shift2.id, new Date(Date.now() - 20 * 60000).toISOString(), new Date(Date.now() + 2 * 3600_000).toISOString());
    svc.attendance(asn2.id, 'check_in');
    svc.attendance(asn2.id, 'check_out');
    const payout = svc.releasePayment(asn2.id);
    assert.equal(svc.updatePayoutStatus(payout.id, 'pending').status, 'pending');
    assert.equal(svc.updatePayoutStatus(payout.id, 'paid').status, 'paid');
    assert.equal(svc.updatePayoutStatus(payout.id, 'paid').status, 'paid');
    assert.throws(() => svc.updatePayoutStatus(payout.id, 'created'), /invalid payout transition/);
  } finally {
    cleanup();
  }
});
