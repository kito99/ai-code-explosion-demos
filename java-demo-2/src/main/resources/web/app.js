/*
 * Draws whatever the server says the game looks like. The secret number, the
 * rules, and the wording all live on the server; this file only renders them.
 */
(() => {
    'use strict';

    const Endpoint = Object.freeze({
        NEW_GAME: '/api/game/new',
        GUESS: '/api/game/guess',
    });

    const Status = Object.freeze({
        ACTIVE: 'ACTIVE',
        WON: 'WON',
        LOST: 'LOST',
    });

    const DukePose = Object.freeze({
        THINKING: {
            source: 'duke-thinking.svg',
            description: 'Duke, the Java mascot, thinking of a number',
        },
        CELEBRATING: {
            source: 'duke-wave.svg',
            description: 'Duke, the Java mascot, jumping and waving to celebrate',
        },
    });

    const MessageTone = Object.freeze({
        GOOD: 'message--good',
        PROBLEM: 'message--problem',
    });

    const CELEBRATING_CLASS = 'stage__duke--celebrating';
    const OFFLINE_MESSAGE = 'Duke is not answering right now. Check that the server is running, then try again.';

    const element = (name) => document.querySelector(`[data-element="${name}"]`);

    const duke = element('duke');
    const message = element('message');
    const guessForm = element('guess-form');
    const guessInput = element('guess-input');
    const guessButton = element('guess-button');
    const newGameButton = element('new-game');

    let previousStatus = null;

    /* Responses can arrive out of order, so each request takes a ticket and only
       the newest one is allowed to draw. Otherwise a slow guess could land after
       a restart and put the finished game back on screen. */
    let latestRequest = 0;

    /** Sends a request and returns the game state it answers with. */
    const requestState = async (endpoint, options = {}) => {
        let response;
        try {
            response = await fetch(endpoint, options);
        } catch {
            throw new Error(OFFLINE_MESSAGE);
        }
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload.message ?? OFFLINE_MESSAGE);
        }
        return payload;
    };

    const showProblem = (text) => {
        message.textContent = text;
        message.classList.remove(MessageTone.GOOD);
        message.classList.add(MessageTone.PROBLEM);
    };

    const toneFor = (status) => {
        if (status === Status.WON) {
            return MessageTone.GOOD;
        }
        return status === Status.LOST ? MessageTone.PROBLEM : null;
    };

    const showPose = (pose) => {
        if (!duke.src.endsWith(pose.source)) {
            duke.src = pose.source;
            duke.alt = pose.description;
        }
    };

    /** Restarts the animation even if Duke is still mid-celebration. */
    const celebrate = () => {
        duke.classList.remove(CELEBRATING_CLASS);
        void duke.offsetWidth;
        duke.classList.add(CELEBRATING_CLASS);
    };

    const fillFields = (state) => {
        document.querySelectorAll('[data-field]').forEach((field) => {
            const value = state[field.dataset.field];
            if (value !== undefined && value !== null) {
                field.textContent = value;
            }
        });
    };

    const render = (state) => {
        fillFields(state);

        message.textContent = state.message;
        message.classList.remove(MessageTone.GOOD, MessageTone.PROBLEM);
        const tone = toneFor(state.status);
        if (tone) {
            message.classList.add(tone);
        }

        const playable = state.status === Status.ACTIVE;
        guessInput.min = state.lowestNumber;
        guessInput.max = state.highestNumber;
        guessInput.disabled = !playable;
        guessButton.disabled = !playable;

        showPose(state.status === Status.WON ? DukePose.CELEBRATING : DukePose.THINKING);
        if (state.status === Status.WON && previousStatus !== Status.WON) {
            celebrate();
        }

        if (playable) {
            guessInput.focus();
        } else if (previousStatus === Status.ACTIVE) {
            newGameButton.focus();
        }
        previousStatus = state.status;
    };

    const run = async (work) => {
        const ticket = ++latestRequest;
        try {
            const state = await work();
            if (ticket === latestRequest) {
                render(state);
            }
        } catch (failure) {
            if (ticket === latestRequest) {
                showProblem(failure.message || OFFLINE_MESSAGE);
            }
        }
    };

    const startNewGame = () => {
        duke.classList.remove(CELEBRATING_CLASS);
        guessInput.value = '';
        return run(() => requestState(Endpoint.NEW_GAME, { method: 'POST' }));
    };

    guessForm.addEventListener('submit', (submission) => {
        submission.preventDefault();
        const guess = guessInput.value;
        guessInput.value = '';
        run(() => requestState(Endpoint.GUESS, { method: 'POST', body: guess }));
    });

    newGameButton.addEventListener('click', startNewGame);

    duke.addEventListener('animationend', () => duke.classList.remove(CELEBRATING_CLASS));

    // A reload is a fresh game, not a resumed one.
    startNewGame();
})();
