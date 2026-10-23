package tech.virtua.duke;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/// The running game: an HTTP server plus the background sweep that forgets
/// games whose players have gone away.
public final class GameServer implements AutoCloseable {

    /// How often idle games are swept.
    public static final Duration SWEEP_INTERVAL = Duration.ofMinutes(5);

    /// Pending connections the operating system may queue, zero meaning the
    /// system default.
    private static final int SYSTEM_DEFAULT_BACKLOG = 0;

    /// Seconds to let in-flight exchanges finish while shutting down.
    private static final int SHUTDOWN_GRACE_SECONDS = 1;

    private final HttpServer httpServer;
    private final ScheduledExecutorService sessionSweeper;

    private GameServer(final HttpServer httpServer, final ScheduledExecutorService sessionSweeper) {
        this.httpServer = requireNonNull(httpServer);
        this.sessionSweeper = requireNonNull(sessionSweeper);
    }

    /// Starts the game on `port`, using `secretNumberPicker` for every new game.
    /// Pass port zero to let the system choose a free port. The game is reachable
    /// from this machine only: it is single-player on one device, so there is
    /// nothing for the wider network to reach.
    public static GameServer start(final int port, final SecretNumberPicker secretNumberPicker) throws IOException {
        final var sessions = new GameSessions(secretNumberPicker, Clock.systemUTC());
        final var address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        final var httpServer = HttpServer.create(address, SYSTEM_DEFAULT_BACKLOG);
        httpServer.createContext("/api", new GameApiHandler(sessions));
        httpServer.createContext("/", StaticAssetHandler.loadFromClasspath());
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        final var sweeper = Executors.newSingleThreadScheduledExecutor(sweeperThreadFactory());
        final var intervalMillis = SWEEP_INTERVAL.toMillis();
        sweeper.scheduleWithFixedDelay(sessions::removeIdleGames, intervalMillis, intervalMillis, MILLISECONDS);

        httpServer.start();
        return new GameServer(httpServer, sweeper);
    }

    /// The port the server is actually listening on.
    public int port() {
        return httpServer.getAddress().getPort();
    }

    /// The address a browser should open.
    public String browserUrl() {
        return "http://localhost:%d/".formatted(port());
    }

    @Override
    public void close() {
        sessionSweeper.shutdownNow();
        httpServer.stop(SHUTDOWN_GRACE_SECONDS);
    }

    /// A daemon platform thread: request handling runs on virtual threads, but a
    /// scheduled timer is not work a virtual thread is suited to carry.
    private static ThreadFactory sweeperThreadFactory() {
        return runnable -> {
            final var thread = new Thread(runnable, "duke-session-sweeper");
            thread.setDaemon(true);
            return thread;
        };
    }
}
