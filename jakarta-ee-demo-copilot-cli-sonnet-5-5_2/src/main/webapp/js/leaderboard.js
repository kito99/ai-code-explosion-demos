(async () => {
  try {
    const response = await fetch('api/leaderboard');
    if (!response.ok) throw new Error(response.statusText);
    const rows = await response.json();
    if (rows.length === 0) {
      document.getElementById('empty').hidden = false;
      return;
    }
    const body = document.querySelector('#board tbody');
    for (const row of rows) {
      const tr = document.createElement('tr');
      for (const value of [row.rank, row.name, row.guesses]) {
        const td = document.createElement('td');
        td.textContent = value;
        tr.append(td);
      }
      body.append(tr);
    }
    document.getElementById('board').hidden = false;
  } catch (e) {
    document.getElementById('error').hidden = false;
  }
})();
