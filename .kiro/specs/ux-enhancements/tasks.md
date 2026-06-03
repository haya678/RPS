# Implementation Plan: UX Enhancements

## Overview

This plan implements five user experience enhancements for the RPS Battle application. All changes are frontend-only (vanilla JavaScript + CSS), with no backend API modifications required. The work is organized by feature, each independently shippable:

1. **Sketch_Loader** — New reusable SVG circle-draw animation component (`sketch-loader.js`)
2. **Auto Login fallback** — Persist and retry credentials on session expiry (modify `app.js`)
3. **Return-to-Match modal** — Wire existing modal to active house-match state (modify `app.js`)
4. **Waiting overlay** — Show waiting screen in match-only tabs (modify `app.js`)
5. **Player profile click** — Extend hover card to support click/tap interactions (modify `app.js`)

The implementation follows the architecture and design documented in `design.md`, with all correctness properties tested via property-based tests where applicable.

---

## Tasks

- [x] 1. Create Sketch_Loader component
  - [x] 1.1 Implement `sketch-loader.js` module
    - Create `src/main/resources/static/js/sketch-loader.js` with the `SketchLoader` global module
    - Implement `mount(container, opts)` method that creates and appends an SVG circle animation
    - Calculate circle circumference: `2 * π * (size/2 - strokeWidth - 2)`
    - Set `stroke-dasharray` to the circumference value
    - Store circumference in CSS custom property `--sl-circ` on the SVG element
    - Return a handle object with `destroy()` method that removes the SVG from container
    - Check `window.matchMedia('(prefers-reduced-motion: reduce)').matches` before animation
    - If reduced motion is active, render static fully-drawn circle (no animation class)
    - Default options: `size=64`, `strokeWidth=3.5`, `color='#163e79'`
    - _Requirements: 4.1, 4.2, 4.7_
  
  - [ ]* 1.2 Write property test for SketchLoader circumference invariant
    - **Property 1: SketchLoader circumference invariant**
    - **Validates: Requirements 4.1, 4.3**
    - Use fast-check to generate random `size` values in range [16, 512]
    - For each size, compute expected circumference: `2 * π * (size/2 - strokeWidth - 2)`
    - Call `SketchLoader.mount()`, extract `stroke-dasharray` from the rendered circle
    - Assert the extracted value matches the computed circumference within epsilon (0.001)
    - _Requirements: 4.1, 4.3_
  
  - [ ]* 1.3 Write property test for SketchLoader reduced-motion behavior
    - **Property 2: SketchLoader reduced-motion static**
    - **Validates: Requirements 4.7**
    - Mock `window.matchMedia('(prefers-reduced-motion: reduce)')` to return `{ matches: true }`
    - Call `SketchLoader.mount()`
    - Verify the circle element has computed style `animation: none`
    - Verify `stroke-dashoffset = 0` (fully drawn, not animating)
    - _Requirements: 4.7_
  
  - [ ]* 1.4 Write property test for SketchLoader destroy cleanup
    - **Property 3: SketchLoader destroy cleanup**
    - **Validates: Requirements 4.1**
    - Call `SketchLoader.mount(container)` and verify SVG element exists in container
    - Call `destroy()` on the returned handle
    - Assert `container.querySelector('svg') === null`
    - Test with multiple mount/destroy cycles to ensure no memory leaks
    - _Requirements: 4.1_

- [x] 2. Add Sketch_Loader CSS animations to stylesheet
  - [x] 2.1 Add keyframes and styles to `style.css`
    - Open `src/main/resources/static/css/style.css`
    - Add `.sketch-loader-svg` class with `display: block`
    - Add `.sketch-loader-circle` class with `fill: none`, `stroke-linecap: round`, and `animation: sketchLoaderDraw 1.6s linear infinite`
    - Create `@keyframes sketchLoaderDraw` with three stages: 0% (offset = circumference), 50% (offset = 0), 100% (offset = circumference)
    - Add `@media (prefers-reduced-motion: reduce)` rule that sets `.sketch-loader-circle` to `animation: none` and `stroke-dashoffset: 0`
    - _Requirements: 4.2, 4.3, 4.7_

