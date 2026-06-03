/**
 * SketchLoader — Pencil draws a circle, eraser wipes it, repeat.
 *
 * A small SVG icon of a pencil orbits the circle as it draws,
 * then a small eraser icon follows as it undraws. The whole thing
 * loops smoothly with requestAnimationFrame so it stays in sync
 * regardless of CSS animation drift.
 */
const SketchLoader = (function () {
  'use strict';

  const INK = '#163e79';
  const INK2 = '#3d69ad';

  /* ── tiny inline SVG icons ──────────────────────────── */
  function pencilSvg() {
    return `<svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">
      <rect x="2" y="12" width="7" height="4" rx="1"
            fill="${INK2}" stroke="${INK}" stroke-width="1"/>
      <polygon points="2,12 9,12 9,8" fill="${INK}" opacity="0.25"/>
      <rect x="9" y="9" width="8" height="4" rx="1"
            fill="${INK}" stroke="${INK}" stroke-width="0.8"/>
      <polygon points="17,9 17,13 20,11" fill="${INK}" opacity="0.7"/>
    </svg>`;
  }

  function eraserSvg() {
    return `<svg viewBox="0 0 20 14" width="18" height="14" aria-hidden="true">
      <rect x="1" y="2" width="18" height="10" rx="2"
            fill="#f4f0e8" stroke="${INK}" stroke-width="1"/>
      <rect x="1" y="2" width="7" height="10" rx="2 0 0 2"
            fill="${INK2}" opacity="0.55"/>
      <line x1="8" y1="2" x2="8" y2="12"
            stroke="${INK}" stroke-width="0.8"/>
    </svg>`;
  }

  /**
   * Mount the sketch-loader into `container`.
   * @param {Element} container
   * @param {{ size?: number, strokeWidth?: number, color?: string }} [opts]
   * @returns {{ destroy: () => void }}
   */
  function mount(container, opts) {
    if (!container) return { destroy: function () {} };

    const size        = opts?.size        ?? 72;
    const sw          = opts?.strokeWidth ?? 3.5;
    const color       = opts?.color       ?? INK;
    const radius      = size / 2 - sw - 4;
    const cx          = size / 2;
    const cy          = size / 2;
    const circ        = 2 * Math.PI * radius;
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    /* ── wrapper div ──────────────────────────────────── */
    const wrap = document.createElement('div');
    wrap.className = 'sketch-loader-wrap';
    wrap.style.cssText = `
      position: relative;
      width: ${size}px;
      height: ${size}px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    `;

    /* ── SVG circle track ─────────────────────────────── */
    const svgNS = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(svgNS, 'svg');
    svg.setAttribute('width', size);
    svg.setAttribute('height', size);
    svg.setAttribute('viewBox', `0 0 ${size} ${size}`);
    svg.style.cssText = 'display:block;overflow:visible;';

    /* faint guide track */
    const track = document.createElementNS(svgNS, 'circle');
    track.setAttribute('cx', cx);
    track.setAttribute('cy', cy);
    track.setAttribute('r', radius);
    track.setAttribute('fill', 'none');
    track.setAttribute('stroke', INK2);
    track.setAttribute('stroke-width', sw * 0.35);
    track.setAttribute('opacity', '0.18');
    svg.appendChild(track);

    /* drawn arc — dashoffset driven by rAF */
    const arc = document.createElementNS(svgNS, 'circle');
    arc.setAttribute('cx', cx);
    arc.setAttribute('cy', cy);
    arc.setAttribute('r', radius);
    arc.setAttribute('fill', 'none');
    arc.setAttribute('stroke', color);
    arc.setAttribute('stroke-width', sw);
    arc.setAttribute('stroke-linecap', 'round');
    arc.setAttribute('stroke-dasharray', circ);
    /* start fully undrawn */
    arc.setAttribute('stroke-dashoffset', circ);
    /* SVG circles start at 3 o'clock; rotate to start at 12 o'clock */
    arc.setAttribute('transform', `rotate(-90 ${cx} ${cy})`);
    svg.appendChild(arc);

    wrap.appendChild(svg);

    /* ── pencil icon ──────────────────────────────────── */
    const pencil = document.createElement('div');
    pencil.innerHTML = pencilSvg();
    pencil.style.cssText = `
      position: absolute;
      pointer-events: none;
      transform-origin: center center;
      filter: drop-shadow(1px 1px 1px rgba(22,62,121,0.25));
      transition: opacity 0.12s;
    `;
    wrap.appendChild(pencil);

    /* ── eraser icon ──────────────────────────────────── */
    const eraser = document.createElement('div');
    eraser.innerHTML = eraserSvg();
    eraser.style.cssText = `
      position: absolute;
      pointer-events: none;
      transform-origin: center center;
      filter: drop-shadow(1px 1px 1px rgba(22,62,121,0.18));
      opacity: 0;
      transition: opacity 0.12s;
    `;
    wrap.appendChild(eraser);

    container.innerHTML = '';
    container.appendChild(wrap);

    /* ── reduced-motion fallback ──────────────────────── */
    if (reducedMotion) {
      arc.setAttribute('stroke-dashoffset', '0');
      pencil.style.display = 'none';
      eraser.style.display = 'none';
      return { destroy };
    }

    /* ── animation loop ───────────────────────────────── */
    /*
     * Phase 0 (0 → 0.5): pencil draws  dashoffset circ → 0
     * Phase 1 (0.5 → 1): eraser erases dashoffset 0 → circ
     * cycleDuration in ms
     */
    const CYCLE = 2000; // ms per full draw+erase cycle
    let startTime = null;
    let rafId = null;
    let destroyed = false;

    function positionIcon(el, angleDeg, extraRot) {
      /* angle is in degrees measured clockwise from 12 o'clock */
      const rad = (angleDeg - 90) * (Math.PI / 180);
      const ix  = cx + radius * Math.cos(rad);
      const iy  = cy + radius * Math.sin(rad);
      /* place top-left corner of icon so it's centred on (ix,iy) */
      const w = el.firstChild.getAttribute('width')  || 18;
      const h = el.firstChild.getAttribute('height') || 18;
      el.style.left = `${ix - w / 2}px`;
      el.style.top  = `${iy - h / 2}px`;
      /* rotate so the icon is tangent to the circle */
      el.style.transform = `rotate(${angleDeg + extraRot}deg)`;
    }

    function tick(ts) {
      if (destroyed) return;
      if (!startTime) startTime = ts;
      const elapsed = (ts - startTime) % CYCLE;
      const t = elapsed / CYCLE; /* 0 → 1 */

      let offset, pencilAngle, eraserAngle;

      if (t < 0.5) {
        /* drawing phase */
        const p = t / 0.5;                   /* 0→1 within draw phase */
        const eased = easeInOut(p);
        offset = circ * (1 - eased);         /* circ → 0 */
        pencilAngle = 360 * eased;            /* 0° → 360° */

        pencil.style.opacity = '1';
        eraser.style.opacity = '0';
        positionIcon(pencil, pencilAngle, 0);
      } else {
        /* erasing phase */
        const p = (t - 0.5) / 0.5;
        const eased = easeInOut(p);
        offset = circ * eased;               /* 0 → circ */
        eraserAngle = 360 * (1 - eased);     /* 360° → 0° (follows drawn arc end) */

        pencil.style.opacity = '0';
        eraser.style.opacity = '1';
        positionIcon(eraser, eraserAngle, 10);
      }

      arc.setAttribute('stroke-dashoffset', offset);
      rafId = requestAnimationFrame(tick);
    }

    rafId = requestAnimationFrame(tick);

    function destroy() {
      destroyed = true;
      if (rafId) cancelAnimationFrame(rafId);
      if (wrap.parentNode === container) container.removeChild(wrap);
    }

    return { destroy };
  }

  function easeInOut(t) {
    return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
  }

  return { mount };
})();
