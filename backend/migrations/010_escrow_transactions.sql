CREATE TABLE escrow_transactions (
  id TEXT PRIMARY KEY,
  business_id TEXT NOT NULL,
  assignment_id TEXT,
  type TEXT NOT NULL,
  amount_cents INTEGER NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
