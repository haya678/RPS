/**
 * 2D Anime Character-themed Drawing animations.
 * Rock: The Thing (stony fist)
 * Paper: Konan (paper sheets & butterflies)
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
      <path d="M25 80 Q15 75 12 55 Q12 35 25 30 Q30 25 50 25 Q70 25 80 35 Q88 45 88 65 Q88 80 70 85 Q50 90 25 80 Z" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2.5"/>
      <!-- Blocky textures -->
      <path d="M20 60 L35 62 M40 55 L55 58 M60 50 L75 52 M30 40 L45 42 M55 35 L70 38" fill="none" stroke="${BLUE_MID}" stroke-width="1.5"/>
      <!-- Deep cracks -->
      <path d="M42 26 L40 45 M58 26 L60 45 M75 35 L72 55" fill="none" stroke="${BLUE_DARK}" stroke-width="1.5"/>
      <!-- Thumb detail -->
      <path d="M15 55 Q30 50 45 55 Q55 65 50 82" fill="none" stroke="${BLUE_DARK}" stroke-width="3"/>
      <!-- Rocky highlight -->
      <path d="M70 40 Q75 45 72 55" fill="none" stroke="${BLUE_LIGHT}" stroke-width="2"/>
    `,
    paper: `
      <!-- Paper Shards (Konan style) -->
      <path d="M25 45 L50 30 L70 40 L45 55 Z" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2"/>
      <path d="M60 25 L90 20 L95 50 L65 55 Z" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2"/>
      <path d="M35 70 L65 65 L80 90 L50 95 Z" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2"/>
      <!-- Fluttering paper butterflies -->
      <path d="M12 25 Q15 15 20 20 Q25 25 15 30 Q10 25 12 25" fill="${BLUE_LIGHT}" stroke="${BLUE_DARK}" stroke-width="1"/>
      <path d="M85 75 Q90 65 95 70 Q100 75 90 85 Q80 75 85 75" fill="${BLUE_LIGHT}" stroke="${BLUE_DARK}" stroke-width="1"/>
      <path d="M45 15 Q50 5 55 10 Q60 15 50 20 Q40 15 45 15" fill="${BLUE_LIGHT}" stroke="${BLUE_DARK}" stroke-width="0.8"/>
      <!-- Ink swirls -->
      <path d="M30 35 Q45 25 60 30 M20 60 Q40 55 55 65" fill="none" stroke="${BLUE_MID}" stroke-width="1" stroke-dasharray="3 2"/>
    `,
    scissors: `
      <!-- Origami Blades (Kartana style) -->
      <path d="M50 90 L40 60 L48 10 L52 10 L60 60 Z" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2.5"/>
      <!-- Sword Arms -->
      <path d="M46 35 L15 25 L20 60 L45 45" fill="${WHITE}" stroke="${BLUE_MID}" stroke-width="2"/>
      <path d="M54 35 L85 25 L80 60 L55 45" fill="${WHITE}" stroke="${BLUE_MID}" stroke-width="2"/>
      <!-- Crossguard detail -->
      <rect x="42" y="55" width="16" height="4" fill="${BLUE_DARK}"/>
      <!-- Sharp edges and gleam -->
      <path d="M20 30 L40 40 M80 30 L60 40" fill="none" stroke="${BLUE_LIGHT}" stroke-width="1.5"/>
      <path d="M48 15 L48 50 M52 15 L52 50" fill="none" stroke="${BLUE_DARK}" stroke-width="0.8"/>
      <path d="M49 10 L49 55" fill="none" stroke="${WHITE}" stroke-width="0.5"/>
    `,
    fist: `
      <!-- The Thing style fist for countdown -->
      <path d="M25 80 Q15 75 12 55 Q12 35 25 30 Q30 25 50 25 Q70 25 80 35 Q88 45 88 65 Q88 80 70 85 Q50 90 25 80 Z" fill="${WHITE}" stroke="${BLUE_DARK}" stroke-width="2.5"/>
      <path d="M20 60 L35 62 M40 55 L55 58 M60 50 L75 52" fill="none" stroke="${BLUE_MID}" stroke-width="1.5"/>
      <path d="M15 55 Q30 50 45 55 Q55 65 50 82" fill="none" stroke="${BLUE_DARK}" stroke-width="3"/>
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
