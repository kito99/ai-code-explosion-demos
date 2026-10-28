(() => {
  const MIN_NUMBER = 1;
  const MAX_NUMBER = 10;
  const MAX_NAME_LENGTH = 30;
  const RESTING_IMAGE = 'img/Thinking.svg';
  const CELEBRATION_IMAGE = 'img/Wave.svg';

  const dukeImage = document.getElementById('duke');
  const counts = document.getElementById('counts');
  const message = document.getElementById('message');
  const guessForm = document.getElementById('guess-form');
  const guessInput = document.getElementById('guess-input');
  const guessButton = document.getElementById('guess-button');
  const nameForm = document.getElementById('name-form');
  const nameInput = document.getElementById('name-input');
  const nameButton = document.getElementById('name-button');
  const postedLinks = document.getElementById('posted-links');
  const newGameButton = document.getElementById('new-game-button');

  let gameId = null;

  const show = (text, kind) => {
    message.textContent = text;
    message.className = kind ? `message ${kind}` : 'message';
  };

  const showCounts = (made, remaining) => {
    counts.textContent = `Guesses made: ${made} · Guesses remaining: ${remaining}`;
  };

  const requestJson = async (url, options) => {
    const response = await fetch(url, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(body.message || 'Something went wrong. Please try again.');
    }
    return body;
  };

  const resetDuke = () => {
    dukeImage.classList.remove('celebrate');
    dukeImage.src = RESTING_IMAGE;
    dukeImage.alt = 'Duke, the Java mascot, thinking of a number';
  };

  const celebrate = () => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      return;
    }
    dukeImage.src = CELEBRATION_IMAGE;
    dukeImage.alt = 'Duke, the Java mascot, jumping and waving';
    dukeImage.classList.add('celebrate');
    dukeImage.addEventListener('animationend', resetDuke, { once: true });
  };

  const setPlaying = (playing) => {
    guessInput.disabled = !playing;
    guessButton.disabled = !playing;
  };

  const startGame = async () => {
    resetDuke();
    nameForm.classList.add('hidden');
    postedLinks.classList.add('hidden');
    nameInput.value = '';
    guessInput.value = '';
    show('Duke has picked a number. Type your guess and press Enter.');
    try {
      const game = await requestJson('/api/games', { method: 'POST' });
      gameId = game.gameId;
      showCounts(game.guessesMade, game.guessesRemaining);
      setPlaying(true);
      guessInput.focus();
    } catch (error) {
      setPlaying(false);
      show(`${error.message} Press "Play again" to retry.`, 'error');
    }
  };

  const outcomeMessage = (outcome, guess) => ({
    TOO_LOW: `${guess} is too low. Try a higher number!`,
    TOO_HIGH: `${guess} is too high. Try a lower number!`,
  })[outcome];

  const finishRound = (result) => {
    setPlaying(false);
    if (result.game.status === 'WON') {
      show(`You got it in ${result.game.guessesMade} ${result.game.guessesMade === 1 ? 'guess' : 'guesses'}! `
        + 'Enter your name to post your score, or press "Play again".', 'success');
      nameForm.classList.remove('hidden');
      nameInput.focus();
      celebrate();
    } else {
      show(`Out of guesses! Duke was thinking of ${result.game.secretNumber}. Press "Play again" to try again.`,
        'error');
    }
  };

  const parseGuess = (text) => {
    const trimmed = text.trim();
    if (!/^\d+$/.test(trimmed)) {
      return null;
    }
    const value = Number(trimmed);
    return value >= MIN_NUMBER && value <= MAX_NUMBER ? value : null;
  };

  guessForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const guess = parseGuess(guessInput.value);
    if (guess === null) {
      show(`Please enter a whole number from ${MIN_NUMBER} to ${MAX_NUMBER}. That didn't count as a guess.`, 'error');
      guessInput.select();
      return;
    }
    try {
      const result = await requestJson(`/api/games/${gameId}/guesses`, {
        method: 'POST',
        body: JSON.stringify({ guess }),
      });
      showCounts(result.game.guessesMade, result.game.guessesRemaining);
      guessInput.value = '';
      if (result.game.status === 'IN_PROGRESS') {
        show(outcomeMessage(result.outcome, guess), null);
        guessInput.focus();
      } else {
        finishRound(result);
      }
    } catch (error) {
      show(error.message, 'error');
    }
  });

  nameForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const name = nameInput.value.trim();
    if (name.length === 0 || [...name].length > MAX_NAME_LENGTH) {
      show(`Please enter a name with 1 to ${MAX_NAME_LENGTH} characters.`, 'error');
      nameInput.focus();
      return;
    }
    nameButton.disabled = true;
    try {
      const score = await requestJson(`/api/games/${gameId}/score`, {
        method: 'POST',
        body: JSON.stringify({ name }),
      });
      nameForm.classList.add('hidden');
      postedLinks.classList.remove('hidden');
      show(`Score posted, ${score.name}! Check the leaderboard or press "Play again".`, 'success');
    } catch (error) {
      show(error.message, 'error');
    } finally {
      nameButton.disabled = false;
    }
  });

  newGameButton.addEventListener('click', startGame);

  startGame();
})();
