const { randomUUID } = require('node:crypto');
const { hashPassword } = require('./passwords');

const nowIso = () => new Date().toISOString();

const db = {
  sessions: [],
  accounts: [],
  onboarding_invites: [],
  worker_profiles: [],
  businesses: [],
  locations: [],
  shifts: [],
  applications: [],
  assignments: [],
  attendance_events: [],
  ratings: [],
  chats: [],
  messages: [],
  escrow_accounts: [],
  escrow_transactions: [],
  payouts: [],
  violation_flags: []
};

const id = (prefix) => `${prefix}_${randomUUID().replace(/-/g, '').slice(0, 10)}`;

function seed() {
  if (db.worker_profiles.length) return;
  db.accounts.push({ id: 'acct_worker_1', user_id: 'worker.demo', password: hashPassword('workerpass'), role: 'worker', display_name: 'Alex Cook' });
  db.accounts.push({ id: 'acct_biz_1', user_id: 'business.demo', password: hashPassword('businesspass'), role: 'business', display_name: 'Kitchen Hub Manager' });
  db.accounts.push({ id: 'acct_admin_1', user_id: 'admin.demo', password: hashPassword('adminpass'), role: 'admin', display_name: 'Platform Admin' });
  db.accounts.push({ id: 'acct_worker_2', user_id: 'worker2.demo', password: hashPassword('worker2pass'), role: 'worker', display_name: 'Sam Prep' });
  db.onboarding_invites.push({ code: 'WORKER-DEMO-2026', role: 'worker' });
  db.onboarding_invites.push({ code: 'BUSINESS-DEMO-2026', role: 'business', business_name: 'New Business', location_name: 'Primary Kitchen', location_address: 'TBD' });
  db.worker_profiles.push({ id: 'worker_1', user_id: 'worker.demo', name: 'Alex Cook', rating_avg: 4.8 });
  db.worker_profiles.push({ id: 'worker_2', user_id: 'worker2.demo', name: 'Sam Prep', rating_avg: 4.7 });
  db.businesses.push({ id: 'biz_1', user_id: 'business.demo', name: 'Kitchen Hub' });
  db.locations.push({ id: 'loc_1', business_id: 'biz_1', name: 'Downtown Kitchen', address: '100 Main St' });
  db.escrow_accounts.push({ id: 'escrow_biz_1', business_id: 'biz_1', balance_cents: 500000 });
  db.shifts.push({
    id: 'shift_1',
    business_id: 'biz_1',
    location_id: 'loc_1',
    title: 'Line Cook',
    start_at: new Date(Date.now() + 3600_000).toISOString(),
    end_at: new Date(Date.now() + 5 * 3600_000).toISOString(),
    pay_rate_cents: 2500,
    status: 'open',
    created_at: nowIso()
  });
}

module.exports = { db, id, nowIso, seed };
