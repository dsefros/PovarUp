const http = require('node:http');
const { URL } = require('node:url');
const { db, id, seed } = require('./domain/store');
const svc = require('./services/marketplaceService');
const { assignmentDto, messageDto, shiftDto } = require('./dto/mappers');

seed();

const send = (res, status, body) => {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
};

const parseBody = (req) => new Promise((resolve) => {
  let data = '';
  req.on('data', (chunk) => (data += chunk));
  req.on('end', () => resolve(data ? JSON.parse(data) : {}));
});

const handleError = (res, err) => send(res, 400, { error: { code: err.message, message: err.message.replaceAll('_', ' '), details: null } });

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://localhost');
    const path = url.pathname;
    const method = req.method;
    if (path === '/api/health' && method === 'GET') return send(res, 200, { status: 'ok' });
    if (path === '/api/ready' && method === 'GET') return send(res, 200, { status: 'ready', services: ['api'] });
    if (path === '/api/seed' && method === 'POST') { seed(); return send(res, 200, { seeded: true }); }
    if (path === '/api/auth/session' && method === 'POST') { const b = await parseBody(req); const token = id('sess'); db.sessions.push({ token, user_id: b.userId, role: b.role }); return send(res, 200, { token, userId: b.userId, role: b.role }); }
    if (path === '/api/workers' && method === 'GET') return send(res, 200, { items: db.worker_profiles });
    if (path === '/api/businesses' && method === 'GET') return send(res, 200, { items: db.businesses });
    if (path === '/api/shifts' && method === 'GET') return send(res, 200, { items: db.shifts.map(shiftDto) });
    if (path === '/api/shifts' && method === 'POST') { const b = await parseBody(req); return send(res, 201, { item: shiftDto(svc.createShift(b)) }); }
    if (path === '/api/applications' && method === 'GET') return send(res, 200, { items: db.applications });
    if (path === '/api/applications' && method === 'POST') { const b = await parseBody(req); return send(res, 201, { item: svc.applyToShift(b.shiftId, b.workerId) }); }
    if (path === '/api/assignments' && method === 'GET') return send(res, 200, { items: db.assignments.map(assignmentDto) });
    if (path === '/api/assignments/offer' && method === 'POST') { const b = await parseBody(req); return send(res, 201, { item: assignmentDto(svc.offerAssignment(b.applicationId, b.businessId)) }); }
    if (path.match(/^\/api\/assignments\/[^/]+\/accept$/) && method === 'POST') { const aid = path.split('/')[3]; return send(res, 200, { item: assignmentDto(svc.acceptAssignment(aid)) }); }
    if (path === '/api/attendance/check-in' && method === 'POST') { const b = await parseBody(req); return send(res, 201, { item: svc.attendance(b.assignmentId, 'check_in') }); }
    if (path === '/api/attendance/check-out' && method === 'POST') { const b = await parseBody(req); return send(res, 201, { item: svc.attendance(b.assignmentId, 'check_out') }); }
    if (path === '/api/ratings' && method === 'POST') { const b = await parseBody(req); return send(res, 201, { item: svc.addRating(b.assignmentId, b.fromRole, b.score, b.note) }); }
    if (path.match(/^\/api\/chats\/by-assignment\//) && method === 'GET') { const assignmentId = path.split('/')[4]; return send(res, 200, { item: svc.getChatByAssignment(assignmentId) }); }
    if (path.match(/^\/api\/chats\/[^/]+\/messages$/) && method === 'GET') { const chatId = path.split('/')[3]; return send(res, 200, { items: db.messages.filter((m) => m.chat_id === chatId).map(messageDto) }); }
    if (path.match(/^\/api\/chats\/[^/]+\/messages$/) && method === 'POST') { const assignmentId = path.split('/')[3]; const b = await parseBody(req); return send(res, 201, { item: messageDto(svc.sendMessage(assignmentId, b.senderId, b.text)) }); }
    if (path.match(/^\/api\/contacts\/reveal\//) && method === 'GET') { const assignmentId = path.split('/')[4]; return send(res, 200, { item: svc.revealContacts(assignmentId, url.searchParams.get('stage') || 'accepted') }); }
    if (path === '/api/escrow/fund' && method === 'POST') { const b = await parseBody(req); return send(res, 201, { item: svc.fundEscrow(b.businessId, b.amountCents) }); }
    if (path.match(/^\/api\/escrow\/balance\//) && method === 'GET') { const businessId = path.split('/')[4]; return send(res, 200, { item: db.escrow_accounts.find((a) => a.business_id === businessId) || null }); }
    if (path.match(/^\/api\/escrow\/release\//) && method === 'POST') { const assignmentId = path.split('/')[4]; const b = await parseBody(req); return send(res, 201, { item: svc.releasePayment(assignmentId, b.force === true) }); }
    if (path.match(/^\/api\/payouts\//) && method === 'GET') { const workerId = path.split('/')[3]; return send(res, 200, { items: db.payouts.filter((p) => p.worker_id === workerId) }); }
    if (path === '/api/violation-flags' && method === 'GET') return send(res, 200, { items: db.violation_flags });
    return send(res, 404, { error: { code: 'not_found', message: 'not found', details: null } });
  } catch (err) {
    return handleError(res, err);
  }
});

if (require.main === module) {
  const port = process.env.PORT || 4000;
  server.listen(port, () => console.log(`Backend listening on ${port}`));
}

module.exports = server;
