const WAVE_FRAMES = ['/img/duke-wave-1.png', '/img/duke-wave-2.png'];
const STANDING_FRAME = '/img/duke-standing.png';
const CELEBRATION_MS = 1500;
const WAVE_INTERVAL_MS = 150;
const MESSAGE_SELECTOR_IDS = ['duke', 'message', 'counts', 'guess-form', 'guess', 'score-form', 'name', 'new-game'];

const el = Object.fromEntries(MESSAGE_SELECTOR_IDS.map(id => [id, document.getElementById(id)]));
let gameId = null;
let celebration = null;

async function call(path, body) {
    try {
        const response = await fetch(path, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: body === undefined ? undefined : JSON.stringify(body)
        });
        const data = response.status === 204 ? null : await response.json().catch(() => null);
        return {ok: response.ok, data};
    } catch {
        return {ok: false, data: {message: 'Duke cannot be reached right now. Please try again in a moment.'}};
    }
}

function say(text, isError = false) {
    el.message.textContent = text;
    el.message.classList.toggle('error', isError);
}

function stopCelebration() {
    if (celebration) {
        clearInterval(celebration);
        celebration = null;
    }
    el.duke.classList.remove('celebrating');
    el.duke.src = STANDING_FRAME;
}

function celebrate() {
    stopCelebration();
    el.duke.classList.add('celebrating');
    let frame = 0;
    el.duke.src = WAVE_FRAMES[frame];
    celebration = setInterval(() => {
        frame = (frame + 1) % WAVE_FRAMES.length;
        el.duke.src = WAVE_FRAMES[frame];
    }, WAVE_INTERVAL_MS);
    setTimeout(() => { if (celebration) stopCelebration(); }, CELEBRATION_MS);
}

function showCounts(game) {
    el.counts.textContent = `Guesses made: ${game.guessesMade} · Guesses remaining: ${game.guessesRemaining}`;
}

function setPlaying(playing) {
    el['guess-form'].hidden = !playing;
    if (playing) el.guess.focus();
}

async function startGame() {
    stopCelebration();
    el['score-form'].hidden = true;
    el.guess.value = '';
    el.name.value = '';
    const {ok, data} = await call('/api/games');
    if (!ok) {
        say(data?.message ?? 'Could not start a game. Please try again.', true);
        return;
    }
    gameId = data.gameId;
    say(data.message);
    showCounts(data);
    setPlaying(true);
}

function parseGuess(text) {
    const trimmed = text.trim();
    return /^-?\d+$/.test(trimmed) ? Number(trimmed) : trimmed;
}

async function submitGuess(event) {
    event.preventDefault();
    if (!gameId) return;
    const {ok, data} = await call(`/api/games/${gameId}/guesses`, {guess: parseGuess(el.guess.value)});
    if (!ok) {
        say(data?.message ?? 'Something went wrong. Please try again.', true);
        el.guess.select();
        return;
    }
    say(data.message);
    showCounts(data);
    el.guess.value = '';
    if (data.status === 'WON') {
        setPlaying(false);
        el['score-form'].hidden = false;
        el.name.focus();
        celebrate();
    } else if (data.status === 'LOST') {
        setPlaying(false);
    } else {
        el.guess.focus();
    }
}

async function submitScore(event) {
    event.preventDefault();
    const {ok, data} = await call(`/api/games/${gameId}/score`, {name: el.name.value});
    if (!ok) {
        say(data?.message ?? 'Your score could not be posted. Please try again.', true);
        el.name.focus();
        return;
    }
    el['score-form'].hidden = true;
    say('Score posted! See the leaderboard, or play again.');
    el['new-game'].focus();
}

el['guess-form'].addEventListener('submit', submitGuess);
el['score-form'].addEventListener('submit', submitScore);
el['new-game'].addEventListener('click', startGame);
startGame();