- [x] 3. Integrate Sketch_Loader into loading screens
  - [x] 3.1 Replace auto-login loading spinner with Sketch_Loader
    - In `app.js`, import or reference `SketchLoader` (via script tag in `index.html`)
    - Find the container `#auto-login-loading .auto-login-pulse` (currently contains `<span class="pulse-fist">✊</span>`)
    - On page load, call `SketchLoader.mount(container)` to replace the pulse-fist span
    - Store the returned handle so it can be destroyed when loading ends
    - Call `destroy()` when `setAutoLoginLoading(false)` is called
    - _Requirements: 4.4_
  
  - [x] 3.2 Replace match-tab waiting spinner with Sketch_Loader
    - In `app.js`, find the container `#match-tab-waiting .auto-login-spinner`
    - On page load (DOMContentLoaded), call `SketchLoader.mount(container)` to replace the CSS spinner
    - No destroy call needed — the overlay is shown/hidden with `.hidden` class, animation continues in background
    - _Requirements: 4.5_
  
  - [x] 3.3 Replace waiting-room-panel spinner with Sketch_Loader
    - In `app.js`, find the container `#waiting-room-panel .waiting-room-spinner`
    - When the waiting panel is shown, call `SketchLoader.mount(container)` to replace the CSS spinner
    - Store handle and call `destroy()` when the panel is hidden or room starts
    - _Requirements: 4.6_

- [x] 4. Checkpoint — Verify Sketch_Loader integration
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement Auto Login fallback logic
  - [x] 5.1 Persist credentials on successful manual login
    - In `app.js`, find the `authForm` submit event handler (where `/api/auth/login` is called)
    - After `setLoggedIn(data.user)` is called, add:
      ```javascript
      localStorage.setItem('rpsApiKey', key);
      localStorage.setItem('rpsPin', pin);
      localStorage.setItem('tornId', data.user.torn_id);
      ```
    - _Requirements: 1.1_
  
  - [x] 5.2 Clear credentials on explicit logout
    - In `app.js`, find the `logoutBtn` click event handler
    - Before or after `setLoggedOut()`, add:
      ```javascript
      localStorage.removeItem('tornId');
      localStorage.removeItem('rpsApiKey');
      localStorage.removeItem('rpsPin');
      ```
    - _Requirements: 1.6_
  
  - [x] 5.3 Implement fallback login attempt in init IIFE
    - In `app.js`, find the init async IIFE that calls `GET /api/auth/me` on page load
    - After the `fetch('/api/auth/me')` call fails, add fallback logic:
      - Read `savedKey = localStorage.getItem('rpsApiKey')` and `savedPin = localStorage.getItem('rpsPin')`
      - If both exist, create `AbortController` and set 10-second timeout
      - Call `fetch('/api/auth/login', { method: 'POST', body: JSON.stringify({ api_key: savedKey, pin: savedPin }), signal: controller.signal })`
      - If response is `ok` and `data.success && data.user`, call `setLoggedIn(data.user)`, `connectWS()`, `setAutoLoginLoading(false)`, and return
      - On any error (non-ok response, network error, or timeout abort), fall through to credential cleanup
    - After fallback attempt fails or is not attempted, clear stale credentials:
      ```javascript
      localStorage.removeItem('rpsApiKey');
      localStorage.removeItem('rpsPin');
      localStorage.removeItem('tornId');
      ```
    - Then show the auth form as usual
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [ ]* 5.4 Write property test for credential hygiene
    - **Property 4: Auto Login credential hygiene**
    - **Validates: Requirements 1.1, 1.4, 1.6**
    - Use fast-check to generate random sequences of operations: `saveCredentials`, `clearCredentials`, `failedFallback`
    - After each operation, check localStorage state
    - Assert that `rpsApiKey` is present if and only if `rpsPin` is also present (both or neither)
    - _Requirements: 1.1, 1.4, 1.6_
  
  - [ ]* 5.5 Write property test for timeout bound
    - **Property 5: Auto Login timeout bound**
    - **Validates: Requirements 1.5**
    - Mock `fetch` to never resolve (simulate network hang)
    - Call the fallback login logic
    - Verify that after 10 seconds (±100ms tolerance), `AbortController.abort()` is called
    - Verify that the auth form is shown and credentials are cleared
    - _Requirements: 1.5_
  
  - [ ]* 5.6 Write property test for credentials cleared on failure
    - **Property 6: Auto Login credentials cleared on failure**
    - **Validates: Requirements 1.4**
    - Mock `fetch` to return various failure responses: 401, 500, network error, abort signal
    - For each failure mode, call the fallback login logic
    - Assert that after the auth form is shown, both `rpsApiKey` and `rpsPin` are absent from localStorage
    - _Requirements: 1.4_

