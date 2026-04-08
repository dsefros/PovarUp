CREATE TABLE assignments (
  id TEXT PRIMARY KEY,
  shift_id TEXT NOT NULL,
  application_id TEXT NOT NULL,
  worker_id TEXT NOT NULL,
  business_id TEXT NOT NULL,
  status TEXT NOT NULL,
  escrow_locked_cents INTEGER NOT NULL,
  contact_reveal_stage TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
