'use strict';

const table = document.getElementById('leaderboard-table');
const body = table.querySelector('tbody');
const emptyMessage = document.getElementById('empty-message');
const errorMessage = document.getElementById('error-message');

function addCell(row, text) {
  const cell = document.createElement('td');
  cell.textContent = text;
  row.appendChild(cell);
}

async function loadLeaderboard() {
  try {
    const response = await fetch('/api/leaderboard', { cache: 'no-store' });
    if (!response.ok) {
      throw new Error();
    }
    const { entries } = await response.json();
    if (entries.length === 0) {
      emptyMessage.hidden = false;
      return;
    }
    entries.forEach((entry) => {
      const row = document.createElement('tr');
      addCell(row, entry.rank);
      addCell(row, entry.name);
      addCell(row, entry.guesses);
      body.appendChild(row);
    });
    table.hidden = false;
  } catch {
    errorMessage.textContent = 'The leaderboard could not be loaded. Reload the page to try again.';
    errorMessage.hidden = false;
  }
}

loadLeaderboard();
