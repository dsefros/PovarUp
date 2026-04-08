CREATE TABLE applications (
  id TEXT PRIMARY KEY,
  shift_id TEXT NOT NULL,
  worker_id TEXT NOT NULL,
  status TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
