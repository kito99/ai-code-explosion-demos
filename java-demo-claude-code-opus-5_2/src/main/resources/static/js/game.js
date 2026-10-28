(() => {
    'use strict';

    const LOWEST = 1;
    const HIGHEST = 10;
    const NAME_MAX_LENGTH = 30;

    const RANGE_MESSAGE = `Please enter a whole number from ${LOWEST} to ${HIGHEST}.`;
    const NAME_MESSAGE = `Please enter a name of 1 to ${NAME_MAX_LENGTH} characters.`;
    const OFFLINE_MESSAGE = 'Duke cannot be reached right now. Try again in a moment.';

    const stage = document.getElementById('duke-stage');
    const guessForm = document.getElementById('guess-form');
    const guessInput = document.getElementById('guess');
    const guessButton = document.getElementById('guess-button');
    const message = document.getElementById('message');
    const tally = document.getElementById('tally');
    const scoreForm = document.getElementById('score-form');
    const nameInput = document.getElementById('player-name');
    const scoreMessage = document.getElementById('score-message');
    const playAgainButton = document.getElementById('play-again');

    let gameId = null;
    // Bumped by every new round. A reply belonging to an earlier round is dropped, so
    // starting a fresh game mid-request never lands the old game's result on the new one.
    let round = 0;

    const say = (element, text, tone) => {
        element.textContent = text;
        if (tone) {
            element.dataset.tone = tone;
        } else {
            delete element.dataset.tone;
        }
    };

    const callApi = async (path, options) => {
        let response;
        try {
            response = await fetch(path, {
                headers: {'Content-Type': 'application/json'},
                ...options
            });
        } catch {
            throw new Error(OFFLINE_MESSAGE);
        }
        const body = await response.json().catch(() => null);
        if (!response.ok) {
            throw new Error(body?.detail ?? OFFLINE_MESSAGE);
        }
        return body;
    };

    const countOfGuesses = (count) => (count === 1 ? 'one guess' : `${count} guesses`);

    // Counts what a reader sees, so an emoji name is measured the way the server measures it.
    const countOfCharacters = (text) => [...text].length;

    const showTally = (game) => {
        tally.textContent = `${countOfGuesses(game.guessesMade)} made · ${game.guessesRemaining} left`;
    };

    const setPlayable = (playable) => {
        guessInput.disabled = !playable;
        guessButton.disabled = !playable;
    };

    const startGame = async () => {
        const thisRound = ++round;
        setPlayable(false);
        say(message, '');
        say(scoreMessage, '');
        scoreForm.hidden = true;
        scoreForm.reset();
        stage.dataset.state = 'playing';
        try {
            const game = await callApi('/api/games', {method: 'POST'});
            if (thisRound !== round) {
                return;
            }
            gameId = game.gameId;
            showTally(game);
            guessForm.reset();
            setPlayable(true);
            guessInput.focus();
        } catch (failure) {
            if (thisRound === round) {
                say(message, failure.message, 'problem');
            }
        }
    };

    const describeVerdict = (verdict) =>
        verdict === 'TOO_LOW' ? 'Too low. Aim higher.' : 'Too high. Aim lower.';

    const handleWin = (game) => {
        stage.dataset.state = 'won';
        setPlayable(false);
        say(message, `That is it! You read Duke's mind in ${countOfGuesses(game.guessesMade)}.`, 'win');
        scoreForm.hidden = false;
        nameInput.focus();
    };

    const handleLoss = (verdict, game) => {
        stage.dataset.state = 'lost';
        setPlayable(false);
        say(message,
            `${describeVerdict(verdict)} That was your last guess, and Duke was thinking of `
                + `${game.secretNumber}. Play again?`,
            'lose');
    };

    const submitGuess = async (event) => {
        event.preventDefault();
        const typed = guessInput.value.trim();
        const guess = Number(typed);
        // Checked here as well as on the server so a bad entry never spends a guess.
        if (typed === '' || !Number.isInteger(guess) || guess < LOWEST || guess > HIGHEST) {
            say(message, RANGE_MESSAGE, 'problem');
            guessInput.focus();
            guessInput.select();
            return;
        }
        const thisRound = round;
        setPlayable(false);
        try {
            const result = await callApi(`/api/games/${gameId}/guesses`, {
                method: 'POST',
                body: JSON.stringify({guess})
            });
            if (thisRound !== round) {
                return;
            }
            showTally(result.game);
            guessForm.reset();
            if (result.verdict === 'CORRECT') {
                handleWin(result.game);
            } else if (result.game.status === 'LOST') {
                handleLoss(result.verdict, result.game);
            } else {
                say(message, describeVerdict(result.verdict), 'nudge');
                setPlayable(true);
                guessInput.focus();
            }
        } catch (failure) {
            if (thisRound !== round) {
                return;
            }
            say(message, failure.message, 'problem');
            setPlayable(true);
            guessInput.focus();
        }
    };

    const postScore = async (event) => {
        event.preventDefault();
        const name = nameInput.value.trim();
        if (name === '' || countOfCharacters(name) > NAME_MAX_LENGTH) {
            say(scoreMessage, NAME_MESSAGE, 'problem');
            nameInput.focus();
            return;
        }
        const thisRound = round;
        say(scoreMessage, 'Posting your score…');
        try {
            const posted = await callApi(`/api/games/${gameId}/score`, {
                method: 'POST',
                body: JSON.stringify({name})
            });
            if (thisRound !== round) {
                return;
            }
            scoreForm.hidden = true;
            say(message,
                `Posted! Your win in ${countOfGuesses(posted.guesses)} is on the leaderboard.`,
                'win');
        } catch (failure) {
            if (thisRound !== round) {
                return;
            }
            say(scoreMessage, failure.message, 'problem');
            nameInput.focus();
        }
    };

    guessForm.addEventListener('submit', submitGuess);
    scoreForm.addEventListener('submit', postScore);
    playAgainButton.addEventListener('click', startGame);

    startGame();
})();
