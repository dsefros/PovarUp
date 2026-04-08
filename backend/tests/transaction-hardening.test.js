const test = require('node:test');
const assert = require('node:assert/strict');
const { buildTestRuntime } = require('./helpers/testContext');

function setupCompletedAssignment(runtime) {
  const svc = runtime.service;
  const repo = runtime.repo;
  const app = svc.applyToShift('shift_1', 'worker_1');
  const asn = svc.offerAssignment(app.id, 'biz_1');
  svc.acceptAssignment(asn.id);
  repo.updateShiftTimes('shift_1', new Date(Date.now() - 10 * 60000).toISOString(), new Date(Date.now() + 3600_000).toISOString());
  svc.attendance(asn.id, 'check_in');
  svc.attendance(asn.id, 'check_out');
  return { app, asn };
}

test('offerAssignment rolls back all writes when assignment insert fails', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const repo = runtime.repo;
    const svc = runtime.service;
    const app = svc.applyToShift('shift_1', 'worker_1');
    const beforeEscrow = repo.findEscrowByBusinessId('biz_1').balance_cents;

    const originalInsertAssignment = repo.insertAssignment;
    repo.insertAssignment = () => {
      throw new Error('simulated assignment insert failure');
    };

    assert.throws(() => svc.offerAssignment(app.id, 'biz_1'), /simulated assignment insert failure/);

    repo.insertAssignment = originalInsertAssignment;

    const escrowAfter = repo.findEscrowByBusinessId('biz_1').balance_cents;
    const appAfter = repo.findApplicationById(app.id);
    const assignmentCount = runtime.database.prepare('SELECT COUNT(*) AS n FROM assignments WHERE application_id = ?').get(app.id).n;
    const lockTxCount = runtime.database.prepare("SELECT COUNT(*) AS n FROM escrow_transactions WHERE type = 'lock'").get().n;

    assert.equal(escrowAfter, beforeEscrow);
    assert.equal(appAfter.status, 'applied');
    assert.equal(assignmentCount, 0);
    assert.equal(lockTxCount, 0);
  } finally {
    cleanup();
  }
});

test('releasePayment rolls back payout write when escrow transaction insert fails', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const repo = runtime.repo;
    const svc = runtime.service;
    const { asn } = setupCompletedAssignment(runtime);

    const originalInsertEscrowTx = repo.insertEscrowTransaction;
    repo.insertEscrowTransaction = (tx) => {
      if (tx.type === 'release') throw new Error('simulated release tx failure');
      return originalInsertEscrowTx(tx);
    };

    assert.throws(() => svc.releasePayment(asn.id), /simulated release tx failure/);

    repo.insertEscrowTransaction = originalInsertEscrowTx;

    const payoutCount = runtime.database.prepare('SELECT COUNT(*) AS n FROM payouts WHERE assignment_id = ?').get(asn.id).n;
    const releaseTxCount = runtime.database.prepare("SELECT COUNT(*) AS n FROM escrow_transactions WHERE assignment_id = ? AND type = 'release'").get(asn.id).n;

    assert.equal(payoutCount, 0);
    assert.equal(releaseTxCount, 0);
  } finally {
    cleanup();
  }
});

test('releasePayment remains idempotent with transaction wrapping', () => {
  const { runtime, cleanup } = buildTestRuntime();
  try {
    const { asn } = setupCompletedAssignment(runtime);

    const p1 = runtime.service.releasePayment(asn.id);
    const p2 = runtime.service.releasePayment(asn.id);

    assert.equal(p1.id, p2.id);

    const payoutCount = runtime.database.prepare('SELECT COUNT(*) AS n FROM payouts WHERE assignment_id = ?').get(asn.id).n;
    const releaseTxCount = runtime.database.prepare("SELECT COUNT(*) AS n FROM escrow_transactions WHERE assignment_id = ? AND type = 'release'").get(asn.id).n;

    assert.equal(payoutCount, 1);
    assert.equal(releaseTxCount, 1);
  } finally {
    cleanup();
  }
});
