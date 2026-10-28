package com.example.duke;

/** Entry point: wires the database, game service, and web server. */
public final class Application {

    private Application() {
    }

    public static void main(final String[] arguments) {
        final var config = AppConfig.fromEnvironment(System.getenv());
        final var dataSource = DataSources.create(config.databaseUrl(), config.databaseUser(), config.databasePassword());
        SchemaInitializer.initialize(dataSource);
        final var service = new GameService(new JdbcLeaderboardRepository(dataSource), GameService::randomSecretNumber);
        final var server = new WebServer(service).start(config.port());
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            server.stop();
            dataSource.close();
        }));
    }
}
