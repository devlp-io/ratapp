const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 8080;
const ROOT = __dirname;
const APK_DIR = path.join(ROOT, 'apk');
const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js':   'text/javascript',
  '.css':  'text/css',
  '.json': 'application/json',
  '.apk':  'application/vnd.android.package-archive',
  '.png':  'image/png',
  '.ico':  'image/x-icon',
};

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);
  let file = url.pathname === '/' ? '/panel.html' : url.pathname;

  // mobile agents pull commands off /apk — serve whatever lands in apk/
  if (file === '/apk' || file.startsWith('/apk/')) {
    const apks = fs.existsSync(APK_DIR) ? fs.readdirSync(APK_DIR).filter(f => f.endsWith('.apk')) : [];
    if (file === '/apk' || file === '/apk/') {
      res.writeHead(200, {'Content-Type': 'application/json'});
      res.end(JSON.stringify({ apks }, null, 2));
      return;
    }
    const wanted = path.basename(file);
    const target = apks.includes(wanted) ? path.join(APK_DIR, wanted) : null;
    if (!target) {
      res.writeHead(404, {'Content-Type': 'text/plain'});
      res.end('no apk in apk/ dir — drop app-debug.apk there');
      return;
    }
    res.writeHead(200, {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Disposition': `attachment; filename="${wanted}"`,
      'Content-Length': fs.statSync(target).size,
    });
    fs.createReadStream(target).pipe(res);
    return;
  }

  const abs = path.join(ROOT, path.normalize(file).replace(/^(\.\.[\/\\])+/, ''));
  if (!abs.startsWith(ROOT)) {
    res.writeHead(403); res.end('forbidden'); return;
  }

  fs.readFile(abs, (err, data) => {
    if (err) {
      res.writeHead(404, {'Content-Type': 'text/plain'});
      res.end('not found: ' + file);
      return;
    }
    res.writeHead(200, {'Content-Type': MIME[path.extname(abs)] || 'application/octet-stream'});
    res.end(data);
  });
});

server.listen(PORT, () => {
  console.log(`panel up    http://localhost:${PORT}`);
  console.log(`apk listing http://localhost:${PORT}/apk`);
});
