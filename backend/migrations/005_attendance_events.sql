CREATE TABLE attendance_events (
  id TEXT PRIMARY KEY,
  assignment_id TEXT NOT NULL,
  type TEXT NOT NULL,
  timestamp TIMESTAMP NOT NULL
);
