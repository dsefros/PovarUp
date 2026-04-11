const { db } = require('../domain/store');

function createMarketplaceRepository(store = db) {
  return {
    withTransaction: (run) => run(),
    listWorkers: () => store.worker_profiles,
    listBusinesses: () => store.businesses,
    listShifts: () => store.shifts,
    listApplications: () => store.applications,
    listAssignments: () => store.assignments,
    listMessagesByChatId: (chatId) => store.messages.filter((m) => m.chat_id === chatId),
    listPayoutsByWorkerId: (workerId) => store.payouts.filter((p) => p.worker_id === workerId),
    listEscrowTransactions: () => store.escrow_transactions,
    listViolationFlags: () => store.violation_flags,
    listAccounts: () => store.accounts,
    findSessionByToken: (token) => store.sessions.find((s) => s.token === token),
    findAccountByUserId: (userId) => store.accounts.find((a) => a.user_id === userId),
    findInviteByCode: (code) => store.onboarding_invites.find((i) => i.code === code),
    insertSession: (session) => store.sessions.push(session),
    deleteSessionByToken: (token) => {
      const idx = store.sessions.findIndex((s) => s.token === token);
      if (idx >= 0) store.sessions.splice(idx, 1);
    },
    insertAccount: (account) => store.accounts.push(account),
    insertWorkerProfile: (worker) => store.worker_profiles.push(worker),
    insertBusiness: (business) => store.businesses.push(business),
    insertLocation: (location) => store.locations.push(location),
    findWorkerById: (id) => store.worker_profiles.find((w) => w.id === id),
    findWorkerByUserId: (userId) => store.worker_profiles.find((w) => w.user_id === userId),
    findBusinessById: (id) => store.businesses.find((b) => b.id === id),
    findBusinessByUserId: (userId) => store.businesses.find((b) => b.user_id === userId),
    findLocationById: (id) => store.locations.find((l) => l.id === id),
    findShiftById: (id) => store.shifts.find((s) => s.id === id),
    findApplicationById: (id) => store.applications.find((a) => a.id === id),
    findAssignmentById: (id) => store.assignments.find((a) => a.id === id),
    findChatByAssignmentId: (assignmentId) => store.chats.find((c) => c.assignment_id === assignmentId),
    findEscrowByBusinessId: (businessId) => store.escrow_accounts.find((e) => e.business_id === businessId),
    findPayoutByAssignmentId: (assignmentId) => store.payouts.find((p) => p.assignment_id === assignmentId),
    hasAttendanceCheckIn: (assignmentId) => store.attendance_events.some((e) => e.assignment_id === assignmentId && e.type === 'check_in'),
    listRatingsByAssignmentId: (assignmentId) => store.ratings.filter((r) => r.assignment_id === assignmentId),
    hasRatingByRole: (assignmentId, role) => store.ratings.some((r) => r.assignment_id === assignmentId && r.from_role === role),
    insertShift: (shift) => store.shifts.push(shift),
    insertApplication: (application) => store.applications.push(application),
    insertAssignment: (assignment) => store.assignments.push(assignment),
    insertChat: (chat) => store.chats.push(chat),
    insertAttendanceEvent: (event) => store.attendance_events.push(event),
    insertRating: (rating) => store.ratings.push(rating),
    insertMessage: (msg) => store.messages.push(msg),
    insertViolationFlag: (flag) => store.violation_flags.push(flag),
    insertEscrowTransaction: (tx) => store.escrow_transactions.push(tx),
    insertPayout: (payout) => store.payouts.push(payout),
    insertEscrowAccount: (account) => store.escrow_accounts.push(account)
    ,
    updateApplicationStatus: (applicationId, status) => {
      const item = store.applications.find((a) => a.id === applicationId);
      if (item) item.status = status;
    },
    updateAssignmentState: (assignmentId, status, acceptedAt = null) => {
      const item = store.assignments.find((a) => a.id === assignmentId);
      if (!item) return;
      item.status = status;
      if (acceptedAt && !item.accepted_at) item.accepted_at = acceptedAt;
    },
    updateEscrowBalance: (accountId, balanceCents) => {
      const item = store.escrow_accounts.find((a) => a.id === accountId);
      if (item) item.balance_cents = balanceCents;
    },
    updateShiftTimes: (shiftId, startAt, endAt) => {
      const item = store.shifts.find((s) => s.id === shiftId);
      if (!item) return;
      item.start_at = startAt;
      item.end_at = endAt;
    },
    updateShiftStatus: (shiftId, status) => {
      const item = store.shifts.find((s) => s.id === shiftId);
      if (item) item.status = status;
    },
    listPayouts: () => store.payouts,
    findPayoutById: (payoutId) => store.payouts.find((p) => p.id === payoutId),
    updatePayoutStatus: (payoutId, status, note = null) => {
      const item = store.payouts.find((p) => p.id === payoutId);
      if (!item) return;
      item.status = status;
      item.updated_at = new Date().toISOString();
      item.note = note;
    },
    updateAccountPassword: (userId, password) => {
      const account = store.accounts.find((a) => a.user_id === userId);
      if (account) account.password = password;
    }
  };
}

module.exports = { createMarketplaceRepository };
