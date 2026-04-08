const { error } = require('./errors');

const requireString = (value, field) => {
  if (typeof value !== 'string' || !value.trim()) {
    throw error('validation_error', `${field} is required`, { status: 422, details: { field } });
  }
  return value.trim();
};

const requireNumber = (value, field, { min } = {}) => {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    throw error('validation_error', `${field} must be a number`, { status: 422, details: { field } });
  }
  if (min !== undefined && value < min) {
    throw error('validation_error', `${field} must be at least ${min}`, { status: 422, details: { field, min } });
  }
  return value;
};

const requireOneOf = (value, field, options) => {
  if (!options.includes(value)) {
    throw error('validation_error', `${field} must be one of: ${options.join(', ')}`, { status: 422, details: { field, options } });
  }
  return value;
};

module.exports = { requireString, requireNumber, requireOneOf };
