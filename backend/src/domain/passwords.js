const { randomBytes, scryptSync, timingSafeEqual } = require('node:crypto');

function hashPassword(password) {
  const salt = randomBytes(16).toString('hex');
  const digest = scryptSync(password, salt, 64).toString('hex');
  return `scrypt$${salt}$${digest}`;
}

function verifyPassword(password, encodedHash) {
  if (!encodedHash) return false;
  const parts = encodedHash.split('$');
  if (parts.length !== 3 || parts[0] !== 'scrypt') return false;
  const [, salt, digestHex] = parts;
  const digest = Buffer.from(digestHex, 'hex');
  const supplied = scryptSync(password, salt, digest.length);
  return digest.length === supplied.length && timingSafeEqual(digest, supplied);
}

module.exports = { hashPassword, verifyPassword };
