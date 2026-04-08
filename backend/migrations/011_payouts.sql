CREATE TABLE payouts (
  id TEXT PRIMARY KEY,
  assignment_id TEXT NOT NULL,
  worker_id TEXT NOT NULL,
  amount_cents INTEGER NOT NULL,
  status TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
