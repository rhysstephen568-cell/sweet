const express = require('express');
const http = require('http');
const WebSocket = require('ws');

const app = express();
app.use(express.json());

const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// Mock endpoints
app.post('/api/v1/auth/request-code', (req, res) => {
    console.log('[API] Request Code:', req.body);
    res.json({ requestId: 'req_123456', message: 'SMS code sent successfully (use 123456 for dev)' });
});

app.post('/api/v1/auth/verify-code', (req, res) => {
    console.log('[API] Verify Code:', req.body);
    res.json({ token: 'jwt_mock_token_sweet', userId: 'usr_sweet_101', isNewUser: false });
});

app.get('/api/v1/chats', (req, res) => {
    res.json([
        { chatId: '101', name: 'SWEET Support Team', lastMessage: 'Welcome to SWEET messenger!', unreadCount: 1 }
    ]);
});

// WebSocket Realtime Events Connection
wss.on('connection', (ws) => {
    console.log('[WebSocket] Client connected to SWEET realtime gateway');

    ws.on('message', (message) => {
        console.log('[WebSocket] Received message:', message.toString());
        // Echo back acknowledgement
        ws.send(JSON.stringify({
            event: 'message.read',
            payload: { chatId: '101', messageId: 'msg_ack', pts: 1 },
            timestamp: Date.now()
        }));
    });

    ws.on('close', () => {
        console.log('[WebSocket] Client disconnected');
    });
});

const PORT = process.env.PORT || 8080;
server.listen(PORT, () => {
    console.log(`SWEET Mock Server running on http://localhost:${PORT}`);
});
