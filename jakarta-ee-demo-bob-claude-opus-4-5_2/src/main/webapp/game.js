/* game.js — front-end game logic */
const API = '/api/game';

let sessionId = null;

const statusMsg    = document.getElementById('status-message');
const scoreInfo    = document.getElementById('score-info');
const guessForm    = document.getElementById('guess-form');
const guessInput   = document.getElementById('guess-input');
const guessBtn     = document.getElementById('guess-btn');
const winForm      = document.getElementById('win-form');
const nameInput    = document.getElementById('name-input');
const postBtn      = document.getElementById('post-btn');
const nameError    = document.getElementById('name-error');
const endActions   = document.getElementById('end-actions');
const newGameBtn   = document.getElementById('new-game-btn');
const restartBtn   = document.getElementById('restart-btn');
const dukeImg      = document.getElementById('duke-img');

// ── Helpers ──────────────────────────────────────────────────────────────────

function setStatus(text) { statusMsg.textContent = text; }

function setScore(guessCount, guessesRemaining) {
  scoreInfo.textContent = `Guesses made: ${guessCount} · Remaining: ${guessesRemaining}`;
}

function clearScore() { scoreInfo.textContent = ''; }

function showGuessForm(enabled) {
  guessForm.classList.toggle('hidden', !enabled);
  guessInput.disabled = !enabled;
  guessBtn.disabled   = !enabled;
}

function showWinForm(show) { winForm.classList.toggle('hidden', !show); }

function showEndActions(show) { endActions.classList.toggle('hidden', !show); }

function celebrateDuke() {
  dukeImg.src = 'images/duke-wave.svg';
  dukeImg.alt = 'Duke is celebrating!';
  dukeImg.classList.remove('celebrating');
  // force reflow so animation restarts if already celebrating
  void dukeImg.offsetWidth;
  dukeImg.classList.add('celebrating');
  // FR-10: animation plays once then Duke returns to resting pose
  dukeImg.addEventListener('animationend', () => {
    dukeImg.classList.remove('celebrating');
    dukeImg.src = 'images/duke-thinking.svg';
    dukeImg.alt = 'Duke is thinking of a number';
  }, { once: true });
}

function resetDuke() {
  dukeImg.src = 'images/duke-thinking.svg';
  dukeImg.alt = 'Duke is thinking of a number';
  dukeImg.classList.remove('celebrating');
}

// ── Game lifecycle ────────────────────────────────────────────────────────────

async function startGame() {
  resetDuke();
  showWinForm(false);
  showEndActions(false);
  nameInput.value = '';
  nameError.classList.add('hidden');
  guessInput.value = '';

  try {
    const res = await fetch(`${API}/start`, { method: 'POST' });
    if (!res.ok) throw new Error('Failed to start game');
    const state = await res.json();
    sessionId = state.sessionId;
    setStatus('Duke is thinking of a number between 1 and 10. Can you guess it?');
    setScore(state.guessCount, state.guessesRemaining);
    showGuessForm(true);
    guessInput.focus();
  } catch (err) {
    setStatus('Could not start a new game. Please refresh the page.');
    showGuessForm(false);
  }
}

async function submitGuess() {
  const raw = guessInput.value.trim();
  const num = parseInt(raw, 10);

  if (!raw || isNaN(num) || num < 1 || num > 10 || !Number.isInteger(num)) {
    setStatus('Please enter a whole number between 1 and 10.');
    guessInput.select();
    return;
  }

  guessBtn.disabled = true;
  guessInput.disabled = true;

  try {
    const res = await fetch(`${API}/${sessionId}/guess`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ guess: num }),
    });

    const result = await res.json();

    switch (result.feedback) {
      case 'TOO_LOW':
        setStatus(`Too low! Try a higher number.`);
        setScore(result.guessCount, result.guessesRemaining);
        guessInput.value = '';
        guessBtn.disabled = false;
        guessInput.disabled = false;
        guessInput.focus();
        break;

      case 'TOO_HIGH':
        setStatus(`Too high! Try a lower number.`);
        setScore(result.guessCount, result.guessesRemaining);
        guessInput.value = '';
        guessBtn.disabled = false;
        guessInput.disabled = false;
        guessInput.focus();
        break;

      case 'CORRECT':
        setStatus(`🎉 Correct! You guessed it in ${result.guessCount} guess${result.guessCount === 1 ? '' : 'es'}!`);
        clearScore();
        celebrateDuke();
        showGuessForm(false);
        showWinForm(true);
        showEndActions(true);
        nameInput.focus();
        break;

      case 'GAME_OVER':
        setStatus(`😞 Out of guesses! The number was ${result.secretNumber}.`);
        clearScore();
        showGuessForm(false);
        showEndActions(true);
        newGameBtn.focus();
        break;

      case 'INVALID':
        setStatus('Please enter a whole number between 1 and 10.');
        setScore(result.guessCount, result.guessesRemaining);
        guessBtn.disabled = false;
        guessInput.disabled = false;
        guessInput.select();
        break;

      default:
        setStatus('Something went wrong. Please try again.');
        guessBtn.disabled = false;
        guessInput.disabled = false;
    }
  } catch (err) {
    setStatus('Network error. Please check your connection and try again.');
    guessBtn.disabled = false;
    guessInput.disabled = false;
  }
}

async function postScore() {
  const name = nameInput.value;

  if (!name || !name.trim()) {
    showNameError('Please enter your name before posting.');
    return;
  }
  if (name.trim().length > 30) {
    showNameError('Name must be 30 characters or fewer.');
    return;
  }

  postBtn.disabled = true;

  try {
    const res = await fetch(`/api/leaderboard/${sessionId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerName: name.trim() }),
    });

    if (res.status === 201) {
      showWinForm(false);
      setStatus('Score posted! Check the leaderboard. 🏆');
    } else {
      const body = await res.json().catch(() => ({}));
      showNameError(body.message || 'Could not post score. Please try again.');
      postBtn.disabled = false;
    }
  } catch (err) {
    showNameError('Network error. Could not post score.');
    postBtn.disabled = false;
  }
}

function showNameError(msg) {
  nameError.textContent = msg;
  nameError.classList.remove('hidden');
}

// ── Event wiring ──────────────────────────────────────────────────────────────

guessBtn.addEventListener('click', submitGuess);
guessInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') submitGuess();
});

postBtn.addEventListener('click', postScore);
nameInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') postScore();
});

newGameBtn.addEventListener('click', startGame);
restartBtn.addEventListener('click', startGame);

// ── Auto-start ────────────────────────────────────────────────────────────────
startGame();
