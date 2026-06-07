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
  - [x] 1.2 Write property test for SketchLoader circumference invariant
  - [x] 1.3 Write property test for SketchLoader reduced-motion behavior
  - [x] 1.4 Write property test for SketchLoader destroy cleanup

- [x] 2. Add Sketch_Loader CSS animations to stylesheet
  - [x] 2.1 Add keyframes and styles to `style.css`

- [x] 3. Integrate Sketch_Loader into loading screens
  - [x] 3.1 Replace auto-login loading spinner with Sketch_Loader
  - [x] 3.2 Replace match-tab waiting spinner with Sketch_Loader
  - [x] 3.3 Replace waiting-room-panel spinner with Sketch_Loader

- [x] 4. Checkpoint — Verify Sketch_Loader integration

- [x] 5. Implement Auto Login fallback logic
  - [x] 5.1 Persist credentials on successful manual login
  - [x] 5.2 Clear credentials on explicit logout
  - [x] 5.3 Implement fallback login attempt in init IIFE
  - [x] 5.4 Write property test for credential hygiene
  - [x] 5.5 Write property test for timeout bound
  - [x] 5.6 Write property test for credentials cleared on failure

- [x] 6. Checkpoint — Verify Auto Login functionality

- [x] 7. Implement Return-to-Match modal wiring
  - [x] 7.1 Add localStorage helpers for active house match
  - [x] 7.2 Add modal show/hide helpers
  - [x] 7.3 Wire modal buttons
  - [x] 7.4 Integrate modal with WebSocket events
  - [x] 7.5 Integrate modal with logout and page load
  - [x] 7.6 Write property test for modal ↔ localStorage consistency
  - [x] 7.7 Write property test for no modal in match-only tab

- [x] 8. Checkpoint — Verify Return-to-Match modal functionality

- [x] 9. Implement Waiting Overlay logic
  - [x] 9.1 Add waiting overlay show/hide helpers
  - [x] 9.2 Integrate waiting overlay with WebSocket events
  - [x] 9.3 Write property test for waiting overlay disables choices
  - [x] 9.4 Write property test for waiting overlay cleared on start

- [x] 10. Checkpoint — Verify Waiting Overlay functionality

- [x] 11. Implement Player Profile click interaction
  - [x] 11.1 Add click state tracking variable
  - [x] 11.2 Replace profile button click handler with toggle logic
  - [x] 11.3 Suppress mouseout auto-hide when card is pinned
  - [x] 11.4 Add keyboard accessibility for Escape key
  - [x] 11.5 Write property test for at most one card visible
  - [x] 11.6 Write property test for toggle off on second click

- [x] 12. Final checkpoint — Integration and end-to-end verification

---

## Notes

- All implementation work is in `app.js`, `sketch-loader.js`, and `style.css` — no backend changes required
- The `SketchLoader` component is designed to be reusable for future loading screens
- Auto Login credentials are stored in `localStorage` as plaintext, consistent with the existing `tornId` storage pattern
- The Return-to-Match modal only appears for bot matches (house games), not human-vs-human matches
- Property tests validate universal correctness properties defined in `design.md`
- Checkpoints ensure incremental validation and provide opportunities for user feedback
