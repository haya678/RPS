/**
 * Full-page notebook doodles — always visible behind the UI.
 */
(function initSiteNotebook() {
  function notebookSeed() {
    const tornId = localStorage.getItem('tornId');
    return tornId ? `site-${tornId}` : 'site-torn-notebook';
  }

  function syncVisibility() {
    const canvas = document.getElementById('notebook-doodle-canvas');
    if (!canvas) return false;
    canvas.classList.remove('hidden');
    document.body.classList.add('notebook-site');
    return true;
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

  window.addEventListener('resize', schedulePaint);
})();
