const fc = require('fast-check');
const { JSDOM } = require('jsdom');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

// Load the app.js code
const appCode = fs.readFileSync(
  path.join(__dirname, '../../../src/main/resources/static/js/app.js'),
  'utf8'
);

function setupDOM(search = '') {
  const dom = new JSDOM('<!DOCTYPE html><html><body><div id="return-match-overlay" class="hidden"></div></body></html>');
  
  const storage = {};
  const localStorage = {
    getItem: (key) => storage[key] || null,
    setItem: (key, value) => { storage[key] = String(value); },
    removeItem: (key) => { delete storage[key]; },
    clear: () => { for (let key in storage) delete storage[key]; }
  };

  const context = {
    window: dom.window,
    document: dom.window.document,
    navigator: dom.window.navigator,
    console: { log: () => {}, error: () => {} },
    setTimeout: setTimeout,
    clearTimeout: clearTimeout,
    localStorage: localStorage,
    location: { protocol: 'http:', host: 'localhost', origin: 'http://localhost', pathname: '/', search: search },
    URLSearchParams: dom.window.URLSearchParams,
    Math: Math,
    Date: Date,
    JSON: JSON,
    Number: Number,
    String: String,
    Promise: Promise,
    history: { replaceState: () => {} },
    // Mock other globals used in app.js
    MoolaIcon: { mountIcons: () => {}, amountHtml: (a) => a, setBalanceElement: () => {}, iconHtml: () => '' },
    RpsSketch: { mountChoiceButtons: () => {}, mountSnowflakes: () => {} },
    Rps3D: { init3DButton: () => {} },
    SketchLoader: { mount: () => ({ destroy: () => {} }) },
    MatchFx: { spawnNotebookCelebration: () => {}, floatText: () => {}, playReveal: () => {}, screenShake: () => {}, flashArena: () => {}, playRoundWin: () => {}, popScore: () => {}, playWin: () => {}, playLose: () => {}, playSelect: () => {} }
  };

  const originalQuerySelector = dom.window.document.querySelector;
  dom.window.document.querySelector = function(selector) {
    const el = originalQuerySelector.call(dom.window.document, selector);
    if (el) return el;
    const dummy = dom.window.document.createElement('div');
    dummy.id = selector.startsWith('#') ? selector.slice(1) : '';
    return dummy;
  };
  dom.window.document.querySelectorAll = () => [];
  
  vm.createContext(context);
  vm.runInContext(appCode, context);
  
  return { context, storage, overlay: dom.window.document.getElementById('return-match-overlay') };
}

describe('Return-to-Match Modal Properties', () => {
  test('Property 7.6: Modal <-> localStorage consistency', () => {
    const { context, storage, overlay } = setupDOM();
    
    // Simulate setting active match
    context.setActiveHouseMatch('ROOM-123');
    context.checkAndShowReturnMatchModal();
    if (overlay.classList.contains('hidden')) throw new Error('Modal should be visible after checkAndShowReturnMatchModal');
    
    context.clearActiveHouseMatch();
    context.hideReturnMatchModal();
    if (!overlay.classList.contains('hidden')) throw new Error('Modal should be hidden after hideReturnMatchModal');
    
    console.log('  [PASS] Property 7.6: Modal <-> localStorage consistency (manual check)');
  });

  test('Property 7.7: No modal in match-only tab', () => {
    const { context, storage, overlay } = setupDOM('?match=ROOM-123');
    
    // Even if we have an active match in storage, it shouldn't show in match-only mode
    storage['activeHouseMatch'] = JSON.stringify({ roomId: 'ROOM-123' });
    context.checkAndShowReturnMatchModal();
    
    if (!overlay.classList.contains('hidden')) {
      throw new Error('Modal should NOT be shown in match-only mode');
    }
  });
});

// Simple test runner
function describe(name, fn) {
  console.log(`\n${name}`);
  fn();
}

function test(name, fn) {
  try {
    fn();
    console.log(`  [PASS] ${name}`);
  } catch (err) {
    console.error(`  [FAIL] ${name}`);
    console.error(err);
    process.exit(1);
  }
}
