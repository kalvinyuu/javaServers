import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TinyHTTPserver {

    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("htm", "text/html");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("xml", "application/xml");
        // Default MIME type for unknown extensions
    }
  public static void main(String[] args) {
    int port = 8080;

    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Server started on port " + port);

      // The main Loop: Keeps the server running forever
      while (true) {
        // .accept() blocks here until a browser/client connects
        Socket clientSocket = serverSocket.accept();
        System.out.println("\nNew connection from: " + clientSocket.getRemoteSocketAddress());

        // Handle the connection in a new thread so the server stays responsive
        new Thread(() -> handleClient(clientSocket)).start();
      }
    } catch (IOException e) {
      System.err.println("Server error: " + e.getMessage());
    }
  }
  
  
  private static void handleClient(Socket clientSocket) {
    try (
        InputStream in = clientSocket.getInputStream();
        OutputStream out = clientSocket.getOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
    ) {
        // 1. Read the Request Line (e.g., "GET /index.html HTTP/1.1")
        String line = reader.readLine();
        if (line == null || line.isEmpty()) return;

        String[] parts = line.split(" ");
        if (parts.length < 2) return;

        String method = parts[0]; // GET, POST, etc.
        String path = parts[1];   // /index.html

        // 2. Read Headers to find Content-Length (needed for POST/PUT)
        int contentLength = 0;
        String headerLine;
        while (!(headerLine = reader.readLine()).isEmpty()) {
            if (headerLine.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
            }
        }

        // 3. Route the request
        switch (method) {
            case "GET":
                serveFile(path, out);
                break;
            case "POST":
            case "PUT":
                saveFile(path, in, contentLength, out);
                break;
            case "DELETE":
                deleteFile(path, out);
                break;
            default:
                sendError(out, "405 Method Not Allowed");
        }

    } catch (IOException e) {
        System.err.println("Client handling error: " + e.getMessage());
    } finally {
        try {
            clientSocket.close();
        } catch (IOException e) {
            // Log closure error
        }
    }
}
  
  private static void sendError(OutputStream out, String errorCode) throws IOException {
    String response = "HTTP/1.1 " + errorCode + "\r\n" +
      "Content-Type: text/plain\r\n" +
      "\r\n" +
      "Error: " + errorCode;
    out.write(response.getBytes());
  }
  
private static void serveFile(String path, OutputStream out) throws IOException {
    File file = new File("web_root" + path);

    if (file.exists() && !file.isDirectory()) {
        // Get MIME type from file extension
        String contentType = getContentType(file.getName());
        
        String header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: " + contentType + "\r\n" +
            "Content-Length: " + file.length() + "\r\n" +
            "\r\n";
        out.write(header.getBytes());

        Files.copy(file.toPath(), out);
    } else {
        sendError(out, "404 Not Found");
    }
}

private static String getContentType(String filename) {
    // Extract file extension
    int lastDotIndex = filename.lastIndexOf('.');
    if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
        String mimeType = MIME_TYPES.get(extension);
        if (mimeType != null) {
            return mimeType;
        }
    }
    // Default for unknown file types
    return "application/octet-stream";
}

  private static void saveFile(String path, InputStream in, int contentLength, OutputStream out) throws IOException {
    File file = new File("web_root" + path);
    
    // Ensure the folders exist
    file.getParentFile().mkdirs();

    try (FileOutputStream fileOut = new FileOutputStream(file)) {
      byte[] buffer = new byte[8192];
      int totalRead = 0;
      int bytesRead;

      // CRITICAL: Only read as many bytes as the Content-Length header says!
      while (totalRead < contentLength && (bytesRead = in.read(buffer, 0, Math.min(buffer.length, contentLength - totalRead))) != -1) {
        fileOut.write(buffer, 0, bytesRead);
        totalRead += bytesRead;
      }
    }

    String response = "HTTP/1.1 201 Created\r\n\r\n";
    out.write(response.getBytes());
  }

  private static void deleteFile(String path, OutputStream out) throws IOException {
    File file = new File("web_root" + path);

    if (file.exists() && file.delete()) {
      out.write("HTTP/1.1 204 No Content\r\n\r\n".getBytes());
    } else {
      sendError(out, "404 Not Found");
    }
  }
}
