const $ = id => document.getElementById(id);
const duke = $('duke');
let over = false;

function show(data, cls) {
  $('message').textContent = data.message;
  $('message').className = cls || '';
  if (data.guessesMade !== undefined) {
    $('made').textContent = data.guessesMade;
    $('remaining').textContent = data.guessesRemaining;
  }
}

function setActive(active) {
  over = !active;
  $('guess-form').hidden = !active;
}

function stopCelebration() {
  duke.classList.remove('celebrate');
  duke.src = 'img/Thinking.svg';
}

function celebrate() {
  duke.src = 'img/Wave.svg';
  duke.classList.remove('celebrate');
  void duke.offsetWidth;
  duke.classList.add('celebrate');
  setTimeout(stopCelebration, 1500);
}

async function api(path, body) {
  const res = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined
  });
  return res.json();
}

async function newGame() {
  stopCelebration();
  $('name-form').hidden = true;
  $('name').value = '';
  $('guess').value = '';
  setActive(true);
  show(await api('/api/game'));
  $('guess').focus();
}

$('guess-form').addEventListener('submit', async e => {
  e.preventDefault();
  const text = $('guess').value.trim();
  const data = await api('/api/game/guess', { guess: text });
  $('guess').value = '';
  if (data.status === 'INVALID') { show(data, 'bad'); }
  else if (data.status === 'WON') {
    show(data, 'good');
    setActive(false);
    $('name-form').hidden = false;
    celebrate();
    $('name').focus();
  } else if (data.status === 'LOST') {
    show(data, 'bad');
    setActive(false);
  } else if (data.status === 'GAME_OVER' || data.status === 'NO_GAME') {
    show(data, 'bad');
    setActive(false);
  } else { show(data); }
  if (!over) $('guess').focus();
});

$('name-form').addEventListener('submit', async e => {
  e.preventDefault();
  const data = await api('/api/game/score', { name: $('name').value });
  if (data.status === 'POSTED') {
    show(data, 'good');
    $('name-form').hidden = true;
  } else {
    show(data, 'bad');
    if (data.status !== 'INVALID_NAME') $('name-form').hidden = true;
  }
});

$('new-game').addEventListener('click', newGame);
newGame();
