function normalizeRow(row) {
  if (!row) return row;
  const out = { ...row };
  if (Object.hasOwn(out, 'violation_detected')) out.violation_detected = Boolean(out.violation_detected);
  if (Object.hasOwn(out, 'score')) out.score = Number(out.score);
  if (Object.hasOwn(out, 'rating_avg') && out.rating_avg !== null) out.rating_avg = Number(out.rating_avg);
  return out;
}

function list(stmt, params = []) {
  return stmt.all(...params).map(normalizeRow);
}

function one(stmt, params = []) {
  return normalizeRow(stmt.get(...params));
}

function createSqlMarketplaceRepository(db) {
  const q = {
    listWorkers: db.prepare('SELECT id, user_id, name, rating_avg, created_at FROM worker_profiles ORDER BY id'),
    listBusinesses: db.prepare('SELECT id, user_id, name, created_at FROM businesses ORDER BY id'),
    listShifts: db.prepare('SELECT * FROM shifts ORDER BY created_at, id'),
    listApplications: db.prepare('SELECT * FROM applications ORDER BY created_at, id'),
    listAssignments: db.prepare('SELECT * FROM assignments ORDER BY created_at, id'),
    listMessagesByChatId: db.prepare('SELECT * FROM messages WHERE chat_id = ? ORDER BY created_at, id'),
    listPayoutsByWorkerId: db.prepare('SELECT * FROM payouts WHERE worker_id = ? ORDER BY created_at, id'),
    listViolationFlags: db.prepare('SELECT * FROM violation_flags ORDER BY created_at, id'),

    findSessionByToken: db.prepare('SELECT token, user_id, role, created_at FROM sessions WHERE token = ?'),
    findWorkerById: db.prepare('SELECT * FROM worker_profiles WHERE id = ?'),
    findWorkerByUserId: db.prepare('SELECT * FROM worker_profiles WHERE user_id = ?'),
    findBusinessById: db.prepare('SELECT * FROM businesses WHERE id = ?'),
    findBusinessByUserId: db.prepare('SELECT * FROM businesses WHERE user_id = ?'),
    findLocationById: db.prepare('SELECT * FROM locations WHERE id = ?'),
    findShiftById: db.prepare('SELECT * FROM shifts WHERE id = ?'),
    findApplicationById: db.prepare('SELECT * FROM applications WHERE id = ?'),
    findAssignmentById: db.prepare('SELECT * FROM assignments WHERE id = ?'),
    findChatByAssignmentId: db.prepare('SELECT * FROM chats WHERE assignment_id = ?'),
    findEscrowByBusinessId: db.prepare('SELECT * FROM escrow_accounts WHERE business_id = ?'),
    findPayoutByAssignmentId: db.prepare('SELECT * FROM payouts WHERE assignment_id = ? ORDER BY created_at, id LIMIT 1'),
    hasAttendanceCheckIn: db.prepare("SELECT 1 AS hit FROM attendance_events WHERE assignment_id = ? AND type = 'check_in' LIMIT 1"),
    listRatingsByAssignmentId: db.prepare('SELECT * FROM ratings WHERE assignment_id = ? ORDER BY created_at, id'),
    hasRatingByRole: db.prepare('SELECT 1 AS hit FROM ratings WHERE assignment_id = ? AND from_role = ? LIMIT 1'),

    insertSession: db.prepare('INSERT INTO sessions (token, user_id, role) VALUES (?, ?, ?)'),
    insertShift: db.prepare('INSERT INTO shifts (id, business_id, location_id, title, start_at, end_at, pay_rate_cents, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'),
    insertApplication: db.prepare('INSERT INTO applications (id, shift_id, worker_id, status, created_at) VALUES (?, ?, ?, ?, ?)'),
    insertAssignment: db.prepare('INSERT INTO assignments (id, shift_id, application_id, worker_id, business_id, status, escrow_locked_cents, contact_reveal_stage, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'),
    insertChat: db.prepare('INSERT INTO chats (id, assignment_id, worker_id, business_id, created_at) VALUES (?, ?, ?, ?, ?)'),
    insertAttendanceEvent: db.prepare('INSERT INTO attendance_events (id, assignment_id, type, timestamp) VALUES (?, ?, ?, ?)'),
    insertRating: db.prepare('INSERT INTO ratings (id, assignment_id, from_role, score, note, created_at) VALUES (?, ?, ?, ?, ?, ?)'),
    insertMessage: db.prepare('INSERT INTO messages (id, chat_id, sender_id, text, created_at, violation_detected) VALUES (?, ?, ?, ?, ?, ?)'),
    insertViolationFlag: db.prepare('INSERT INTO violation_flags (id, assignment_id, actor_id, reasons, content, created_at) VALUES (?, ?, ?, ?, ?, ?)'),
    insertEscrowTransaction: db.prepare('INSERT INTO escrow_transactions (id, business_id, assignment_id, type, amount_cents, created_at) VALUES (?, ?, ?, ?, ?, ?)'),
    insertPayout: db.prepare('INSERT INTO payouts (id, assignment_id, worker_id, amount_cents, status, created_at) VALUES (?, ?, ?, ?, ?, ?)'),
    insertEscrowAccount: db.prepare('INSERT INTO escrow_accounts (id, business_id, balance_cents, created_at) VALUES (?, ?, ?, ?)'),

    updateEscrowBalance: db.prepare('UPDATE escrow_accounts SET balance_cents = ? WHERE id = ?'),
    updateApplicationStatus: db.prepare('UPDATE applications SET status = ? WHERE id = ?'),
    updateAssignmentState: db.prepare('UPDATE assignments SET status = ?, accepted_at = COALESCE(?, accepted_at) WHERE id = ?'),
    updateShiftTimes: db.prepare('UPDATE shifts SET start_at = ?, end_at = ? WHERE id = ?')
  };

  let txDepth = 0;
  function withTransaction(run) {
    const isRoot = txDepth === 0;
    if (isRoot) db.exec('BEGIN IMMEDIATE');
    txDepth += 1;
    try {
      const result = run();
      txDepth -= 1;
      if (isRoot) db.exec('COMMIT');
      return result;
    } catch (err) {
      txDepth -= 1;
      if (isRoot) db.exec('ROLLBACK');
      throw err;
    }
  }

  return {
    withTransaction,
    listWorkers: () => list(q.listWorkers),
    listBusinesses: () => list(q.listBusinesses),
    listShifts: () => list(q.listShifts),
    listApplications: () => list(q.listApplications),
    listAssignments: () => list(q.listAssignments),
    listMessagesByChatId: (chatId) => list(q.listMessagesByChatId, [chatId]),
    listPayoutsByWorkerId: (workerId) => list(q.listPayoutsByWorkerId, [workerId]),
    listViolationFlags: () => list(q.listViolationFlags),

    findSessionByToken: (token) => one(q.findSessionByToken, [token]),
    insertSession: (session) => q.insertSession.run(session.token, session.user_id, session.role),
    findWorkerById: (id) => one(q.findWorkerById, [id]),
    findWorkerByUserId: (userId) => one(q.findWorkerByUserId, [userId]),
    findBusinessById: (id) => one(q.findBusinessById, [id]),
    findBusinessByUserId: (userId) => one(q.findBusinessByUserId, [userId]),
    findLocationById: (id) => one(q.findLocationById, [id]),
    findShiftById: (id) => one(q.findShiftById, [id]),
    findApplicationById: (id) => one(q.findApplicationById, [id]),
    findAssignmentById: (id) => one(q.findAssignmentById, [id]),
    findChatByAssignmentId: (assignmentId) => one(q.findChatByAssignmentId, [assignmentId]),
    findEscrowByBusinessId: (businessId) => one(q.findEscrowByBusinessId, [businessId]),
    findPayoutByAssignmentId: (assignmentId) => one(q.findPayoutByAssignmentId, [assignmentId]),
    hasAttendanceCheckIn: (assignmentId) => Boolean(one(q.hasAttendanceCheckIn, [assignmentId])),
    listRatingsByAssignmentId: (assignmentId) => list(q.listRatingsByAssignmentId, [assignmentId]),
    hasRatingByRole: (assignmentId, role) => Boolean(one(q.hasRatingByRole, [assignmentId, role])),

    insertShift: (shift) => q.insertShift.run(shift.id, shift.business_id, shift.location_id, shift.title, shift.start_at, shift.end_at, shift.pay_rate_cents, shift.status, shift.created_at),
    insertApplication: (application) => q.insertApplication.run(application.id, application.shift_id, application.worker_id, application.status, application.created_at),
    insertAssignment: (assignment) => q.insertAssignment.run(assignment.id, assignment.shift_id, assignment.application_id, assignment.worker_id, assignment.business_id, assignment.status, assignment.escrow_locked_cents, assignment.contact_reveal_stage, assignment.created_at),
    insertChat: (chat) => q.insertChat.run(chat.id, chat.assignment_id, chat.worker_id, chat.business_id, chat.created_at),
    insertAttendanceEvent: (event) => q.insertAttendanceEvent.run(event.id, event.assignment_id, event.type, event.timestamp),
    insertRating: (rating) => q.insertRating.run(rating.id, rating.assignment_id, rating.from_role, rating.score, rating.note, rating.created_at),
    insertMessage: (msg) => q.insertMessage.run(msg.id, msg.chat_id, msg.sender_id, msg.text, msg.created_at, msg.violation_detected ? 1 : 0),
    insertViolationFlag: (flag) => q.insertViolationFlag.run(flag.id, flag.assignment_id, flag.actor_id, JSON.stringify(flag.reasons), flag.content, flag.created_at),
    insertEscrowTransaction: (tx) => q.insertEscrowTransaction.run(tx.id, tx.business_id, tx.assignment_id ?? null, tx.type, tx.amount_cents, tx.created_at),
    insertPayout: (payout) => q.insertPayout.run(payout.id, payout.assignment_id, payout.worker_id, payout.amount_cents, payout.status, payout.created_at),
    insertEscrowAccount: (account) => q.insertEscrowAccount.run(account.id, account.business_id, account.balance_cents, account.created_at),

    updateEscrowBalance: (accountId, balanceCents) => q.updateEscrowBalance.run(balanceCents, accountId),
    updateApplicationStatus: (applicationId, status) => q.updateApplicationStatus.run(status, applicationId),
    updateAssignmentState: (assignmentId, status, acceptedAt = null) => q.updateAssignmentState.run(status, acceptedAt, assignmentId),
    updateShiftTimes: (shiftId, startAt, endAt) => q.updateShiftTimes.run(startAt, endAt, shiftId)
  };
}

module.exports = { createSqlMarketplaceRepository };
