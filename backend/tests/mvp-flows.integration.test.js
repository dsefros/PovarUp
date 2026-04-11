const test = require('node:test');
const assert = require('node:assert/strict');
const { buildTestRuntime } = require('./helpers/testContext');

async function withServer(run) {
  const { server, cleanup } = buildTestRuntime();
  await new Promise((resolve) => server.listen(0, resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    await run(base);
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
  return { status: res.status, body: await res.json() };
}

test('blocks deprecated technical session creation', async () => {
  await withServer(async (base) => {
    const res = await fetch(`${base}/api/auth/session`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ userId: 'whatever', role: 'admin' })
    });
    assert.equal(res.status, 410);
  });
});

test('worker and business end-to-end MVP flow', async () => {
  await withServer(async (base) => {
    const worker = await login(base, 'worker.demo', 'workerpass');
    const business = await login(base, 'business.demo', 'businesspass');
    assert.equal(worker.status, 200);
    assert.equal(business.status, 200);

    const createShift = await fetch(`${base}/api/shifts`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${business.body.token}` },
      body: JSON.stringify({
        locationId: 'loc_1',
        title: 'Prep Cook',
        startAt: new Date().toISOString(),
        endAt: new Date(Date.now() + 2 * 3600_000).toISOString(),
        payRateCents: 2200
      })
    });
    assert.equal(createShift.status, 201);
    const createdShift = (await createShift.json()).item;

    const shiftDetailRes = await fetch(`${base}/api/shifts/${createdShift.id}`);
    assert.equal(shiftDetailRes.status, 200);

    const applyRes = await fetch(`${base}/api/applications`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${worker.body.token}` },
      body: JSON.stringify({ shiftId: createdShift.id })
    });
    assert.equal(applyRes.status, 201);
    const application = (await applyRes.json()).item;

    const bizAppsRes = await fetch(`${base}/api/business/shifts/${createdShift.id}/applications`, {
      headers: { authorization: `Bearer ${business.body.token}` }
    });
    const bizApps = await bizAppsRes.json();
    assert.equal(bizAppsRes.status, 200);
    assert.equal(bizApps.items.length, 1);

    const offerRes = await fetch(`${base}/api/assignments/offer`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${business.body.token}` },
      body: JSON.stringify({ applicationId: application.id })
    });
    assert.equal(offerRes.status, 201);
    const assignment = (await offerRes.json()).item;

    const acceptRes = await fetch(`${base}/api/assignments/${assignment.id}/accept`, {
      method: 'POST',
      headers: { authorization: `Bearer ${worker.body.token}` }
    });
    assert.equal(acceptRes.status, 200);

    const checkInRes = await fetch(`${base}/api/attendance/check-in`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${worker.body.token}` },
      body: JSON.stringify({ assignmentId: assignment.id })
    });
    assert.equal(checkInRes.status, 201);

    const checkOutRes = await fetch(`${base}/api/attendance/check-out`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${worker.body.token}` },
      body: JSON.stringify({ assignmentId: assignment.id })
    });
    assert.equal(checkOutRes.status, 201);

    const assignmentDetailRes = await fetch(`${base}/api/assignments/${assignment.id}`, {
      headers: { authorization: `Bearer ${business.body.token}` }
    });
    const assignmentDetail = await assignmentDetailRes.json();
    assert.equal(assignmentDetailRes.status, 200);
    assert.equal(assignmentDetail.item.status, 'completed_pending_rating');
    assert.equal(assignmentDetail.item.productStatus, 'checked_out');

    const payoutRes = await fetch(`${base}/api/escrow/release/${assignment.id}`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${business.body.token}` },
      body: JSON.stringify({ force: false })
    });
    assert.equal(payoutRes.status, 201);
    const payout = await payoutRes.json();
    assert.equal(payout.item.status, 'created');
  });
});
