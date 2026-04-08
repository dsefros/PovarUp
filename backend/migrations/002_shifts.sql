CREATE TABLE shifts (
  id TEXT PRIMARY KEY,
  business_id TEXT NOT NULL,
  location_id TEXT NOT NULL,
  title TEXT NOT NULL,
  start_at TIMESTAMP NOT NULL,
  end_at TIMESTAMP NOT NULL,
  pay_rate_cents INTEGER NOT NULL,
  status TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
