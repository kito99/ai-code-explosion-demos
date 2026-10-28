(async () => {
  const status = document.getElementById('status');
  const table = document.getElementById('leaderboard-table');
  const body = document.getElementById('leaderboard-body');

  const addCell = (row, text) => {
    const cell = document.createElement('td');
    cell.textContent = text;
    row.appendChild(cell);
  };

  try {
    const response = await fetch('/api/leaderboard');
    if (!response.ok) {
      throw new Error('bad response');
    }
    const entries = await response.json();
    if (entries.length === 0) {
      status.textContent = 'No scores have been posted so far. Win a game and be the first!';
      return;
    }
    entries.forEach((entry) => {
      const row = document.createElement('tr');
      addCell(row, entry.rank);
      addCell(row, entry.name);
      addCell(row, entry.guesses);
      body.appendChild(row);
    });
    status.textContent = '';
    table.classList.remove('hidden');
  } catch {
    status.textContent = "Duke couldn't load the leaderboard. Reload the page to try again.";
    status.className = 'message error';
  }
})();
