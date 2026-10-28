const API = "api";

const elements = {
    duke: document.getElementById("duke"),
    dukeImage: document.getElementById("duke-image"),
    message: document.getElementById("message"),
    guessesMade: document.getElementById("guesses-made"),
    guessesRemaining: document.getElementById("guesses-remaining"),
    guessForm: document.getElementById("guess-form"),
    guessInput: document.getElementById("guess"),
    guessButton: document.getElementById("guess-button"),
    scoreForm: document.getElementById("score-form"),
    nameInput: document.getElementById("player-name"),
    postButton: document.getElementById("post-button"),
    newGameButton: document.getElementById("new-game-button")
};

const DUKE_THINKING = "images/duke-thinking.svg";
const DUKE_WAVING = "images/duke-wave.svg";

// One request at a time, so a slow reply can never land on top of a newer game.
let requestInFlight = false;

async function callApi(path, options) {
    const response = await fetch(`${API}${path}`, {
        headers: {"Content-Type": "application/json", "Accept": "application/json"},
        ...options
    });
    const body = await response.json();
    return {ok: response.ok, body};
}

function showMessage(text, isError) {
    elements.message.textContent = text;
    elements.message.classList.toggle("error", Boolean(isError));
}

function celebrate() {
    elements.dukeImage.src = DUKE_WAVING;
    elements.dukeImage.alt = "Duke celebrating your win";
    elements.duke.classList.remove("celebrating");
    // Reading offsetWidth restarts the animation when the player wins twice in a row.
    void elements.duke.offsetWidth;
    elements.duke.classList.add("celebrating");
}

function restDuke() {
    elements.duke.classList.remove("celebrating");
    elements.dukeImage.src = DUKE_THINKING;
    elements.dukeImage.alt = "Duke, thinking of a number";
}

function render(state) {
    elements.guessesMade.textContent = state.guessesMade;
    elements.guessesRemaining.textContent = state.guessesRemaining;
    showMessage(state.message, false);

    const active = state.status === "ACTIVE";
    elements.guessInput.disabled = !active;
    elements.guessButton.disabled = !active;
    elements.guessInput.value = "";

    elements.scoreForm.hidden = state.status !== "WON";
    if (state.status === "WON") {
        elements.nameInput.value = "";
        elements.nameInput.focus();
    } else if (active) {
        elements.guessInput.focus();
    }
}

async function startNewGame() {
    const {body} = await callApi("/game/new", {method: "POST"});
    restDuke();
    render(body);
}

async function withOneRequestAtATime(action) {
    if (requestInFlight) {
        return;
    }
    requestInFlight = true;
    try {
        await action();
    } finally {
        requestInFlight = false;
    }
}

elements.guessForm.addEventListener("submit", (event) => {
    event.preventDefault();
    withOneRequestAtATime(submitGuess);
});

async function submitGuess() {
    const {ok, body} = await callApi("/game/guesses", {
        method: "POST",
        body: JSON.stringify({guess: elements.guessInput.value})
    });

    if (!ok) {
        showMessage(body.message, true);
        elements.guessInput.select();
        return;
    }

    render(body);
    if (body.status === "WON") {
        celebrate();
    }
}

elements.scoreForm.addEventListener("submit", (event) => {
    event.preventDefault();
    withOneRequestAtATime(submitScore);
});

async function submitScore() {
    const {ok, body} = await callApi("/leaderboard", {
        method: "POST",
        body: JSON.stringify({playerName: elements.nameInput.value})
    });

    if (!ok) {
        showMessage(body.message, true);
        elements.nameInput.select();
        return;
    }

    elements.scoreForm.hidden = true;
    showMessage(`${body.message} Play again or visit the leaderboard.`, false);
}

elements.newGameButton.addEventListener("click", () => withOneRequestAtATime(startNewGame));

// Loading the page always starts a fresh game (FR-1).
withOneRequestAtATime(startNewGame);
