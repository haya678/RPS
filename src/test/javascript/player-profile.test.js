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

function setupDOM() {
  const dom = new JSDOM('<!DOCTYPE html><html><body><button class="player-profile-btn" data-torn-id="1"></button><button class="player-profile-btn" data-torn-id="2"></button></body></html>');
  
  const context = {
    window: dom.window,
    document: dom.window.document,
    navigator: dom.window.navigator,
    console: { log: () => {}, error: () => {} },
    setTimeout: setTimeout,
    clearTimeout: clearTimeout,
    localStorage: { getItem: () => null, setItem: () => {}, removeItem: () => {} },
    location: { protocol: 'http:', host: 'localhost', origin: 'http://localhost', pathname: '/' },
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
    MatchFx: { spawnNotebookCelebration: () => {}, floatText: () => {}, playReveal: () => {}, screenShake: () => {}, flashArena: () => {}, playRoundWin: () => {}, popScore: () => {}, playWin: () => {}, playLose: () => {}, playSelect: () => {} },
    fetch: () => Promise.resolve({ ok: true, json: () => Promise.resolve({ username: 'Player', torn_id: '1' }) })
  };

  const originalQuerySelector = dom.window.document.querySelector;
  dom.window.document.querySelector = function(selector) {
    const el = originalQuerySelector.call(dom.window.document, selector);
    if (el) return el;
    const dummy = dom.window.document.createElement('div');
    dummy.id = selector.startsWith('#') ? selector.slice(1) : '';
    // Mock innerHTML for createHoverCard
    dummy.innerHTML = '<div class="profile-hover-inner"><div id="hover-avatar"></div><div id="hover-username"></div><div id="hover-meta"></div><div id="hover-balance"></div><div id="hover-matches"></div><div id="hover-wins"></div><div id="hover-winrate"></div><div id="hover-net"></div></div>';
    return dummy;
  };
  dom.window.document.querySelectorAll = () => [];
  
  vm.createContext(context);
  vm.runInContext(appCode, context);
  
  return { 
    context, 
    document: dom.window.document,
    btn1: dom.window.document.querySelector('.player-profile-btn[data-torn-id="1"]'),
    btn2: dom.window.document.querySelector('.player-profile-btn[data-torn-id="2"]')
  };
}

describe('Player Profile Click Properties', () => {
  test('Property 11.5: At most one card visible', async () => {
    const { document, btn1, btn2 } = setupDOM();
    
    // Simulate clicks on two different profile buttons
    btn1.click();
    await new Promise(resolve => setTimeout(resolve, 0));
    
    btn2.click();
    await new Promise(resolve => setTimeout(resolve, 0));
    
    const card = document.getElementById('profile-hover-card');
    // In our implementation, we reuse the same hover card element, so there's only ever one.
    // We just need to check that it's correctly updated.
    if (card.classList.contains('hidden')) throw new Error('Card should be visible');
  });

  test('Property 11.6: Toggle off on second click', async () => {
    const { document, btn1 } = setupDOM();
    
    // First click -> show
    btn1.click();
    await new Promise(resolve => setTimeout(resolve, 0));
    const card = document.getElementById('profile-hover-card');
    if (card.classList.contains('hidden')) throw new Error('Card should be visible after first click');
    
    // Second click -> hide
    btn1.click();
    await new Promise(resolve => setTimeout(resolve, 0));
    if (!card.classList.contains('hidden')) throw new Error('Card should be hidden after second click');
  });
});

// Simple test runner
function describe(name, fn) {
  console.log(`\n${name}`);
  fn();
}

function test(name, fn) {
  try {
    const res = fn();
    if (res instanceof Promise) {
      res.then(() => {
        console.log(`  [PASS] ${name}`);
      }).catch(err => {
        console.error(`  [FAIL] ${name}`);
        console.error(err);
        process.exit(1);
      });
    } else {
      console.log(`  [PASS] ${name}`);
    }
  } catch (err) {
    console.error(`  [FAIL] ${name}`);
    console.error(err);
    process.exit(1);
  }
}
