const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { createServer } = require('../src/server');

function tmpDb() {
  return path.join(os.tmpdir(), `povarup-upgrade-${Date.now()}-${Math.random().toString(16).slice(2)}.sqlite`);
}

test('startup backfills legacy plaintext account passwords to scrypt hash', async () => {
  const dbPath = tmpDb();
  const first = createServer({ persistence: 'sql', dbPath });
  first.server.close();
  first.runtime.database.exec("UPDATE accounts SET password = 'workerpass' WHERE user_id = 'worker.demo'");
  first.runtime.database.close();

  const second = createServer({ persistence: 'sql', dbPath });
  await new Promise((resolve) => second.server.listen(0, resolve));
  try {
    const account = second.runtime.repo.findAccountByUserId('worker.demo');
    assert.ok(account.password.startsWith('scrypt$'));

    const base = `http://127.0.0.1:${second.server.address().port}`;
    const login = await fetch(`${base}/api/auth/login`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ userId: 'worker.demo', password: 'workerpass' })
    });
    assert.equal(login.status, 200);
  } finally {
    await new Promise((resolve) => second.server.close(resolve));
    second.runtime.database.close();
    if (fs.existsSync(dbPath)) fs.rmSync(dbPath, { force: true });
  }
});
