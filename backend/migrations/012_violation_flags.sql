CREATE TABLE violation_flags (
  id TEXT PRIMARY KEY,
  assignment_id TEXT NOT NULL,
  actor_id TEXT NOT NULL,
  reasons TEXT NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
