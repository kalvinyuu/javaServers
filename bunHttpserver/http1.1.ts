class MyWebServer {
  // State Management
  private requestCount: number = 0;
  private readonly startTime: number;
  private readonly port: number;
  private readonly webRoot: string;

  // Same MIME types as Java version
  private readonly mimeTypes: Record<string, string> = {
    "html": "text/html",
    "htm": "text/html",
    "css": "text/css",
    "js": "application/javascript",
    "json": "application/json",
    "txt": "text/plain",
    "jpg": "image/jpeg",
    "jpeg": "image/jpeg",
    "png": "image/png",
    "gif": "image/gif",
    "pdf": "application/pdf",
    "ico": "image/x-icon",
    "svg": "image/svg+xml",
    "xml": "application/xml",
  };

  constructor(port: number = 3000, webRoot: string = "./web_root") {
    this.port = port;
    this.webRoot = webRoot;
    this.startTime = Date.now();
  }

  // The main entry point for Bun.serve
  private async handleRequest(req: Request): Promise<Response> {
    try {
      const url = new URL(req.url);
      const path = url.pathname;
      this.requestCount++;

      // Log request
      console.log(`${req.method} ${path} - Request #${this.requestCount}`);

      // Route based on HTTP method (like Java version)
      switch (req.method) {
        case "GET":
          return await this.handleGet(path);
        case "POST":
        case "PUT":
          return await this.handlePostPut(req, path);
        case "DELETE":
          return await this.handleDelete(path);
        case "HEAD":
          return await this.handleHead(path);
        default:
          return this.errorResponse(405, "Method Not Allowed");
      }
    } catch (error) {
      console.error('Request handling error:', error);
      return this.errorResponse(500, "Internal Server Error");
    }
  }

  // GET handler - identical to Java serveFile()
  private async handleGet(path: string): Promise<Response> {
    // Handle root path
    if (path === "/" || path === "") {
      path = "/index.html";
    }

    try {
      const filePath = this.resolveFilePath(path);
      
      // Check if file exists
      const file = Bun.file(filePath);
      const exists = await file.exists();
      
      if (!exists) {
        return this.errorResponse(404, "Not Found");
      }

      // Get file info
      const stats = await file.stat();
      if (stats.isDirectory) {
        // Try index.html in directory (optional, like Java)
        const indexFile = Bun.file(`${filePath}/index.html`);
        if (await indexFile.exists()) {
          return await this.serveFile(`${path}/index.html`);
        }
        return this.errorResponse(404, "Not Found");
      }

      return await this.serveFile(path);
    } catch (error) {
      console.error(`Error serving ${path}:`, error);
      return this.errorResponse(500, "Internal Server Error");
    }
  }

  // POST/PUT handler - identical to Java saveFile()
  private async handlePostPut(req: Request, path: string): Promise<Response> {
    try {
      // Security: Prevent directory traversal
      if (path.includes("..")) {
        return this.errorResponse(403, "Forbidden");
      }

      const filePath = this.resolveFilePath(path);
      
      // Create directory if it doesn't exist
      const dir = filePath.substring(0, filePath.lastIndexOf("/"));
      try {
        await Bun.$`mkdir -p ${dir}`.quiet();
      } catch {
        // Directory creation failed, but we'll try to write anyway
      }
      
      // Read request body and write to file
      const body = await req.arrayBuffer();
      await Bun.write(filePath, new Uint8Array(body));
      
      return this.errorResponse(201, "Created");
    } catch (error) {
      console.error(`Error saving ${path}:`, error);
      return this.errorResponse(500, "Internal Server Error");
    }
  }

  // DELETE handler - identical to Java deleteFile()
  private async handleDelete(path: string): Promise<Response> {
    try {
      // Security: Prevent directory traversal
      if (path.includes("..")) {
        return this.errorResponse(403, "Forbidden");
      }

      const filePath = this.resolveFilePath(path);
      
      // Check if file exists
      const file = Bun.file(filePath);
      const exists = await file.exists();
      
      if (!exists) {
        return this.errorResponse(404, "Not Found");
      }

      // Delete the file
      try {
        await Bun.$`rm ${filePath}`.quiet();
      } catch (rmError) {
        // Try Bun's built-in method if shell fails
        await Bun.write(filePath, "");
        const fs = require('fs');
        fs.unlinkSync(filePath);
      }
      
      return new Response(null, { status: 204 }); // No Content
    } catch (error) {
      console.error(`Error deleting ${path}:`, error);
      return this.errorResponse(500, "Internal Server Error");
    }
  }

  // HEAD handler - identical to Java would be
  private async handleHead(path: string): Promise<Response> {
    // Same as GET but without body
    if (path === "/" || path === "") {
      path = "/index.html";
    }

    try {
      const filePath = this.resolveFilePath(path);
      const file = Bun.file(filePath);
      const exists = await file.exists();
      
      if (!exists) {
        return this.errorResponse(404, "Not Found");
      }

      const stats = await file.stat();
      if (stats.isDirectory) {
        return this.errorResponse(404, "Not Found");
      }

      const contentType = this.getContentType(path);
      return new Response(null, {
        status: 200,
        headers: {
          "Content-Type": contentType,
          "Content-Length": stats.size.toString()
        }
      });
    } catch (error) {
      console.error(`Error HEAD ${path}:`, error);
      return this.errorResponse(500, "Internal Server Error");
    }
  }

  // Serve file with proper MIME type - identical to Java version
  private async serveFile(path: string): Promise<Response> {
    const filePath = this.resolveFilePath(path);
    const file = Bun.file(filePath);
    
    // Get file stats
    const stats = await file.stat();
    
    // Get MIME type
    const contentType = this.getContentType(path);
    
    // Create response headers
    const headers = new Headers({
      "Content-Type": contentType,
      "Content-Length": stats.size.toString()
    });
    
    // Add charset for text files (same as Java)
    if (contentType.startsWith("text/") || 
        contentType.includes("javascript") || 
        contentType.includes("json") || 
        contentType.includes("xml")) {
      headers.set("Content-Type", `${contentType}; charset=utf-8`);
    }
    
    return new Response(file, {
      status: 200,
      headers: headers
    });
  }

  // Utility methods
  private resolveFilePath(path: string): string {
    // Remove leading slash if present
    const cleanPath = path.startsWith("/") ? path.substring(1) : path;
    return `${this.webRoot}/${cleanPath}`;
  }

  private getContentType(path: string): string {
    // Extract file extension
    const lastDot = path.lastIndexOf(".");
    if (lastDot === -1) {
      return "application/octet-stream";
    }
    
    const ext = path.substring(lastDot + 1).toLowerCase();
    return this.mimeTypes[ext] || "application/octet-stream";
  }

  private errorResponse(status: number, message: string): Response {
    // Same format as Java sendError()
    const response = `HTTP/1.1 ${status} ${message}\n\n${message}`;
    return new Response(response, {
      status: status,
      headers: { "Content-Type": "text/plain" }
    });
  }

  // Start Method
  public start() {
    Bun.serve({
      port: this.port,
      fetch: (req) => this.handleRequest(req),
      error: (err) => {
        console.error("Server error:", err);
        return new Response("Internal Server Error", { status: 500 });
      },
    });

    console.log(`Server started on port ${this.port}`);
    console.log(`Web root: ${this.webRoot}`);
  }
}

// Initialization
const port = 8080;
const app = new MyWebServer(port);
app.start();
