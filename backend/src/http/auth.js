const { error } = require('../domain/errors');
const { requireString } = require('../domain/validation');
const { hashPassword, verifyPassword } = require('../domain/passwords');

const SESSION_TTL_MS = Number(process.env.POVARUP_SESSION_TTL_MS || 12 * 60 * 60 * 1000);

function parseBearerToken(req) {
  const auth = req.headers.authorization || '';
  if (!auth.startsWith('Bearer ')) throw error('unauthorized', 'Missing or invalid bearer token', { status: 401 });
  return auth.slice('Bearer '.length);
}

function requireAuth(req, repo) {
  const token = parseBearerToken(req);
  const session = repo.findSessionByToken(token);
  if (!session) throw error('unauthorized', 'Session not found', { status: 401 });
  if (session.expires_at && Date.parse(session.expires_at) <= Date.now()) {
    repo.deleteSessionByToken(token);
    throw error('session_expired', 'Session expired', { status: 401 });
  }
  req.auth = session;
  return session;
}

function createLoginSession(repo, idGenerator, body) {
  const userId = requireString(body.userId, 'userId');
  const password = requireString(body.password, 'password');
  const account = repo.findAccountByUserId(userId);
  const passwordHash = account?.password_hash || account?.password;
  if (!account || !passwordHash || !verifyPassword(password, passwordHash)) {
    throw error('invalid_credentials', 'Invalid user id or password', { status: 401 });
  }

  const token = idGenerator('sess');
  const session = {
    token,
    user_id: account.user_id,
    role: account.role,
    expires_at: new Date(Date.now() + SESSION_TTL_MS).toISOString()
  };
  repo.insertSession(session);
  return { token, userId: account.user_id, role: account.role, displayName: account.display_name, expiresAt: session.expires_at };
}

function onboardAccount(repo, idGenerator, nowIso, body) {
  const inviteCode = requireString(body.inviteCode, 'inviteCode');
  const userId = requireString(body.userId, 'userId');
  const password = requireString(body.password, 'password');
  const displayName = requireString(body.displayName, 'displayName');
  const invite = repo.findInviteByCode(inviteCode);
  if (!invite) throw error('invalid_invite', 'Invite code is invalid', { status: 403 });
  if (repo.findAccountByUserId(userId)) throw error('user_exists', 'User already exists', { status: 409 });

  return repo.withTransaction(() => {
    const passwordHash = hashPassword(password);
    const account = {
      id: idGenerator('acct'),
      user_id: userId,
      password_hash: passwordHash,
      role: invite.role,
      display_name: displayName,
      created_at: nowIso()
    };
    repo.insertAccount(account);

    let workerId = null;
    let businessId = null;
    let locationId = null;

    if (invite.role === 'worker') {
      workerId = idGenerator('worker');
      repo.insertWorkerProfile({ id: workerId, user_id: account.user_id, name: displayName, rating_avg: null, created_at: nowIso() });
    }

    if (invite.role === 'business') {
      businessId = idGenerator('biz');
      locationId = idGenerator('loc');
      repo.insertBusiness({ id: businessId, user_id: account.user_id, name: invite.business_name || displayName, created_at: nowIso() });
      repo.insertLocation({
        id: locationId,
        business_id: businessId,
        name: invite.location_name || `${displayName} Main Location`,
        address: invite.location_address || 'TBD',
        created_at: nowIso()
      });
    }

    return { userId: account.user_id, role: account.role, displayName: account.display_name, workerId, businessId, locationId };
  });
}

function logoutSession(req, repo) {
  const token = parseBearerToken(req);
  repo.deleteSessionByToken(token);
  return { ok: true };
}

module.exports = { requireAuth, createLoginSession, onboardAccount, logoutSession, SESSION_TTL_MS };
