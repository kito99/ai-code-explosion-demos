(() => {
    'use strict';

    const message = document.getElementById('leaderboard-message');
    const table = document.getElementById('leaderboard-table');
    const body = document.getElementById('leaderboard-body');

    const cell = (text, className) => {
        const td = document.createElement('td');
        td.textContent = text;
        if (className) {
            td.className = className;
        }
        return td;
    };

    const render = (entries) => {
        if (entries.length === 0) {
            message.textContent = 'No scores have been posted so far. Win a game to be the first!';
            return;
        }
        entries.forEach((entry) => {
            const row = document.createElement('tr');
            row.append(cell(entry.rank, 'rank'), cell(entry.name), cell(entry.guesses, 'guesses'));
            body.append(row);
        });
        message.hidden = true;
        table.hidden = false;
    };

    const load = async () => {
        try {
            const response = await fetch('/api/leaderboard');
            if (!response.ok) {
                throw new Error('The leaderboard is not available right now. Please try again.');
            }
            const payload = await response.json();
            render(payload.entries ?? []);
        } catch (error) {
            message.textContent = error.message;
        }
    };

    load();
})();
