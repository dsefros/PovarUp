const test = require('node:test');
const assert = require('node:assert/strict');
const { buildTestRuntime } = require('./helpers/testContext');

async function withServer(run) {
  const { server, runtime, cleanup } = buildTestRuntime();
  await new Promise((resolve) => server.listen(0, resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    await run(base, runtime);
  } finally {
    await new Promise((resolve) => server.close(resolve));
    cleanup();
  }
}

async function login(base, userId, password) {
  const res = await fetch(`${base}/api/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ userId, password })
  });
  return await res.json();
}

test('duplicate apply is blocked and statuses include normalizedStatus', async () => {
  await withServer(async (base) => {
    const worker = await login(base, 'worker.demo', 'workerpass');
    const apply1 = await fetch(`${base}/api/applications`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${worker.token}` },
      body: JSON.stringify({ shiftId: 'shift_1' })
    });
    assert.equal(apply1.status, 201);

    const apply2 = await fetch(`${base}/api/applications`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${worker.token}` },
      body: JSON.stringify({ shiftId: 'shift_1' })
    });
    assert.equal(apply2.status, 409);

    const apps = await fetch(`${base}/api/applications`, { headers: { authorization: `Bearer ${worker.token}` } });
    const body = await apps.json();
    assert.equal(body.items[0].normalizedStatus, 'applied');
    assert.equal(body.items[0].productStatus, 'applied');
  });
});

test('admin can progress manual payout lifecycle and inspect ops surfaces', async () => {
  await withServer(async (base, runtime) => {
    const svc = runtime.service;
    const app = svc.applyToShift('shift_1', 'worker_1');
    const asn = svc.offerAssignment(app.id, 'biz_1');
    svc.acceptAssignment(asn.id);
    runtime.repo.updateShiftTimes('shift_1', new Date(Date.now() - 20 * 60000).toISOString(), new Date(Date.now() + 2 * 3600_000).toISOString());
    svc.attendance(asn.id, 'check_in');
    svc.attendance(asn.id, 'check_out');
    const payout = svc.releasePayment(asn.id, false);

    const admin = await login(base, 'admin.demo', 'adminpass');

    const payouts = await fetch(`${base}/api/admin/payouts`, { headers: { authorization: `Bearer ${admin.token}` } });
    assert.equal(payouts.status, 200);

    const toPending = await fetch(`${base}/api/admin/payouts/${payout.id}/status`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${admin.token}` },
      body: JSON.stringify({ status: 'pending' })
    });
    assert.equal(toPending.status, 200);

    const toPaid = await fetch(`${base}/api/admin/payouts/${payout.id}/status`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${admin.token}` },
      body: JSON.stringify({ status: 'paid' })
    });
    assert.equal(toPaid.status, 200);
    const paidBody = await toPaid.json();
    assert.equal(paidBody.item.status, 'paid');
    assert.equal(paidBody.item.internalStatus, 'paid');

    const problems = await fetch(`${base}/api/admin/problem-cases`, { headers: { authorization: `Bearer ${admin.token}` } });
    assert.equal(problems.status, 200);

    const assignments = await fetch(`${base}/api/admin/assignments`, { headers: { authorization: `Bearer ${admin.token}` } });
    assert.equal(assignments.status, 200);
  });
});
