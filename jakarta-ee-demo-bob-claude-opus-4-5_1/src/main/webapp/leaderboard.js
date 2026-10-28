/* leaderboard.js */
const container = document.getElementById('leaderboard-container');

async function loadLeaderboard() {
  try {
    const res = await fetch('/api/leaderboard');
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const entries = await res.json();

    if (entries.length === 0) {
      container.innerHTML = '<p class="empty-msg">No scores have been posted yet. Play a game and be the first!</p>';
      return;
    }

    const rows = entries.map(e => `
      <tr>
        <td>${e.rank}</td>
        <td>${escHtml(e.playerName)}</td>
        <td>${e.guessCount}</td>
      </tr>`).join('');

    container.innerHTML = `
      <table class="leaderboard-table" aria-label="Top 10 scores">
        <thead>
          <tr>
            <th scope="col">Rank</th>
            <th scope="col">Player</th>
            <th scope="col">Guesses</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;
  } catch (err) {
    container.innerHTML = '<p class="error-msg">Could not load the leaderboard. Please try again later.</p>';
  }
}

function escHtml(str) {
  return str.replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
}

loadLeaderboard();
