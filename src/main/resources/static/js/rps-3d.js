/**
 * 2D Anime Character-themed Drawing animations.
 * Rock: The Thing (stony fist)
 * Paper: Konan (paper sheets)
 * Scissors: Kartana (origami blades)
 */
const Rps3D = (() => {
  const BLUE_DARK = '#0a295c';
  const BLUE_MID = '#3d69ad';
  const BLUE_LIGHT = '#aecbf7';
  const WHITE = '#ffffff';

  const PATHS = {
    rock: `
      <!-- Stony Fist (The Thing style) -->
      <path d="M30 75 Q20 70 18 50 Q18 35 30 30 Q35 25 50 25 Q65 25 75 35 Q82 45 82 60 Q82 75 65 82 Q50 88 30 75 Z" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2.5"/>
      <!-- Rocky textures -->
      <path d="M25 45 L35 48 M45 35 L48 45 M60 30 L58 42 M72 40 L65 48" fill="none" stroke="${BLUE_MID}" stroke-width="1.5"/>
      <path d="M35 70 Q50 68 70 72" fill="none" stroke="${BLUE_MID}" stroke-width="1.5"/>
      <!-- Knuckles -->
      <path d="M38 26 L38 38 M52 25 L52 38 M66 28 L66 40" fill="none" stroke="${BLUE_DARK}" stroke-width="2"/>
      <path d="M22 55 Q35 50 45 55 Q55 60 50 78" fill="none" stroke="${BLUE_DARK}" stroke-width="2.5"/>
    `,
    paper: `
      <!-- Paper Sheets (Konan style) -->
      <rect x="25" y="25" width="30" height="40" transform="rotate(-15 40 45)" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2"/>
      <rect x="45" y="20" width="30" height="40" transform="rotate(10 60 40)" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2"/>
      <rect x="35" y="45" width="30" height="40" transform="rotate(-5 50 65)" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2"/>
      <!-- Sheet details -->
      <path d="M30 35 L50 35 M55 30 L75 30 M40 60 L60 60" fill="none" stroke="${BLUE_LIGHT}" stroke-width="1"/>
      <path d="M20 70 Q30 85 50 80" fill="none" stroke="${BLUE_MID}" stroke-width="1.5" stroke-dasharray="4 2"/>
      <path d="M80 30 Q90 45 85 60" fill="none" stroke="${BLUE_MID}" stroke-width="1.5" stroke-dasharray="4 2"/>
    `,
    scissors: `
      <!-- Origami Blades (Kartana style) -->
      <path d="M50 85 L35 60 L45 20 L55 20 L65 60 Z" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2.5"/>
      <path d="M45 20 L20 15 L25 45 L45 35" fill="${WHITE}" stroke="${BLUE_MID}" stroke-width="2"/>
      <path d="M55 20 L80 15 L75 45 L55 35" fill="${WHITE}" stroke="${BLUE_MID}" stroke-width="2"/>
      <!-- Blade edges -->
      <path d="M48 25 L48 75 M52 25 L52 75" fill="none" stroke="${BLUE_LIGHT}" stroke-width="1"/>
      <path d="M30 20 L40 25 M70 20 L60 25" fill="none" stroke="${BLUE_DARK}" stroke-width="1.5"/>
    `,
    fist: `
      <!-- Stony Fist (Countdown) -->
      <path d="M30 75 Q20 70 18 50 Q18 35 30 30 Q35 25 50 25 Q65 25 75 35 Q82 45 82 60 Q82 75 65 82 Q50 88 30 75 Z" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2.5"/>
      <path d="M25 45 L35 48 M45 35 L48 45 M60 30 L58 42 M72 40 L65 48" fill="none" stroke="${BLUE_MID}" stroke-width="1.5"/>
      <path d="M22 55 Q35 50 45 55 Q55 60 50 78" fill="none" stroke="${BLUE_DARK}" stroke-width="2.5"/>
    `
  };

  function createSvg(choice) {
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 100 100');
    svg.style.width = '100%';
    svg.style.height = '100%';
    svg.innerHTML = PATHS[choice] || PATHS.rock;
    return svg;
  }

  function init3DButton(container, choice) {
    container.innerHTML = '';
    const svg = createSvg(choice);
    svg.style.transition = 'transform 0.3s ease';
    container.appendChild(svg);
    
    container.addEventListener('mouseenter', () => svg.style.transform = 'scale(1.1) rotate(5deg)');
    container.addEventListener('mouseleave', () => svg.style.transform = 'scale(1) rotate(0deg)');
  }

  function init3DCountdown(container, text, color = '#0a295c') {
    container.innerHTML = '';
    const div = document.createElement('div');
    div.className = 'countdown-2d-text drawing-text';
    div.textContent = text;
    div.style.color = color;
    container.appendChild(div);

    div.style.transform = 'scale(0) rotate(-10deg)';
    div.style.opacity = '0';
    div.style.transition = 'all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
    
    requestAnimationFrame(() => {
      div.style.transform = 'scale(1.3) rotate(5deg)';
      div.style.opacity = '1';
      setTimeout(() => {
        div.style.transform = 'scale(1) rotate(0deg)';
      }, 200);
    });

    return div;
  }

  function init3DReveal(container, choice, isCountdown = false) {
    container.innerHTML = '';
    const wrapper = document.createElement('div');
    wrapper.style.width = '100%';
    wrapper.style.height = '100%';
    wrapper.style.display = 'flex';
    wrapper.style.alignItems = 'center';
    wrapper.style.justifyContent = 'center';
    
    let currentChoice = isCountdown ? 'fist' : choice;
    const svg = createSvg(currentChoice);
    wrapper.appendChild(svg);
    container.appendChild(wrapper);

    if (isCountdown) {
      wrapper.classList.add('anime-pump');
    }

    return { 
      wrapper,
      reveal: (newChoice) => {
        wrapper.classList.remove('anime-pump');
        wrapper.style.transition = 'all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)';
        wrapper.style.transform = 'scale(0.1)';
        
        setTimeout(() => {
          svg.innerHTML = PATHS[newChoice] || PATHS.rock;
          wrapper.style.transform = 'scale(1.4)';
        }, 150);
      },
      updateChoice: (newChoice) => {
        svg.innerHTML = PATHS[newChoice] || PATHS.rock;
      }
    };
  }

  return {
    init3DReveal,
    init3DButton,
    init3DCountdown
  };
})();
