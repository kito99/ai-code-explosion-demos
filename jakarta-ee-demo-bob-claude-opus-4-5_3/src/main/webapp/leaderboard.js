/**
 * leaderboard.js — loads and renders the leaderboard. FR-17, FR-18, FR-20.
 */

const API_BASE = 'api';

window.addEventListener('DOMContentLoaded', loadLeaderboard);

async function loadLeaderboard() {
    const container = document.getElementById('leaderboard-content');
    try {
        const resp    = await fetch(`${API_BASE}/leaderboard`, { headers: { Accept: 'application/json' } });
        const entries = await resp.json();

        if (!Array.isArray(entries) || entries.length === 0) {
            container.innerHTML = '<p class="empty-msg">No scores have been posted yet. Be the first to win!</p>';
            return;
        }

        const rows = entries.map(e => `
            <tr>
                <td class="rank">${e.rank}</td>
                <td>${escHtml(e.playerName)}</td>
                <td class="guesses">${e.guessesTaken}</td>
            </tr>`).join('');

        container.innerHTML = `
            <table class="leaderboard-table" aria-label="Top 10 leaderboard">
                <thead>
                    <tr>
                        <th class="rank">Rank</th>
                        <th>Name</th>
                        <th class="guesses">Guesses</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>`;
    } catch (err) {
        container.innerHTML = '<p class="empty-msg">Could not load the leaderboard. Please try again.</p>';
        console.error(err);
    }
}

function escHtml(s) {
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
