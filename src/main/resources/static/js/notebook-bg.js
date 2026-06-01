/**
 * Notebook doodles on the homepage (login + lobby), hidden during matches.
 */
(function initSiteNotebook() {
  function notebookSeed() {
    const tornId = localStorage.getItem('tornId');
    return tornId ? `site-${tornId}` : 'site-torn-notebook';
  }

  function isHomepageVisible() {
    const match = document.getElementById('game-section-panel');
    if (!match) return true;

    const auth = document.getElementById('auth-section');
    const game = document.getElementById('game-section');
    const authVisible = auth && !auth.classList.contains('hidden');
    const gameVisible = game && !game.classList.contains('hidden');
    const matchVisible = !match.classList.contains('hidden');
    return authVisible || (gameVisible && !matchVisible);
  }

  function syncVisibility() {
    const canvas = document.getElementById('notebook-doodle-canvas');
    if (!canvas) return false;
    const show = isHomepageVisible();
    canvas.classList.toggle('hidden', !show);
    return show;
  }

  function paint() {
    if (!syncVisibility()) return;
    const canvas = document.getElementById('notebook-doodle-canvas');
    if (!canvas || typeof MatchFx === 'undefined') return;
    MatchFx.paintGameNotebookBackground(canvas, notebookSeed());
  }

  function schedulePaint() {
    requestAnimationFrame(paint);
    setTimeout(paint, 120);
  }

  window.repaintNotebookBackground = schedulePaint;
  window.syncNotebookDoodleVisibility = syncVisibility;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', schedulePaint);
  } else {
    schedulePaint();
  }

  window.addEventListener('resize', () => {
    if (isHomepageVisible()) schedulePaint();
  });
})();
