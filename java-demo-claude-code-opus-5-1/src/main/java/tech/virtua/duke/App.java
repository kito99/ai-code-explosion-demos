package tech.virtua.duke;

import java.io.IOException;

import static java.lang.String.format;

/// Entry point. Starts the game on the port given as the first argument, the
/// `PORT` environment variable, or 8080.
public final class App {

    private static final int DEFAULT_PORT = 8080;
    private static final String PORT_VARIABLE = "PORT";
    private static final int LOWEST_PORT = 0;
    private static final int HIGHEST_PORT = 65_535;

    private App() {
    }

    public static void main(final String[] args) throws IOException {
        final var server = GameServer.start(resolvePort(args), SecretNumberPicker.random());
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "duke-shutdown"));
        System.out.println(format("Guess the Number with Duke is running at %s", server.browserUrl()));
    }

    private static int resolvePort(final String[] args) {
        if (args.length > 0) {
            return parsePort(args[0]);
        }
        final var fromEnvironment = System.getenv(PORT_VARIABLE);
        return fromEnvironment == null ? DEFAULT_PORT : parsePort(fromEnvironment);
    }

    private static int parsePort(final String candidate) {
        final int port;
        try {
            port = Integer.parseInt(candidate.strip());
        } catch (final NumberFormatException notANumber) {
            throw new IllegalArgumentException(format("'%s' is not a port number.", candidate), notANumber);
        }
        if (port < LOWEST_PORT || port > HIGHEST_PORT) {
            throw new IllegalArgumentException(
                    format("Port %d is outside %d-%d.", port, LOWEST_PORT, HIGHEST_PORT));
        } 
        return port;
    }
}
