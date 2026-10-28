(() => {
    'use strict';

    const EMPTY_MESSAGE = 'No scores have been posted so far. Win a round and yours goes first.';
    const OFFLINE_MESSAGE = 'The leaderboard cannot be reached right now. Try again in a moment.';

    const board = document.getElementById('board');
    const rows = document.getElementById('board-rows');
    const message = document.getElementById('board-message');

    const say = (text, tone) => {
        message.hidden = false;
        message.textContent = text;
        if (tone) {
            message.dataset.tone = tone;
        } else {
            delete message.dataset.tone;
        }
    };

    const addRow = (standing) => {
        const row = document.createElement('tr');
        [standing.rank, standing.name, standing.guesses].forEach((value) => {
            const cell = document.createElement('td');
            // Set as text, never as markup, so a posted name cannot inject anything.
            cell.textContent = value;
            row.appendChild(cell);
        });
        rows.appendChild(row);
    };

    const show = (standings) => {
        if (standings.length === 0) {
            say(EMPTY_MESSAGE);
            return;
        }
        standings.forEach(addRow);
        message.hidden = true;
        board.hidden = false;
    };

    const load = async () => {
        try {
            const response = await fetch('/api/leaderboard', {headers: {Accept: 'application/json'}});
            if (!response.ok) {
                const problem = await response.json().catch(() => null);
                say(problem?.detail ?? OFFLINE_MESSAGE, 'problem');
                return;
            }
            show(await response.json());
        } catch {
            say(OFFLINE_MESSAGE, 'problem');
        }
    };

    load();
})();
