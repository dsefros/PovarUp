const { db, id, nowIso } = require('../domain/store');
const { detectViolation, maskedContact } = require('./antiBypassService');

function createShift(input) {
  const shift = { id: id('shift'), status: 'open', created_at: nowIso(), ...input };
  db.shifts.push(shift);
  return shift;
}

function applyToShift(shiftId, workerId) {
  const application = { id: id('app'), shift_id: shiftId, worker_id: workerId, status: 'applied', created_at: nowIso() };
  db.applications.push(application);
  return application;
}

function offerAssignment(applicationId, businessId) {
  const application = db.applications.find((a) => a.id === applicationId);
  if (!application) throw new Error('application_not_found');
  const shift = db.shifts.find((s) => s.id === application.shift_id);
  const required = ((new Date(shift.end_at) - new Date(shift.start_at)) / 3600000) * shift.pay_rate_cents;
  const escrow = db.escrow_accounts.find((e) => e.business_id === businessId);
  if (!escrow || escrow.balance_cents < required) throw new Error('insufficient_escrow');
  escrow.balance_cents -= required;
  db.escrow_transactions.push({ id: id('etx'), business_id: businessId, assignment_id: null, type: 'lock', amount_cents: required, created_at: nowIso() });

  const assignment = {
    id: id('asn'),
    shift_id: shift.id,
    application_id: application.id,
    worker_id: application.worker_id,
    business_id: businessId,
    status: 'offered',
    escrow_locked_cents: required,
    contact_reveal_stage: 'accepted',
    created_at: nowIso()
  };
  db.assignments.push(assignment);
  let chat = db.chats.find((c) => c.assignment_id === assignment.id);
  if (!chat) {
    chat = { id: id('chat'), assignment_id: assignment.id, worker_id: assignment.worker_id, business_id: businessId, created_at: nowIso() };
    db.chats.push(chat);
  }
  return assignment;
}

function acceptAssignment(assignmentId) {
  const assignment = db.assignments.find((a) => a.id === assignmentId);
  if (!assignment) throw new Error('assignment_not_found');
  assignment.status = 'active';
  assignment.accepted_at = nowIso();
  return assignment;
}

function attendance(assignmentId, type) {
  const assignment = db.assignments.find((a) => a.id === assignmentId);
  const shift = db.shifts.find((s) => s.id === assignment.shift_id);
  const now = Date.now();
  if (type === 'check_in') {
    const start = new Date(shift.start_at).getTime();
    if (now < start - 30 * 60000 || now > start + 60 * 60000) throw new Error('checkin_window_invalid');
    assignment.status = 'in_progress';
  }
  if (type === 'check_out') {
    const hasIn = db.attendance_events.some((e) => e.assignment_id === assignmentId && e.type === 'check_in');
    if (!hasIn) throw new Error('checkin_required');
    assignment.status = 'completed_pending_rating';
  }
  const event = { id: id('att'), assignment_id: assignmentId, type, timestamp: nowIso() };
  db.attendance_events.push(event);
  return event;
}

function addRating(assignmentId, fromRole, score, note) {
  const rating = { id: id('rat'), assignment_id: assignmentId, from_role: fromRole, score, note, created_at: nowIso() };
  db.ratings.push(rating);
  const all = db.ratings.filter((r) => r.assignment_id === assignmentId);
  if (all.some((r) => r.from_role === 'worker') && all.some((r) => r.from_role === 'business')) {
    const assignment = db.assignments.find((a) => a.id === assignmentId);
    assignment.status = 'completed_rated';
  }
  return rating;
}

function getChatByAssignment(assignmentId) {
  return db.chats.find((c) => c.assignment_id === assignmentId);
}

function sendMessage(assignmentId, senderId, text) {
  const assignment = db.assignments.find((a) => a.id === assignmentId);
  if (!assignment || !['offered', 'active', 'in_progress', 'completed_pending_rating', 'completed_rated'].includes(assignment.status)) {
    throw new Error('chat_not_available');
  }
  const reasons = detectViolation(text);
  if (reasons.length) {
    db.violation_flags.push({ id: id('vio'), assignment_id: assignmentId, actor_id: senderId, reasons, content: text, created_at: nowIso() });
  }
  const chat = getChatByAssignment(assignmentId);
  const msg = { id: id('msg'), chat_id: chat.id, sender_id: senderId, text, created_at: nowIso(), violation_detected: reasons.length > 0 };
  db.messages.push(msg);
  return msg;
}

function revealContacts(assignmentId, stage = 'accepted') {
  const assignment = db.assignments.find((a) => a.id === assignmentId);
  if (!assignment) throw new Error('assignment_not_found');
  if (stage === 'check_in' && assignment.status !== 'in_progress') throw new Error('contact_reveal_not_allowed');
  if (stage === 'accepted' && !['active', 'in_progress', 'completed_pending_rating', 'completed_rated'].includes(assignment.status)) throw new Error('contact_reveal_not_allowed');
  return {
    worker_contact: maskedContact('worker@example.com', assignmentId),
    business_contact: maskedContact('biz@example.com', assignmentId)
  };
}

function fundEscrow(businessId, amount) {
  let account = db.escrow_accounts.find((a) => a.business_id === businessId);
  if (!account) {
    account = { id: id('escrow'), business_id: businessId, balance_cents: 0 };
    db.escrow_accounts.push(account);
  }
  account.balance_cents += amount;
  const tx = { id: id('etx'), business_id: businessId, type: 'fund', amount_cents: amount, created_at: nowIso() };
  db.escrow_transactions.push(tx);
  return { account, tx };
}

function releasePayment(assignmentId, force = false) {
  const assignment = db.assignments.find((a) => a.id === assignmentId);
  if (!assignment) throw new Error('assignment_not_found');
  if (!['completed_pending_rating', 'completed_rated'].includes(assignment.status) && !force) throw new Error('assignment_not_completed');
  const payout = { id: id('pay'), assignment_id: assignmentId, worker_id: assignment.worker_id, amount_cents: assignment.escrow_locked_cents, status: 'created', created_at: nowIso() };
  db.payouts.push(payout);
  db.escrow_transactions.push({ id: id('etx'), business_id: assignment.business_id, assignment_id: assignmentId, type: 'release', amount_cents: assignment.escrow_locked_cents, created_at: nowIso() });
  return payout;
}

module.exports = {
  createShift,
  applyToShift,
  offerAssignment,
  acceptAssignment,
  attendance,
  addRating,
  getChatByAssignment,
  sendMessage,
  revealContacts,
  fundEscrow,
  releasePayment
};
