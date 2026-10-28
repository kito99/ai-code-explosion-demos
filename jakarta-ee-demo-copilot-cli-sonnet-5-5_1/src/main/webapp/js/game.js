(() => {
    'use strict';

    const GAME_URL = '/api/game';
    const GUESSES_URL = '/api/game/guesses';
    const LEADERBOARD_URL = '/api/leaderboard';
    const LOWEST = 1;
    const HIGHEST = 10;
    const MAX_NAME_LENGTH = 30;

    const duke = document.getElementById('duke');
    const message = document.getElementById('message');
    const guessForm = document.getElementById('guess-form');
    const guessInput = document.getElementById('guess');
    const guessButton = document.getElementById('guess-button');
    const counters = document.getElementById('counters');
    const scoreForm = document.getElementById('score-form');
    const nameInput = document.getElementById('player-name');
    const postButton = document.getElementById('post-score');
    const scoreMessage = document.getElementById('score-message');
    const newGameButton = document.getElementById('new-game');

    const guessCount = (count) => `${count} ${count === 1 ? 'guess' : 'guesses'}`;

    const showMessage = (text, modifier) => {
        message.textContent = text;
        message.className = modifier ? `message message--${modifier}` : 'message';
    };

    const DUKE_LABEL_THINKING = 'Duke, the Java mascot, thinking of a number';
    const DUKE_LABEL_CELEBRATING = 'Duke celebrating with a jump and a wave';

    const showDuke = (celebrating) => {
        duke.classList.remove('duke--celebrating');
        duke.setAttribute('aria-label', celebrating ? DUKE_LABEL_CELEBRATING : DUKE_LABEL_THINKING);
        if (celebrating) {
            // Re-adding the class after a reflow restarts the animation on a repeat win.
            void duke.offsetWidth;
            duke.classList.add('duke--celebrating');
        }
    };

    const render = (state) => {
        const finished = state.status !== 'ACTIVE';
        showMessage(state.message, state.status === 'WON' ? 'win' : state.status === 'LOST' ? 'lose' : null);
        counters.textContent = `${guessCount(state.guessesMade)} made \u00b7 ${state.guessesRemaining} remaining`;
        guessInput.disabled = finished;
        guessButton.disabled = finished;
        showDuke(state.status === 'WON');

        const canPostScore = state.status === 'WON' && !state.scorePosted;
        scoreForm.hidden = !canPostScore;
        if (canPostScore) {
            nameInput.disabled = false;
            postButton.disabled = false;
            scoreMessage.textContent = '';
            nameInput.focus();
        } else if (!finished) {
            guessInput.focus();
        }
    };

    const readErrorMessage = async (response) => {
        try {
            const body = await response.json();
            if (body && body.message) {
                return body.message;
            }
        } catch (ignored) {
            // Fall through to the generic message below.
        }
        return 'Something went wrong talking to the server. Please try again.';
    };

    const send = async (url, method, body) => {
        const options = {method, headers: {Accept: 'application/json'}};
        if (body !== undefined) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(body);
        }
        return fetch(url, options);
    };

    const startNewGame = async () => {
        guessForm.reset();
        scoreForm.reset();
        scoreForm.hidden = true;
        scoreMessage.textContent = '';
        try {
            const response = await send(GAME_URL, 'POST');
            if (!response.ok) {
                showMessage(await readErrorMessage(response), 'error');
                return;
            }
            render(await response.json());
        } catch (failure) {
            showMessage('Duke could not be reached. Check that the app is running and try again.', 'error');
        }
    };

    const submitGuess = async (guess) => {
        try {
            const response = await send(GUESSES_URL, 'POST', {guess});
            const state = await response.json();
            if (!response.ok) {
                showMessage(state.message || 'That guess could not be read. Please try again.', 'error');
                return;
            }
            render(state);
        } catch (failure) {
            showMessage('Duke could not be reached. Check that the app is running and try again.', 'error');
        }
    };

    const postScore = async (playerName) => {
        postButton.disabled = true;
        try {
            const response = await send(LEADERBOARD_URL, 'POST', {playerName});
            const body = await response.json();
            if (!response.ok) {
                scoreMessage.textContent = body.message || 'That score could not be posted. Please try again.';
                postButton.disabled = false;
                return;
            }
            scoreMessage.textContent = body.message;
            nameInput.disabled = true;
        } catch (failure) {
            scoreMessage.textContent = 'Duke could not be reached. Please try again.';
            postButton.disabled = false;
        }
    };

    guessForm.addEventListener('submit', (event) => {
        event.preventDefault();
        // Checked here as well as on the server so a typo never costs the player a guess.
        const entered = guessInput.value.trim();
        const guess = Number(entered);
        if (entered === '' || !Number.isInteger(guess) || guess < LOWEST || guess > HIGHEST) {
            showMessage(`Please pick a whole number between ${LOWEST} and ${HIGHEST}.`, 'error');
            guessInput.focus();
            guessInput.select();
            return;
        }
        guessInput.value = '';
        submitGuess(guess);
    });

    scoreForm.addEventListener('submit', (event) => {
        event.preventDefault();
        const playerName = nameInput.value.trim();
        if (playerName === '' || playerName.length > MAX_NAME_LENGTH) {
            scoreMessage.textContent =
                `Please enter a name of 1 to ${MAX_NAME_LENGTH} characters so Duke knows who to credit.`;
            nameInput.focus();
            return;
        }
        postScore(playerName);
    });

    newGameButton.addEventListener('click', startNewGame);

    startNewGame();
})();
