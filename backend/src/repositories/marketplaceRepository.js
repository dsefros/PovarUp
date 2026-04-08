const { db } = require('../domain/store');

function createMarketplaceRepository(store = db) {
  return {
    listWorkers: () => store.worker_profiles,
    listBusinesses: () => store.businesses,
    listShifts: () => store.shifts,
    listApplications: () => store.applications,
    listAssignments: () => store.assignments,
    listMessagesByChatId: (chatId) => store.messages.filter((m) => m.chat_id === chatId),
    listPayoutsByWorkerId: (workerId) => store.payouts.filter((p) => p.worker_id === workerId),
    listViolationFlags: () => store.violation_flags,
    findSessionByToken: (token) => store.sessions.find((s) => s.token === token),
    insertSession: (session) => store.sessions.push(session),
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
  };
}

module.exports = { createMarketplaceRepository };
