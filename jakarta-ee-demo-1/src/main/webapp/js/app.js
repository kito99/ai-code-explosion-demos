/*
 * Front end for Guess the Number with Duke.
 *
 * The server owns every rule, including what counts as a valid guess, so this file
 * sends the raw input along and renders whatever comes back. The HTTP status says
 * whether the guess counted; the body is always the resulting game state.
 */
(function () {
    'use strict';

    var GAME_URL = 'api/game';
    var GUESS_URL = 'api/game/guesses';
    var CELEBRATING = 'duke--celebrating';

    var elements = {
        duke: document.getElementById('duke'),
        message: document.getElementById('message'),
        pips: document.getElementById('pips'),
        count: document.getElementById('tracker-count'),
        form: document.getElementById('guess-form'),
        input: document.getElementById('guess'),
        submit: document.getElementById('submit-guess'),
        playAgain: document.getElementById('play-again')
    };

    var requestInFlight = false;

    function plural(count, singular, pluralForm) {
        return count + ' ' + (count === 1 ? singular : pluralForm);
    }

    /** Picks the message styling from the response status and the game state. */
    function toneFor(httpStatus, game) {
        if (httpStatus === 400) {
            return 'warn';
        }
        if (game.status === 'WON') {
            return 'win';
        }
        if (game.status === 'LOST') {
            return 'lose';
        }
        return game.lastHint ? 'hint' : 'neutral';
    }

    /** Puts one line in front of the player, styled to match what just happened. */
    function showMessage(text, tone) {
        elements.message.textContent = text;
        elements.message.className = 'message' + (tone === 'neutral' ? '' : ' message--' + tone);
    }

    /** True when a response body carries game state rather than only an error message. */
    function isGameState(body) {
        return Boolean(body) && typeof body.status === 'string';
    }

    function render(game, httpStatus) {
        showMessage(game.message, toneFor(httpStatus, game));

        renderPips(game);
        elements.count.textContent =
            plural(game.guessesMade, 'guess', 'guesses') + ' made · ' +
            game.guessesRemaining + ' left';

        var over = game.status !== 'IN_PROGRESS';
        elements.input.disabled = over;
        elements.submit.disabled = over;
    }

    /** Rebuilds the row of pips so it always has one per allowed guess. */
    function renderPips(game) {
        while (elements.pips.children.length > game.maxGuesses) {
            elements.pips.removeChild(elements.pips.lastChild);
        }
        while (elements.pips.children.length < game.maxGuesses) {
            elements.pips.appendChild(document.createElement('li'));
        }
        for (var i = 0; i < elements.pips.children.length; i++) {
            elements.pips.children[i].className = 'pip' + (i < game.guessesMade ? ' pip--spent' : '');
        }
    }

    /**
     * Plays Duke's win animation. Dropping the class and reading layout before putting it
     * back forces the removal to land, which is what lets the animation replay on a later
     * win. It reads a bounding rect rather than offsetWidth, which SVG elements lack.
     */
    function celebrate() {
        elements.duke.classList.remove(CELEBRATING);
        elements.duke.getBoundingClientRect();
        elements.duke.classList.add(CELEBRATING);
    }

    function stopCelebrating() {
        elements.duke.classList.remove(CELEBRATING);
    }

    function showConnectionProblem() {
        showMessage("Duke isn't answering right now. Check that the server is running, then try again.",
            'warn');
    }

    /** Sends one request and renders the result. Overlapping requests are ignored. */
    function call(url, options) {
        if (requestInFlight) {
            return Promise.resolve();
        }
        requestInFlight = true;

        return fetch(url, options)
            .then(function (response) {
                return response.json().then(function (body) {
                    return { status: response.status, ok: response.ok, body: body };
                });
            })
            .then(function (result) {
                // A reply that is not game state can only be an error the page did not
                // provoke, so show its message and leave the board exactly as it was.
                if (isGameState(result.body)) {
                    render(result.body, result.status);
                } else {
                    showMessage(messageFrom(result.body), 'warn');
                }
                return result;
            })
            .catch(showConnectionProblem)
            .finally(function () {
                requestInFlight = false;
            });
    }

    function messageFrom(body) {
        return (body && body.message)
            || 'Something went wrong on Duke\u2019s end. Try Play again.';
    }

    function jsonRequest(method, payload) {
        var options = {
            method: method,
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        };
        if (payload !== undefined) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(payload);
        }
        return options;
    }

    /**
     * Sends keyboard focus wherever the player's next move is. A finished game disables
     * the field, and a disabled field cannot take focus, so it goes to Play again.
     */
    function focusNextMove(game) {
        if (game.status === 'IN_PROGRESS') {
            elements.input.focus();
        } else {
            elements.playAgain.focus();
        }
    }

    function startNewGame() {
        stopCelebrating();
        return call(GAME_URL, jsonRequest('POST')).then(function (result) {
            elements.input.value = '';
            if (result && isGameState(result.body)) {
                focusNextMove(result.body);
            }
        });
    }

    function submitGuess(rawInput) {
        return call(GUESS_URL, jsonRequest('POST', { guess: rawInput })).then(function (result) {
            if (!result || !isGameState(result.body)) {
                return;
            }
            if (result.ok && result.body.status === 'WON') {
                celebrate();
            }
            elements.input.value = '';
            focusNextMove(result.body);
        });
    }

    elements.form.addEventListener('submit', function (event) {
        event.preventDefault();
        submitGuess(elements.input.value);
    });

    elements.playAgain.addEventListener('click', function () {
        startNewGame();
    });

    // Loading the page deals a fresh game, so a refresh always starts the player over.
    startNewGame();
}());