- [x] 6. Checkpoint — Verify Auto Login functionality
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement Return-to-Match modal wiring
  - [x] 7.1 Add localStorage helpers for active house match
    - In `app.js`, define helper functions:
      ```javascript
      function setActiveHouseMatch(roomId) {
        localStorage.setItem('activeHouseMatch', JSON.stringify({ roomId }));
      }
      function clearActiveHouseMatch() {
        localStorage.removeItem('activeHouseMatch');
      }
      function getActiveHouseMatch() {
        try {
          return JSON.parse(localStorage.getItem('activeHouseMatch') || 'null');
        } catch { return null; }
      }
      ```
    - _Requirements: 2.1_
  
  - [x] 7.2 Add modal show/hide helpers
    - In `app.js`, get references to `#return-match-modal`, `#reopen-match-tab-btn`, and `#forfeit-match-btn`
    - Define `showReturnMatchModal(roomId)`: check if `matchOnlyMode` is true (return early if so), then remove `.hidden` from the modal
    - Define `hideReturnMatchModal()`: add `.hidden` to the modal
    - Define `checkAndShowReturnMatchModal()`: call `getActiveHouseMatch()`, if it returns a valid `{roomId}` and `!matchOnlyMode`, call `showReturnMatchModal(roomId)`
    - _Requirements: 2.1, 2.6_
  
  - [x] 7.3 Wire modal buttons
    - In `app.js`, add click listener to `#reopen-match-tab-btn`:
      - Read `active = getActiveHouseMatch()`
      - If `active?.roomId` exists, call `window.open()` with URL `?match=${encodeURIComponent(active.roomId)}`
    - Add click listener to `#forfeit-match-btn`:
      - Read `active = getActiveHouseMatch()`
      - If `active?.roomId` exists, send WebSocket message `{ action: 'cancelRoom', tornId: currentUser?.torn_id, roomId: active.roomId }`
      - Call `clearActiveHouseMatch()` and `hideReturnMatchModal()`
    - _Requirements: 2.3, 2.4_
  
  - [x] 7.4 Integrate modal with WebSocket events
    - In `handleWsMessage`, find the `matchStarted` case:
      - If `data.player2Id === 'BOT_BAINING'` and `openMatchInNewTab` is true, call `setActiveHouseMatch(data.roomId)` and `showReturnMatchModal(data.roomId)`
    - In `matchEnd` case:
      - If `getActiveHouseMatch()?.roomId === data.roomId`, call `clearActiveHouseMatch()` and `hideReturnMatchModal()`
    - In `roomCancelled` case:
      - If `getActiveHouseMatch()?.roomId === data.roomId`, call `clearActiveHouseMatch()` and `hideReturnMatchModal()`
    - _Requirements: 2.1, 2.4, 2.5_
  
  - [x] 7.5 Integrate modal with logout and page load
    - In `setLoggedOut()` function, add calls to `clearActiveHouseMatch()` and `hideReturnMatchModal()`
    - In the init IIFE, after successful login (either via `/api/auth/me` or fallback), call `checkAndShowReturnMatchModal()` before returning
    - _Requirements: 2.1_
  
  - [ ]* 7.6 Write property test for modal ↔ localStorage consistency
    - **Property 7: Return-to-Match modal ↔ localStorage consistency**
    - **Validates: Requirements 2.1, 2.5**
    - Simulate `matchStarted` WebSocket message with bot opponent
    - Assert that `#return-match-modal` is visible (no `.hidden` class) and `localStorage.getItem('activeHouseMatch')` is non-null
    - Simulate `matchEnd` WebSocket message for the same room
    - Assert that the modal has `.hidden` class and the localStorage key is null
    - _Requirements: 2.1, 2.5_
  
  - [ ]* 7.7 Write property test for no modal in match-only tab
    - **Property 8: Return-to-Match no modal in match-only tab**
    - **Validates: Requirements 2.1**
    - Set `matchOnlyMode = true` (simulating `?match=<roomId>` tab)
    - Call `showReturnMatchModal('test-room')`
    - Assert that `#return-match-modal` remains `.hidden`
    - _Requirements: 2.1_

- [x] 8. Checkpoint — Verify Return-to-Match modal functionality
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Implement Waiting Overlay logic
  - [x] 9.1 Add waiting overlay show/hide helpers
    - In `app.js`, get references to `#match-tab-waiting` and `#match-tab-waiting-room`
    - Define `showMatchTabWaiting(roomId)`:
      - Return early if `!matchOnlyMode`
      - Set `#match-tab-waiting-room` text content to `roomId` or empty string
      - Remove `.hidden` from `#match-tab-waiting`
      - Call `enableChoices(false)` to disable Rock/Paper/Scissors buttons
    - Define `hideMatchTabWaiting()`:
      - Add `.hidden` to `#match-tab-waiting`
    - _Requirements: 3.1, 3.5_
  
  - [x] 9.2 Integrate waiting overlay with WebSocket events
    - In `handleWsMessage`, find the `identified` case:
      - If `matchOnlyMode && directMatchRoomId` are both true, after sending `joinRoom`, call `showMatchTabWaiting(directMatchRoomId)`
    - In `matchStarted` case:
      - At the beginning (before other match-start logic), call `hideMatchTabWaiting()`
    - In `roomCancelled` case:
      - Call `hideMatchTabWaiting()`
    - _Requirements: 3.1, 3.3, 3.4_
  
  - [ ]* 9.3 Write property test for waiting overlay disables choices
    - **Property 9: Waiting Overlay disables choices**
    - **Validates: Requirements 3.5**
    - Call `showMatchTabWaiting('test-room')`
    - Query all `.choice-btn` elements
    - Assert that all buttons have `disabled === true`
    - _Requirements: 3.5_
  
  - [ ]* 9.4 Write property test for waiting overlay cleared on start
    - **Property 10: Waiting Overlay cleared on start**
    - **Validates: Requirements 3.3**
    - Call `showMatchTabWaiting('test-room')` to show the overlay
    - Simulate `matchStarted` WebSocket message for `currentRoomId`
    - Assert that `#match-tab-waiting` has `.hidden` class
    - Assert that all `.choice-btn` elements have `disabled === false`
    - _Requirements: 3.3_

