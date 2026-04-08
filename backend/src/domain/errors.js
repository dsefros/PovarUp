class DomainError extends Error {
  constructor(code, message, { status = 400, details = null } = {}) {
    super(message || code);
    this.name = 'DomainError';
    this.code = code;
    this.status = status;
    this.details = details;
  }
}

const error = (code, message, opts) => new DomainError(code, message, opts);

module.exports = { DomainError, error };
