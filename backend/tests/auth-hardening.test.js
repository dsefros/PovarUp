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

test('login uses hashed password and returns expiring session; logout invalidates token', async () => {
  await withServer(async (base, runtime) => {
    const account = runtime.repo.findAccountByUserId('worker.demo');
    assert.ok(account.password.startsWith('scrypt$'));
    assert.equal(account.password.includes('workerpass'), false);

    const login = await fetch(`${base}/api/auth/login`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ userId: 'worker.demo', password: 'workerpass' })
    });
    assert.equal(login.status, 200);
    const session = await login.json();
    assert.ok(session.expiresAt);

    const authed = await fetch(`${base}/api/applications`, {
      headers: { authorization: `Bearer ${session.token}` }
    });
    assert.equal(authed.status, 200);

    const logout = await fetch(`${base}/api/auth/logout`, {
      method: 'POST',
      headers: { authorization: `Bearer ${session.token}` }
    });
    assert.equal(logout.status, 200);

    const after = await fetch(`${base}/api/applications`, {
      headers: { authorization: `Bearer ${session.token}` }
    });
    assert.equal(after.status, 401);
  });
});

test('session expiry is enforced during authorization', async () => {
  await withServer(async (base, runtime) => {
    runtime.repo.insertSession({ token: 'expired_test', user_id: 'worker.demo', role: 'worker', expires_at: '2000-01-01T00:00:00.000Z' });
    const res = await fetch(`${base}/api/applications`, { headers: { authorization: 'Bearer expired_test' } });
    assert.equal(res.status, 401);
    const body = await res.json();
    assert.equal(body.error.code, 'session_expired');
  });
});
