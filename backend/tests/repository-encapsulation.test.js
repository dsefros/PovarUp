const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('marketplace service does not directly access repo.store', () => {
  const servicePath = path.join(__dirname, '../src/services/marketplaceService.js');
  const source = fs.readFileSync(servicePath, 'utf8');
  assert.equal(source.includes('repo.store'), false);
});
