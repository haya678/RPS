const fc = require('fast-check');
const { JSDOM } = require('jsdom');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

// Load the SketchLoader code
const sketchLoaderCode = fs.readFileSync(
  path.join(__dirname, '../../../src/main/resources/static/js/sketch-loader.js'),
  'utf8'
);

function setupDOM() {
  const dom = new JSDOM('<!DOCTYPE html><html><body><div id="container"></div></body></html>');
  
  // Create a context with all necessary globals
  const context = {
    window: dom.window,
    document: dom.window.document,
    navigator: dom.window.navigator,
    console: console,
    setTimeout: setTimeout,
    clearTimeout: clearTimeout,
    requestAnimationFrame: (callback) => setTimeout(callback, 16),
    cancelAnimationFrame: (id) => clearTimeout(id),
    Math: Math,
    parseFloat: parseFloat,
    Node: dom.window.Node,
    Element: dom.window.Element,
    SVGElement: dom.window.SVGElement
  };
  
  // Mock matchMedia on the window object
  dom.window.matchMedia = function() {
    return {
      matches: false,
      addListener: function() {},
      removeListener: function() {}
    };
  };

  vm.createContext(context);
  
  // Wrap code to ensure SketchLoader is assigned to the context
  const wrappedCode = sketchLoaderCode.replace('const SketchLoader =', 'this.SketchLoader =');
  vm.runInContext(wrappedCode, context);
  
  return { 
    container: context.document.getElementById('container'), 
    SketchLoader: context.SketchLoader,
    context
  };
}

describe('SketchLoader Properties', () => {
  test('Property 1: Circumference invariant', () => {
    const { container, SketchLoader } = setupDOM();
    
    fc.assert(
      fc.property(
        fc.record({
          size: fc.integer({ min: 16, max: 512 }),
          strokeWidth: fc.double({ min: 0.5, max: 10 })
        }),
        (opts) => {
          const handle = SketchLoader.mount(container, opts);
          const svg = container.querySelector('svg');
          const circles = svg.querySelectorAll('circle');
          const arc = circles[1]; // The second circle is the drawn arc
          
          const size = opts.size || 72;
          const sw = opts.strokeWidth || 3.5;
          const radius = size / 2 - sw - 4;
          const expectedCirc = 2 * Math.PI * radius;
          
          const actualCirc = parseFloat(arc.getAttribute('stroke-dasharray'));
          
          handle.destroy();
          
          // Epsilon check for floating point
          return Math.abs(actualCirc - expectedCirc) < 0.001;
        }
      )
    );
  });

  test('Property 2: Reduced-motion behavior', () => {
    const { container, SketchLoader, context } = setupDOM();
    
    // Override matchMedia to simulate reduced motion
    context.window.matchMedia = () => ({ matches: true });
    
    fc.assert(
      fc.property(
        fc.record({
          size: fc.integer({ min: 16, max: 512 })
        }),
        (opts) => {
          const handle = SketchLoader.mount(container, opts);
          const svg = container.querySelector('svg');
          const arc = svg.querySelectorAll('circle')[1];
          
          const offset = arc.getAttribute('stroke-dashoffset');
          const pencil = container.querySelector('.sketch-loader-wrap > div:nth-child(2)');
          const eraser = container.querySelector('.sketch-loader-wrap > div:nth-child(3)');
          
          const isCorrect = offset === '0' && 
                            pencil.style.display === 'none' && 
                            eraser.style.display === 'none';
          
          handle.destroy();
          return isCorrect;
        }
      )
    );
  });

  test('Property 3: Destroy cleanup', () => {
    const { container, SketchLoader } = setupDOM();
    
    fc.assert(
      fc.property(
        fc.integer({ min: 16, max: 512 }),
        (size) => {
          const handle = SketchLoader.mount(container, { size });
          if (container.children.length === 0) return false;
          
          handle.destroy();
          return container.children.length === 0;
        }
      )
    );
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
