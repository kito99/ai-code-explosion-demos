(() => {
    'use strict';

    const LEADERBOARD_URL = '/api/leaderboard';

    const table = document.getElementById('leaderboard');
    const body = document.getElementById('leaderboard-body');
    const empty = document.getElementById('empty-leaderboard');
    const error = document.getElementById('leaderboard-error');

    const addRow = (entry) => {
        const row = document.createElement('tr');
        const rank = document.createElement('td');
        rank.textContent = entry.rank;
        const name = document.createElement('td');
        name.textContent = entry.playerName;
        const guesses = document.createElement('td');
        guesses.textContent = entry.guessCount;
        row.append(rank, name, guesses);
        body.append(row);
    };

    const render = (entries) => {
        body.replaceChildren();
        entries.forEach(addRow);
        table.hidden = entries.length === 0;
        empty.hidden = entries.length > 0;
    };

    const showError = (text) => {
        error.textContent = text;
        error.hidden = false;
        table.hidden = true;
        empty.hidden = true;
    };

    const load = async () => {
        try {
            const response = await fetch(LEADERBOARD_URL, {headers: {Accept: 'application/json'}});
            if (!response.ok) {
                showError('The leaderboard could not be loaded. Please reload the page.');
                return;
            }
            render(await response.json());
        } catch (failure) {
            showError('Duke could not be reached. Check that the app is running and reload the page.');
        }
    };

    load();
})();
