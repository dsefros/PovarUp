const { id, nowIso } = require('../domain/store');
const { detectViolation, maskedContact } = require('./antiBypassService');
const { createMarketplaceRepository } = require('../repositories/marketplaceRepository');
const { error } = require('../domain/errors');
const { requireNumber, requireOneOf, requireString } = require('../domain/validation');

function createMarketplaceService(repo = createMarketplaceRepository()) {
  function createShift(input) {
    const businessId = requireString(input.business_id || input.businessId, 'businessId');
    const locationId = requireString(input.location_id || input.locationId, 'locationId');
    requireString(input.title, 'title');
    requireString(input.start_at || input.startAt, 'startAt');
    requireString(input.end_at || input.endAt, 'endAt');
    const payRateCents = requireNumber(input.pay_rate_cents ?? input.payRateCents, 'payRateCents', { min: 1 });

    if (!repo.findBusinessById(businessId)) throw error('business_not_found', 'business not found', { status: 404 });
    if (!repo.findLocationById(locationId)) throw error('location_not_found', 'location not found', { status: 404 });

    const shift = {
      id: id('shift'),
      business_id: businessId,
      location_id: locationId,
      title: input.title,
      start_at: input.start_at || input.startAt,
      end_at: input.end_at || input.endAt,
      pay_rate_cents: payRateCents,
      status: 'open',
      created_at: nowIso()
    };
    repo.insertShift(shift);
    return shift;
  }

  function applyToShift(shiftId, workerId) {
    requireString(shiftId, 'shiftId');
    requireString(workerId, 'workerId');
    const shift = repo.findShiftById(shiftId);
    if (!shift) throw error('shift_not_found', 'shift not found', { status: 404 });
    if (shift.status !== 'open') throw error('shift_not_open', 'shift is not open', { status: 409 });
    if (!repo.findWorkerById(workerId)) throw error('worker_not_found', 'worker not found', { status: 404 });

    const application = { id: id('app'), shift_id: shiftId, worker_id: workerId, status: 'applied', created_at: nowIso() };
    repo.insertApplication(application);
    return application;
  }

  function offerAssignment(applicationId, businessId) {
    return repo.withTransaction(() => {
      const application = repo.findApplicationById(requireString(applicationId, 'applicationId'));
      if (!application) throw error('application_not_found', 'application not found', { status: 404 });
      const shift = repo.findShiftById(application.shift_id);
      if (!shift) throw error('shift_not_found', 'shift not found', { status: 404 });
      if (shift.business_id !== businessId) throw error('forbidden', 'business cannot offer assignment for this shift', { status: 403 });
      if (application.status !== 'applied') throw error('invalid_application_state', 'application state does not allow offer', { status: 409 });

      const required = Math.round(((new Date(shift.end_at) - new Date(shift.start_at)) / 3600000) * shift.pay_rate_cents);
      const escrow = repo.findEscrowByBusinessId(businessId);
      if (!escrow || escrow.balance_cents < required) throw error('insufficient_escrow', 'insufficient escrow balance', { status: 409 });
      repo.updateEscrowBalance(escrow.id, escrow.balance_cents - required);
      repo.insertEscrowTransaction({ id: id('etx'), business_id: businessId, assignment_id: null, type: 'lock', amount_cents: required, created_at: nowIso() });

      repo.updateApplicationStatus(application.id, 'offered');
      const assignment = {
        id: id('asn'), shift_id: shift.id, application_id: application.id, worker_id: application.worker_id, business_id: businessId,
        status: 'offered', escrow_locked_cents: required, contact_reveal_stage: 'accepted', created_at: nowIso()
      };
      repo.insertAssignment(assignment);
      if (!repo.findChatByAssignmentId(assignment.id)) {
        repo.insertChat({ id: id('chat'), assignment_id: assignment.id, worker_id: assignment.worker_id, business_id: businessId, created_at: nowIso() });
      }
      return assignment;
    });
  }

  function acceptAssignment(assignmentId) {
    const assignment = repo.findAssignmentById(requireString(assignmentId, 'assignmentId'));
    if (!assignment) throw error('assignment_not_found', 'assignment not found', { status: 404 });
    if (assignment.status !== 'offered') throw error('invalid_assignment_state', 'assignment cannot be accepted from current state', { status: 409 });
    const acceptedAt = nowIso();
    repo.updateAssignmentState(assignment.id, 'active', acceptedAt);
    return repo.findAssignmentById(assignment.id);
  }

  function attendance(assignmentId, type) {
    requireOneOf(type, 'type', ['check_in', 'check_out']);
    const assignment = repo.findAssignmentById(requireString(assignmentId, 'assignmentId'));
    if (!assignment) throw error('assignment_not_found', 'assignment not found', { status: 404 });
    const shift = repo.findShiftById(assignment.shift_id);
    if (!shift) throw error('shift_not_found', 'shift not found', { status: 404 });
    const now = Date.now();
    if (type === 'check_in') {
      if (assignment.status !== 'active') throw error('invalid_assignment_state', 'assignment must be active to check in', { status: 409 });
      const start = new Date(shift.start_at).getTime();
      if (now < start - 30 * 60000 || now > start + 60 * 60000) throw error('checkin_window_invalid', 'check-in outside allowed window', { status: 409 });
      repo.updateAssignmentState(assignment.id, 'in_progress');
    }
    if (type === 'check_out') {
      if (!['in_progress', 'completed_pending_rating'].includes(assignment.status)) throw error('invalid_assignment_state', 'assignment must be in progress to check out', { status: 409 });
      const hasIn = repo.hasAttendanceCheckIn(assignmentId);
      if (!hasIn) throw error('checkin_required', 'check-in required before check-out', { status: 409 });
      repo.updateAssignmentState(assignment.id, 'completed_pending_rating');
    }
    const event = { id: id('att'), assignment_id: assignmentId, type, timestamp: nowIso() };
    repo.insertAttendanceEvent(event);
    return event;
  }

  function addRating(assignmentId, fromRole, score, note) {
    const assignment = repo.findAssignmentById(requireString(assignmentId, 'assignmentId'));
    if (!assignment) throw error('assignment_not_found', 'assignment not found', { status: 404 });
    requireOneOf(fromRole, 'fromRole', ['worker', 'business']);
    requireNumber(score, 'score', { min: 1 });

    if (repo.hasRatingByRole(assignmentId, fromRole)) throw error('duplicate_rating', 'role already rated this assignment', { status: 409 });
    const rating = { id: id('rat'), assignment_id: assignmentId, from_role: fromRole, score, note: note || '', created_at: nowIso() };
    repo.insertRating(rating);
    const all = repo.listRatingsByAssignmentId(assignmentId);
    if (all.some((r) => r.from_role === 'worker') && all.some((r) => r.from_role === 'business')) {
      repo.updateAssignmentState(assignment.id, 'completed_rated');
    }
    return rating;
  }

  function getChatByAssignment(assignmentId) {
    const chat = repo.findChatByAssignmentId(requireString(assignmentId, 'assignmentId'));
    if (!chat) throw error('chat_not_found', 'chat not found', { status: 404 });
    return chat;
  }

  function sendMessage(assignmentId, senderId, text) {
    const assignment = repo.findAssignmentById(requireString(assignmentId, 'assignmentId'));
    if (!assignment || !['offered', 'active', 'in_progress', 'completed_pending_rating', 'completed_rated'].includes(assignment.status)) {
      throw error('chat_not_available', 'chat not available', { status: 409 });
    }
    requireString(senderId, 'senderId');
    if (![assignment.worker_id, assignment.business_id].includes(senderId)) throw error('forbidden', 'sender is not assignment participant', { status: 403 });
    const reasons = detectViolation(requireString(text, 'text'));
    if (reasons.length) repo.insertViolationFlag({ id: id('vio'), assignment_id: assignmentId, actor_id: senderId, reasons, content: text, created_at: nowIso() });
    const chat = getChatByAssignment(assignmentId);
    const msg = { id: id('msg'), chat_id: chat.id, sender_id: senderId, text, created_at: nowIso(), violation_detected: reasons.length > 0 };
    repo.insertMessage(msg);
    return msg;
  }

  function revealContacts(assignmentId, stage = 'accepted') {
    const assignment = repo.findAssignmentById(requireString(assignmentId, 'assignmentId'));
    if (!assignment) throw error('assignment_not_found', 'assignment not found', { status: 404 });
    if (stage === 'check_in' && assignment.status !== 'in_progress') throw error('contact_reveal_not_allowed', 'contact reveal not allowed', { status: 409 });
    if (stage === 'accepted' && !['active', 'in_progress', 'completed_pending_rating', 'completed_rated'].includes(assignment.status)) throw error('contact_reveal_not_allowed', 'contact reveal not allowed', { status: 409 });
    return { worker_contact: maskedContact('worker@example.com', assignmentId), business_contact: maskedContact('biz@example.com', assignmentId) };
  }

  function fundEscrow(businessId, amount) {
    requireString(businessId, 'businessId');
    requireNumber(amount, 'amountCents', { min: 1 });
    let account = repo.findEscrowByBusinessId(businessId);
    if (!account) {
      account = { id: id('escrow'), business_id: businessId, balance_cents: 0, created_at: nowIso() };
      repo.insertEscrowAccount(account);
    }
    repo.updateEscrowBalance(account.id, account.balance_cents + amount);
    const tx = { id: id('etx'), business_id: businessId, type: 'fund', amount_cents: amount, created_at: nowIso() };
    repo.insertEscrowTransaction(tx);
    return { account: repo.findEscrowByBusinessId(businessId), tx };
  }

  function releasePayment(assignmentId, force = false) {
    return repo.withTransaction(() => {
      const assignment = repo.findAssignmentById(requireString(assignmentId, 'assignmentId'));
      if (!assignment) throw error('assignment_not_found', 'assignment not found', { status: 404 });
      if (!['completed_pending_rating', 'completed_rated'].includes(assignment.status) && !force) throw error('assignment_not_completed', 'assignment not completed', { status: 409 });

      const existing = repo.findPayoutByAssignmentId(assignmentId);
      if (existing) return existing;

      const payout = { id: id('pay'), assignment_id: assignmentId, worker_id: assignment.worker_id, amount_cents: assignment.escrow_locked_cents, status: 'created', created_at: nowIso() };
      try {
        repo.insertPayout(payout);
      } catch (err) {
        if (String(err.message || '').includes('payouts.assignment_id')) {
          const concurrent = repo.findPayoutByAssignmentId(assignmentId);
          if (concurrent) return concurrent;
        }
        throw err;
      }
      repo.insertEscrowTransaction({ id: id('etx'), business_id: assignment.business_id, assignment_id: assignmentId, type: 'release', amount_cents: assignment.escrow_locked_cents, created_at: nowIso() });
      return payout;
    });
  }


  return { createShift, applyToShift, offerAssignment, acceptAssignment, attendance, addRating, getChatByAssignment, sendMessage, revealContacts, fundEscrow, releasePayment };
}

module.exports = { createMarketplaceService };
