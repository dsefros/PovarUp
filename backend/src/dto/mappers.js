function assignmentDto(a) {
  return {
    id: a.id,
    shiftId: a.shift_id,
    workerId: a.worker_id,
    businessId: a.business_id,
    status: a.status,
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

function shiftDto(s) {
  return {
    id: s.id,
    businessId: s.business_id,
    locationId: s.location_id,
    title: s.title,
    startAt: s.start_at,
    endAt: s.end_at,
    payRateCents: s.pay_rate_cents,
    status: s.status
  };
}

function applicationDto(a) {
  return {
    id: a.id,
    shiftId: a.shift_id,
    workerId: a.worker_id,
    status: a.status
  };
}

function payoutDto(p) {
  return {
    id: p.id,
    assignmentId: p.assignment_id,
    workerId: p.worker_id,
    amountCents: p.amount_cents,
    status: p.status,
    createdAt: p.created_at
  };
}

module.exports = { assignmentDto, messageDto, shiftDto, applicationDto, payoutDto };
