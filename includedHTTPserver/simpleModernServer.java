import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class simpleModernServer {
    
    private static final String WEB_ROOT = "web_root";
    private static final Map<String, String> MIME_TYPES = createMimeTypes();
    
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        System.out.println("Server started on port " + port);
        System.out.println("Web root: " + new File(WEB_ROOT).getAbsolutePath());
        
        // Single handler like your original design
        server.createContext("/", exchange -> {
            try {
                handleRequest(exchange);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        });
        
        server.start();
    }
    
    private static void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        System.out.println(method + " " + path);
        
        switch (method) {
            case "GET":
                serveFile(exchange, path);
                break;
            case "POST":
            case "PUT":
                saveFile(exchange, path);
                break;
            case "DELETE":
                deleteFile(exchange, path);
                break;
            default:
                sendResponse(exchange, 405, "Method Not Allowed");
        }
    }
    
    private static void serveFile(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/")) path = "/index.html";
        
        File file = new File(WEB_ROOT + path);
        
        if (file.exists() && !file.isDirectory()) {
            byte[] data = Files.readAllBytes(file.toPath());
            String contentType = getMimeType(file.getName());
            
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, data.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        } else {
            sendResponse(exchange, 404, "Not Found");
        }
    }
    
    private static void saveFile(HttpExchange exchange, String path) throws IOException {
        File file = new File(WEB_ROOT + path);
        file.getParentFile().mkdirs();
        
        try (InputStream is = exchange.getRequestBody();
             FileOutputStream fos = new FileOutputStream(file)) {
            is.transferTo(fos);
        }
        
        sendResponse(exchange, 201, "Created");
    }
    
    private static void deleteFile(HttpExchange exchange, String path) throws IOException {
        File file = new File(WEB_ROOT + path);
        
        if (file.delete()) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            sendResponse(exchange, 404, "Not Found");
        }
    }
    
    private static void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        byte[] data = message.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(code, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }
    
    private static String getMimeType(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            String ext = filename.substring(dot + 1).toLowerCase();
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }
    
    private static Map<String, String> createMimeTypes() {
        Map<String, String> map = new HashMap<>();
        map.put("html", "text/html; charset=utf-8");
        map.put("htm", "text/html; charset=utf-8");
        map.put("css", "text/css");
        map.put("js", "application/javascript");
        map.put("txt", "text/plain; charset=utf-8");
        map.put("jpg", "image/jpeg");
        map.put("jpeg", "image/jpeg");
        map.put("png", "image/png");
        map.put("json", "application/json");
        return map;
    }
}