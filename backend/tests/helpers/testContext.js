const { db, seed } = require('../../src/domain/store');

function resetDb() {
  Object.keys(db).forEach((k) => (db[k].length = 0));
  seed();
}

module.exports = { db, resetDb };