- [x] 10. Checkpoint — Verify Waiting Overlay functionality
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Implement Player Profile click interaction
  - [x] 11.1 Add click state tracking variable
    - In `app.js`, declare a new module-level variable: `let hoverCardPinnedTornId = null;`
    - This tracks the `tornId` of the currently click-pinned profile, or null if none
    - _Requirements: 5.1_
  
  - [x] 11.2 Replace profile button click handler with toggle logic
    - In `app.js`, find the existing `click` event listener for `.player-profile-btn, .player-avatar-btn` (currently calls `openProfileModal` or `showPlayerProfile`)
    - Replace it with a new document-level `click` listener:
      - Use `e.target.closest('.player-profile-btn, .player-avatar-btn')` to detect profile button clicks
      - If no button clicked and `hoverCardPinnedTornId` is set, clear it and call `hideProfileHoverCard()` (click outside to dismiss)
      - If button clicked, get `tornId = btn.dataset.tornId`
      - If `hoverCardPinnedTornId === tornId`, toggle off: set to null and call `hideProfileHoverCard()`
      - Otherwise, set `hoverCardPinnedTornId = tornId` and call `showHoverForButton(btn, e.pageX, e.pageY)`
    - _Requirements: 5.1, 5.5_
  
  - [x] 11.3 Suppress mouseout auto-hide when card is pinned
    - In `app.js`, find the existing `mouseout` event listener for `.player-profile-btn, .player-avatar-btn`
    - At the beginning of the handler, add: `if (hoverCardPinnedTornId) return;` (skip auto-hide if card is pinned by click)
    - _Requirements: 5.1_
  
  - [x] 11.4 Add keyboard accessibility for Escape key
    - In `app.js`, add a document-level `keydown` listener:
      - If `e.key === 'Escape'` and `hoverCardPinnedTornId` is not null, set `hoverCardPinnedTornId = null` and call `hideProfileHoverCard()`
    - _Requirements: 5.7_
  
  - [ ]* 11.5 Write property test for at most one card visible
    - **Property 11: Player Profile at most one card visible**
    - **Validates: Requirements 5.5**
    - Simulate clicking two different `.player-profile-btn` elements in sequence
    - After each click, query all `.profile-hover-card` elements and count how many lack the `.hidden` class
    - Assert that at most one card is visible at any time
    - _Requirements: 5.5_
  
  - [ ]* 11.6 Write property test for toggle off on second click
    - **Property 12: Player Profile toggle off on second click**
    - **Validates: Requirements 5.1**
    - Simulate clicking the same `.player-profile-btn` once (card should be visible)
    - Simulate clicking the same button again
    - Assert that `.profile-hover-card` has the `.hidden` class (card is hidden)
    - _Requirements: 5.1_

- [x] 12. Final checkpoint — Integration and end-to-end verification
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP (all property-based test tasks are optional)
- Each task references specific requirements from `requirements.md` for traceability
- All implementation work is in `app.js`, `sketch-loader.js`, and `style.css` — no backend changes required
- The `SketchLoader` component is designed to be reusable for future loading screens
- Auto Login credentials are stored in `localStorage` as plaintext, consistent with the existing `tornId` storage pattern
- The Return-to-Match modal only appears for bot matches (house games), not human-vs-human matches
- Property tests validate universal correctness properties defined in `design.md`
- Checkpoints ensure incremental validation and provide opportunities for user feedback

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["1.2", "1.3", "1.4", "3.1"] },
    { "id": 2, "tasks": ["3.2", "3.3", "5.1", "5.2"] },
    { "id": 3, "tasks": ["5.3", "7.1"] },
    { "id": 4, "tasks": ["5.4", "5.5", "5.6", "7.2", "7.3"] },
    { "id": 5, "tasks": ["7.4", "7.5", "9.1"] },
    { "id": 6, "tasks": ["7.6", "7.7", "9.2", "11.1"] },
    { "id": 7, "tasks": ["9.3", "9.4", "11.2"] },
    { "id": 8, "tasks": ["11.3", "11.4"] },
    { "id": 9, "tasks": ["11.5", "11.6"] }
  ]
}
```
