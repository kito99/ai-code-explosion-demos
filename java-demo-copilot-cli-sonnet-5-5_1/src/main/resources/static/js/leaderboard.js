async function load() {
    const empty = document.getElementById('empty');
    const board = document.getElementById('board');
    try {
        const response = await fetch('/api/leaderboard');
        if (!response.ok) throw new Error(response.status);
        const {entries} = await response.json();
        if (entries.length === 0) {
            empty.hidden = false;
            return;
        }
        const body = board.querySelector('tbody');
        for (const entry of entries) {
            const row = body.insertRow();
            row.insertCell().textContent = entry.rank;
            row.insertCell().textContent = entry.name;
            row.insertCell().textContent = entry.guesses;
        }
        board.hidden = false;
    } catch {
        document.getElementById('error').hidden = false;
    }
}

load();
