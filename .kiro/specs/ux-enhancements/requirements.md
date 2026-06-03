# Requirements Document

## Introduction

This document covers four user experience enhancements for the RPS Battle application — a Rock-Paper-Scissors browser game backed by a Java/Spring Boot WebSocket server with a vanilla-JS frontend.

The enhancements are:

1. **Auto Login** — Users who have previously authenticated should be logged back in automatically when they revisit the app, using credentials stored in `localStorage`, without requiring them to re-enter their API key and PIN.
2. **Return-to-Match Warning** — When a user who is in an active house (bot) game closes the match view and returns to the home/lobby screen, the UI should warn them and give them the option to resume or forfeit the match.
3. **Waiting-for-Opponent Popup** — When a player is waiting inside a match room (e.g., opened via the match-tab flow) and no opponent has joined yet, a visual overlay/popup should indicate that the room is in the waiting state.
4. **Custom Loading Screen Animation** — Any loading screen in the app (auto-login, tab-waiting, and future loading moments) should display a pencil-draws-a-circle animation that loops with an erase-and-redraw cycle instead of the current spinner.

---

## Glossary

- **App**: The RPS Battle single-page application served from `index.html`.
- **Auto_Login_Module**: The client-side logic that reads credentials from `localStorage` and calls `/api/auth/login` on page load.
- **Loading_Screen**: Any full-page or modal overlay displayed while the App is performing an asynchronous operation (auto-login check, match-tab wait).
- **Sketch_Loader**: The pencil-draws-circle animation component used on Loading_Screens.
- **Match_Tab**: A browser tab opened by the App to host an active match in isolation (`?match=<roomId>`).
- **Match_Room**: An in-progress or waiting game room identified by a `roomId`.
- **House_Match**: A match in which the opponent is the bot player (`BOT_BAINING`, "The House").
- **Return_Match_Modal**: The modal/banner shown on the home screen when the user has an active House_Match and has navigated away from the match view.
- **Waiting_Overlay**: The fullscreen overlay shown inside a Match_Tab while the room status is `WAITING` (no second player has joined yet).
- **WebSocket**: The persistent connection between the App and the backend at `/ws/game`.
- **WAITING**: The `RoomStatus` value indicating a room has been created but the second player has not yet joined.
- **IN_PROGRESS**: The `RoomStatus` value indicating both players are connected and the match is active.

---

## Requirements

### Requirement 1: Auto Login

**User Story:** As a returning user, I want to be automatically logged in when I open the app, so that I do not have to re-enter my API key and PIN every session.

#### Acceptance Criteria

1. WHEN the App loads AND `localStorage` contains a previously saved `rpsApiKey` and `rpsPin`, THE Auto_Login_Module SHALL call `/api/auth/login` with those credentials before showing the authentication form.
2. WHILE the Auto_Login_Module is waiting for the `/api/auth/login` response, THE Loading_Screen SHALL be displayed and the authentication form SHALL remain hidden.
3. IF the `/api/auth/login` call succeeds, THEN THE Auto_Login_Module SHALL transition the App directly to the logged-in game view AND SHALL explicitly hide the authentication form.
4. IF the `/api/auth/login` call fails (network error or invalid credentials), THEN THE Auto_Login_Module SHALL remove the saved credentials from `localStorage` and display the authentication form so the user can log in manually.
5. THE Auto_Login_Module SHALL complete the login attempt or failure transition within 10 seconds; IF no response is received within 10 seconds, THEN THE Auto_Login_Module SHALL treat the attempt as a failure, hide the Loading_Screen, and display the authentication form. THE authentication form SHALL remain hidden until the 10-second timeout expires or a failure response is received — it SHALL NOT be shown while the auto-login attempt is in progress.
6. WHEN a user explicitly logs out via the logout button, THE App SHALL remove all saved credentials from `localStorage` so that the next page load does not trigger an auto-login attempt.

---

### Requirement 2: Return-to-Match Warning

**User Story:** As a player in an active house game, I want to see a warning when I return to the home screen, so that I know my match is still running and I can choose to return to it or forfeit.

