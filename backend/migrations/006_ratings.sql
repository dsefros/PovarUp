CREATE TABLE ratings (
  id TEXT PRIMARY KEY,
  assignment_id TEXT NOT NULL,
  from_role TEXT NOT NULL,
  score INTEGER NOT NULL,
  note TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
