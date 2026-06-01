/**
 * Hand-drawn rock / paper / scissors SVGs and flipbook reveal animation.
 */
const RpsSketch = (() => {
  const INK = '#163e79';
  const INK2 = '#3d69ad';
  const SVG_NS = 'http://www.w3.org/2000/svg';

  const FINAL = {
    rock: `<svg class="rps-sketch" xmlns="${SVG_NS}" viewBox="0 0 80 80" aria-hidden="true">
      <path d="M18 42 Q14 28 26 22 Q38 16 48 24 Q56 18 62 28 Q68 38 62 50 Q58 62 44 64 L36 68 Q28 66 22 58 Q16 52 18 42Z" fill="none" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M26 38 L30 52 M34 36 L36 54 M42 34 L42 56 M50 36 L48 54" fill="none" stroke="${INK2}" stroke-width="1.6" stroke-linecap="round"/>
      <path d="M20 48 Q24 56 32 58" fill="none" stroke="${INK}" stroke-width="1.4" stroke-linecap="round"/>
    </svg>`,
    paper: `<svg class="rps-sketch" xmlns="${SVG_NS}" viewBox="0 0 80 80" aria-hidden="true">
      <path d="M22 58 L22 28 Q22 18 32 16 L52 14 Q62 14 64 24 L66 48 Q66 58 56 60 L34 64 Q24 64 22 58Z" fill="rgba(240,246,255,0.9)" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M30 24 L30 56 M38 22 L38 58 M46 22 L46 56 M54 24 L54 52" fill="none" stroke="${INK2}" stroke-width="1.5" stroke-linecap="round"/>
      <path d="M18 32 Q20 26 26 24" fill="none" stroke="${INK}" stroke-width="1.3" stroke-linecap="round"/>
    </svg>`,
    scissors: `<svg class="rps-sketch" xmlns="${SVG_NS}" viewBox="0 0 80 80" aria-hidden="true">
      <path d="M22 58 Q18 50 20 42 Q24 34 32 30 L38 28 Q48 26 54 32 Q60 38 58 48 Q56 58 46 64 Q36 68 28 64 Q22 62 22 58Z" fill="rgba(240,246,255,0.88)" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M32 30 Q28 16 34 10 Q38 6 42 12 L40 30" fill="rgba(240,246,255,0.88)" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M40 30 Q44 14 50 8 Q56 6 60 14 L54 32" fill="rgba(240,246,255,0.88)" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M26 46 Q32 40 40 42 Q48 40 54 46" fill="none" stroke="${INK2}" stroke-width="1.5" stroke-linecap="round"/>
      <path d="M30 52 Q36 48 42 50 Q48 52 52 48" fill="none" stroke="${INK2}" stroke-width="1.4" stroke-linecap="round"/>
      <path d="M34 36 L38 22" fill="none" stroke="${INK2}" stroke-width="1.2" stroke-linecap="round" opacity="0.55"/>
      <path d="M42 34 L48 20" fill="none" stroke="${INK2}" stroke-width="1.2" stroke-linecap="round" opacity="0.55"/>
    </svg>`
  };

  const SNOWFLAKE = `<svg class="baining-snowflake" xmlns="${SVG_NS}" viewBox="0 0 24 24" aria-hidden="true">
    <path d="M12 2 L12 22 M2 12 L22 12 M5 5 L19 19 M19 5 L5 19" fill="none" stroke="${INK}" stroke-width="1.5" stroke-linecap="round"/>
    <path d="M12 5 L10 8 L12 10 L14 8 Z M12 14 L10 17 L12 19 L14 17 Z M5 12 L8 10 L10 12 L8 14 Z M14 12 L17 10 L19 12 L17 14 Z" fill="none" stroke="${INK2}" stroke-width="1.1" stroke-linejoin="round"/>
    <circle cx="12" cy="12" r="1.2" fill="${INK}"/>
  </svg>`;

  const CYCLE = {
    rock: ['paper', 'scissors', 'rock', 'paper', 'rock'],
    paper: ['scissors', 'rock', 'paper', 'rock', 'paper'],
    scissors: ['rock', 'paper', 'scissors', 'paper', 'scissors']
  };

  function hasSketch(choice) {
    return Boolean(FINAL[choice]);
  }

  function withXmlns(svg) {
    if (!svg || svg.includes('xmlns=')) return svg;
    return svg.replace('<svg ', `<svg xmlns="${SVG_NS}" `);
  }

  function sketchHtml(choice) {
    return withXmlns(FINAL[choice] || FINAL.rock);
  }

  function mountSketch(el, choice) {
    if (!el) return;
    el.innerHTML = sketchHtml(choice);
  }

  function mountChoiceButtons() {
    document.querySelectorAll('.choice-btn[data-choice]').forEach((btn) => {
      const choice = btn.dataset.choice;
      const host = btn.querySelector('.choice-sketch');
      if (host) mountSketch(host, choice);
    });
    document.querySelectorAll('.sketch-flipbook-host[data-choice]').forEach((host) => {
      mountSketch(host, host.dataset.choice);
    });
  }

  function mountSnowflakes() {
    document.querySelectorAll('[data-baining-snowflake]').forEach((slot) => {
      slot.innerHTML = SNOWFLAKE;
    });
  }

  function runFlipbook(hostEl, finalChoice, options = {}) {
    const {
      frameMs = 95,
      cycles = 5,
      onFrame = null,
      onDone = null
    } = options;

    if (!hostEl || !hasSketch(finalChoice)) {
      if (onDone) onDone();
      return;
    }

    const sequence = CYCLE[finalChoice] || CYCLE.rock;
    let i = 0;
    const total = cycles;

    function tick() {
      const choice = i < total - 1 ? sequence[i % sequence.length] : finalChoice;
      hostEl.innerHTML = sketchHtml(choice);
      hostEl.classList.add('flip-frame-pop');
      setTimeout(() => hostEl.classList.remove('flip-frame-pop'), frameMs * 0.8);
      if (onFrame) onFrame(choice, i);
      i += 1;
      if (i < total) {
        setTimeout(tick, frameMs);
      } else {
        hostEl.classList.add('flip-landed');
        if (onDone) onDone();
      }
    }

    hostEl.classList.remove('flip-landed');
    tick();
  }

  function runRevealFlipbook(slotEl, finalChoice) {
    return new Promise((resolve) => {
      let host = slotEl?.querySelector('.sketch-flipbook-host');
      if (!host && slotEl?.classList?.contains('sketch-flipbook-host')) {
        host = slotEl;
      }
      const label = slotEl?.querySelector('.choice-label');
      if (!host) {
        resolve();
        return;
      }
      // Faster cycles for smoother flipbook, slower frame timing for naturality
      runFlipbook(host, finalChoice, {
        frameMs: 65,
        cycles: 9,
        onDone: () => {
          if (label) {
            const names = { rock: 'Rock', paper: 'Paper', scissors: 'Scissors' };
            label.textContent = names[finalChoice] || '';
          }
          host.classList.add('flip-settled');
          resolve();
        }
      });
    });
  }

  return {
    sketchHtml,
    mountSketch,
    mountChoiceButtons,
    mountSnowflakes,
    runFlipbook,
    runRevealFlipbook
  };
})();
