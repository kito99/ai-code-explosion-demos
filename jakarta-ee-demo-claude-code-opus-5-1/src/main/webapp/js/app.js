'use strict';

const GAME_URL = 'api/game';

const POSES = {
    thinking: {src: 'images/duke-thinking.svg', alt: 'Duke, thinking of a number'},
    waving: {src: 'images/duke-wave.svg', alt: 'Duke waving an arm and jumping for joy'}
};

const OFFLINE_MESSAGE = 'Duke is not answering right now. Give it another try in a moment.';

const elements = {
    game: document.getElementById('game'),
    duke: document.getElementById('duke'),
    message: document.getElementById('message'),
    form: document.getElementById('guess-form'),
    guess: document.getElementById('guess'),
    submit: document.getElementById('submit-guess'),
    guessesMade: document.getElementById('guesses-made'),
    guessesRemaining: document.getElementById('guesses-remaining'),
    newGame: document.getElementById('new-game')
};

// Answers can arrive out of order, so every request takes a ticket and only the newest
// one is allowed to draw. Without this a slow guess could land after a new game and put
// the finished one back on screen.
let newestTicket = 0;

let shownStatus = null;

// A guess already on its way is left to finish. Letting a second one go would spend two
// guesses while the page, which draws only the newest answer, could only show one.
let guessInFlight = false;

async function ask(method, path, body) {
    const ticket = ++newestTicket;
    const options = {method, headers: {'Accept': 'application/json'}};
    if (body !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(path, options);
        const view = await response.json();
        if (ticket === newestTicket) {
            draw(view);
        }
    } catch (failure) {
        if (ticket === newestTicket) {
            elements.message.textContent = OFFLINE_MESSAGE;
        }
    }
}

function draw(view) {
    const over = view.status !== 'ACTIVE';
    const won = view.status === 'WON';

    // Noted before anything is disabled: disabling the focused control drops focus to
    // the body, which would lose the only clue about where the player was.
    const wasFocused = document.activeElement;

    elements.game.dataset.status = view.status;
    elements.message.textContent = view.message;
    elements.guessesMade.textContent = view.guessesMade;
    elements.guessesRemaining.textContent = view.guessesRemaining;

    showDuke(won ? POSES.waving : POSES.thinking, won ? 'waving' : 'thinking');
    if (won && shownStatus !== 'WON') {
        celebrate();
    }

    elements.guess.disabled = over;
    elements.submit.disabled = over;
    if (view.outcome === 'COUNTED') {
        elements.guess.value = '';
    }
    moveFocus(over, wasFocused);

    shownStatus = view.status;
}

function showDuke(pose, name) {
    if (elements.duke.dataset.pose !== name) {
        elements.duke.src = pose.src;
        elements.duke.alt = pose.alt;
        elements.duke.dataset.pose = name;
    }
}

function celebrate() {
    elements.duke.classList.remove('celebrating');
    // Reading the layout restarts the animation when a win follows a win.
    void elements.duke.offsetWidth;
    elements.duke.classList.add('celebrating');
}

function moveFocus(gameIsOver, wasFocused) {
    // Only chase focus that was already in the game's controls, so a player reading the
    // page with the keyboard is never yanked somewhere they did not ask to go.
    const wasPlaying = wasFocused === elements.guess || wasFocused === elements.submit;
    if (gameIsOver && wasPlaying) {
        elements.newGame.focus();
    } else if (!gameIsOver && wasFocused === elements.newGame) {
        elements.guess.focus();
    }
}

elements.form.addEventListener('submit', async event => {
    event.preventDefault();
    if (guessInFlight) {
        return;
    }
    guessInFlight = true;
    try {
        await ask('POST', `${GAME_URL}/guess`, {guess: elements.guess.value});
    } finally {
        guessInFlight = false;
    }
});

elements.newGame.addEventListener('click', () => {
    elements.duke.classList.remove('celebrating');
    elements.guess.value = '';
    shownStatus = null;
    // A guess still in flight belongs to the game being abandoned, so it no longer has
    // any claim on the next one.
    guessInFlight = false;
    ask('POST', `${GAME_URL}/new`);
});

// A fresh page is a fresh game, so reloading always starts the player over.
ask('POST', `${GAME_URL}/new`);
