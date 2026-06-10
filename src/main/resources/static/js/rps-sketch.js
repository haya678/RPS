/**
 * Hand-drawn rock / paper / scissors SVGs and flipbook reveal animation.
 */
const RpsSketch = (() => {
  const INK = '#163e79';
  const INK2 = '#3d69ad';
  const SVG_NS = 'http://www.w3.org/2000/svg';

  const FINAL = {
    rock: `<svg class="rps-sketch" xmlns="${SVG_NS}" viewBox="0 0 100 100" aria-hidden="true">
      <path d="M20 75 Q15 65 15 50 Q15 35 25 25 Q35 20 55 20 Q75 20 85 30 Q92 40 92 60 Q92 75 80 85 Q60 90 20 75 Z" fill="#ffffff" stroke="#163e79" stroke-width="2"/>
      <path d="M30 50 L50 52 M55 45 L75 48 M35 65 L55 68" fill="none" stroke="#163e79" opacity="0.4"/>
      <path d="M15 55 Q30 50 48 55 Q60 65 55 85" fill="none" stroke="#163e79" stroke-width="3"/>
    </svg>`,
    paper: `<svg class="rps-sketch" xmlns="${SVG_NS}" viewBox="0 0 100 100" aria-hidden="true">
      <path d="M15 45 L45 25 L75 40 L45 65 Z" fill="#ffffff" stroke="#163e79" stroke-width="1"/>
      <path d="M50 20 L85 15 L95 50 L60 60 Z" fill="#f0f0f0" stroke="#163e79" stroke-width="1"/>
      <path d="M25 70 L65 65 L85 95 L35 98 Z" fill="#e8e8e8" stroke="#163e79" stroke-width="1"/>
      <path d="M12 25 Q18 12 24 18 Q30 24 18 30 Q12 24 12 25" fill="#aecbf7" stroke="#163e79" stroke-width="0.8"/>
    </svg>`,
    scissors: `<svg class="rps-sketch" xmlns="${SVG_NS}" viewBox="0 0 100 100" aria-hidden="true">
      <path d="M50 95 L38 60 L48 5 L52 5 L62 60 Z" fill="#ffffff" stroke="#163e79" stroke-width="1.5"/>
      <path d="M45 45 L5 25 L12 70 L40 55" fill="#ffffff" stroke="#163e79" stroke-width="1.8"/>
      <path d="M54 45 L95 25 L88 70 L60 55" fill="#ffffff" stroke="#163e79" stroke-width="1.8"/>
      <rect x="38" y="55" width="24" height="6" rx="1" fill="#ffffff" stroke="#163e79" stroke-width="1"/>
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
          
          // Switch to 3D reveal if Rps3D is available
          if (typeof Rps3D !== 'undefined' && host) {
            Rps3D.init3DReveal(host, finalChoice);
          }
          
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
