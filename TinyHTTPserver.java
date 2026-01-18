import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class TinyHTTPserver {
	public static void main(String[] args) throws IOException {
		int port = 8080; // not 443 (needs admin)
		ServerSocket serverSocket = new ServerSocket(port);
		System.out.println("Listening on http://localhost:" + port);
    while(true) {
      Socket clientSocket=  clientSocket.accept();
      System.out.println("Someone connected!");

      new Thread(() -> {
        handleClient(clientSocket);
    }).start();
    }
	}
}
