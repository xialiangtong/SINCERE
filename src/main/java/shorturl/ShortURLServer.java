package shorturl;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * HTTP Server for URL Shortener Service
 * Provides REST API and serves static frontend
 */
public class ShortURLServer {

    private final HttpServer server;
    private final ShortURLService service;
    private final int port;

    public ShortURLServer(int port) throws IOException {
        this.port = port;
        this.service = new ShortURLService();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        
        setupRoutes();
        server.setExecutor(Executors.newFixedThreadPool(10));
    }

    private void setupRoutes() {
        // API endpoints
        server.createContext("/api/shorten", new ShortenHandler());
        server.createContext("/api/resolve", new ResolveHandler());
        server.createContext("/api/stats", new StatsHandler());
        
        // Redirect endpoint
        server.createContext("/s/", new RedirectHandler());
        
        // Static files (frontend)
        server.createContext("/", new StaticHandler());
    }

    public void start() {
        server.start();
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║         URL Shortener Server Started                  ║");
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        System.out.printf("║  Server running at: http://localhost:%-17d║%n", port);
        System.out.println("║  API Endpoints:                                       ║");
        System.out.println("║    POST /api/shorten  - Shorten a URL                 ║");
        System.out.println("║    GET  /api/resolve  - Resolve a short code          ║");
        System.out.println("║    GET  /api/stats    - Get service statistics        ║");
        System.out.println("║    GET  /s/{code}     - Redirect to original URL      ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Server stopped.");
    }

    // ==================== Handlers ====================

    /**
     * POST /api/shorten
     * Body: { "url": "https://example.com/long/path" }
     * Response: { "shortCode": "Ab3k9Z", "shortUrl": "http://localhost:8080/s/Ab3k9Z", "longUrl": "..." }
     */
    private class ShortenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String body = readBody(exchange);
                Map<String, String> json = parseJson(body);
                String longUrl = json.get("url");

                if (longUrl == null || longUrl.trim().isEmpty()) {
                    sendError(exchange, 400, "URL is required");
                    return;
                }

                ShortURLService.ShortURLResult result = service.shorten(longUrl.trim());
                
                String localShortUrl = String.format("http://localhost:%d/s/%s", port, result.getShortCode());
                
                String response = String.format(
                    "{\"shortCode\":\"%s\",\"shortUrl\":\"%s\",\"longUrl\":\"%s\"}",
                    result.getShortCode(),
                    localShortUrl,
                    escapeJson(result.getLongUrl())
                );
                
                sendJson(exchange, 200, response);
                
            } catch (Exception e) {
                sendError(exchange, 500, "Error: " + e.getMessage());
            }
        }
    }

    /**
     * GET /api/resolve?code=Ab3k9Z
     * Response: { "longUrl": "https://example.com/long/path" }
     */
    private class ResolveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                String code = params.get("code");

                if (code == null || code.trim().isEmpty()) {
                    sendError(exchange, 400, "Code parameter is required");
                    return;
                }

                String longUrl = service.resolve(code.trim());
                String response = String.format("{\"longUrl\":\"%s\"}", escapeJson(longUrl));
                sendJson(exchange, 200, response);
                
            } catch (ShortURLService.URLNotFoundException e) {
                sendError(exchange, 404, "Short code not found");
            } catch (Exception e) {
                sendError(exchange, 500, "Error: " + e.getMessage());
            }
        }
    }

    /**
     * GET /s/{code} - Redirect to original URL
     */
    private class RedirectHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String code = path.substring("/s/".length());

            try {
                String longUrl = service.resolve(code);
                
                // Increment access count
                service.recordAccess(code);
                
                // Send redirect
                exchange.getResponseHeaders().set("Location", longUrl);
                exchange.sendResponseHeaders(302, -1);
                
            } catch (ShortURLService.URLNotFoundException e) {
                sendError(exchange, 404, "Short URL not found");
            } catch (Exception e) {
                sendError(exchange, 500, "Error: " + e.getMessage());
            }
        }
    }

    /**
     * GET /api/stats
     * Response: { "totalUrls": 10, "totalAccesses": 100 }
     */
    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            String response = String.format(
                "{\"totalUrls\":%d,\"totalAccesses\":%d}",
                service.getTotalUrls(),
                service.getTotalAccesses()
            );
            sendJson(exchange, 200, response);
        }
    }

    /**
     * Serve static files
     */
    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if ("/".equals(path) || "/index.html".equals(path)) {
                serveHtml(exchange);
            } else if ("/style.css".equals(path)) {
                serveCss(exchange);
            } else if ("/app.js".equals(path)) {
                serveJs(exchange);
            } else {
                sendError(exchange, 404, "Not found");
            }
        }

        private void serveHtml(HttpExchange exchange) throws IOException {
            String html = getIndexHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }

        private void serveCss(HttpExchange exchange) throws IOException {
            String css = getStyleCss();
            byte[] bytes = css.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/css; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }

        private void serveJs(HttpExchange exchange) throws IOException {
            String js = getAppJs();
            byte[] bytes = js.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }
    }

    // ==================== Utility Methods ====================

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }

    private Map<String, String> parseJson(String json) {
        Map<String, String> result = new HashMap<>();
        // Simple JSON parsing for {"key": "value"} format
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2) {
                    params.put(
                        URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                    );
                }
            }
        }
        return params;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        String json = String.format("{\"error\":\"%s\"}", escapeJson(message));
        sendJson(exchange, status, json);
    }

    // ==================== Embedded Frontend ====================

    private String getIndexHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>URL Shortener</title>
    <link rel="stylesheet" href="/style.css">
