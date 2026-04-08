const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { createServer } = require('../src/server');

async function startServer(dbPath) {
  const { server, runtime } = createServer({ persistence: 'sql', dbPath });
  await new Promise((resolve) => server.listen(0, resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  return {
    base,
    runtime,
    stop: () => new Promise((resolve) => server.close(resolve))
  };
}

async function createSession(base, userId, role) {
  const res = await fetch(`${base}/api/auth/session`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ userId, role })
  });
  assert.equal(res.status, 200);
  return res.json();
}

test('sql-backed lifecycle persists across server restarts', async () => {
  const dbPath = path.join(os.tmpdir(), `povarup-persist-${Date.now()}.sqlite`);

  const run1 = await startServer(dbPath);
  let workerSession;
  let businessSession;
  let assignmentId;

  try {
    workerSession = await createSession(run1.base, 'u_worker_1', 'worker');
    businessSession = await createSession(run1.base, 'u_biz_1', 'business');

    const appRes = await fetch(`${run1.base}/api/applications`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${workerSession.token}` },
      body: JSON.stringify({ shiftId: 'shift_1' })
    });
    assert.equal(appRes.status, 201);
    const applicationId = (await appRes.json()).item.id;

    const offerRes = await fetch(`${run1.base}/api/assignments/offer`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${businessSession.token}` },
      body: JSON.stringify({ applicationId })
    });
    assert.equal(offerRes.status, 201);
    assignmentId = (await offerRes.json()).item.id;

    const acceptRes = await fetch(`${run1.base}/api/assignments/${assignmentId}/accept`, {
      method: 'POST',
      headers: { authorization: `Bearer ${workerSession.token}` }
    });
    assert.equal(acceptRes.status, 200);

    const unauthorizedRead = await fetch(`${run1.base}/api/payouts/worker_1`);
    assert.equal(unauthorizedRead.status, 401);
  } finally {
    await run1.stop();
  }

  const run2 = await startServer(dbPath);
  try {
    const workerSession2 = await createSession(run2.base, 'u_worker_1', 'worker');
    const businessSession2 = await createSession(run2.base, 'u_biz_1', 'business');

    const assignmentsRes = await fetch(`${run2.base}/api/assignments`, {
      headers: { authorization: `Bearer ${workerSession2.token}` }
    });
    assert.equal(assignmentsRes.status, 200);
    const assignments = (await assignmentsRes.json()).items;
    assert.equal(assignments.some((a) => a.id === assignmentId), true);

    run2.runtime.repo.updateShiftTimes('shift_1', new Date(Date.now() - 10 * 60000).toISOString(), new Date(Date.now() + 3600_000).toISOString());

    const checkIn = await fetch(`${run2.base}/api/attendance/check-in`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${workerSession2.token}` },
      body: JSON.stringify({ assignmentId })
    });
    assert.equal(checkIn.status, 201);

    const checkOut = await fetch(`${run2.base}/api/attendance/check-out`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${workerSession2.token}` },
      body: JSON.stringify({ assignmentId })
    });
    assert.equal(checkOut.status, 201);

    const rateWorker = await fetch(`${run2.base}/api/ratings`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${workerSession2.token}` },
      body: JSON.stringify({ assignmentId, fromRole: 'worker', score: 5, note: 'good' })
    });
    assert.equal(rateWorker.status, 201);

    const rateBusiness = await fetch(`${run2.base}/api/ratings`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${businessSession2.token}` },
      body: JSON.stringify({ assignmentId, fromRole: 'business', score: 5, note: 'great' })
    });
    assert.equal(rateBusiness.status, 201);

    const release1 = await fetch(`${run2.base}/api/escrow/release/${assignmentId}`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${businessSession2.token}` },
      body: JSON.stringify({ force: false })
    });
    assert.equal(release1.status, 201);
    const payout1 = (await release1.json()).item;

    const release2 = await fetch(`${run2.base}/api/escrow/release/${assignmentId}`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${businessSession2.token}` },
      body: JSON.stringify({ force: false })
    });
    assert.equal(release2.status, 201);
    const payout2 = (await release2.json()).item;
    assert.equal(payout1.id, payout2.id);

    const workerPayouts = await fetch(`${run2.base}/api/payouts/worker_1`, {
      headers: { authorization: `Bearer ${workerSession2.token}` }
    });
    assert.equal(workerPayouts.status, 200);
    assert.equal((await workerPayouts.json()).items.length, 1);
  } finally {
    await run2.stop();
    if (fs.existsSync(dbPath)) fs.rmSync(dbPath, { force: true });
  }
});
