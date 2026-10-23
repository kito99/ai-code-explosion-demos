package tech.virtua.duke.web;

/// A submitted guess. It arrives as text rather than as a number on purpose: deciding
/// what counts as a legal guess is the server's job, not the browser's.
public record GuessRequest(String guess) {
}
