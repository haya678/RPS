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
      <!-- Realistic The Thing Stony Fist -->
      <path d="M20 75 Q15 65 15 50 Q15 35 25 25 Q35 20 55 20 Q75 20 85 30 Q92 40 92 60 Q92 75 80 85 Q60 90 20 75 Z" fill="#ffffff" stroke="#163e79" stroke-width="2"/>
      <!-- Stony textures and cracks -->
      <path d="M25 35 L40 38 M45 30 L60 32 M65 28 L80 32 M30 50 L50 52 M55 45 L75 48 M35 65 L55 68 M60 62 L85 65" fill="none" stroke="#3d69ad" stroke-width="1.5" opacity="0.6"/>
      <path d="M42 21 L40 45 M58 21 L60 45 M75 31 L72 55 M22 55 L45 58 M28 72 L50 75" fill="none" stroke="#0a295c" stroke-width="1.2"/>
      <!-- Knuckle highlights -->
      <path d="M25 30 Q35 25 45 28 M50 25 Q65 22 78 28" fill="none" stroke="#aecbf7" stroke-width="2.5" opacity="0.4"/>
      <!-- Thumb detail -->
      <path d="M15 55 Q30 50 48 55 Q60 65 55 85" fill="none" stroke="#163e79" stroke-width="3"/>
      <!-- Deep shadows -->
      <path d="M20 75 Q35 85 60 85 Q80 80 88 60" fill="none" stroke="#0a295c" stroke-width="1.5"/>
    `,
    paper: `
      <!-- Realistic Konan Paper Style -->
      <!-- Main sheets -->
      <path d="M15 45 L45 25 L75 40 L45 65 Z" fill="#ffffff" stroke="#163e79" stroke-width="1"/>
      <path d="M50 20 L85 15 L95 50 L60 60 Z" fill="#f0f0f0" stroke="#163e79" stroke-width="1"/>
      <path d="M25 70 L65 65 L85 95 L35 98 Z" fill="#e8e8e8" stroke="#163e79" stroke-width="1"/>
      <!-- Fold lines -->
      <path d="M45 25 L45 65 M15 45 L75 40 M60 20 L60 60 M85 15 L95 50" fill="none" stroke="#3d69ad" stroke-width="0.5" opacity="0.5"/>
      <!-- Paper butterflies (Konan style) -->
      <path d="M12 25 Q18 12 24 18 Q30 24 18 30 Q12 24 12 25" fill="#aecbf7" stroke="#163e79" stroke-width="0.8"/>
      <path d="M85 75 Q95 62 100 70 Q105 78 92 88 Q82 78 85 75" fill="#aecbf7" stroke="#163e79" stroke-width="0.8"/>
      <path d="M45 15 Q52 3 60 10 Q68 17 55 22 Q45 17 45 15" fill="#ffffff" stroke="#163e79" stroke-width="0.8"/>
      <!-- Ink swirls -->
      <path d="M30 35 Q50 20 70 30 M20 65 Q45 55 65 70" fill="none" stroke="#3d69ad" stroke-width="1" stroke-dasharray="2 1" opacity="0.4"/>
    `,
    scissors: `
      <!-- Realistic Kartana Origami Blades -->
      <!-- Main Central Blade -->
      <path d="M50 95 L38 60 L48 5 L52 5 L62 60 Z" fill="#ffffff" stroke="#163e79" stroke-width="1.5"/>
      <!-- Side Sword Arms -->
      <path d="M45 45 L5 25 L12 70 L40 55" fill="#ffffff" stroke="#163e79" stroke-width="1.8"/>
      <path d="M55 45 L95 25 L88 70 L60 55" fill="#ffffff" stroke="#163e79" stroke-width="1.8"/>
      <!-- Yellow/Gold crossguard -->
      <rect x="38" y="55" width="24" height="6" rx="1" fill="#ffffff" stroke="#163e79" stroke-width="1"/>
      <rect x="48" y="52" width="4" height="12" fill="#ffffff" stroke="#163e79"/>
      <!-- Red "eye" or core detail -->
      <circle cx="50" cy="58" r="2" fill="#aecbf7"/>
      <!-- Sharp edges and gleam highlights -->
      <path d="M15 35 L35 48 M85 35 L65 48" fill="none" stroke="#f0f8ff" stroke-width="1.2"/>
      <path d="M49 10 L49 50 M51 10 L51 50" fill="none" stroke="#dcdcdc" stroke-width="0.5"/>
      <path d="M50 5 L50 90" fill="none" stroke="#aecbf7" stroke-width="0.4" opacity="0.3"/>
    `,
    fist: `
      <!-- The Thing style fist for countdown (Orange Stony) -->
      <path d="M20 75 Q15 65 15 50 Q15 35 25 25 Q35 20 55 20 Q75 20 85 30 Q92 40 92 60 Q92 75 80 85 Q60 90 20 75 Z" fill="#ffffff" stroke="#163e79" stroke-width="2"/>
      <path d="M30 50 L50 52 M55 45 L75 48 M35 65 L55 68" fill="none" stroke="#3d69ad" stroke-width="1.5" opacity="0.6"/>
      <path d="M15 55 Q30 50 48 55 Q60 65 55 85" fill="none" stroke="#163e79" stroke-width="3"/>
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
