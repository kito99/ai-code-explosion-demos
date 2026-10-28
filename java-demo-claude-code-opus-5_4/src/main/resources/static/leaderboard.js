/* Leaderboard screen. Reads the rankings as they stand the moment the page opens. */

const board = {
  message: document.getElementById('boardMessage'),
  table: document.getElementById('boardTable'),
  body: document.getElementById('boardBody'),
};

function renderRows(rows) {
  board.body.replaceChildren(...rows.map((row) => {
    const tr = document.createElement('tr');
    for (const value of [row.rank, row.playerName, row.guessCount]) {
      const cell = document.createElement('td');
      cell.textContent = value;
      tr.append(cell);
    }
    return tr;
  }));
}

async function loadBoard() {
  try {
    const response = await fetch('/api/leaderboard');
    if (!response.ok) {
      throw new Error('The leaderboard could not be loaded. Reload the page to try again.');
    }
    const rows = await response.json();
    if (rows.length === 0) {
      board.message.textContent = 'No scores have been posted so far. Win a round and you could be first.';
      return;
    }
    renderRows(rows);
    board.message.hidden = true;
    board.table.hidden = false;
  } catch (error) {
    board.message.textContent = error.message;
  }
}

loadBoard();
