const fs = require('node:fs');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');

function ensureDirFor(filePath) {
  const dir = path.dirname(filePath);
  if (dir && dir !== '.') fs.mkdirSync(dir, { recursive: true });
}

function runMigrations(db) {
  db.exec(`
    CREATE TABLE IF NOT EXISTS schema_migrations (
      id TEXT PRIMARY KEY,
      applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
  `);

  const appliedStmt = db.prepare('SELECT 1 FROM schema_migrations WHERE id = ? LIMIT 1');
  const markStmt = db.prepare('INSERT INTO schema_migrations (id) VALUES (?)');

  const migrationsDir = path.resolve(__dirname, '../../migrations');
  const files = fs.readdirSync(migrationsDir).filter((f) => f.endsWith('.sql')).sort();
  for (const file of files) {
    const already = appliedStmt.get(file);
    if (already) continue;
    const sql = fs.readFileSync(path.join(migrationsDir, file), 'utf8').trim();
    if (sql) db.exec(sql);
    markStmt.run(file);
  }
}

function createSqliteDatabase(filePath) {
  if (filePath !== ':memory:') ensureDirFor(filePath);
  const db = new DatabaseSync(filePath);
  runMigrations(db);
  return db;
}

module.exports = { createSqliteDatabase };
