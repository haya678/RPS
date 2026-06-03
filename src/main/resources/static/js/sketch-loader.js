/**
 * SketchLoader — Reusable SVG circle-draw animation component
 * 
 * Provides a pencil-draw animation in which a circle outline is progressively drawn
 * and then erased in a continuous loop. Respects prefers-reduced-motion by displaying
 * a static fully-drawn circle when motion should be reduced.
 */
const SketchLoader = (function() {
  'use strict';

  /**
   * Mount a sketch-loader SVG into the given container.
   * Replaces any existing content in the container.
   * 
   * @param {Element} container - DOM element to mount the SVG into
   * @param {object} [opts] - Optional configuration
   * @param {number} [opts.size=64] - SVG size in pixels
   * @param {number} [opts.strokeWidth=3.5] - Circle stroke width
   * @param {string} [opts.color='#163e79'] - Circle stroke color (default: INK)
   * @returns {{ destroy: () => void }} Handle with destroy method to remove the SVG
   */
  function mount(container, opts) {
    if (!container) {
      return { destroy: function() {} };
    }

    // Default options
    const options = {
      size: opts?.size ?? 64,
      strokeWidth: opts?.strokeWidth ?? 3.5,
      color: opts?.color ?? '#163e79'
    };

    // Calculate circle radius and circumference
    // Formula: r = (size/2) - strokeWidth - 2
    const radius = (options.size / 2) - options.strokeWidth - 2;
    const circumference = 2 * Math.PI * radius;

    // Check for reduced motion preference
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Create SVG element
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('class', 'sketch-loader-svg');
    svg.setAttribute('width', options.size);
    svg.setAttribute('height', options.size);
    svg.setAttribute('viewBox', `0 0 ${options.size} ${options.size}`);
    svg.style.setProperty('--sl-circ', circumference);

    // Create circle element
    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    circle.setAttribute('class', 'sketch-loader-circle');
    circle.setAttribute('cx', options.size / 2);
    circle.setAttribute('cy', options.size / 2);
    circle.setAttribute('r', radius);
    circle.setAttribute('stroke', options.color);
    circle.setAttribute('stroke-width', options.strokeWidth);
    circle.setAttribute('stroke-dasharray', circumference);

    // If reduced motion is active, render static fully-drawn circle
    if (prefersReducedMotion) {
      circle.setAttribute('stroke-dashoffset', '0');
      circle.style.animation = 'none';
    }

    // Assemble and mount
    svg.appendChild(circle);
    container.innerHTML = '';
    container.appendChild(svg);

    // Return handle with destroy method
    return {
      destroy: function() {
        if (svg.parentNode === container) {
          container.removeChild(svg);
        }
      }
    };
  }

  // Public API
  return {
    mount: mount
  };
})();
