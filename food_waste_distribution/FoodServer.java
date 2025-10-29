import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;

// Custom Queue implementation
class FoodQueue {
    private static final int MAX = 100;
    private String[] items = new String[MAX];
    private int front = -1, rear = -1;

    public boolean isEmpty() {
        return front == -1;
    }

    public boolean isFull() {
        return rear == MAX - 1;
    }

    public void enqueue(String item) {
        if (isFull()) {
            System.out.println("Queue is full. Cannot add more items.");
            return;
        }
        if (isEmpty()) front = 0;
        items[++rear] = item;
    }

    public String dequeue() {
        if (isEmpty()) return null;
        String item = items[front];
        if (front == rear) {
            front = rear = -1;
        } else {
            front++;
        }
        return item;
    }

    public String display() {
        if (isEmpty()) return "Queue is empty.";
        StringBuilder sb = new StringBuilder();
        sb.append("Current Food Queue:\n");
        for (int i = front; i <= rear; i++) {
            sb.append((i - front + 1) + ". " + items[i] + "\n");
        }
        return sb.toString();
    }
}

public class FoodServer {
    private static FoodQueue queue = new FoodQueue();

    public static void main(String[] args) throws Exception {
        int port = 9090; // You can change the port if needed
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("🚀 Server started at http://localhost:" + port);

        // Serve static files (HTML, CSS, JS)
        server.createContext("/", new StaticFileHandler());
        // Handle API requests
        server.createContext("/api", new FoodHandler());

        server.setExecutor(null);
        server.start();
    }

    // Serve index.html and other static files from /web folder
    static class StaticFileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            File file = new File("web" + path);
            if (!file.exists()) {
                String msg = "404 Not Found";
                exchange.sendResponseHeaders(404, msg.length());
                OutputStream os = exchange.getResponseBody();
                os.write(msg.getBytes());
                os.close();
                return;
            }

            @SuppressWarnings("resource")
            byte[] bytes = new FileInputStream(file).readAllBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // Handle /api requests for Add, Distribute, and View Queue
    static class FoodHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String formData = br.readLine();
                br.close();

                if (formData != null) {
                    formData = URLDecoder.decode(formData, "UTF-8");
                    String[] pairs = formData.split("&");
                    String action = "", food = "", priority = "";
                    for (String pair : pairs) {
                        String[] kv = pair.split("=");
                        if (kv.length == 2) {
                            switch (kv[0]) {
                                case "action": action = kv[1]; break;
                                case "food": food = kv[1]; break;
                                case "priority": priority = kv[1]; break;
                            }
                        }
                    }

                    if (action.equals("add")) {
                        queue.enqueue(food + " (Priority: " + priority + ")");
                        response = "✅ Added: " + food + " with priority " + priority;
                    } else if (action.equals("distribute")) {
                        String item = queue.dequeue();
                        response = (item == null)
                                ? "❌ No food to distribute!"
                                : "🍽️ Distributed: " + item;
                    }
                }
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                response = queue.display();
            } else {
                response = "Invalid Request!";
            }

            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
