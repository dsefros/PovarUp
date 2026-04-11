ALTER TABLE sessions ADD COLUMN expires_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS idx_applications_shift_worker_unique ON applications(shift_id, worker_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_assignments_application_unique ON assignments(application_id);

ALTER TABLE payouts ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE payouts ADD COLUMN note TEXT;
