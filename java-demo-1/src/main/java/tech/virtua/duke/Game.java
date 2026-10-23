package tech.virtua.duke;

import java.util.random.RandomGenerator;

/**
 * One round of "guess the number". All of the game rules live here so they can be
 * tested without starting a server.
 *
 * <p>A game is created already in progress: the secret is drawn and the guess count
 * starts at zero. Instances are not thread safe; {@link GameStore} guards access.
 */
public final class Game {

    public static final int MIN_NUMBER = 1;
    public static final int MAX_NUMBER = 10;
    public static final int MAX_GUESSES = 5;

    public enum Status { PLAYING, WON, LOST }

    public enum Hint { NONE, TOO_HIGH, TOO_LOW }

    private final int secret;
    private int guessesMade;
    private Status status = Status.PLAYING;
    private Hint hint = Hint.NONE;
    private Integer lastGuess;
    private String message;

    /** Starts a game with a secret drawn uniformly from {@value #MIN_NUMBER}..{@value #MAX_NUMBER}. */
    public Game(RandomGenerator random) {
        this.secret = random.nextInt(MIN_NUMBER, MAX_NUMBER + 1);
        this.message = "Duke is thinking of a number between " + MIN_NUMBER + " and " + MAX_NUMBER
                + ". You have " + MAX_GUESSES + " guesses — take your best shot!";
    }

    /** Visible only to tests; production code always draws at random. */
    Game(int secret) {
        if (secret < MIN_NUMBER || secret > MAX_NUMBER) {
            throw new IllegalArgumentException("secret out of range: " + secret);
        }
        this.secret = secret;
        this.message = "Duke is thinking of a number between " + MIN_NUMBER + " and " + MAX_NUMBER
                + ". You have " + MAX_GUESSES + " guesses — take your best shot!";
    }

    public int secret() {
        return secret;
    }

    /**
     * Scores one submitted guess. Anything that is not a whole number in range is
     * rejected with a friendly message and leaves the guess count untouched, and so
     * are guesses arriving after the game has already ended.
     */
    public GameState guess(String raw) {
        if (status != Status.PLAYING) {
            return snapshot(false, "This game is over. Hit Play again for a fresh number from Duke.");
        }

        Integer parsed = parseGuess(raw);
        if (parsed == null) {
            return snapshot(false, "Please enter a whole number from " + MIN_NUMBER + " to " + MAX_NUMBER
                    + ". That one didn't count — you still have " + remainingPhrase(guessesRemaining()) + ".");
        }

        int value = parsed;
        guessesMade++;
        lastGuess = value;

        if (value == secret) {
            status = Status.WON;
            hint = Hint.NONE;
            return snapshot(true, "You got it! Duke's number was " + secret + ", and you found it in "
                    + guessesMade + (guessesMade == 1 ? " guess." : " guesses.") + " Play again?");
        }

        hint = value > secret ? Hint.TOO_HIGH : Hint.TOO_LOW;
        String direction = hint == Hint.TOO_HIGH ? "too high" : "too low";

        // The losing guess still earns its high-low feedback (FR-7) alongside the reveal (FR-11).
        if (guessesRemaining() == 0) {
            status = Status.LOST;
            return snapshot(true, value + " is " + direction + ", and that was your last guess. Duke was thinking of "
                    + secret + ". Hit Play again for a fresh number.");
        }

        String advice = hint == Hint.TOO_HIGH ? "Try a lower number" : "Try a higher number";
        return snapshot(true, value + " is " + direction + ". " + advice + " — "
                + remainingPhrase(guessesRemaining()) + " left.");
    }

    /** The current state, repeating whatever message the last action produced. */
    public GameState state() {
        return snapshot(false, message);
    }

    private GameState snapshot(boolean accepted, String text) {
        this.message = text;
        return new GameState(
                status,
                guessesMade,
                guessesRemaining(),
                MAX_GUESSES,
                MIN_NUMBER,
                MAX_NUMBER,
                lastGuess,
                hint,
                text,
                accepted,
                status == Status.LOST ? Integer.valueOf(secret) : null);
    }

    private int guessesRemaining() {
        return Math.max(0, MAX_GUESSES - guessesMade);
    }

    private static Integer parseGuess(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.strip();
        if (trimmed.isEmpty() || trimmed.length() > 3) {
            return null;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) < '0' || trimmed.charAt(i) > '9') {
                return null;
            }
        }
        int value = Integer.parseInt(trimmed);
        return (value >= MIN_NUMBER && value <= MAX_NUMBER) ? value : null;
    }

    private static String remainingPhrase(int remaining) {
        return remaining == 1 ? "1 guess" : remaining + " guesses";
    }
}
