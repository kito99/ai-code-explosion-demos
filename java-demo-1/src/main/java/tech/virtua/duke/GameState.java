package tech.virtua.duke;

/**
 * An immutable view of a game, ready to hand to the browser. {@code secret} is only
 * filled in once the game is lost, so a curious player cannot read it off the wire
 * mid-game.
 */
public record GameState(
        Game.Status status,
        int guessesMade,
        int guessesRemaining,
        int maxGuesses,
        int minNumber,
        int maxNumber,
        Integer lastGuess,
        Game.Hint hint,
        String message,
        boolean guessAccepted,
        Integer secret) {

    public String toJson() {
        return Json.object(
                Json.member("status", status.name()),
                Json.member("guessesMade", guessesMade),
                Json.member("guessesRemaining", guessesRemaining),
                Json.member("maxGuesses", maxGuesses),
                Json.member("minNumber", minNumber),
                Json.member("maxNumber", maxNumber),
                Json.member("lastGuess", lastGuess),
                Json.member("hint", hint.name()),
                Json.member("message", message),
                Json.member("guessAccepted", guessAccepted),
                Json.member("secret", secret));
    }
}
