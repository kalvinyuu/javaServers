class MyWebServer {
  // State Management
  private requestCount: number = 0;
  private readonly startTime: number;
  private readonly port: number;

  constructor(port: number = 3000) {
    this.port = port;
    this.startTime = Date.now();
  }

  // The main entry point for Bun.serve
	private async handleRequest(req: Request): Promise<Response> {
		try {
			const url = new URL(req.url);
			this.requestCount++;

			// Routing Logic
			switch (url.pathname) {
				case "/":
					return this.homeRoute();
				case "/stats":
					return this.statsRoute();
				case "/increment":
					return this.incrementRoute(req);
				default:
					return this.notFound();
			}
		} catch (error) {
    console.error('Request handling error:', error);
    return new Response("Internal Server Error", { status: 500 });
  }
	  
  }

  // Logic: Route Handlers
  private homeRoute(): Response {
    return new Response("<h1>Class-based Bun Server</h1>", {
      headers: { "Content-Type": "text/html" },
    });
  }

  private statsRoute(): Response {
    const uptime = (Date.now() - this.startTime) / 1000;
    return Response.json({
      uptime: `${uptime.toFixed(2)}s`,
      totalRequests: this.requestCount,
    });
  }

  private async incrementRoute(req: Request): Promise<Response> {
    if (req.method !== "POST") {
      return new Response("Method Not Allowed", { status: 405 });
    }
    // Logic for processing data
    return Response.json({ success: true, count: this.requestCount });
  }

  private notFound(): Response {
    return new Response("404 Not Found", { status: 404 });
  }

  // Start Method
  public start() {
    Bun.serve({
      port: this.port,
      // We must bind 'this' so the handler can access class properties
      fetch: (req) => this.handleRequest(req),
      error: (err) => {
        console.error(err);
        return new Response("Internal Server Error", { status: 500 });
      },
    });

    console.log(`🚀 Server started on http://localhost:${this.port}`);
  }
}

// Initialization
const app = new MyWebServer(8080);
app.start();
