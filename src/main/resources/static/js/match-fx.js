/**
 * Match visuals: notebook doodles, Torn notes, confetti, screen shake, Web Audio SFX.
 */
const MatchFx = (() => {
  const INK = 'rgba(22, 62, 121, 0.55)';
  const INK_LIGHT = 'rgba(174, 203, 247, 0.45)';
  const INK_FAINT = 'rgba(22, 62, 121, 0.18)';
  const INK_NOTE = 'rgba(22, 62, 121, 0.42)';

  const TORN_NOTES = [
    'deposit msg must say RPS !!!',
    'xanax → moola (do the math)',
    'hospital in 3... 2...',
    'got mugged again lol',
    'RPS is a real job now',
    'tell faction: busy gambling',
    'energy refill when??',
    'don\'t spend all your xanax here',
    'winner buys round at Sweet Shop',
    'is this legal? ...probably not',
    'daily bonus > my salary',
    'travel to Torn City for this',
    'chain attack but emotionally',
    'bazaar listing: my dignity',
    'note to self: log out',
    'PO box full of scissors energy',
    'rank #1 in bad decisions',
    'caught in act: playing RPS',
    'fly to Hawaii (lose match)',
    'respect the rake (3%)'
  ];

  let audioCtx = null;

  function getAudio() {
    if (!audioCtx) {
      try {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      } catch (_) {
        return null;
      }
    }
    if (audioCtx.state === 'suspended') {
      audioCtx.resume().catch(() => {});
    }
    return audioCtx;
  }

  function playTone(freq, duration, type = 'sine', gain = 0.08, when = 0) {
    const ctx = getAudio();
    if (!ctx) return;
    const osc = ctx.createOscillator();
    const g = ctx.createGain();
    osc.type = type;
    osc.frequency.value = freq;
    g.gain.setValueAtTime(0, ctx.currentTime + when);
    g.gain.linearRampToValueAtTime(gain, ctx.currentTime + when + 0.02);
    g.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + when + duration);
    osc.connect(g);
    g.connect(ctx.destination);
    osc.start(ctx.currentTime + when);
    osc.stop(ctx.currentTime + when + duration + 0.05);
  }

  function hashSeed(str) {
    let h = 2166136261;
    for (let i = 0; i < str.length; i++) {
      h ^= str.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    return h >>> 0;
  }

  function mulberry32(seed) {
    let a = seed;
    return () => {
      a |= 0;
      a = (a + 0x6d2b79f5) | 0;
      let t = Math.imul(a ^ (a >>> 15), 1 | a);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function pickMany(rng, arr, count) {
    const copy = [...arr];
    const out = [];
    for (let i = 0; i < count && copy.length; i++) {
      const idx = Math.floor(rng() * copy.length);
      out.push(copy.splice(idx, 1)[0]);
    }
    return out;
  }

  function drawSquiggle(ctx, rng, w, h) {
    const x = rng() * w;
    const y = rng() * h;
    const len = 40 + rng() * 120;
    const angle = rng() * Math.PI * 2;
    ctx.beginPath();
    ctx.moveTo(x, y);
    for (let i = 1; i <= 8; i++) {
      const t = i / 8;
      ctx.lineTo(
        x + Math.cos(angle) * len * t + (rng() - 0.5) * 30,
        y + Math.sin(angle) * len * t + (rng() - 0.5) * 30
      );
    }
    ctx.stroke();
  }

  function drawStar(ctx, cx, cy, r, points) {
    ctx.beginPath();
    for (let i = 0; i < points * 2; i++) {
      const rad = (i * Math.PI) / points - Math.PI / 2;
      const dist = i % 2 === 0 ? r : r * 0.4;
      const x = cx + Math.cos(rad) * dist;
      const y = cy + Math.sin(rad) * dist;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.closePath();
    ctx.stroke();
  }

  function drawCloud(ctx, x, y, scale) {
    ctx.beginPath();
    ctx.arc(x, y, 12 * scale, 0, Math.PI * 2);
    ctx.arc(x + 14 * scale, y - 4 * scale, 10 * scale, 0, Math.PI * 2);
    ctx.arc(x + 26 * scale, y, 11 * scale, 0, Math.PI * 2);
    ctx.stroke();
  }

  function drawHeart(ctx, x, y, s) {
    ctx.beginPath();
    ctx.moveTo(x, y + s * 0.3);
    ctx.bezierCurveTo(x, y, x - s, y, x - s, y + s * 0.3);
    ctx.bezierCurveTo(x - s, y + s * 0.7, x, y + s, x, y + s * 1.3);
    ctx.bezierCurveTo(x, y + s, x + s, y + s * 0.7, x + s, y + s * 0.3);
    ctx.bezierCurveTo(x + s, y, x, y, x, y + s * 0.3);
    ctx.stroke();
  }

  function drawArrow(ctx, x1, y1, x2, y2) {
    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.quadraticCurveTo((x1 + x2) / 2 + 8, (y1 + y2) / 2 - 8, x2, y2);
    ctx.stroke();
    const angle = Math.atan2(y2 - y1, x2 - x1);
    ctx.beginPath();
    ctx.moveTo(x2, y2);
    ctx.lineTo(x2 - 8 * Math.cos(angle - 0.4), y2 - 8 * Math.sin(angle - 0.4));
    ctx.moveTo(x2, y2);
    ctx.lineTo(x2 - 8 * Math.cos(angle + 0.4), y2 - 8 * Math.sin(angle + 0.4));
    ctx.stroke();
  }

  function drawHandwrittenNote(ctx, text, x, y, maxWidth, angle, rng) {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(angle);
    ctx.fillStyle = INK_NOTE;
    ctx.font = `${13 + Math.floor(rng() * 4)}px "Gochi Hand", "Patrick Hand", cursive`;
    const words = text.split(' ');
    let line = '';
    let ly = 0;
    const lineHeight = 16;
    words.forEach((word, i) => {
      const test = line + word + ' ';
      if (ctx.measureText(test).width > maxWidth && line) {
        ctx.fillText(line.trim(), 0, ly);
        line = word + ' ';
        ly += lineHeight;
      } else {
        line = test;
      }
      if (i === words.length - 1) ctx.fillText(line.trim(), 0, ly);
    });
    ctx.beginPath();
    ctx.moveTo(0, ly + 6);
    for (let i = 0; i <= maxWidth; i += 8) {
      ctx.lineTo(i, ly + 6 + (rng() - 0.5) * 3);
    }
    ctx.strokeStyle = INK_LIGHT;
    ctx.lineWidth = 0.8;
    ctx.stroke();
    ctx.restore();
  }

  function drawXanax(ctx, x, y, s) {
    ctx.beginPath();
    ctx.ellipse(x, y, s * 1.4, s * 0.55, -0.3, 0, Math.PI * 2);
    ctx.stroke();
    ctx.font = `bold ${s * 1.1}px "Gochi Hand", cursive`;
    ctx.fillStyle = INK;
    ctx.fillText('X', x - s * 0.35, y + s * 0.2);
  }

  function drawMug(ctx, x, y) {
    ctx.beginPath();
    ctx.arc(x, y, 14, 0, Math.PI * 2);
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(x + 18, y - 2, 8, -Math.PI / 2, Math.PI / 2);
    ctx.stroke();
    ctx.font = '9px "Gochi Hand", cursive';
    ctx.fillStyle = INK_LIGHT;
    ctx.fillText('$', x - 3, y + 4);
  }

  function drawDice(ctx, x, y) {
    ctx.strokeRect(x - 12, y - 12, 24, 24);
    ctx.beginPath();
    ctx.arc(x - 5, y - 5, 2, 0, Math.PI * 2);
    ctx.arc(x + 5, y + 5, 2, 0, Math.PI * 2);
    ctx.fillStyle = INK;
    ctx.fill();
  }

  function drawFist(ctx, x, y) {
    ctx.beginPath();
    ctx.arc(x, y, 16, 0, Math.PI * 2);
    ctx.stroke();
    for (let i = 0; i < 4; i++) {
      ctx.beginPath();
      ctx.arc(x - 8 + i * 5, y - 18, 4, 0, Math.PI * 2);
      ctx.stroke();
    }
  }

  function drawHospital(ctx, x, y) {
    ctx.strokeRect(x - 14, y - 16, 28, 32);
    ctx.beginPath();
    ctx.moveTo(x - 6, y);
    ctx.lineTo(x + 6, y);
    ctx.moveTo(x, y - 6);
    ctx.lineTo(x, y + 6);
    ctx.stroke();
  }

  function drawStickRunner(ctx, x, y) {
    ctx.beginPath();
    ctx.arc(x, y - 22, 7, 0, Math.PI * 2);
    ctx.moveTo(x, y - 15);
    ctx.lineTo(x, y + 2);
    ctx.moveTo(x, y - 8);
    ctx.lineTo(x - 14, y + 4);
    ctx.moveTo(x, y - 8);
    ctx.lineTo(x + 16, y - 2);
    ctx.moveTo(x, y + 2);
    ctx.lineTo(x - 10, y + 22);
    ctx.moveTo(x, y + 2);
    ctx.lineTo(x + 12, y + 20);
    ctx.stroke();
    ctx.font = '10px "Gochi Hand", cursive';
    ctx.fillStyle = INK_LIGHT;
    ctx.fillText('RUN', x + 18, y - 10);
  }

  function drawTornGraffiti(ctx, x, y) {
    ctx.font = `bold 22px "Caveat", cursive`;
    ctx.fillStyle = INK;
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(-0.12);
    ctx.fillText('Torn', 0, 0);
    ctx.font = '14px "Gochi Hand", cursive';
    ctx.fillStyle = INK_LIGHT;
    ctx.fillText('City vibes', 4, 18);
    ctx.restore();
  }

  function drawChain(ctx, x, y) {
    for (let i = 0; i < 3; i++) {
      ctx.beginPath();
      ctx.arc(x + i * 14, y, 6, 0, Math.PI * 2);
      ctx.stroke();
    }
  }

  function drawCoffeeRing(ctx, x, y) {
    ctx.beginPath();
    ctx.ellipse(x, y, 22, 18, 0, 0, Math.PI * 2);
    ctx.stroke();
    ctx.font = '11px "Patrick Hand", cursive';
    ctx.fillStyle = INK_FAINT;
    ctx.fillText('afk', x - 8, y + 4);
  }

  const TORN_DOODLES = [
    (ctx, x, y, rng) => drawXanax(ctx, x, y, 8 + rng() * 4),
    (ctx, x, y) => drawMug(ctx, x, y),
    (ctx, x, y) => drawDice(ctx, x, y),
    (ctx, x, y) => drawFist(ctx, x, y),
    (ctx, x, y) => drawHospital(ctx, x, y),
    (ctx, x, y) => drawStickRunner(ctx, x, y),
    (ctx, x, y) => drawTornGraffiti(ctx, x, y),
    (ctx, x, y) => drawChain(ctx, x, y),
    (ctx, x, y) => drawCoffeeRing(ctx, x, y)
  ];

  function paintNotebookSurface(ctx, w, h, seedKey, density = 'match') {
    const rng = mulberry32(hashSeed(seedKey));
    const isMatch = density === 'match';
    const isSite = density === 'lobby' || density === 'site';
    const noteCount = isMatch ? 3 + Math.floor(rng() * 2) : 8 + Math.floor(rng() * 4);
    const doodleCount = isMatch ? 4 + Math.floor(rng() * 3) : 10 + Math.floor(rng() * 5);
    const shapeCount = isMatch ? 8 + Math.floor(rng() * 6) : 10 + Math.floor(rng() * 6);

    ctx.strokeStyle = INK_FAINT;
    ctx.lineWidth = 1;
    for (let y = 28; y < h; y += 28) {
      ctx.beginPath();
      ctx.moveTo(0, y + (rng() - 0.5) * 2);
      ctx.lineTo(w, y + (rng() - 0.5) * 2);
      ctx.stroke();
    }

    ctx.strokeStyle = INK_LIGHT;
    ctx.setLineDash([6, 8]);
    ctx.beginPath();
    ctx.moveTo(w - 48, 0);
    ctx.lineTo(w - 52 + (rng() - 0.5) * 6, h);
    ctx.stroke();
    ctx.setLineDash([]);

    const notes = pickMany(rng, TORN_NOTES, noteCount);
    const noteWidth = Math.min(210, Math.max(130, w * 0.2));
    notes.forEach((text, i) => {
      const onRight = i % 2 === 0 || isMatch;
      const nx = onRight
        ? Math.max(16, w - noteWidth - 32)
        : 88 + rng() * 40;
      const row = Math.floor(i / 2);
      const ny = 32 + row * (isMatch ? 72 : 58) + rng() * 28;
      if (ny < h - 40) {
        drawHandwrittenNote(ctx, text, nx, ny, noteWidth, (rng() - 0.5) * 0.12, rng);
      }
    });

    if (isSite) {
      drawHandwrittenNote(ctx, '← torn.com side quest', 92, 28, 160, -0.08, rng);
      drawHandwrittenNote(ctx, 'RPS Battle — not official lol', 24, h - 72, 200, 0.05, rng);
      drawHandwrittenNote(ctx, 'remember: energy exists', Math.max(90, w - 240), h - 120, 180, -0.04, rng);
    }

    for (let i = 0; i < doodleCount; i++) {
      const doodle = TORN_DOODLES[Math.floor(rng() * TORN_DOODLES.length)];
      const pad = 70;
      const dx = pad + rng() * (w - pad * 2 - (isMatch ? 0 : 80));
      const dy = pad + rng() * (h - pad * 2);
      ctx.strokeStyle = rng() > 0.4 ? INK : INK_LIGHT;
      ctx.lineWidth = 1.2 + rng() * 1.2;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      doodle(ctx, dx, dy, rng);
    }

    for (let i = 0; i < shapeCount; i++) {
      const kind = Math.floor(rng() * 4);
      ctx.strokeStyle = rng() > 0.5 ? INK : INK_LIGHT;
      ctx.lineWidth = 0.8 + rng() * 1.4;
      ctx.lineCap = 'round';
      if (kind === 0) drawSquiggle(ctx, rng, w, h);
      else if (kind === 1) drawStar(ctx, rng() * w, rng() * h, 5 + rng() * 10, 5);
      else if (kind === 2) drawCloud(ctx, rng() * (w - 100), rng() * h, 0.5 + rng());
      else drawHeart(ctx, rng() * w, rng() * h, 6 + rng() * 8);
    }

    if (rng() > 0.4) {
      ctx.strokeStyle = INK_LIGHT;
      ctx.lineWidth = 1;
      drawArrow(ctx, 30 + rng() * 40, 60 + rng() * 40, w - 55, 80 + rng() * 60);
    }

    ctx.strokeStyle = INK;
    ctx.lineWidth = 1.5;
    for (let c = 0; c < 2; c++) {
      const cx = rng() > 0.5 ? 16 + rng() * 30 : w - 50 - rng() * 30;
      const cy = rng() > 0.5 ? 16 + rng() * 30 : h - 50 - rng() * 30;
      ctx.beginPath();
      ctx.arc(cx, cy, 10 + rng() * 18, rng() * Math.PI, rng() * Math.PI * 3);
      ctx.stroke();
    }
  }

  function setupCanvas(canvas, w, h) {
    const dpr = window.devicePixelRatio || 1;
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    canvas.style.width = `${w}px`;
    canvas.style.height = `${h}px`;
    const ctx = canvas.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);
    return ctx;
  }

  function paintMatchBackground(canvas, seedKey) {
    if (!canvas) return;
    const parent = canvas.parentElement;
    const w = parent ? parent.clientWidth : 800;
    const h = parent ? parent.clientHeight : 600;
    const ctx = setupCanvas(canvas, w, h);
    paintNotebookSurface(ctx, w, h, seedKey, 'match');
  }

  function paintGameNotebookBackground(canvas, seedKey) {
    if (!canvas) return;
    const w = window.innerWidth || document.documentElement.clientWidth || 1100;
    const h = window.innerHeight || document.documentElement.clientHeight || 900;
    const ctx = setupCanvas(canvas, w, h);
    paintNotebookSurface(ctx, w, h, seedKey || 'torn-site-notebook', 'site');
  }

  function screenShake(el, intensity = 1) {
    if (!el) return;
    el.classList.remove('shake-mild', 'shake-hard');
    void el.offsetWidth;
    el.classList.add(intensity > 1.2 ? 'shake-hard' : 'shake-mild');
    setTimeout(() => el.classList.remove('shake-mild', 'shake-hard'), 500);
  }

  function flashArena(arena, win) {
    if (!arena) return;
    arena.classList.remove('flash-win', 'flash-lose');
    void arena.offsetWidth;
    arena.classList.add(win ? 'flash-win' : 'flash-lose');
    setTimeout(() => arena.classList.remove('flash-win', 'flash-lose'), 450);
  }

  function spawnConfetti(container, amount = 80, burst = false) {
    if (!container) return;
    const colors = ['#163e79', '#3d69ad', '#ffeb3b', '#4caf50', '#e91e63', '#00bcd4'];
    for (let i = 0; i < amount; i++) {
      const piece = document.createElement('span');
      piece.className = 'confetti-piece';
      const left = burst ? 40 + Math.random() * 20 : Math.random() * 100;
      const delay = Math.random() * (burst ? 0.4 : 1.2);
      const dur = 1.8 + Math.random() * 1.5;
      const rot = Math.random() * 720 - 360;
      piece.style.left = `${left}%`;
      piece.style.background = colors[Math.floor(Math.random() * colors.length)];
      piece.style.animationDelay = `${delay}s`;
      piece.style.animationDuration = `${dur}s`;
      piece.style.setProperty('--rot', `${rot}deg`);
      piece.style.width = `${6 + Math.random() * 8}px`;
      piece.style.height = `${10 + Math.random() * 12}px`;
      container.appendChild(piece);
      setTimeout(() => piece.remove(), (delay + dur) * 1000 + 200);
    }
  }

  function floatText(container, text, className = 'fx-pop') {
    if (!container) return;
    const el = document.createElement('div');
    el.className = className;
    el.textContent = text;
    container.appendChild(el);
    setTimeout(() => el.remove(), 1200);
  }

  function playSelect() {
    playTone(520, 0.08, 'triangle', 0.06);
    playTone(780, 0.06, 'sine', 0.04, 0.04);
  }

  function playReveal() {
    playTone(180, 0.15, 'square', 0.05);
    playTone(320, 0.12, 'sawtooth', 0.04, 0.08);
    playTone(640, 0.2, 'sine', 0.07, 0.15);
  }

  function playWin() {
    [523, 659, 784, 1047].forEach((f, i) => playTone(f, 0.2, 'sine', 0.09, i * 0.1));
    playTone(1200, 0.15, 'triangle', 0.05, 0.45);
  }

  function playLose() {
    playTone(220, 0.25, 'sawtooth', 0.06);
    playTone(160, 0.35, 'sine', 0.05, 0.2);
  }

  function playRoundWin() {
    playTone(880, 0.12, 'sine', 0.07);
    playTone(1100, 0.15, 'sine', 0.06, 0.08);
  }

  function popScore(el) {
    if (!el) return;
    el.classList.remove('score-pop');
    void el.offsetWidth;
    el.classList.add('score-pop');
  }

  return {
    paintMatchBackground,
    paintGameNotebookBackground,
    screenShake,
    flashArena,
    spawnConfetti,
    floatText,
    playSelect,
    playReveal,
    playWin,
    playLose,
    playRoundWin,
    popScore
  };
})();
