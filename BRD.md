# Business Requirements Document: Guess the Number with Duke

## 1. Overview

This document describes the business requirements for a small, web-based game
where a player tries to guess a secret number between 1 and 10. Duke, the Java
mascot, plays the role of the character who is thinking of the number. Players
who win can post their score to a leaderboard that ranks wins by how few guesses
they took. The game is meant to be simple, friendly, and fun, and it doubles as
a demo.

## 2. Purpose and Background

We want a lightweight, self-contained game. It should be approachable for
anyone, require no sign-in, and finish in well under a minute. Using Duke as the
on-screen character gives the app a bit of personality and ties it to the Java
theme of the presentation.

## 3. Objectives

- Give the player a quick, satisfying guessing game they can replay instantly.
- Show Duke as the character who "knows" the secret number.
- Give players a reason to replay by ranking wins by how few guesses they took.
- Keep the whole thing simple enough to build and explain in a single sitting.

## 4. Scope

### 4.1 In Scope

- A single-player game guessing a whole number from 1 through 10.
- Duke shown on screen as the character thinking of the number.
- A celebration animation of Duke when the player guesses correctly.
- Feedback after each guess: too high, too low, or correct.
- A running count of guesses made and guesses remaining.
- A limited number of guesses per game, with a lose state when they run out.
- The ability to start a new game at any time.
- A name entered by the player when they win, so their score can be posted.
- A leaderboard of winning results that outlives the app process.
- A screen that displays the leaderboard.

### 4.2 Out of Scope

- User accounts, logins, or saved profiles. The name a player types to post a
  score is not an account, and no identity sits behind it; two players may use
  the same name.
- Editing or deleting leaderboard entries once they are posted.
- Judging names by their content, such as screening for profanity. The only
  checks applied to a name are the ones FR-14 describes.
- Per-player history or statistics beyond the single list of best results.
- Difficulty levels or configurable number ranges or guess limits.
- Multiplayer or turn-based play against other people.

## 5. Stakeholders

| Role | Interest |
| --- | --- |
| Presenter | Wants a clean, working demo of a server-side Java app. |
| Player | Wants a quick, fun game that is easy to understand. |
| Developer | Wants clear, testable requirements to build against. |

## 6. Functional Requirements

### 6.1 Game Lifecycle

- **FR-1 — Game start.** A game begins automatically when the page loads, and
  again whenever the player chooses to play again (see FR-12). At start, the
  system picks the secret number and resets the guess count to zero.
- **FR-2 — Secret number.** At the start of each game, the system picks a random
  whole number between 1 and 10, inclusive. Every value in that range is equally
  likely on each draw. Draws are independent, so the same number may come up in
  consecutive games.
- **FR-3 — Guess limit.** Each game allows the player up to 5 guesses. Only
  accepted guesses (see FR-6) count against this limit.

### 6.2 Playing a Round

- **FR-4 — Duke on screen.** Duke is displayed as the character who is thinking
  of the number, so the player understands who they are guessing against.
- **FR-5 — Enter a guess.** While the game is active, the player can enter a
  whole number between 1 and 10 and submit it.
- **FR-6 — Input validation.** If the player submits something that is not a
  whole number from 1 to 10, the system shows a friendly message and does not
  count it as a guess or change the guesses remaining.
- **FR-7 — Feedback.** After each accepted guess that is not correct, the system
  tells the player whether the guess was too high or too low.
- **FR-8 — Guess count.** During an active game, the system shows the number of
  guesses made and the number of guesses remaining.

### 6.3 Ending a Round

- **FR-9 — Win state.** When the player guesses the secret number, the system
  congratulates them, shows how many guesses it took, and offers to post the
  score to the leaderboard (see FR-13). No further guesses are accepted until a
  new game starts.
- **FR-10 — Win animation.** On a correct guess, Duke plays a short celebration
  animation to reward the player. Duke does a happy jump: he springs up and
  settles back down two or three times over roughly 1.5 seconds while waving an
  arm, and the animation plays once and then stops with Duke back in his resting
  pose. The animation must not block the player from starting a new game (see
  FR-12). If animation is unavailable, the win still shows the congratulations
  message from FR-9.