#### Acceptance Criteria

1. WHEN the user is in an active House_Match AND the user navigates to or is shown the home/lobby view (e.g., closes the game panel or loads the home URL while a House_Match is `IN_PROGRESS`), THE Return_Match_Modal SHALL be displayed.
2. THE Return_Match_Modal SHALL display the current room ID and a message informing the user that their House_Match is still running.
3. WHEN the user clicks "Return to Match" in the Return_Match_Modal, THE App SHALL navigate or scroll to the active match view for that House_Match room.
4. WHEN the user clicks "Forfeit" in the Return_Match_Modal, THE App SHALL send a forfeit or cancel action to the server for the active House_Match AND remove the House_Match state so the Return_Match_Modal is not shown again. IF the House_Match ends naturally at the same moment as the forfeit action, THE App SHALL process the forfeit action first and then hide the Return_Match_Modal.
5. IF the active House_Match ends (match result received via WebSocket) while the Return_Match_Modal is visible, THEN THE App SHALL hide the Return_Match_Modal automatically.
6. THE Return_Match_Modal SHALL only be shown for House_Matches (opponent is `BOT_BAINING`); when a user navigates away from an active match against a human opponent, THE App SHALL display no warning.

---

### Requirement 3: Waiting-for-Opponent Popup

**User Story:** As a player waiting for an opponent to join my room, I want to see a clear waiting indicator inside the match room, so that I know the system is ready and I just need to wait.

#### Acceptance Criteria

1. WHEN a Match_Tab is opened for a room whose status is `WAITING`, THE Waiting_Overlay SHALL be displayed over the match room content.
2. THE Waiting_Overlay SHALL display the room code, bet amount, and a message indicating that the system is waiting for an opponent to join.
3. WHEN the server sends a `matchStarted` WebSocket message for the current room, THE Waiting_Overlay SHALL be dismissed and the match UI SHALL become active.
4. WHEN the server sends a `roomCancelled` WebSocket message for the current room while the Waiting_Overlay is visible, THE Waiting_Overlay SHALL be dismissed and an appropriate message SHALL be shown.
5. WHILE the Waiting_Overlay is visible, THE match choice buttons (Rock, Paper, Scissors) SHALL be disabled so the user cannot submit a move before the match begins.
6. THE Waiting_Overlay SHALL remain visible until either a `matchStarted` or `roomCancelled` message is received; THE App SHALL NOT auto-dismiss the Waiting_Overlay after a fixed timeout without a server message.

---

### Requirement 4: Custom Loading Screen Animation

**User Story:** As a user, I want loading screens to display a sketch-style circle-drawing animation, so that the visual style is consistent with the hand-drawn notebook aesthetic of the app.

#### Acceptance Criteria

1. THE Sketch_Loader SHALL render an SVG canvas animation in which a circle outline is progressively drawn by a pencil path, consistent with the `INK` (`#163e79`) and `INK2` (`#3d69ad`) color palette used throughout the app.
2. WHEN the circle drawing is complete, THE Sketch_Loader SHALL erase the circle (reverse the draw path) and then immediately begin drawing again, creating a continuous loop.
3. THE Sketch_Loader animation loop SHALL complete one full draw-and-erase cycle within 1200ms to 2000ms so the animation feels deliberate but not sluggish.
4. THE Sketch_Loader SHALL replace the existing CSS spinner (`auto-login-spinner`) on the Auto_Login Loading_Screen (`#auto-login-loading`).
5. THE Sketch_Loader SHALL replace the existing CSS spinner on the Match_Tab waiting screen (`#match-tab-waiting`).
6. WHERE additional Loading_Screens are added to the App in future, THE App SHALL use the Sketch_Loader component regardless of the timing constraints of those screens, so that all loading states share the same animation.
7. THE Sketch_Loader SHALL check the `prefers-reduced-motion` media query immediately upon initialization, before any animation begins. IF the reduced-motion preference is active at that point, THEN THE Sketch_Loader SHALL display a static fully-drawn circle instead of the looping animation.
