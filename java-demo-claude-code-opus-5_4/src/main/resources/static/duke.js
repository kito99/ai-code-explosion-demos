/* Game screen. The server owns the secret number, the guess count, and every
   message; this script only submits what the player typed and renders the reply. */

const Status = Object.freeze({ ACTIVE: 'ACTIVE', WON: 'WON', LOST: 'LOST' });

const elements = {
  duke: document.getElementById('duke'),
  celebrating: document.getElementById('dukeCelebrating'),
  message: document.getElementById('message'),
  guessForm: document.getElementById('guessForm'),
  guessInput: document.getElementById('guessInput'),
  guessButton: document.getElementById('guessButton'),
  nameForm: document.getElementById('nameForm'),
  nameInput: document.getElementById('nameInput'),
  postButton: document.getElementById('postButton'),
  scoreMessage: document.getElementById('scoreMessage'),
  guessesMade: document.getElementById('guessesMade'),
  guessesRemaining: document.getElementById('guessesRemaining'),
  newGameButton: document.getElementById('newGameButton'),
};

/* The server counts every guess it accepts, so a double press must never become two
   guesses. One request at a time per player closes that window at the source. */
let requestInFlight = false;

async function exclusively(action) {
  if (requestInFlight) {
    return;
  }
  requestInFlight = true;
  try {
    await action();
  } finally {
    requestInFlight = false;
  }
}

async function post(path, body) {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    const failure = await response.json().catch(() => ({}));
    throw new Error(failure.message ?? 'Duke could not be reached. Try again in a moment.');
  }
  return response.json();
}

function render(game) {
  elements.message.textContent = game.message;
  elements.message.classList.toggle('message--win', game.status === Status.WON);
  elements.guessesMade.textContent = game.guessesMade;
  elements.guessesRemaining.textContent = game.guessesRemaining;

  const playing = game.status === Status.ACTIVE;
  elements.guessInput.disabled = !playing;
  elements.guessButton.disabled = !playing;
  elements.guessInput.value = '';

  const askForName = game.status === Status.WON && !game.scorePosted;
  elements.nameForm.hidden = !askForName;
  elements.nameInput.disabled = !askForName;
  elements.postButton.disabled = !askForName;

  if (game.status === Status.WON) {
    celebrate();
  } else {
    elements.duke.classList.remove('duke--celebrating');
    elements.celebrating.setAttribute('aria-hidden', 'true');
  }

  if (playing) {
    elements.guessInput.focus();
  } else if (askForName) {
    elements.nameInput.focus();
  } else {
    elements.newGameButton.focus();
  }
}

/* A second win in a row re-renders with the class already applied, and nothing
   replays. Reading offsetWidth between the removal and the add forces the reflow
   that restarts the animation, without waiting on a frame a hidden tab may not paint. */
function celebrate() {
  elements.duke.classList.remove('duke--celebrating');
  elements.celebrating.setAttribute('aria-hidden', 'false');
  void elements.duke.offsetWidth;
  elements.duke.classList.add('duke--celebrating');
}

function showFailure(error) {
  elements.message.textContent = error.message;
}

function startGame() {
  return exclusively(async () => {
    elements.scoreMessage.textContent = '';
    elements.nameInput.value = '';
    try {
      render(await post('/api/game'));
    } catch (error) {
      showFailure(error);
    }
  });
}

elements.guessForm.addEventListener('submit', (event) => {
  event.preventDefault();
  exclusively(async () => {
    try {
      render(await post('/api/game/guesses', { guess: elements.guessInput.value }));
    } catch (error) {
      showFailure(error);
    }
  });
});

elements.nameForm.addEventListener('submit', (event) => {
  event.preventDefault();
  exclusively(async () => {
    try {
      const submission = await post('/api/game/scores', { name: elements.nameInput.value });
      elements.scoreMessage.textContent = submission.message;
      if (submission.posted) {
        elements.nameForm.hidden = true;
        elements.newGameButton.focus();
      }
    } catch (error) {
      elements.scoreMessage.textContent = error.message;
    }
  });
});

elements.newGameButton.addEventListener('click', startGame);

startGame();
