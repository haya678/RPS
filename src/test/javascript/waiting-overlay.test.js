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
  const dom = new JSDOM('<!DOCTYPE html><html><body><div id="match-tab-waiting" class="hidden"></div><div id="match-tab-waiting-room"></div><button class="choice-btn"></button><button class="choice-btn"></button><button class="choice-btn"></button></body></html>');
  
  const storage = {};
  const context = {
    window: dom.window,
    document: dom.window.document,
    navigator: dom.window.navigator,
    console: { log: () => {}, error: () => {} },
    setTimeout: setTimeout,
    clearTimeout: clearTimeout,
    localStorage: {
      getItem: (key) => storage[key] || null,
      setItem: (key, value) => { storage[key] = String(value); },
      removeItem: (key) => { delete storage[key]; }
    },
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
  const originalQuerySelectorAll = dom.window.document.querySelectorAll;
  dom.window.document.querySelectorAll = function(selector) {
    const els = originalQuerySelectorAll.call(dom.window.document, selector);
    if (els && els.length > 0) return els;
    return [];
  };
  
  vm.createContext(context);
  vm.runInContext(appCode, context);
  
  return { 
    context, 
    storage, 
    overlay: dom.window.document.getElementById('match-tab-waiting'),
    btns: dom.window.document.querySelectorAll('.choice-btn')
  };
}

describe('Waiting Overlay Properties', () => {
  test('Property 9.3: Waiting overlay disables choices', () => {
    const { context, overlay, btns } = setupDOM('?match=ROOM-123');
    
    context.showMatchTabWaiting('ROOM-123');
    
    if (overlay.classList.contains('hidden')) throw new Error('Overlay should be visible');
    
    const allDisabled = Array.from(btns).every(b => b.disabled);
    if (!allDisabled) throw new Error('All choice buttons should be disabled');
  });

  test('Property 9.4: Waiting overlay cleared on start', () => {
    const { context, overlay, btns } = setupDOM('?match=ROOM-123');
    
    context.showMatchTabWaiting('ROOM-123');
    context.hideMatchTabWaiting();
    
    if (!overlay.classList.contains('hidden')) throw new Error('Overlay should be hidden');
    // Note: hideMatchTabWaiting itself doesn't re-enable choices, matchStarted does.
    // In app.js:
    // case 'matchStarted':
    //   hideMatchTabWaiting();
    //   ...
    //   enableChoices(true);
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
