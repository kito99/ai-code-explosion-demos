(() => {
    'use strict';

    const GAMES_URL = '/api/games';

    const elements = {
        stage: document.getElementById('duke-stage'),
        caption: document.getElementById('duke-caption'),
        guessForm: document.getElementById('guess-form'),
        guessInput: document.getElementById('guess-input'),
        guessButton: document.getElementById('guess-button'),
        counts: document.getElementById('counts'),
        message: document.getElementById('message'),
        nameForm: document.getElementById('name-form'),
        nameInput: document.getElementById('name-input'),
        postButton: document.getElementById('post-button'),
        newGameButton: document.getElementById('new-game-button')
    };

    let gameId = null;

    const send = async (url, body) => {
        const response = await fetch(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body ?? {})
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload.detail || 'Something went wrong. Please try again.');
        }
        return payload;
    };

    const setMessage = (text, tone) => {
        elements.message.textContent = text;
        elements.message.className = tone ? `message ${tone}` : 'message';
    };

    const showCounts = (game) => {
        elements.counts.textContent =
            `Guesses made: ${game.guessesMade} · Guesses left: ${game.guessesRemaining}`;
    };

    const celebrate = () => {
        const stage = elements.stage;
        stage.classList.remove('celebrating');
        void stage.offsetWidth; // Restart the animation when two wins happen in a row.
        stage.classList.add('celebrating');
    };

    elements.stage.addEventListener('animationend', (event) => {
        if (event.target === elements.stage) {
            elements.stage.classList.remove('celebrating');
        }
    });

    const setPlaying = (playing) => {
        elements.guessInput.disabled = !playing;
        elements.guessButton.disabled = !playing;
    };

    const startGame = async () => {
        try {
            const game = await send(GAMES_URL);
            gameId = game.gameId;
            elements.caption.textContent = 'I’m thinking of a number between 1 and 10.';
            elements.nameForm.hidden = true;
            elements.nameInput.value = '';
            elements.guessInput.value = '';
            elements.stage.classList.remove('celebrating');
            setPlaying(true);
            showCounts(game);
            setMessage('Good luck! Take your best guess.');
            elements.guessInput.focus();
        } catch (error) {
            setMessage(error.message, 'invalid');
        }
    };

    const reportGuess = (game) => {
        showCounts(game);
        if (game.status === 'WON') {
            const guesses = game.guessesMade === 1 ? '1 guess' : `${game.guessesMade} guesses`;
            elements.caption.textContent = 'You got it!';
            setMessage(`Correct! You found my number in ${guesses}. Post your score, or start a new game.`, 'won');
            setPlaying(false);
            elements.nameForm.hidden = false;
            celebrate();
            elements.nameInput.focus();
            return;
        }
        if (game.status === 'LOST') {
            elements.caption.textContent = 'Out of guesses!';
            setMessage(`Out of guesses. My number was ${game.secretNumber}. Start a new game to try again.`, 'lost');
            setPlaying(false);
            elements.newGameButton.focus();
            return;
        }
        const hint = game.outcome === 'TOO_HIGH' ? 'Too high.' : 'Too low.';
        setMessage(`${hint} Keep going, you have ${game.guessesRemaining} left.`);
        elements.guessInput.value = '';
        elements.guessInput.focus();
    };

    elements.guessForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!gameId) {
            await startGame();
            return;
        }
        const raw = elements.guessInput.value.trim();
        try {
            const game = await send(`${GAMES_URL}/${gameId}/guesses`, {guess: raw === '' ? null : Number(raw)});
            reportGuess(game);
        } catch (error) {
            setMessage(error.message, 'invalid');
            elements.guessInput.focus();
        }
    });

    elements.nameForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        elements.postButton.disabled = true;
        try {
            const score = await send(`${GAMES_URL}/${gameId}/score`, {name: elements.nameInput.value});
            elements.nameForm.hidden = true;
            setMessage(`Posted! Check the leaderboard to see where ${score.guesses} guesses landed you.`, 'won');
            elements.newGameButton.focus();
        } catch (error) {
            setMessage(error.message, 'invalid');
            elements.nameInput.focus();
        } finally {
            elements.postButton.disabled = false;
        }
    });

    elements.newGameButton.addEventListener('click', startGame);

    startGame();
})();
