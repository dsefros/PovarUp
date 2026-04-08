const http = require('node:http');
const { seed } = require('./domain/store');
const { createMarketplaceRepository } = require('./repositories/marketplaceRepository');
const { createApp } = require('./http/app');
const { handleError } = require('./http/http');

seed();
const repo = createMarketplaceRepository();
const app = createApp({ repo, seed });

const server = http.createServer(async (req, res) => {
  try {
    await app(req, res);
  } catch (err) {
    handleError(res, err);
  }
});

if (require.main === module) {
  const port = process.env.PORT || 4000;
  server.listen(port, () => console.log(`Backend listening on ${port}`));
}

module.exports = server;
