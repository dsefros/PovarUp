const { id, nowIso } = require('../domain/store');
const { detectViolation } = require('./antiBypassService');
const { createMarketplaceRepository } = require('../repositories/marketplaceRepository');
const { error } = require('../domain/errors');
const { requireNumber, requireOneOf, requireString } = require('../domain/validation');

function createMarketplaceService(repo = createMarketplaceRepository()) {
  const transitions = {
    shift: { draft: ['published', 'cancelled'], published: ['closed', 'cancelled'], closed: ['cancelled'], cancelled: [] },
    application: { applied: ['accepted', 'rejected', 'withdrawn'], accepted: ['withdrawn'], rejected: [], withdrawn: [] },
    assignment: { assigned: ['in_progress', 'cancelled'], in_progress: ['completed', 'cancelled'], completed: [], cancelled: [] },
    payout: { created: ['pending', 'failed'], pending: ['paid', 'failed'], paid: [], failed: ['pending'] }
  };

  function logTransition(entity, idValue, from, to, actor = 'system') {
    // lightweight structured logging
    console.log(JSON.stringify({ event: 'lifecycle_transition', entity, id: idValue, from, to, actor, at: nowIso() }));
  }

  function assertTransition(entity, current, next) {
    if (current === next) return true;
    if (!transitions[entity]?.[current]?.includes(next)) {
      throw error('invalid_transition', `invalid ${entity} transition: ${current} -> ${next}`, { status: 409 });
    }
    return false;
  }

  function createShift(input) {
    const businessId = requireString(input.business_id || input.businessId, 'businessId');
    const locationId = requireString(input.location_id || input.locationId, 'locationId');
    requireString(input.title, 'title');
    requireString(input.start_at || input.startAt, 'startAt');
    requireString(input.end_at || input.endAt, 'endAt');
    const payRateCents = requireNumber(input.pay_rate_cents ?? input.payRateCents, 'payRateCents', { min: 1 });

    if (!repo.findBusinessById(businessId)) throw error('business_not_found', 'business not found', { status: 404 });
    const location = repo.findLocationById(locationId);
    if (!location) throw error('location_not_found', 'location not found', { status: 404 });
    if (location.business_id !== businessId) throw error('forbidden', 'business cannot use this location', { status: 403 });

    const shift = {
      id: id('shift'),
      business_id: businessId,
      location_id: locationId,
      title: input.title,
      start_at: input.start_at || input.startAt,
      end_at: input.end_at || input.endAt,
      pay_rate_cents: payRateCents,
      status: 'draft',
      created_at: nowIso()
    };
    repo.insertShift(shift);
    return shift;
  }

  function updateShiftStatus(shiftId, nextStatus, actor = 'business') {
    requireString(shiftId, 'shiftId');
    requireOneOf(nextStatus, 'status', ['draft', 'published', 'closed', 'cancelled']);
    const shift = repo.findShiftById(shiftId);
    if (!shift) throw error('shift_not_found', 'shift not found', { status: 404 });
    const idempotent = assertTransition('shift', shift.status, nextStatus);
    if (idempotent) return shift;
    repo.updateShiftStatus(shiftId, nextStatus);
    logTransition('shift', shiftId, shift.status, nextStatus, actor);
    return repo.findShiftById(shiftId);
  }

  function applyToShift(shiftId, workerId) {
    requireString(shiftId, 'shiftId');
    requireString(workerId, 'workerId');
    const shift = repo.findShiftById(shiftId);
    if (!shift) throw error('shift_not_found', 'shift not found', { status: 404 });
    if (shift.status !== 'published') throw error('shift_not_published', 'shift is not published', { status: 409 });
    if (!repo.findWorkerById(workerId)) throw error('worker_not_found', 'worker not found', { status: 404 });
    const existingApplication = repo.listApplications().find((item) => item.shift_id === shiftId && item.worker_id === workerId);
    if (existingApplication) throw error('duplicate_application', 'worker already applied to this shift', { status: 409 });
    const existingAssignment = repo.listAssignments().find((item) => item.shift_id === shiftId && item.worker_id === workerId);
    if (existingAssignment) throw error('duplicate_assignment', 'worker is already assigned to this shift', { status: 409 });

    const application = { id: id('app'), shift_id: shiftId, worker_id: workerId, status: 'applied', created_at: nowIso() };
    try {
      repo.insertApplication(application);
    } catch (err) {
      if (String(err.message || '').includes('idx_applications_shift_worker_unique')) {
        throw error('duplicate_application', 'worker already applied to this shift', { status: 409 });
      }
      throw err;
    }
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

      repo.updateApplicationStatus(application.id, 'accepted');
      logTransition('application', application.id, application.status, 'accepted', 'business');
      const assignment = {
        id: id('asn'), shift_id: shift.id, application_id: application.id, worker_id: application.worker_id, business_id: businessId,
        status: 'assigned', escrow_locked_cents: required, contact_reveal_stage: 'accepted', created_at: nowIso()
      };
      try {
        repo.insertAssignment(assignment);
      } catch (err) {
        if (String(err.message || '').includes('idx_assignments_application_unique')) {
          throw error('duplicate_offer', 'assignment already offered for this application', { status: 409 });
        }
        throw err;
      }
      if (!repo.findChatByAssignmentId(assignment.id)) {
        repo.insertChat({ id: id('chat'), assignment_id: assignment.id, worker_id: assignment.worker_id, business_id: businessId, created_at: nowIso() });
      }
      return assignment;
    });
  }

  function acceptAssignment(assignmentId) {
    const assignment = repo.findAssignmentById(requireString(assignmentId, 'assignmentId'));
    if (!assignment) throw error('assignment_not_found', 'assignment not found', { status: 404 });
    // Deprecated compatibility no-op. Assignment is accepted at offer time.
    return assignment;
  }

  function attendance(assignmentId, type) {
    requireOneOf(type, 'type', ['check_in', 'check_out']);
    const assignment = repo.findAssignmentById(requireString(assignmentId, 'assignmentId'));
    if (!assignment) throw error('assignment_not_found', 'assignment not found', { status: 404 });
    const shift = repo.findShiftById(assignment.shift_id);
    if (!shift) throw error('shift_not_found', 'shift not found', { status: 404 });
    const now = Date.now();
    if (type === 'check_in') {
      if (assignment.status !== 'assigned') throw error('invalid_assignment_state', 'assignment must be assigned to check in', { status: 409 });
      const start = new Date(shift.start_at).getTime();
      if (now < start - 30 * 60000 || now > start + 60 * 60000) throw error('checkin_window_invalid', 'check-in outside allowed window', { status: 409 });
      repo.updateAssignmentState(assignment.id, 'in_progress');
      logTransition('assignment', assignment.id, assignment.status, 'in_progress', 'worker');
    }
    if (type === 'check_out') {
      if (assignment.status !== 'in_progress') throw error('invalid_assignment_state', 'assignment must be in progress to check out', { status: 409 });
      const hasIn = repo.hasAttendanceCheckIn(assignmentId);
      if (!hasIn) throw error('checkin_required', 'check-in required before check-out', { status: 409 });
      repo.updateAssignmentState(assignment.id, 'completed');
      logTransition('assignment', assignment.id, assignment.status, 'completed', 'worker');
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
    return rating;
  }

  function getChatByAssignment(assignmentId) {
    const chat = repo.findChatByAssignmentId(requireString(assignmentId, 'assignmentId'));
    if (!chat) throw error('chat_not_found', 'chat not found', { status: 404 });
    return chat;
  }

  function sendMessage(assignmentId, senderId, text) {
    const assignment = repo.findAssignmentById(requireString(assignmentId, 'assignmentId'));
    if (!assignment || !['assigned', 'in_progress', 'completed'].includes(assignment.status)) {
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
    requireString(assignmentId, 'assignmentId');
    requireString(stage, 'stage');
    throw error('contact_reveal_removed', 'Use in-platform chat for participant communication', { status: 410 });
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
      if (assignment.status === 'cancelled') throw error('assignment_cancelled', 'cannot release payout for cancelled assignment', { status: 409 });
      if (assignment.status !== 'completed') throw error('assignment_not_completed', 'assignment not completed', { status: 409 });

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

  function updatePayoutStatus(payoutId, nextStatus, note = null) {
    requireString(payoutId, 'payoutId');
    requireOneOf(nextStatus, 'status', ['created', 'pending', 'paid', 'failed']);
    const payout = repo.findPayoutById(payoutId);
    if (!payout) throw error('payout_not_found', 'payout not found', { status: 404 });
    const idempotent = assertTransition('payout', payout.status, nextStatus);
    if (idempotent) return payout;
    repo.updatePayoutStatus(payoutId, nextStatus, nowIso(), note);
    logTransition('payout', payout.id, payout.status, nextStatus, 'admin');
    return repo.findPayoutById(payoutId);
  }

  function rejectApplication(applicationId, actor = 'business') {
    requireString(applicationId, 'applicationId');
    const application = repo.findApplicationById(applicationId);
    if (!application) throw error('application_not_found', 'application not found', { status: 404 });
    const idempotent = assertTransition('application', application.status, 'rejected');
    if (idempotent) return application;
    repo.updateApplicationStatus(applicationId, 'rejected');
    logTransition('application', applicationId, application.status, 'rejected', actor);
    return repo.findApplicationById(applicationId);
  }

  function withdrawApplication(applicationId, actor = 'worker') {
    requireString(applicationId, 'applicationId');
    const application = repo.findApplicationById(applicationId);
    if (!application) throw error('application_not_found', 'application not found', { status: 404 });
    const idempotent = assertTransition('application', application.status, 'withdrawn');
    if (idempotent) return application;
    repo.updateApplicationStatus(applicationId, 'withdrawn');
    logTransition('application', applicationId, application.status, 'withdrawn', actor);
    return repo.findApplicationById(applicationId);
  }

  function cancelAssignment(assignmentId, actor = 'business') {
    return repo.withTransaction(() => {
      requireString(assignmentId, 'assignmentId');
      const assignment = repo.findAssignmentById(assignmentId);
      if (!assignment) throw error('assignment_not_found', 'assignment not found', { status: 404 });
      const idempotent = assertTransition('assignment', assignment.status, 'cancelled');
      if (idempotent) return assignment;

      const escrow = repo.findEscrowByBusinessId(assignment.business_id);
      if (!escrow) throw error('escrow_not_found', 'escrow account not found', { status: 404 });
      repo.updateEscrowBalance(escrow.id, escrow.balance_cents + assignment.escrow_locked_cents);
      repo.insertEscrowTransaction({
        id: id('etx'),
        business_id: assignment.business_id,
        assignment_id: assignment.id,
        type: 'unlock_cancel',
        amount_cents: assignment.escrow_locked_cents,
        created_at: nowIso()
      });

      repo.updateAssignmentState(assignmentId, 'cancelled');
      logTransition('assignment', assignmentId, assignment.status, 'cancelled', actor);
      return repo.findAssignmentById(assignmentId);
    });
  }

  function listProblemCases() {
    const flags = repo.listViolationFlags();
    const failedPayouts = repo.listPayouts().filter((p) => p.status === 'failed');
    const stalledAssignments = repo.listAssignments().filter((a) => a.status === 'completed');
    return { flags, failedPayouts, stalledAssignments };
  }

  return {
    createShift,
    updateShiftStatus,
    applyToShift,
    offerAssignment,
    rejectApplication,
    withdrawApplication,
    acceptAssignment,
    cancelAssignment,
    attendance,
    addRating,
    getChatByAssignment,
    sendMessage,
    revealContacts,
    fundEscrow,
    releasePayment,
    updatePayoutStatus,
    listProblemCases
  };
}

module.exports = { createMarketplaceService };
