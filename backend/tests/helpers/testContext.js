const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { createServer } = require('../../src/server');

function tempDbPath() {
  return path.join(os.tmpdir(), `povarup-test-${Date.now()}-${Math.random().toString(16).slice(2)}.sqlite`);
}

function buildTestRuntime() {
  const dbPath = tempDbPath();
  const { server, runtime } = createServer({ persistence: 'sql', dbPath });

  function cleanup() {
    if (runtime.database) runtime.database.close();
    if (fs.existsSync(dbPath)) fs.rmSync(dbPath, { force: true });
  }

  return { server, runtime, dbPath, cleanup };
}

module.exports = { buildTestRuntime };
