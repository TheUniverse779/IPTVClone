// Run worker.js locally (Node 18+): node docs/review/xtream-demo/local.mjs  → http://127.0.0.1:8787
// Same code Cloudflare runs, so the app can be tested before deploying (adb reverse tcp:8787 tcp:8787).
import http from 'node:http';
import worker from './worker.js';

const PORT = Number(process.env.PORT || 8787);

http.createServer(async (req, res) => {
  const url = `http://${req.headers.host || `127.0.0.1:${PORT}`}${req.url}`;
  const response = await worker.fetch(new Request(url, { method: req.method, headers: req.headers }));
  const headers = Object.fromEntries(response.headers.entries());
  res.writeHead(response.status, headers);
  res.end(Buffer.from(await response.arrayBuffer()));
  console.log(req.method, req.url, response.status);
}).listen(PORT, '0.0.0.0', () => console.log(`Xtream demo on http://127.0.0.1:${PORT}`));
