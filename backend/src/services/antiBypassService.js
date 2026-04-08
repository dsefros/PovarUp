const PHONE_RE = /\+?\d[\d\s().-]{6,}\d/g;
const EMAIL_RE = /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/ig;

function detectViolation(text) {
  const reasons = [];
  if (PHONE_RE.test(text)) reasons.push('phone_detected');
  if (EMAIL_RE.test(text)) reasons.push('email_detected');
  return reasons;
}

function maskedContact(real, assignmentId) {
  return `relay_${assignmentId}_${Buffer.from(real).toString('base64').slice(0, 8)}`;
}

module.exports = { detectViolation, maskedContact };
