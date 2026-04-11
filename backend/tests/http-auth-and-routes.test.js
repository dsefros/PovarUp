const test = require('node:test');
const assert = require('node:assert/strict');
const { buildTestRuntime } = require('./helpers/testContext');

async function withServer(run) {
  const { server, runtime, cleanup } = buildTestRuntime();
  const app = runtime.service.applyToShift('shift_1', 'worker_1');
  const asn = runtime.service.offerAssignment(app.id, 'biz_1');
  await new Promise((resolve) => server.listen(0, resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    await run(base, asn.id, runtime);
  } finally {
    await new Promise((resolve) => server.close(resolve));
    cleanup();
  }
}

async function createSession(base, userId, password) {
  const res = await fetch(`${base}/api/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ userId, password })
  });
  return res.json();
}

test('unauthorized mutation is rejected', async () => {
  await withServer(async (base) => {
    const res = await fetch(`${base}/api/applications`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ shiftId: 'shift_1' })
    });
    assert.equal(res.status, 401);
  });
});

test('authorized worker can mutate and dto keys are camelCase', async () => {
  await withServer(async (base) => {
    const sess = await createSession(base, 'worker.demo', 'workerpass');
    const res = await fetch(`${base}/api/applications`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${sess.token}` },
      body: JSON.stringify({ shiftId: 'shift_1' })
    });
    assert.equal(res.status, 201);
    const body = await res.json();
    assert.equal(typeof body.item.shiftId, 'string');
    assert.equal(body.item.shift_id, undefined);
  });
});

test('service validation missing entity returns mapped status', async () => {
  await withServer(async (base) => {
    const sess = await createSession(base, 'worker.demo', 'workerpass');
    const res = await fetch(`${base}/api/applications`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${sess.token}` },
      body: JSON.stringify({ shiftId: 'shift_missing' })
    });
    assert.equal(res.status, 404);
    const body = await res.json();
    assert.equal(body.error.code, 'shift_not_found');
  });
});

test('rating authorization enforces participant role and duplicate prevention', async () => {
  await withServer(async (base, assignmentId) => {
    const worker = await createSession(base, 'worker.demo', 'workerpass');
    const biz = await createSession(base, 'business.demo', 'businesspass');

    const invalidRole = await fetch(`${base}/api/ratings`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${worker.token}` },
      body: JSON.stringify({ assignmentId, fromRole: 'business', score: 5, note: 'x' })
    });
    assert.equal(invalidRole.status, 403);

    const first = await fetch(`${base}/api/ratings`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${worker.token}` },
      body: JSON.stringify({ assignmentId, fromRole: 'worker', score: 5, note: 'ok' })
    });
    assert.equal(first.status, 201);

    const dup = await fetch(`${base}/api/ratings`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${worker.token}` },
      body: JSON.stringify({ assignmentId, fromRole: 'worker', score: 4, note: 'dup' })
    });
    assert.equal(dup.status, 409);

    const bizRate = await fetch(`${base}/api/ratings`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${biz.token}` },
      body: JSON.stringify({ assignmentId, fromRole: 'business', score: 5, note: 'good' })
    });
    assert.equal(bizRate.status, 201);
  });
});

test('chat message sender identity is derived from session and cannot be spoofed', async () => {
  await withServer(async (base, assignmentId) => {
    const worker = await createSession(base, 'worker.demo', 'workerpass');
    const res = await fetch(`${base}/api/chats/${assignmentId}/messages`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${worker.token}` },
      body: JSON.stringify({ senderId: 'biz_1', text: 'hello' })
    });
    assert.equal(res.status, 201);
    const body = await res.json();
    assert.equal(body.item.senderId, 'worker_1');
  });
});

