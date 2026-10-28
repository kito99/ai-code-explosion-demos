/**
 * game.js — front-end logic for the Guess the Number game.
 * Communicates with the Jakarta REST API at /api/game/*.
 */

const API_BASE = 'api';

const $status   = () => document.getElementById('status-message');
const $counter  = () => document.getElementById('guess-counter');
const $guessForm = () => document.getElementById('guess-form');
const $guessInput = () => document.getElementById('guess-input');
const $scoreForm = () => document.getElementById('score-form');
const $nameInput = () => document.getElementById('name-input');
const $nameError = () => document.getElementById('name-error');
const $dukeImg   = () => document.getElementById('duke-img');

// ---- Boot ----

window.addEventListener('DOMContentLoaded', () => {
    startNewGame();
    // Allow submitting guess with Enter key (NFR-3)
    document.getElementById('guess-input').addEventListener('keydown', e => {
        if (e.key === 'Enter') submitGuess();
    });
    document.getElementById('name-input').addEventListener('keydown', e => {
        if (e.key === 'Enter') postScore();
    });
});

// ---- Game actions ----

async function startNewGame() {
    hideScoreForm();
    clearNameError();
    $dukeImg().classList.remove('celebrating');
    $dukeImg().src = 'images/duke-thinking.svg';
    $dukeImg().alt = 'Duke thinking of a number';

    const data = await callApi('POST', `${API_BASE}/game/start`);
    if (data) renderActive(data);
}

async function submitGuess() {
    const raw = $guessInput().value.trim();
    const num = parseInt(raw, 10);
    if (!raw || isNaN(num) || num < 1 || num > 10) {
        setStatus('Please enter a whole number between 1 and 10.', '');
        $guessInput().focus();
        return;
    }

    const data = await callApi('POST', `${API_BASE}/game/guess`, { guess: num });
    if (!data) return;

    $guessInput().value = '';
    renderGuessResult(data);
}

async function postScore() {
    clearNameError();
    const name = $nameInput().value;
    const data = await callApi('POST', `${API_BASE}/leaderboard`, { name });
    if (!data) return;
    if (data.message) {
        $nameError().textContent = data.message;
        return;
    }
    hideScoreForm();
    setStatus(`🎉 Score posted! Great job, ${data.playerName}!`, 'win');
}

// ---- Render helpers ----

function renderActive(state) {
    setStatus('I\'m thinking of a number between 1 and 10…', '');
    setCounter(state.guessesMade, state.guessesRemaining);
    showGuessForm();
}

function renderGuessResult(result) {
    const { feedback, guessesMade, guessesRemaining, secretNumber } = result;

    if (feedback === 'CORRECT') {
        celebrate();
        setStatus(`🎉 Correct! You got it in ${guessesMade} guess${guessesMade === 1 ? '' : 'es'}!`, 'win');
        setCounter(guessesMade, 0);
        hideGuessForm();
        showScoreForm();
        return;
    }

    if (guessesRemaining === 0) {
        $dukeImg().src = 'images/duke-thinking.svg';
        setStatus(`😔 Out of guesses! The number was ${secretNumber}.`, 'lose');
        setCounter(guessesMade, 0);
        hideGuessForm();
        return;
    }

    const hint = feedback === 'TOO_HIGH' ? 'Too high! Try lower.' : 'Too low! Try higher.';
    setStatus(hint, 'hint');
    setCounter(guessesMade, guessesRemaining);
    $guessInput().focus();
}

function celebrate() {
    const img = $dukeImg();
    img.src = 'images/duke-wave.svg';
    img.alt = 'Duke celebrating your win';
    img.classList.remove('celebrating');
    // Force reflow so the animation replays even if already celebrating
    void img.offsetWidth;
    img.classList.add('celebrating');
    // Remove class after animation completes (1.5s) so it stops cleanly
    img.addEventListener('animationend', () => img.classList.remove('celebrating'), { once: true });
}

function setStatus(msg, cssClass) {
    const el = $status();
    el.textContent = msg;
    el.className   = 'status-message ' + (cssClass || '');
}

function setCounter(made, remaining) {
    $counter().textContent = `Guesses made: ${made}  |  Remaining: ${remaining}`;
}

function showGuessForm()  { $guessForm().classList.remove('hidden'); $guessInput().focus(); }
function hideGuessForm()  { $guessForm().classList.add('hidden'); }
function showScoreForm()  { $scoreForm().classList.remove('hidden'); $nameInput().focus(); }
function hideScoreForm()  { $scoreForm().classList.add('hidden'); $nameInput().value = ''; }
function clearNameError() { $nameError().textContent = ''; }

// ---- API utility ----

async function callApi(method, url, body) {
    try {
        const opts = {
            method,
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        };
        if (body !== undefined) opts.body = JSON.stringify(body);
        const resp = await fetch(url, opts);
        const json = await resp.json();
        return json;
    } catch (err) {
        setStatus('Something went wrong. Please refresh and try again.', '');
        console.error(err);
        return null;
    }
}