</head>
<body>
    <div class="container">
        <header>
            <h1>🔗 URL Shortener</h1>
            <p class="subtitle">Create short, shareable links instantly</p>
        </header>

        <main>
            <!-- Shorten Form -->
            <section class="card">
                <h2>Shorten a URL</h2>
                <form id="shortenForm">
                    <div class="input-group">
                        <input type="url" id="longUrl" placeholder="Enter your long URL here..." required>
                        <button type="submit">Shorten</button>
                    </div>
                </form>
                
                <div id="result" class="result hidden">
                    <h3>Your shortened URL:</h3>
                    <div class="short-url-box">
                        <a id="shortUrl" href="#" target="_blank"></a>
                        <button id="copyBtn" class="copy-btn">📋 Copy</button>
                    </div>
                    <p class="original">Original: <span id="originalUrl"></span></p>
                </div>
            </section>

            <!-- Resolve Form -->
            <section class="card">
                <h2>Resolve a Short Code</h2>
                <form id="resolveForm">
                    <div class="input-group">
                        <input type="text" id="shortCode" placeholder="Enter short code (e.g., Ab3k9Z)" required>
                        <button type="submit">Resolve</button>
                    </div>
                </form>
                
                <div id="resolveResult" class="result hidden">
                    <h3>Original URL:</h3>
                    <a id="resolvedUrl" href="#" target="_blank"></a>
                </div>
            </section>

            <!-- Stats -->
            <section class="card stats-card">
                <h2>📊 Statistics</h2>
                <div class="stats">
                    <div class="stat">
                        <span class="stat-value" id="totalUrls">0</span>
                        <span class="stat-label">URLs Shortened</span>
                    </div>
                    <div class="stat">
                        <span class="stat-value" id="totalAccesses">0</span>
                        <span class="stat-label">Total Redirects</span>
                    </div>
                </div>
            </section>
        </main>

        <footer>
            <p>Built with Java HttpServer • In-memory storage</p>
        </footer>
    </div>

    <script src="/app.js"></script>
</body>
</html>
""";
    }

    private String getStyleCss() {
        return """
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    min-height: 100vh;
    padding: 20px;
}

.container {
    max-width: 700px;
    margin: 0 auto;
}

header {
    text-align: center;
    color: white;
    margin-bottom: 30px;
}

header h1 {
    font-size: 2.5rem;
    margin-bottom: 10px;
    text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
}

.subtitle {
    opacity: 0.9;
    font-size: 1.1rem;
}

.card {
    background: white;
    border-radius: 16px;
    padding: 24px;
    margin-bottom: 20px;
    box-shadow: 0 10px 40px rgba(0,0,0,0.15);
}

.card h2 {
    color: #333;
    margin-bottom: 16px;
    font-size: 1.3rem;
}

.input-group {
    display: flex;
    gap: 10px;
}

.input-group input {
    flex: 1;
    padding: 14px 18px;
    border: 2px solid #e0e0e0;
    border-radius: 10px;
    font-size: 1rem;
    transition: border-color 0.2s;
}

