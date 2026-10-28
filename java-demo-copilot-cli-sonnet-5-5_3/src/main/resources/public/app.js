'use strict';

const REST_IMAGE = 'images/duke-hips.png';
const WAVE_IMAGES = ['images/duke-wave1.png', 'images/duke-wave2.png'];
const CELEBRATION_MS = 1500;
const WAVE_INTERVAL_MS = 250;

const duke = document.getElementById('duke');
const message = document.getElementById('message');
const guessesMade = document.getElementById('guesses-made');
const guessesRemaining = document.getElementById('guesses-remaining');
const guessForm = document.getElementById('guess-form');
const guessInput = document.getElementById('guess-input');
const guessButton = document.getElementById('guess-button');
const scoreForm = document.getElementById('score-form');
const nameInput = document.getElementById('name-input');
const newGameButton = document.getElementById('new-game-button');

let gameId = null;
let celebrationTimer = null;
let waveTimer = null;

WAVE_IMAGES.forEach((source) => { new Image().src = source; });

async function request(url, body) {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(payload.error || 'Something went wrong. Please try again.');
  }
  return payload;
}

function showMessage(text, isError = false) {
  message.textContent = text;
  message.classList.toggle('error', isError);
}

function stopCelebration() {
  clearTimeout(celebrationTimer);
  clearInterval(waveTimer);
  duke.classList.remove('celebrating');
  duke.src = REST_IMAGE;
}

function celebrate() {
  stopCelebration();
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    return;
  }
  let frame = 0;
  duke.src = WAVE_IMAGES[frame];
  duke.classList.add('celebrating');
  waveTimer = setInterval(() => {
    frame = (frame + 1) % WAVE_IMAGES.length;
    duke.src = WAVE_IMAGES[frame];
  }, WAVE_INTERVAL_MS);
  celebrationTimer = setTimeout(stopCelebration, CELEBRATION_MS);
}

function render(game) {
  guessesMade.textContent = game.guessesMade;
  guessesRemaining.textContent = game.guessesRemaining;
  showMessage(game.message);
  const active = game.status === 'IN_PROGRESS';
  guessInput.disabled = !active;
  guessButton.disabled = !active;
  scoreForm.hidden = game.status !== 'WON';
}

async function startGame() {
  stopCelebration();
  try {
    const game = await request('/api/games');
    gameId = game.gameId;
    render(game);
    guessInput.value = '';
    nameInput.value = '';
    guessInput.focus();
  } catch (error) {
    showMessage(error.message, true);
  }
}

guessForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!gameId) {
    return;
  }
  try {
    const game = await request(`/api/games/${gameId}/guesses`, { guess: guessInput.value });
    render(game);
    guessInput.value = '';
    if (game.status === 'WON') {
      celebrate();
      nameInput.focus();
    } else {
      guessInput.focus();
    }
  } catch (error) {
    showMessage(error.message, true);
    guessInput.select();
  }
});

scoreForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    const result = await request(`/api/games/${gameId}/score`, { name: nameInput.value });
    scoreForm.hidden = true;
    showMessage(`${result.message} Start a new game to play again.`);
    newGameButton.focus();
  } catch (error) {
    showMessage(error.message, true);
    nameInput.focus();
  }
});

newGameButton.addEventListener('click', startGame);

startGame();
