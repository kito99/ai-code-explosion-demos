/* The leaderboard screen. It reads the list once, when the page loads, which is what the
   requirements ask for: an open screen does not have to keep itself up to date. */
(() => {
    'use strict';

    const LEADERBOARD_URL = 'api/leaderboard';

    const table = document.getElementById('scores');
    const body = document.getElementById('scores-body');
    const empty = document.getElementById('empty');
    const error = document.getElementById('error');

    function addRow(entry) {
        const row = document.createElement('tr');
        for (const value of [entry.rank, entry.name, entry.guessCount]) {
            const cell = document.createElement('td');
            // textContent rather than innerHTML, so a name is shown exactly as it was typed.
            cell.textContent = value;
            row.appendChild(cell);
        }
        body.appendChild(row);
    }

    async function load() {
        try {
            const response = await fetch(LEADERBOARD_URL, {
                credentials: 'same-origin',
                headers: {'Accept': 'application/json'}
            });
            if (!response.ok) {
                throw new Error(`The leaderboard responded with ${response.status}`);
            }
            const entries = await response.json();
            if (entries.length === 0) {
                empty.hidden = false;
                return;
            }
            entries.forEach(addRow);
            table.hidden = false;
        } catch (failure) {
            error.textContent = 'The leaderboard could not be read right now. Reload to try again.';
            error.hidden = false;
        }
    }

    load();
})();
