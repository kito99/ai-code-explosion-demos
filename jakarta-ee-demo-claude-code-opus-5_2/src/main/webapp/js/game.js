/* The game screen. Every rule lives on the server: this file sends what the player typed,
   shows the message that comes back, and plays Duke's celebration on a win. */
(() => {
    'use strict';

    const GAMES_URL = 'api/games';
    const GUESSES_URL = 'api/games/current/guesses';
    const LEADERBOARD_URL = 'api/leaderboard';

    const DUKE_RESTING = 'images/duke-thinking.svg';
    const DUKE_CELEBRATING = 'images/duke-wave.svg';
    const DUKE_RESTING_ALT = 'Duke, thinking of a number';
    const DUKE_CELEBRATING_ALT = 'Duke, waving and jumping for joy';

    const STATUS_WON = 'WON';

    const duke = document.getElementById('duke');
    const message = document.getElementById('message');
    const guessesMade = document.getElementById('guesses-made');
    const guessesLeft = document.getElementById('guesses-left');
    const guessForm = document.getElementById('guess-form');
    const guessField = document.getElementById('guess');
    const guessButton = document.getElementById('guess-button');
    const scoreForm = document.getElementById('score-form');
    const nameField = document.getElementById('player-name');
    const playAgain = document.getElementById('play-again');

    /* Each request carries the number of the round it belongs to. A reply from an earlier
       round is dropped, so clicking Play again while a guess is still in flight cannot let
       the stale answer paint over the fresh game. */
    let round = 0;

    async function call(url, options) {
        const sentInRound = round;
        try {
            const response = await fetch(url, {
                credentials: 'same-origin',
                headers: {'Accept': 'application/json', 'Content-Type': 'application/json'},
                ...options
            });
            const body = await response.json().catch(() => ({}));
            return {ok: response.ok, body, stale: sentInRound !== round};
        } catch (unreachable) {
            return {ok: false, body: {}, stale: sentInRound !== round, offline: true};
        }
    }

    const OFFLINE_MESSAGE = 'Duke could not be reached just now. Check your connection and try again.';

    function show(text) {
        message.textContent = text;
    }

    function render(state) {
        guessesMade.textContent = state.guessesMade;
        guessesLeft.textContent = state.guessesRemaining;
        show(state.message);

        const over = state.status !== 'IN_PROGRESS';
        guessField.disabled = over;
        guessButton.disabled = over;
        scoreForm.hidden = !state.canPostScore;

        if (state.status === STATUS_WON) {
            celebrate();
        }
        if (!over) {
            guessField.focus();
        } else if (state.canPostScore) {
            nameField.focus();
        }
    }

    /* Duke springs up and settles back down a few times while waving, then returns to his
       resting pose. Nothing waits on the animation, so Play again stays usable throughout. */
    function celebrate() {
        if (duke.classList.contains('is-celebrating')) {
            return;
        }
        duke.src = DUKE_CELEBRATING;
        duke.alt = DUKE_CELEBRATING_ALT;
        duke.classList.add('is-celebrating');
        duke.addEventListener('animationend', rest, {once: true});
    }

    function rest() {
        duke.classList.remove('is-celebrating');
        duke.src = DUKE_RESTING;
        duke.alt = DUKE_RESTING_ALT;
    }

    /* Anything that is not a whole number is sent as null so the server answers with its own
       wording, which keeps one set of rules behind every message the player reads. */
    function typedGuess() {
        const typed = guessField.value.trim();
        return /^-?\d+$/.test(typed) ? Number.parseInt(typed, 10) : null;
    }

    async function startGame() {
        round++;
        rest();
        guessField.value = '';
        nameField.value = '';
        scoreForm.hidden = true;
        guessField.disabled = false;
        guessButton.disabled = false;
        const {ok, body, stale, offline} = await call(GAMES_URL, {method: 'POST'});
        if (stale) {
            return;
        }
        if (!ok) {
            show(offline ? OFFLINE_MESSAGE
                : body.message || 'The game could not be started. Reload the page to try again.');
            return;
        }
        render(body);
    }

    async function submitGuess(event) {
        event.preventDefault();
        const {ok, body, stale, offline} = await call(GUESSES_URL, {
            method: 'POST',
            body: JSON.stringify({guess: typedGuess()})
        });
        if (stale) {
            return;
        }
        if (!ok) {
            // A rejected guess never counted, so the counters on screen are still right.
            show(offline ? OFFLINE_MESSAGE
                : body.message || 'That guess could not be read. Try a number from 1 to 10.');
            guessField.select();
            return;
        }
        guessField.value = '';
        render(body);
    }

    async function submitScore(event) {
        event.preventDefault();
        const {ok, body, stale, offline} = await call(LEADERBOARD_URL, {
            method: 'POST',
            body: JSON.stringify({name: nameField.value})
        });
        if (stale) {
            return;
        }
        if (!ok) {
            show(offline ? OFFLINE_MESSAGE : body.message || 'That score could not be posted. Try again.');
            nameField.focus();
            return;
        }
        scoreForm.hidden = true;
        show(body.message);
        playAgain.focus();
    }

    guessForm.addEventListener('submit', submitGuess);
    scoreForm.addEventListener('submit', submitScore);
    playAgain.addEventListener('click', startGame);

    startGame();
})();
