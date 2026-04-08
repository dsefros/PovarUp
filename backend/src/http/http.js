const { DomainError, error } = require('../domain/errors');

const send = (res, status, body) => {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
};

const parseBody = (req) => new Promise((resolve, reject) => {
  let data = '';
  req.on('data', (chunk) => (data += chunk));
  req.on('end', () => {
    if (!data) return resolve({});
    try {
      return resolve(JSON.parse(data));
    } catch {
      return reject(error('invalid_json', 'Invalid JSON body', { status: 400 }));
    }
  });
});

const handleError = (res, err) => {
  if (err instanceof DomainError) {
    return send(res, err.status, { error: { code: err.code, message: err.message, details: err.details || null } });
  }
  return send(res, 500, { error: { code: 'internal_error', message: 'Internal server error', details: null } });
};

module.exports = { send, parseBody, handleError };