test('sensitive reads enforce authorization', async () => {
  await withServer(async (base, assignmentId, runtime) => {
    const worker = await createSession(base, 'worker.demo', 'workerpass');
    const biz = await createSession(base, 'business.demo', 'businesspass');
    const outsider = await createSession(base, 'worker2.demo', 'worker2pass');
    const admin = await createSession(base, 'admin.demo', 'adminpass');

    const noAuthApplications = await fetch(`${base}/api/applications`);
    assert.equal(noAuthApplications.status, 401);

    const outsiderApplications = await fetch(`${base}/api/applications`, {
      headers: { authorization: `Bearer ${outsider.token}` }
    });
    const outsiderAppsBody = await outsiderApplications.json();
    assert.equal(outsiderAppsBody.items.length, 0);

    const bizApplications = await fetch(`${base}/api/applications`, {
      headers: { authorization: `Bearer ${biz.token}` }
    });
    const bizAppsBody = await bizApplications.json();
    assert.equal(bizAppsBody.items.length, 1);

    const adminApplications = await fetch(`${base}/api/applications`, {
      headers: { authorization: `Bearer ${admin.token}` }
    });
    const adminAppsBody = await adminApplications.json();
    assert.equal(adminAppsBody.items.length, 1);

    const outsiderAssignments = await fetch(`${base}/api/assignments`, {
      headers: { authorization: `Bearer ${outsider.token}` }
    });
    assert.equal(outsiderAssignments.status, 200);
    const aBody = await outsiderAssignments.json();
    assert.equal(aBody.items.length, 0);

    const outsiderChat = await fetch(`${base}/api/chats/by-assignment/${assignmentId}`, {
      headers: { authorization: `Bearer ${outsider.token}` }
    });
    assert.equal(outsiderChat.status, 403);

    const outsiderPayouts = await fetch(`${base}/api/payouts/worker_1`, {
      headers: { authorization: `Bearer ${outsider.token}` }
    });
    assert.equal(outsiderPayouts.status, 403);

    const workerMePayouts = await fetch(`${base}/api/me/payouts`, {
      headers: { authorization: `Bearer ${worker.token}` }
    });
    assert.equal(workerMePayouts.status, 200);

    const businessMePayouts = await fetch(`${base}/api/me/payouts`, {
      headers: { authorization: `Bearer ${biz.token}` }
    });
    assert.equal(businessMePayouts.status, 403);


    const noAuthContacts = await fetch(`${base}/api/contacts/reveal/${assignmentId}`);
    assert.equal(noAuthContacts.status, 401);

    const outsiderContacts = await fetch(`${base}/api/contacts/reveal/${assignmentId}`, {
      headers: { authorization: `Bearer ${outsider.token}` }
    });
    assert.equal(outsiderContacts.status, 403);

    runtime.service.acceptAssignment(assignmentId);
    const ownerContacts = await fetch(`${base}/api/contacts/reveal/${assignmentId}`, {
      headers: { authorization: `Bearer ${worker.token}` }
    });
    assert.equal(ownerContacts.status, 200);

    const noAuthBalance = await fetch(`${base}/api/escrow/balance/biz_1`);
    assert.equal(noAuthBalance.status, 401);

    const outsiderBalance = await fetch(`${base}/api/escrow/balance/biz_1`, {
      headers: { authorization: `Bearer ${outsider.token}` }
    });
    assert.equal(outsiderBalance.status, 403);

    const ownerBalance = await fetch(`${base}/api/escrow/balance/biz_1`, {
      headers: { authorization: `Bearer ${biz.token}` }
    });
    assert.equal(ownerBalance.status, 200);

    const adminBalance = await fetch(`${base}/api/escrow/balance/biz_1`, {
      headers: { authorization: `Bearer ${admin.token}` }
    });
    assert.equal(adminBalance.status, 200);

    const nonAdminFlags = await fetch(`${base}/api/violation-flags`, {
      headers: { authorization: `Bearer ${worker.token}` }
    });
    assert.equal(nonAdminFlags.status, 403);

    const adminFlags = await fetch(`${base}/api/violation-flags`, {
      headers: { authorization: `Bearer ${admin.token}` }
    });
    assert.equal(adminFlags.status, 200);

    const validBizAssignments = await fetch(`${base}/api/assignments`, {
      headers: { authorization: `Bearer ${biz.token}` }
    });
    const validBody = await validBizAssignments.json();
    assert.equal(validBody.items.length, 1);
  });
});
