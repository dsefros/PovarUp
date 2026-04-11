const http = require('node:http');
const path = require('node:path');
const { hashPassword } = require('./domain/passwords');
const { seed } = require('./domain/store');
const { createMarketplaceRepository } = require('./repositories/marketplaceRepository');
const { createSqlMarketplaceRepository } = require('./repositories/sqlMarketplaceRepository');
const { createSqliteDatabase } = require('./domain/sqlite');
const { createMarketplaceService } = require('./services/marketplaceService');
const { createApp } = require('./http/app');
const { handleError } = require('./http/http');

function backfillLegacyPasswords(repo) {
  if (!repo.listAccounts || !repo.updateAccountPassword) return;
  const accounts = repo.listAccounts();
  for (const account of accounts) {
    const value = account.password || account.password_hash;
    if (!value || String(value).startsWith('scrypt$')) continue;
    repo.updateAccountPassword(account.user_id, hashPassword(String(value)));
  }
}

function createSqlSeed(database) {
  const workerHash = hashPassword('workerpass');
  const businessHash = hashPassword('businesspass');
  const adminHash = hashPassword('adminpass');
  const workerTwoHash = hashPassword('worker2pass');
  return () => {
    database.exec(`
      INSERT OR IGNORE INTO accounts (id, user_id, password, role, display_name) VALUES ('acct_worker_1', 'worker.demo', '${workerHash}', 'worker', 'Alex Cook');
      INSERT OR IGNORE INTO accounts (id, user_id, password, role, display_name) VALUES ('acct_biz_1', 'business.demo', '${businessHash}', 'business', 'Kitchen Hub Manager');
      INSERT OR IGNORE INTO accounts (id, user_id, password, role, display_name) VALUES ('acct_admin_1', 'admin.demo', '${adminHash}', 'admin', 'Platform Admin');
      INSERT OR IGNORE INTO accounts (id, user_id, password, role, display_name) VALUES ('acct_worker_2', 'worker2.demo', '${workerTwoHash}', 'worker', 'Sam Prep');
      INSERT OR IGNORE INTO onboarding_invites (code, role, business_name, location_name, location_address) VALUES ('WORKER-DEMO-2026', 'worker', null, null, null);
      INSERT OR IGNORE INTO onboarding_invites (code, role, business_name, location_name, location_address) VALUES ('BUSINESS-DEMO-2026', 'business', 'New Business', 'Primary Kitchen', 'TBD');
      INSERT OR IGNORE INTO worker_profiles (id, user_id, name, rating_avg) VALUES ('worker_1', 'worker.demo', 'Alex Cook', 4.8);
      INSERT OR IGNORE INTO worker_profiles (id, user_id, name, rating_avg) VALUES ('worker_2', 'worker2.demo', 'Sam Prep', 4.7);
      INSERT OR IGNORE INTO businesses (id, user_id, name) VALUES ('biz_1', 'business.demo', 'Kitchen Hub');
      INSERT OR IGNORE INTO locations (id, business_id, name, address) VALUES ('loc_1', 'biz_1', 'Downtown Kitchen', '100 Main St');
      INSERT OR IGNORE INTO escrow_accounts (id, business_id, balance_cents) VALUES ('escrow_biz_1', 'biz_1', 500000);
      INSERT OR IGNORE INTO shifts (id, business_id, location_id, title, start_at, end_at, pay_rate_cents, status, created_at)
      VALUES ('shift_1', 'biz_1', 'loc_1', 'Line Cook', datetime('now', '+1 hour'), datetime('now', '+5 hour'), 2500, 'published', CURRENT_TIMESTAMP);
    `);
  };
}

function resolveRuntime(overrides = {}) {
  const persistence = (overrides.persistence || process.env.POVARUP_PERSISTENCE || 'sql').toLowerCase();
  if (persistence === 'memory') {
    seed();
    const repo = createMarketplaceRepository();
    backfillLegacyPasswords(repo);
    return { repo, seed, service: createMarketplaceService(repo), persistence: 'memory' };
  }

  const dbFile = overrides.dbPath || process.env.POVARUP_DB_PATH || path.resolve(__dirname, '../data/povarup.sqlite');
  const database = createSqliteDatabase(dbFile);
  const repo = createSqlMarketplaceRepository(database);
  const sqlSeed = createSqlSeed(database);
  sqlSeed();
  backfillLegacyPasswords(repo);
  return { repo, seed: sqlSeed, service: createMarketplaceService(repo), persistence: 'sql', database };
}

function createServer(overrides = {}) {
  const runtime = resolveRuntime(overrides);
  const app = createApp({ repo: runtime.repo, seed: runtime.seed, service: runtime.service });
  const server = http.createServer(async (req, res) => {
    try {
      await app(req, res);
    } catch (err) {
      handleError(res, err);
    }
  });
  return { server, runtime };
}

if (require.main === module) {
  const { server, runtime } = createServer();
  const port = process.env.PORT || 4000;
  server.listen(port, () => console.log(`Backend listening on ${port} (${runtime.persistence})`));
}

module.exports = { createServer, resolveRuntime };