.input-group input:focus {
    outline: none;
    border-color: #667eea;
}

.input-group button {
    padding: 14px 28px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border: none;
    border-radius: 10px;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
}

.input-group button:hover {
    transform: translateY(-2px);
    box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
}

.result {
    margin-top: 20px;
    padding: 20px;
    background: #f8f9fa;
    border-radius: 10px;
}

.result.hidden {
    display: none;
}

.result h3 {
    color: #666;
    font-size: 0.9rem;
    margin-bottom: 10px;
}

.short-url-box {
    display: flex;
    align-items: center;
    gap: 10px;
    background: white;
    padding: 12px 16px;
    border-radius: 8px;
    border: 2px solid #667eea;
}

.short-url-box a {
    flex: 1;
    color: #667eea;
    font-size: 1.1rem;
    font-weight: 600;
    text-decoration: none;
    word-break: break-all;
}

.copy-btn {
    padding: 8px 16px;
    background: #667eea;
    color: white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 0.9rem;
    transition: background 0.2s;
}

.copy-btn:hover {
    background: #5a6fd6;
}

.original {
    margin-top: 12px;
    color: #888;
    font-size: 0.85rem;
    word-break: break-all;
}

#resolveResult a {
    color: #667eea;
    font-size: 1rem;
    word-break: break-all;
}

.stats-card {
    background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.stats {
    display: flex;
    justify-content: space-around;
    text-align: center;
}

.stat {
    padding: 20px;
}

.stat-value {
    display: block;
    font-size: 2.5rem;
    font-weight: 700;
    color: #667eea;
}

.stat-label {
    color: #666;
    font-size: 0.9rem;
}

footer {
    text-align: center;
    color: rgba(255,255,255,0.7);
    margin-top: 30px;
    font-size: 0.9rem;
}

@media (max-width: 600px) {
    .input-group {
        flex-direction: column;
    }
    
    .stats {
        flex-direction: column;
        gap: 10px;
    }
}
""";
    }

    private String getAppJs() {
        return """
// API Base URL
const API_BASE = '';

// DOM Elements
const shortenForm = document.getElementById('shortenForm');
const resolveForm = document.getElementById('resolveForm');
const resultDiv = document.getElementById('result');
const resolveResultDiv = document.getElementById('resolveResult');

// Shorten URL
shortenForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const longUrl = document.getElementById('longUrl').value;
    
    try {
        const response = await fetch(`${API_BASE}/api/shorten`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ url: longUrl })
        });
        
        const data = await response.json();
        
        if (response.ok) {
            document.getElementById('shortUrl').href = data.shortUrl;
            document.getElementById('shortUrl').textContent = data.shortUrl;
            document.getElementById('originalUrl').textContent = data.longUrl;
            resultDiv.classList.remove('hidden');
            updateStats();
        } else {
            alert('Error: ' + data.error);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
});

// Resolve Short Code
resolveForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const shortCode = document.getElementById('shortCode').value;
    
    try {
        const response = await fetch(`${API_BASE}/api/resolve?code=${encodeURIComponent(shortCode)}`);
        const data = await response.json();
        
        if (response.ok) {
            document.getElementById('resolvedUrl').href = data.longUrl;
            document.getElementById('resolvedUrl').textContent = data.longUrl;
            resolveResultDiv.classList.remove('hidden');
        } else {
            alert('Error: ' + data.error);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
});

// Copy to Clipboard
document.getElementById('copyBtn').addEventListener('click', () => {
    const shortUrl = document.getElementById('shortUrl').textContent;
    navigator.clipboard.writeText(shortUrl).then(() => {
        const btn = document.getElementById('copyBtn');
        btn.textContent = '✓ Copied!';
        setTimeout(() => { btn.textContent = '📋 Copy'; }, 2000);
    });
});

// Update Stats
async function updateStats() {
    try {
        const response = await fetch(`${API_BASE}/api/stats`);
        const data = await response.json();
        document.getElementById('totalUrls').textContent = data.totalUrls;
        document.getElementById('totalAccesses').textContent = data.totalAccesses;
    } catch (error) {
        console.error('Failed to fetch stats:', error);
    }
}

// Initial stats load
updateStats();
setInterval(updateStats, 5000); // Refresh every 5 seconds
""";
    }

    // ==================== Main ====================

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        
        ShortURLServer server = new ShortURLServer(port);
        server.start();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
