(function () {
  "use strict";

  var REQUEST_TIMEOUT_MS = 8000;

  var form = document.getElementById("guess-form");
  var input = document.getElementById("guess");
  var submitButton = document.getElementById("submit");
  var playAgainButton = document.getElementById("play-again");
  var message = document.getElementById("message");
  var score = document.getElementById("score");
  var pips = document.getElementById("pips");
  var duke = document.getElementById("duke");

  var lastStatus = null;
  var currentRequest = 0;
  var pending = false;

  duke.addEventListener("animationend", function (event) {
    // Let the class go once the celebration finishes so the next win can replay it.
    if (event.animationName === "duke-jump") {
      duke.classList.remove("duke--celebrate");
    }
  });

  function celebrate() {
    duke.classList.remove("duke--celebrate");
    void duke.offsetWidth; // force a reflow so the animation restarts from the top
    duke.classList.add("duke--celebrate");
  }

  function renderPips(state) {
    if (pips.children.length !== state.maxGuesses) {
      pips.replaceChildren();
      for (var i = 0; i < state.maxGuesses; i++) {
        pips.appendChild(document.createElement("li"));
      }
    }
    for (var j = 0; j < pips.children.length; j++) {
      pips.children[j].classList.toggle("is-used", j < state.guessesMade);
    }
  }

  function render(state, clearInput) {
    var playing = state.status === "PLAYING";

    message.textContent = state.message;
    message.classList.toggle("bubble--win", state.status === "WON");
    message.classList.toggle("bubble--lose", state.status === "LOST");

    renderPips(state);
    score.textContent = playing
      ? "Guesses made: " + state.guessesMade + " · Guesses remaining: " + state.guessesRemaining
      : "Final tally: " + state.guessesMade + (state.guessesMade === 1 ? " guess" : " guesses") + " used.";

    input.disabled = !playing;
    input.setAttribute("aria-label", "Your guess, a whole number from "
      + state.minNumber + " to " + state.maxNumber);

    if (clearInput || state.guessAccepted || !playing) {
      input.value = "";
    }

    if (state.status === "WON" && lastStatus !== "WON") {
      celebrate();
    }
    if (playing) {
      duke.classList.remove("duke--celebrate");
      input.focus();
    } else if (lastStatus === "PLAYING") {
      // The round just ended; send the keyboard to the only thing left to do.
      playAgainButton.focus();
    }

    lastStatus = state.status;
  }

  function showTrouble(text) {
    message.textContent = text;
    message.classList.remove("bubble--win");
    message.classList.add("bubble--lose");
    input.disabled = false;
  }

  function setPending(value) {
    pending = value;
    // Play again stays live even mid-request, so a slow or wedged call never traps the player.
    submitButton.disabled = value || input.disabled;
  }

  /**
   * Sends one request. A newer call supersedes an older one: the stale response is
   * dropped rather than allowed to overwrite fresher state.
   */
  function call(path, clearInput) {
    var id = ++currentRequest;
    var controller = new AbortController();
    var timer = setTimeout(function () {
      controller.abort();
    }, REQUEST_TIMEOUT_MS);

    setPending(true);

    return fetch(path, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
      },
      body: path === "api/game/guess" ? "guess=" + encodeURIComponent(input.value) : "",
      signal: controller.signal
    })
      .then(function (response) {
        if (!response.ok) {
          throw new Error("Request failed with status " + response.status);
        }
        return response.json();
      })
      .then(function (state) {
        if (id === currentRequest) {
          render(state, clearInput);
        }
      })
      .catch(function () {
        if (id === currentRequest) {
          showTrouble("Duke lost his train of thought — check that the server is running, "
            + "then hit Play again.");
        }
      })
      .finally(function () {
        clearTimeout(timer);
        if (id === currentRequest) {
          setPending(false);
        }
      });
  }

  function newGame() {
    duke.classList.remove("duke--celebrate");
    lastStatus = null;
    return call("api/game/new", true);
  }

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    // One guess at a time, so a double submit cannot spend two turns.
    if (pending || input.disabled) {
      return;
    }
    call("api/game/guess", false);
  });

  playAgainButton.addEventListener("click", newGame);

  // FR-1: loading the page starts a fresh game, so a refresh always deals a new number.
  newGame();
})();
