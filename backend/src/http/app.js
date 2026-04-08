const { URL } = require('node:url');
const { id } = require('../domain/store');
const { send, parseBody } = require('./http');
const { requireAuth, createSession } = require('./auth');
const { assignmentDto, applicationDto, messageDto, payoutDto, shiftDto } = require('../dto/mappers');
const { error } = require('../domain/errors');

function actorFromSession(session, repo) {
  const worker = repo.findWorkerByUserId(session.user_id);
  const business = repo.findBusinessByUserId(session.user_id);
  return { session, worker, business };
}

function canAccessAssignment(actor, assignment) {
  if (!assignment) return false;
  if (actor.session.role === 'admin') return true;
  if (actor.worker && assignment.worker_id === actor.worker.id) return true;
  if (actor.business && assignment.business_id === actor.business.id) return true;
  return false;
}


function canAccessApplication(actor, application, repo) {
  if (!application) return false;
  if (actor.session.role === 'admin') return true;
  if (actor.worker && application.worker_id === actor.worker.id) return true;
  if (actor.business) {
    const shift = repo.findShiftById(application.shift_id);
    return Boolean(shift && shift.business_id === actor.business.id);
  }
  return false;
}
function createApp({ repo, seed, service }) {
  return async function app(req, res) {
    const url = new URL(req.url, 'http://localhost');
    const path = url.pathname;
    const method = req.method;

    if (path === '/api/health' && method === 'GET') return send(res, 200, { status: 'ok' });
    if (path === '/api/ready' && method === 'GET') return send(res, 200, { status: 'ready', services: ['api'] });
    if (path === '/api/seed' && method === 'POST') { seed(); return send(res, 200, { seeded: true }); }
    if (path === '/api/auth/session' && method === 'POST') return send(res, 200, createSession(repo, id, await parseBody(req)));

    if (path === '/api/workers' && method === 'GET') return send(res, 200, { items: repo.listWorkers() });
    if (path === '/api/businesses' && method === 'GET') return send(res, 200, { items: repo.listBusinesses() });
    if (path === '/api/shifts' && method === 'GET') return send(res, 200, { items: repo.listShifts().map(shiftDto) });

    if (path === '/api/shifts' && method === 'POST') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      if (actor.session.role !== 'business' || !actor.business) throw error('forbidden', 'Only business users can create shifts', { status: 403 });
      const b = await parseBody(req);
      const item = service.createShift({ ...b, businessId: actor.business.id });
      return send(res, 201, { item: shiftDto(item) });
    }

    if (path === '/api/applications' && method === 'GET') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const items = repo.listApplications().filter((a) => canAccessApplication(actor, a, repo)).map(applicationDto);
      return send(res, 200, { items });
    }
    if (path === '/api/applications' && method === 'POST') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      if (actor.session.role !== 'worker' || !actor.worker) throw error('forbidden', 'Only workers can apply', { status: 403 });
      const b = await parseBody(req);
      const item = service.applyToShift(b.shiftId, actor.worker.id);
      return send(res, 201, { item: applicationDto(item) });
    }

    if (path === '/api/assignments' && method === 'GET') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const items = repo.listAssignments().filter((a) => canAccessAssignment(actor, a)).map(assignmentDto);
      return send(res, 200, { items });
    }

    if (path === '/api/assignments/offer' && method === 'POST') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      if (actor.session.role !== 'business' || !actor.business) throw error('forbidden', 'Only businesses can offer', { status: 403 });
      const b = await parseBody(req);
      return send(res, 201, { item: assignmentDto(service.offerAssignment(b.applicationId, actor.business.id)) });
    }

    if (path.match(/^\/api\/assignments\/[^/]+\/accept$/) && method === 'POST') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const aid = path.split('/')[3];
      const assignment = repo.findAssignmentById(aid);
      if (actor.session.role !== 'worker' || !actor.worker || !assignment || assignment.worker_id !== actor.worker.id) {
        throw error('forbidden', 'Only assigned worker can accept', { status: 403 });
      }
      return send(res, 200, { item: assignmentDto(service.acceptAssignment(aid)) });
    }

    if (path === '/api/attendance/check-in' && method === 'POST') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const b = await parseBody(req);
      const assignment = repo.findAssignmentById(b.assignmentId);
      if (actor.session.role !== 'worker' || !actor.worker || !assignment || assignment.worker_id !== actor.worker.id) {
        throw error('forbidden', 'Only assigned worker can check in', { status: 403 });
      }
      return send(res, 201, { item: service.attendance(b.assignmentId, 'check_in') });
    }

    if (path === '/api/attendance/check-out' && method === 'POST') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const b = await parseBody(req);
      const assignment = repo.findAssignmentById(b.assignmentId);
      if (actor.session.role !== 'worker' || !actor.worker || !assignment || assignment.worker_id !== actor.worker.id) {
        throw error('forbidden', 'Only assigned worker can check out', { status: 403 });
      }
      return send(res, 201, { item: service.attendance(b.assignmentId, 'check_out') });
    }

    if (path === '/api/ratings' && method === 'POST') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const b = await parseBody(req);
      const assignment = repo.findAssignmentById(b.assignmentId);
      if (!canAccessAssignment(actor, assignment)) throw error('forbidden', 'Only assignment participants can rate', { status: 403 });
      if (actor.session.role === 'worker' && b.fromRole !== 'worker') throw error('forbidden', 'Worker must rate as worker', { status: 403 });
      if (actor.session.role === 'business' && b.fromRole !== 'business') throw error('forbidden', 'Business must rate as business', { status: 403 });
      return send(res, 201, { item: service.addRating(b.assignmentId, b.fromRole, b.score, b.note) });
    }

    if (path.match(/^\/api\/chats\/by-assignment\//) && method === 'GET') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const assignmentId = path.split('/')[4];
      const assignment = repo.findAssignmentById(assignmentId);
      if (!canAccessAssignment(actor, assignment)) throw error('forbidden', 'Chat access denied', { status: 403 });
      return send(res, 200, { item: service.getChatByAssignment(assignmentId) });
    }

    if (path.match(/^\/api\/chats\/[^/]+\/messages$/) && method === 'GET') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const assignmentId = path.split('/')[3];
      const assignment = repo.findAssignmentById(assignmentId);
      if (!canAccessAssignment(actor, assignment)) throw error('forbidden', 'Messages access denied', { status: 403 });
      const chat = repo.findChatByAssignmentId(assignmentId);
      return send(res, 200, { items: repo.listMessagesByChatId(chat.id).map(messageDto) });
    }

    if (path.match(/^\/api\/chats\/[^/]+\/messages$/) && method === 'POST') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const assignmentId = path.split('/')[3];
      const assignment = repo.findAssignmentById(assignmentId);
      if (!canAccessAssignment(actor, assignment)) throw error('forbidden', 'Messages access denied', { status: 403 });
      const b = await parseBody(req);
      const senderId = actor.worker?.id || actor.business?.id;
      return send(res, 201, { item: messageDto(service.sendMessage(assignmentId, senderId, b.text)) });
    }

    if (path.match(/^\/api\/contacts\/reveal\//) && method === 'GET') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const assignmentId = path.split('/')[4];
      const assignment = repo.findAssignmentById(assignmentId);
      if (!canAccessAssignment(actor, assignment)) throw error('forbidden', 'Contact reveal access denied', { status: 403 });
      return send(res, 200, { item: service.revealContacts(assignmentId, url.searchParams.get('stage') || 'accepted') });
    }

    if (path === '/api/escrow/fund' && method === 'POST') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      if (actor.session.role !== 'business' || !actor.business) throw error('forbidden', 'Only business can fund escrow', { status: 403 });
      const b = await parseBody(req);
      return send(res, 201, { item: service.fundEscrow(actor.business.id, b.amountCents) });
    }

    if (path.match(/^\/api\/escrow\/balance\//) && method === 'GET') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const businessId = path.split('/')[4];
      if (actor.session.role !== 'admin' && (!actor.business || actor.business.id !== businessId)) {
        throw error('forbidden', 'Escrow balance access denied', { status: 403 });
      }
      return send(res, 200, { item: repo.findEscrowByBusinessId(businessId) || null });
    }
    if (path.match(/^\/api\/escrow\/release\//) && method === 'POST') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const assignmentId = path.split('/')[4];
      const assignment = repo.findAssignmentById(assignmentId);
      if (!assignment || actor.session.role !== 'business' || !actor.business || assignment.business_id !== actor.business.id) {
        throw error('forbidden', 'Only assignment business can release escrow', { status: 403 });
      }
      const b = await parseBody(req);
      return send(res, 201, { item: payoutDto(service.releasePayment(assignmentId, b.force === true)) });
    }

    if (path.match(/^\/api\/payouts\//) && method === 'GET') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      const workerId = path.split('/')[3];
      if (actor.session.role !== 'admin' && (!actor.worker || actor.worker.id !== workerId)) {
        throw error('forbidden', 'Payout access denied', { status: 403 });
      }
      return send(res, 200, { items: repo.listPayoutsByWorkerId(workerId).map(payoutDto) });
    }

    if (path === '/api/violation-flags' && method === 'GET') {
      const actor = actorFromSession(requireAuth(req, repo), repo);
      if (actor.session.role !== 'admin') throw error('forbidden', 'Only admin can view violation flags', { status: 403 });
      return send(res, 200, { items: repo.listViolationFlags() });
    }

    return send(res, 404, { error: { code: 'not_found', message: 'not found', details: null } });
  };
}

module.exports = { createApp };
