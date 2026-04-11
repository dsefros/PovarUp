CREATE TABLE IF NOT EXISTS accounts (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL UNIQUE,
  password TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('worker', 'business', 'admin')),
  display_name TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS onboarding_invites (
  code TEXT PRIMARY KEY,
  role TEXT NOT NULL CHECK (role IN ('worker', 'business')),
  business_name TEXT,
  location_name TEXT,
  location_address TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
