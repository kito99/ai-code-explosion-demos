const table = document.getElementById("leaderboard");
const tableBody = document.getElementById("leaderboard-body");
const emptyMessage = document.getElementById("empty-message");

function addRow(result) {
    const row = document.createElement("tr");
    [result.rank, result.playerName, result.guessCount].forEach((value) => {
        const cell = document.createElement("td");
        cell.textContent = value;
        row.appendChild(cell);
    });
    tableBody.appendChild(row);
}

async function loadLeaderboard() {
    const response = await fetch("api/leaderboard", {headers: {"Accept": "application/json"}});
    const body = await response.json();

    if (!body.results || body.results.length === 0) {
        emptyMessage.textContent = body.message;
        emptyMessage.hidden = false;
        return;
    }

    body.results.forEach(addRow);
    table.hidden = false;
}

loadLeaderboard();
