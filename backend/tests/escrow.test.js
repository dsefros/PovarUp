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

test('cancel restores escrow exactly once and payout is rejected after cancellation', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const svc = runtime.service;
    const repo = runtime.repo;
    const startingBalance = repo.findEscrowByBusinessId('biz_1').balance_cents;

    const app = svc.applyToShift('shift_1', 'worker_1');
    const asn = svc.offerAssignment(app.id, 'biz_1');
    const lockedBalance = repo.findEscrowByBusinessId('biz_1').balance_cents;
    assert.equal(lockedBalance, startingBalance - asn.escrow_locked_cents);

    svc.cancelAssignment(asn.id);
    const restoredBalance = repo.findEscrowByBusinessId('biz_1').balance_cents;
    assert.equal(restoredBalance, startingBalance);
    const unlockTx = repo.listEscrowTransactions().filter((tx) => tx.assignment_id === asn.id && tx.type === 'unlock_cancel');
    assert.equal(unlockTx.length, 1);

    svc.cancelAssignment(asn.id);
    assert.equal(repo.findEscrowByBusinessId('biz_1').balance_cents, startingBalance);
    const unlockTxAfterRepeat = repo.listEscrowTransactions().filter((tx) => tx.assignment_id === asn.id && tx.type === 'unlock_cancel');
    assert.equal(unlockTxAfterRepeat.length, 1);

    assert.throws(() => svc.releasePayment(asn.id, false), /cannot release payout for cancelled assignment/);
    assert.throws(() => svc.releasePayment(asn.id, true), /cannot release payout for cancelled assignment/);
  } finally {
    cleanup();
  }
});
