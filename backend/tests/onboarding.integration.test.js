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

async function onboard(base, payload) {
  const res = await fetch(`${base}/api/auth/onboard`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return { status: res.status, body: await res.json() };
}

async function login(base, userId, password) {
  const res = await fetch(`${base}/api/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ userId, password })
  });
  return { status: res.status, body: await res.json() };
}

test('worker onboarding creates usable profile and can apply', async () => {
  await withServer(async (base) => {
    const onboarded = await onboard(base, {
      inviteCode: 'WORKER-DEMO-2026',
      userId: 'new.worker.demo',
      password: 'pass123',
      displayName: 'Nina Worker'
    });
    assert.equal(onboarded.status, 201);

    const session = await login(base, 'new.worker.demo', 'pass123');
    assert.equal(session.status, 200);
    assert.equal(session.body.role, 'worker');

    const applyRes = await fetch(`${base}/api/applications`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${session.body.token}` },
      body: JSON.stringify({ shiftId: 'shift_1' })
    });
    assert.equal(applyRes.status, 201);
  });
});

test('business onboarding creates usable business and location and can create shift', async () => {
  await withServer(async (base) => {
    const onboarded = await onboard(base, {
      inviteCode: 'BUSINESS-DEMO-2026',
      userId: 'new.business.demo',
      password: 'pass123',
      displayName: 'Fresh Kitchens'
    });
    assert.equal(onboarded.status, 201);

    const session = await login(base, 'new.business.demo', 'pass123');
    assert.equal(session.status, 200);
    assert.equal(session.body.role, 'business');

    const shiftsRes = await fetch(`${base}/api/business/shifts`, {
      headers: { authorization: `Bearer ${session.body.token}` }
    });
    assert.equal(shiftsRes.status, 200);

    const createShift = await fetch(`${base}/api/shifts`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${session.body.token}` },
      body: JSON.stringify({
        locationId: 'loc_1',
        title: 'Temp bad location should fail',
        startAt: new Date().toISOString(),
        endAt: new Date(Date.now() + 3600000).toISOString(),
        payRateCents: 2100
      })
    });
    assert.equal(createShift.status, 403);

    const locationId = onboarded.body.item.locationId;
    assert.ok(locationId);
    const createShift2 = await fetch(`${base}/api/shifts`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${session.body.token}` },
      body: JSON.stringify({
        locationId,
        title: 'Onboarded Shift',
        startAt: new Date().toISOString(),
        endAt: new Date(Date.now() + 3600000).toISOString(),
        payRateCents: 2100
      })
    });
    assert.equal(createShift2.status, 201);
  });
});
