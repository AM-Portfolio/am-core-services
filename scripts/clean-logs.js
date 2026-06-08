const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const logsDir = path.join(rootDir, 'logs');

if (!fs.existsSync(logsDir)) {
  console.log('[clean-logs] Logs directory does not exist.');
  process.exit(0);
}

fs.rmSync(logsDir, { recursive: true, force: true });
console.log('[clean-logs] Cleared logs directory.');
