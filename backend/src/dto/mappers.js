function shiftProductStatus(shift, assignments) {
  if (shift.status === 'cancelled') return 'cancelled';
  if (shift.status === 'closed') return 'closed';
  const active = assignments.filter((a) => a.shift_id === shift.id && !['cancelled'].includes(a.status));
  if (active.some((a) => ['offered', 'active', 'in_progress', 'completed_pending_rating', 'completed_rated'].includes(a.status))) return 'filled';
  return 'open';
}

function normalizeApplicationStatus(status) {
  if (status === 'applied') return 'submitted';
  if (status === 'offered') return 'offered';
  if (status === 'rejected') return 'rejected';
  if (status === 'withdrawn') return 'withdrawn';
  return 'submitted';
}

function normalizeAssignmentStatus(status, payoutStatus = null) {
  if (status === 'cancelled') return 'cancelled';
  if (status === 'offered') return 'offered';
  if (status === 'active') return 'active';
  if (status === 'in_progress') return 'checked_in';
  if (status === 'completed_pending_rating') return 'checked_out';
  if (status === 'completed_rated') return payoutStatus === 'paid' ? 'paid' : 'completed';
  return 'unknown';
}

function normalizePayoutStatus(status) {
  if (['created', 'pending', 'paid', 'failed'].includes(status)) return status;
  if (status === 'released') return 'paid';
  return 'created';
}

function assignmentDto(a, payoutByAssignmentId = {}) {
  const payoutStatus = payoutByAssignmentId[a.id]?.status || null;
  return {
    id: a.id,
    shiftId: a.shift_id,
    workerId: a.worker_id,
    businessId: a.business_id,
    status: a.status,
    normalizedStatus: normalizeAssignmentStatus(a.status, payoutStatus),
    productStatus: normalizeAssignmentStatus(a.status, payoutStatus),
    escrowLockedCents: a.escrow_locked_cents
  };
}

function messageDto(m) {
  return {
    id: m.id,
    chatId: m.chat_id,
    senderId: m.sender_id,
    text: m.text,
    createdAt: m.created_at,
    violationDetected: m.violation_detected
  };
}

function shiftDto(s, assignments = []) {
  return {
    id: s.id,
    businessId: s.business_id,
    locationId: s.location_id,
    title: s.title,
    startAt: s.start_at,
    endAt: s.end_at,
    payRateCents: s.pay_rate_cents,
    status: s.status,
    normalizedStatus: shiftProductStatus(s, assignments),
    productStatus: shiftProductStatus(s, assignments)
  };
}

function applicationDto(a) {
  return {
    id: a.id,
    shiftId: a.shift_id,
    workerId: a.worker_id,
    status: a.status,
    normalizedStatus: normalizeApplicationStatus(a.status),
    productStatus: normalizeApplicationStatus(a.status)
  };
}

function payoutDto(p) {
  return {
    id: p.id,
    assignmentId: p.assignment_id,
    workerId: p.worker_id,
    amountCents: p.amount_cents,
    status: normalizePayoutStatus(p.status),
    internalStatus: p.status,
    createdAt: p.created_at,
    updatedAt: p.updated_at || null,
    note: p.note || null
  };
}

module.exports = { assignmentDto, messageDto, shiftDto, applicationDto, payoutDto };
