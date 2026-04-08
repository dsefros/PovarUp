const { error } = require('../domain/errors');
const { requireOneOf, requireString } = require('../domain/validation');

const ROLES = ['worker', 'business', 'admin'];

function parseBearerToken(req) {
  const auth = req.headers.authorization || '';
  if (!auth.startsWith('Bearer ')) throw error('unauthorized', 'Missing or invalid bearer token', { status: 401 });
  return auth.slice('Bearer '.length);
}

function requireAuth(req, repo) {
  const token = parseBearerToken(req);
  const session = repo.findSessionByToken(token);
  if (!session) throw error('unauthorized', 'Session not found', { status: 401 });
  req.auth = session;
  return session;
}

function createSession(repo, idGenerator, body) {
  const userId = requireString(body.userId, 'userId');
  const role = requireOneOf(body.role, 'role', ROLES);
  const token = idGenerator('sess');
  const session = { token, user_id: userId, role };
  repo.insertSession(session);
  return { token, userId, role };
}

module.exports = { requireAuth, createSession };
