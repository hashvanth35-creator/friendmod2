// Minimal WebSocket relay for the Friend Mod.
//
// Protocol (matches mod/src/main/java/com/friendmod/RelayClient.java exactly):
//   Client -> Server:
//     { "type": "hello",    "from": "username", "location": {"type":"menu","name":""} }
//     { "type": "presence", "from": "username", "location": {"type":"singleplayer"|"multiplayer"|"menu","name":"..."} }
//     { "type": "chat",     "from": "username", "to": "friendUsername", "message": "..." }
//
//   Server -> Client:
//     { "type": "presence", "from": "username", "status": "online"|"offline", "location": {...} }
//     { "type": "chat",     "from": "username", "message": "...", "timestamp": 1234567890 }
//
// No auth, no friend-graph on the server side — it just broadcasts presence to
// everyone connected and routes chat by username. The mod itself only acts on
// messages from users already on your local friends list, so this stays simple
// on purpose. Good enough for two friends; don't expose this to a large group
// without adding real auth.

const http = require('http');
const { WebSocketServer } = require('ws');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;

// username (lowercase) -> { ws, username, location }
const clients = new Map();

// Queue chat messages for users who are currently offline so they get them
// on reconnect. Persisted to disk so a Render restart doesn't lose them.
const QUEUE_FILE = path.join(__dirname, 'offline_queue.json');
let offlineQueue = loadQueue();

function loadQueue() {
    try {
        if (fs.existsSync(QUEUE_FILE)) {
            return JSON.parse(fs.readFileSync(QUEUE_FILE, 'utf8'));
        }
    } catch (e) {
        console.error('Failed to load offline queue:', e);
    }
    return {}; // lowercase username -> [ {from, message, timestamp}, ... ]
}

function saveQueue() {
    try {
        fs.writeFileSync(QUEUE_FILE, JSON.stringify(offlineQueue));
    } catch (e) {
        console.error('Failed to save offline queue:', e);
    }
}

const server = http.createServer((req, res) => {
    // Simple health check so Render (and you) can confirm it's alive.
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('friendmod relay ok\n');
});

const wss = new WebSocketServer({ server });

function send(ws, obj) {
    if (ws.readyState === ws.OPEN) {
        ws.send(JSON.stringify(obj));
    }
}

function broadcastPresence(fromUsername, status, location) {
    const msg = { type: 'presence', from: fromUsername, status, location };
    for (const [key, client] of clients) {
        if (key !== fromUsername.toLowerCase()) {
            send(client.ws, msg);
        }
    }
}

wss.on('connection', (ws) => {
    let username = null;

    ws.on('message', (raw) => {
        let data;
        try {
            data = JSON.parse(raw.toString());
        } catch (e) {
            return; // ignore malformed messages
        }
        if (!data || !data.type || !data.from) return;

        if (data.type === 'hello') {
            username = data.from;
            const key = username.toLowerCase();
            clients.set(key, { ws, username, location: data.location || { type: 'menu', name: '' } });

            broadcastPresence(username, 'online', data.location || { type: 'menu', name: '' });

            // Flush any chat messages that arrived while this user was offline.
            const queued = offlineQueue[key];
            if (queued && queued.length) {
                for (const m of queued) {
                    send(ws, { type: 'chat', from: m.from, message: m.message, timestamp: m.timestamp });
                }
                delete offlineQueue[key];
                saveQueue();
            }
            return;
        }

        if (!username) return; // must say hello first

        if (data.type === 'presence') {
            const key = username.toLowerCase();
            const entry = clients.get(key);
            if (entry) entry.location = data.location;
            broadcastPresence(username, 'online', data.location);
            return;
        }

        if (data.type === 'chat') {
            const toKey = (data.to || '').toLowerCase();
            const payload = {
                type: 'chat',
                from: username,
                message: data.message,
                timestamp: Date.now(),
            };
            const target = clients.get(toKey);
            if (target) {
                send(target.ws, payload);
            } else {
                // recipient offline right now — queue it for when they reconnect
                if (!offlineQueue[toKey]) offlineQueue[toKey] = [];
                offlineQueue[toKey].push({ from: username, message: data.message, timestamp: payload.timestamp });
                saveQueue();
            }
            return;
        }
    });

    ws.on('close', () => {
        if (username) {
            const key = username.toLowerCase();
            clients.delete(key);
            broadcastPresence(username, 'offline', { type: 'menu', name: '' });
        }
    });
});

server.listen(PORT, () => {
    console.log(`friendmod relay listening on ${PORT}`);
});
