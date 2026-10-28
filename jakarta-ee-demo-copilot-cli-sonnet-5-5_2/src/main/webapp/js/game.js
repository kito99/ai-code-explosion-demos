(() => {
  const $ = (id) => document.getElementById(id);
  const duke = $('duke');
  const REST = 'images/Thinking.svg';
  const HAPPY = 'images/Wave.svg';
  let game = null;

  async function api(path, body) {
    const response = await fetch('api/' + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const data = await response.json().catch(() => ({}));
    return { ok: response.ok, data };
  }

  function showCounts() {
    $('counts').textContent = game.status === 'ACTIVE' || game.guessesMade > 0
      ? `Guesses made: ${game.guessesMade}. Guesses remaining: ${game.guessesRemaining}.`
      : `Guesses made: 0. Guesses remaining: ${game.maxGuesses}.`;
  }

  function setPlayable(playable) {
    $('guess').disabled = !playable;
    $('guess-button').disabled = !playable;
    if (playable) $('guess').focus();
  }

  function restDuke() {
    duke.classList.remove('celebrate');
    duke.src = REST;
    duke.alt = 'Duke, thinking of a number';
  }

  function celebrate() {
    duke.src = HAPPY;
    duke.alt = 'Duke, waving and jumping for joy';
    duke.classList.remove('celebrate');
    void duke.offsetWidth;
    duke.classList.add('celebrate');
    duke.addEventListener('animationend', restDuke, { once: true });
  }

  async function newGame() {
    restDuke();
    $('message').textContent = '';
    $('message').classList.remove('error');
    $('score-message').textContent = '';
    $('score-form').hidden = true;
    $('name').value = '';
    $('guess').value = '';
    const { ok, data } = await api('games');
    if (!ok) {
      $('message').textContent = 'We couldn\u2019t start a game. Please try again.';
      $('message').classList.add('error');
      return;
    }
    game = data;
    showCounts();
    setPlayable(true);
  }

  $('guess-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!game || game.status !== 'ACTIVE') return;
    const { ok, data } = await api(`games/${game.id}/guesses`, { guess: $('guess').value });
    $('message').textContent = data.message;
    $('message').classList.toggle('error', !ok);
    if (!ok) {
      $('guess').select();
      return;
    }
    game = data.game;
    showCounts();
    $('guess').value = '';
    if (game.status !== 'ACTIVE') {
      setPlayable(false);
    }
    if (game.status === 'WON') {
      celebrate();
      $('score-form').hidden = false;
      $('name').focus();
    } else {
      $('guess').focus();
    }
  });

  $('score-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const { ok, data } = await api(`games/${game.id}/score`, { name: $('name').value });
    $('score-message').classList.toggle('error', !ok);
    if (!ok) {
      $('score-message').textContent = data.message;
      $('name').focus();
      return;
    }
    $('score-form').hidden = true;
    $('score-message').innerHTML = '';
    $('score-message').append('Your score is posted! ');
    const link = document.createElement('a');
    link.href = 'leaderboard.html';
    link.textContent = 'See the leaderboard';
    $('score-message').append(link, ' or start a new game.');
  });

  $('new-game').addEventListener('click', newGame);
  newGame();
})();
