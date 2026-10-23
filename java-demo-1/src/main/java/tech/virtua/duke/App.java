package tech.virtua.duke;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

/** Boots the game on the JDK's built-in HTTP server. No framework, no container. */
public final class App {

    private static final int DEFAULT_PORT = 8080;

    private App() {
    }

    public static void main(String[] args) throws IOException {
        GameServer server = start(resolvePort(args), new SecureRandom());
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "duke-shutdown"));
        System.out.println("Guess the Number with Duke is running at http://localhost:" + server.port() + "/");
        System.out.println("Press Ctrl+C to stop.");
    }

    /** Starts a server on {@code port} (0 picks a free one) and returns it already running. */
    public static GameServer start(int port, RandomGenerator random) throws IOException {
        GameStore store = new GameStore(random);

        HttpServer http = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        http.createContext("/", new StaticHandler());
        http.createContext("/api/", new ApiHandler(store));

        ExecutorService requests = Executors.newVirtualThreadPerTaskExecutor();
        http.setExecutor(requests);

        ScheduledExecutorService janitor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "duke-session-janitor");
            thread.setDaemon(true);
            return thread;
        });
        janitor.scheduleAtFixedRate(store::evictExpired, 10, 10, TimeUnit.MINUTES);

        http.start();
        return new GameServer(http, requests, janitor);
    }

    /** A running server plus the threads it owns, so stopping it really stops everything. */
    public record GameServer(HttpServer http, ExecutorService requests, ScheduledExecutorService janitor) {

        public int port() {
            return http.getAddress().getPort();
        }

        public void stop() {
            http.stop(0);
            janitor.shutdownNow();
            requests.shutdown();
        }
    }

    private static int resolvePort(String[] args) {
        String raw = args.length > 0 ? args[0] : System.getenv("PORT");
        if (raw == null || raw.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(raw.strip());
        } catch (NumberFormatException e) {
            System.err.println("Ignoring unusable port \"" + raw + "\"; falling back to " + DEFAULT_PORT + ".");
            return DEFAULT_PORT;
        }
    }
}