- **FR-11 — Lose state.** If the player uses all 5 guesses without finding the
  secret number, the game ends, the system reveals the secret number, and no
  further guesses are accepted until a new game starts.
- **FR-12 — Play again.** From either the win or lose state, and at any point
  during an active game, the player can start a fresh game. Starting a fresh game
  clears the previous feedback and result message and applies FR-1.

### 6.4 Leaderboard

- **FR-13 — Name capture on a win.** When the player wins, the system asks for a
  name before recording the result. A name must be entered for the score to be
  posted. The player can decline and start a new game (see FR-12), in which case
  nothing is recorded.
- **FR-14 — Name validation.** A name is accepted when it has at least one
  visible character after surrounding whitespace is trimmed and is no longer than
  30 characters. If it is not acceptable, the system shows a friendly message and
  records nothing until a valid name is entered.
- **FR-15 — Recording a result.** A posted result stores the name, the number of
  accepted guesses the win took, and when it happened. Each winning game creates
  its own entry; nothing is overwritten.
- **FR-16 — No entry for a loss.** A game that ends in the lose state (see
  FR-11) records nothing, because there is no winning guess count.
- **FR-17 — Leaderboard screen.** A separate screen lists the best results,
  showing each entry's rank, name, and number of guesses. It shows the top 10
  entries, ordered by fewest guesses first, with ties broken by which was
  recorded first.
- **FR-18 — Empty leaderboard.** When nothing has been recorded yet, the screen
  says no scores have been posted so far instead of showing an empty table.
- **FR-19 — Navigation.** The player can reach the leaderboard from the game and
  return to the game from the leaderboard. Returning may start a fresh game under
  FR-1; no in-progress game needs to be preserved.
- **FR-20 — Shared results.** The leaderboard reflects results recorded by every
  player on that server. Opening or reloading the screen shows the results as
  they stand at that moment, including any another player posted in the meantime.
  A screen that is already open does not have to update on its own.

## 7. Non-Functional Requirements

- **NFR-1 — Technology constraint.** The game is built with a server-side Java
  backend and an HTML front-end. No specific framework or library beyond that is
  mandated by this document.
- **NFR-2 — Responsiveness.** After the player submits a guess, feedback appears
  within 1 second under normal local or single-user conditions.
- **NFR-3 — Accessibility.** Guesses can be entered and submitted using only the
  keyboard, and all game text meets a readable contrast against its background.
- **NFR-4 — Clarity.** Messages use plain, friendly language and always tell the
  player what just happened and what they can do next.
- **NFR-5 - Duke Images.** The game includes the official Duke image to 
  represent the character (repository: https://github.com/openjdk/duke).
- **NFR-8 — Durable storage.** Leaderboard entries are kept in a database and
  survive a restart of the app. Consistent with NFR-1, no specific database
  product or data access technology is mandated by this document.

## 8. Assumptions

- The player is on a single device and plays one game at a time.
- An in-progress game does not persist; refreshing the page starts a new game.
  Leaderboard entries do persist (see NFR-8).
- The app runs as a single instance against one database, so every player shares
  the same leaderboard.
- A suitable Duke image or graphic is available to display.

## 9. Success Criteria

- A player can start a game, make guesses, receive correct high/low feedback,
  reach either the win or lose state, and start over.
- Every accepted guess produces accurate feedback, the win message reports the
  right number of guesses, and the lose state reveals the correct secret number.
- The guesses-remaining count is always accurate and never drops below zero.
- Duke is clearly visible as the character thinking of the number, and plays his
  celebration animation when the player wins.
- Winning and entering a valid name adds a leaderboard entry showing that name
  and the right number of guesses, while losing adds nothing.
- The leaderboard orders entries by fewest guesses, breaks ties by which was
  recorded first, and shows no more than 10.
- Leaderboard entries are still there after the app restarts.
- With nothing recorded, the leaderboard screen explains that no scores have
  been posted yet.
