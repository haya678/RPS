/**
 * Extensive Error Logging for RPS Battle.
 * Logs to console and potentially a hidden UI panel for debugging.
 */
const AppLogger = (() => {
  const logs = [];
  const MAX_LOGS = 100;

  function log(level, context, message, data = null) {
    const entry = {
      timestamp: new Date().toISOString(),
      level,
      context,
      message,
      data: data ? (data instanceof Error ? { name: data.name, message: data.message, stack: data.stack } : data) : null
    };

    logs.unshift(entry);
    if (logs.length > MAX_LOGS) logs.pop();

    const color = level === 'ERROR' ? 'color: #ff4d4d' : level === 'WARN' ? 'color: #ffcc00' : 'color: #3d69ad';
    console.log(`%c[${level}] [${context}] ${message}`, color, data || '');

    // If there's a debug panel, we could render it here.
    updateDebugPanel();
  }

  function updateDebugPanel() {
    const panel = document.getElementById('debug-log-panel');
    if (!panel || panel.classList.contains('hidden')) return;
    
    panel.innerHTML = logs.map(l => `
      <div class="log-entry ${l.level.toLowerCase()}">
        <span class="log-time">${new Date(l.timestamp).toLocaleTimeString()}</span>
        <span class="log-ctx">[${l.context}]</span>
        <span class="log-msg">${l.message}</span>
      </div>
    `).join('');
  }

  return {
    info: (ctx, msg, data) => log('INFO', ctx, msg, data),
    warn: (ctx, msg, data) => log('WARN', ctx, msg, data),
    error: (ctx, msg, data) => log('ERROR', ctx, msg, data),
    getLogs: () => [...logs],
    togglePanel: () => {
      let panel = document.getElementById('debug-log-panel');
      if (!panel) {
        panel = document.createElement('div');
        panel.id = 'debug-log-panel';
        panel.className = 'debug-panel hidden';
        document.body.appendChild(panel);
      }
      panel.classList.toggle('hidden');
      updateDebugPanel();
    }
  };
})();

// Add to global scope
window.AppLogger = AppLogger;

// Keyboard shortcut to toggle debug panel: Ctrl + Shift + L
window.addEventListener('keydown', (e) => {
  if (e.ctrlKey && e.shiftKey && e.key === 'L') {
    AppLogger.togglePanel();
  }
});
