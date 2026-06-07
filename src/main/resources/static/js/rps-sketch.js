/**
 * Hand-drawn rock / paper / scissors SVGs and flipbook reveal animation.
 */
const RpsSketch = (() => {
  const INK = '#163e79';
  const INK2 = '#3d69ad';
  const SVG_NS = 'http://www.w3.org/2000/svg';

  const FINAL = {
    rock: `<svg class="rps-sketch" xmlns="${SVG_NS}" viewBox="0 0 80 80" aria-hidden="true">
      <!-- Wrist/Hand base -->
      <path d="M32 68 Q40 64 48 68" fill="none" stroke="${INK}" stroke-width="2" stroke-linecap="round"/>
      <!-- Fist outline -->
      <path d="M28 62 Q22 55 24 42 Q26 30 36 26 Q46 22 56 28 Q66 35 64 50 Q62 62 52 65 Q42 68 32 62 Z" fill="rgba(240,246,255,0.9)" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <!-- Knuckles/Fingers -->
      <path d="M36 26 Q38 38 36 48 M46 24 Q48 36 46 46 M56 28 Q58 40 56 50" fill="none" stroke="${INK2}" stroke-width="1.8" stroke-linecap="round" opacity="0.8"/>
      <!-- Thumb curled over -->
      <path d="M24 42 Q32 48 44 46" fill="none" stroke="${INK}" stroke-width="2.2" stroke-linecap="round"/>
    </svg>`,
    paper: `<svg class="rps-sketch" xmlns="${SVG_NS}" viewBox="0 0 80 80" aria-hidden="true">
      <!-- Wrist -->
      <path d="M35 72 Q40 70 45 72" fill="none" stroke="${INK}" stroke-width="2" stroke-linecap="round"/>
      <!-- Palm and fingers -->
      <path d="M32 65 Q28 58 30 48 L26 22 Q26 16 32 16 Q38 16 38 24 L38 42 M38 42 L42 12 Q42 6 48 6 Q54 6 54 14 L54 42 M54 42 L58 16 Q58 10 64 10 Q70 10 70 18 L70 45 M70 45 L74 30 Q74 24 80 24 Q84 24 84 34 L80 58 Q78 68 60 72 Q45 74 32 65 Z" fill="rgba(240,246,255,0.9)" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <!-- Thumb -->
      <path d="M30 48 Q22 42 16 46 Q10 50 18 56 Q25 62 32 60" fill="rgba(240,246,255,0.9)" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>`,
    scissors: `<svg class="rps-sketch" xmlns="${SVG_NS}" viewBox="0 0 80 80" aria-hidden="true">
      <!-- Wrist -->
      <path d="M35 72 Q40 70 45 72" fill="none" stroke="${INK}" stroke-width="2" stroke-linecap="round"/>
      <!-- Base of hand with folded fingers -->
      <path d="M32 65 Q28 58 30 48 Q40 42 55 45 Q65 48 68 58 Q70 68 55 72 Q42 74 32 65 Z" fill="rgba(240,246,255,0.9)" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <!-- Index Finger (V) -->
      <path d="M38 42 L44 14 Q46 8 52 10 Q58 12 54 20 L48 45" fill="rgba(240,246,255,0.9)" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <!-- Middle Finger (V) -->
      <path d="M52 45 L66 22 Q68 16 74 18 Q80 20 76 28 L62 52" fill="rgba(240,246,255,0.9)" stroke="${INK}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <!-- Thumb folded -->
      <path d="M30 48 Q24 50 26 58 Q28 62 35 58" fill="none" stroke="${INK}" stroke-width="2.2" stroke-linecap="round"/>
      <!-- Lines for other folded fingers -->
      <path d="M50 48 Q55 52 58 58 M58 52 Q62 55 64 60" fill="none" stroke="${INK2}" stroke-width="1.5" stroke-linecap="round" opacity="0.7"/>
    </svg>`
  };

  const SNOWFLAKE = `<svg class="baining-snowflake" xmlns="${SVG_NS}" viewBox="0 0 24 24" aria-hidden="true">
    <path d="M12 2 L12 22 M2 12 L22 12 M5 5 L19 19 M19 5 L5 19" fill="none" stroke="${INK}" stroke-width="1.5" stroke-linecap="round"/>
    <path d="M12 5 L10 8 L12 10 L14 8 Z M12 14 L10 17 L12 19 L14 17 Z M5 12 L8 10 L10 12 L8 14 Z M14 12 L17 10 L19 12 L17 14 Z" fill="none" stroke="${INK2}" stroke-width="1.1" stroke-linejoin="round"/>
    <circle cx="12" cy="12" r="1.2" fill="${INK}"/>
  </svg>`;

  const CYCLE = {
    rock: ['paper', 'scissors', 'rock', 'paper', 'scissors', 'rock'],
    paper: ['scissors', 'rock', 'paper', 'scissors', 'rock', 'paper'],
    scissors: ['rock', 'paper', 'scissors', 'rock', 'paper', 'scissors']
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
