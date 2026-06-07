/**
 * 3D Doodle hands using Three.js.
 * Converts 2D SVG paths into extruded 3D "cardboard" models.
 */
const Rps3D = (() => {
  const INK = 0x163e79;
  const PAPER = 0xf0f6ff;

  const PATHS = {
    rock: 'M28 62 Q22 55 24 42 Q26 30 36 26 Q46 22 56 28 Q66 35 64 50 Q62 62 52 65 Q42 68 32 62 Z',
    paper: 'M32 65 Q28 58 30 48 L26 22 Q26 16 32 16 Q38 16 38 24 L38 42 M38 42 L42 12 Q42 6 48 6 Q54 6 54 14 L54 42 M54 42 L58 16 Q58 10 64 10 Q70 10 70 18 L70 45 M70 45 L74 30 Q74 24 80 24 Q84 24 84 34 L80 58 Q78 68 60 72 Q45 74 32 65 Z',
    scissors: 'M32 65 Q28 58 30 48 Q40 42 55 45 Q65 48 68 58 Q70 68 55 72 Q42 74 32 65 Z',
    fist: 'M28 62 Q22 55 24 42 Q26 30 36 26 Q46 22 56 28 Q66 35 64 50 Q62 62 52 65 Q42 68 32 62 Z M36 26 Q38 38 36 48 M46 24 Q48 36 46 46 M56 28 Q58 40 56 50'
  };

  const SCISSORS_PATHS = [
    'M32 65 Q28 58 30 48 Q40 42 55 45 Q65 48 68 58 Q70 68 55 72 Q42 74 32 65 Z',
    'M38 42 L44 14 Q46 8 52 10 Q58 12 54 20 L48 45',
    'M52 45 L66 22 Q68 16 74 18 Q80 20 76 28 L62 52'
  ];

  function create3DHand(choice, isButton = false) {
    const group = new THREE.Group();
    const loader = new THREE.SVGLoader();
    
    let pathsToProcess = choice === 'scissors' ? SCISSORS_PATHS : [PATHS[choice] || PATHS.rock];
    
    pathsToProcess.forEach(pathStr => {
      const path = loader.parse(` <svg xmlns="http://www.w3.org/2000/svg"><path d="${pathStr}" /></svg>`).paths[0];
      const shapes = THREE.SVGLoader.createShapes(path);
      
      shapes.forEach(shape => {
        const extrudeSettings = {
          steps: 1,
          depth: isButton ? 2 : 4,
          bevelEnabled: true,
          bevelThickness: 1,
          bevelSize: 1,
          bevelOffset: 0,
          bevelSegments: 1
        };

        const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings);
        geometry.center();
        
        const material = new THREE.MeshToonMaterial({ 
          color: PAPER,
          side: THREE.DoubleSide
        });

        const wireframe = new THREE.MeshBasicMaterial({ 
          color: INK, 
          wireframe: true,
          transparent: true,
          opacity: 0.4
        });

        const mesh = new THREE.Mesh(geometry, material);
        const outline = new THREE.Mesh(geometry, wireframe);
        outline.scale.multiplyScalar(1.02);

        group.add(mesh);
        group.add(outline);
      });
    });

    group.rotation.x = Math.PI;
    return group;
  }

  function init3DButton(container, choice) {
    const width = 80;
    const height = 80;
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, 1, 1, 1000);
    camera.position.z = 80;

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(width, height);
    renderer.setPixelRatio(window.devicePixelRatio);
    container.innerHTML = '';
    container.appendChild(renderer.domElement);

    const light = new THREE.AmbientLight(0xffffff, 1);
    scene.add(light);

    const hand = create3DHand(choice, true);
    scene.add(hand);

    function animate() {
      if (!container.isConnected) return;
      requestAnimationFrame(animate);
      hand.rotation.y += 0.03; // Increased rotation speed
      hand.rotation.z = Math.sin(Date.now() * 0.002) * 0.15;
      renderer.render(scene, camera);
    }
    animate();
  }

  function init3DCountdown(container, text, color = 0x163e79) {
    const width = container.clientWidth;
    const height = container.clientHeight;

    // Use a large stylized div for the countdown text with 3D animation
    container.innerHTML = '';
    const div = document.createElement('div');
    div.className = 'countdown-3d-text doodle-text';
    div.textContent = text;
    div.style.color = '#' + color.toString(16).padStart(6, '0');
    container.appendChild(div);

    // Apply animation
    div.style.transform = 'scale(0) rotate(-10deg)';
    div.style.opacity = '0';
    div.style.transition = 'all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
    
    requestAnimationFrame(() => {
      div.style.transform = 'scale(1.2) rotate(5deg)';
      div.style.opacity = '1';
      setTimeout(() => {
        div.style.transform = 'scale(1) rotate(0deg)';
      }, 300);
    });

    return div;
  }

  function init3DReveal(container, choice, isCountdown = false) {
    const width = container.clientWidth;
    const height = container.clientHeight;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, width / height, 1, 1000);
    camera.position.z = 100;

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(width, height);
    renderer.setPixelRatio(window.devicePixelRatio);
    container.innerHTML = '';
    container.appendChild(renderer.domElement);

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
    scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.5);
    directionalLight.position.set(0, 10, 10);
    scene.add(directionalLight);

    let currentHand = create3DHand(isCountdown ? 'fist' : choice);
    scene.add(currentHand);

    currentHand.scale.set(1.2, 1.2, 1.2);

    let frame = 0;
    let revealChoice = null;

    function animate() {
      if (!container.isConnected) return;
      requestAnimationFrame(animate);
      
      frame++;
      
      if (isCountdown && !revealChoice) {
        // Pumping fist motion
        const pump = Math.abs(Math.sin(frame * 0.2)); // Faster pump
        currentHand.position.y = pump * 15;
        currentHand.rotation.x = Math.PI + pump * 0.3;
      } else if (revealChoice) {
        // Transition to choice
        if (currentHand.choice !== revealChoice) {
           scene.remove(currentHand);
           currentHand = create3DHand(revealChoice);
           currentHand.choice = revealChoice;
           currentHand.scale.set(0.1, 0.1, 0.1);
           scene.add(currentHand);
        }
        
        const s = currentHand.scale.x + (1.4 - currentHand.scale.x) * 0.2;
        currentHand.scale.set(s, s, s);
        currentHand.position.y *= 0.8;
        currentHand.rotation.x = currentHand.rotation.x + (Math.PI - currentHand.rotation.x) * 0.2;
        currentHand.rotation.z = Math.sin(frame * 0.05) * 0.05;
        currentHand.rotation.y = Math.cos(frame * 0.03) * 0.08;
      } else {
        // Normal reveal / idle
        currentHand.rotation.z = Math.sin(frame * 0.05) * 0.05;
        currentHand.rotation.y = Math.cos(frame * 0.03) * 0.08;
      }
      
      renderer.render(scene, camera);
    }

    animate();
    return { 
      scene, 
      camera, 
      renderer, 
      hand: currentHand, 
      reveal: (newChoice) => {
        revealChoice = newChoice;
        isCountdown = false;
      },
      updateChoice: (newChoice) => {
        scene.remove(currentHand);
        currentHand = create3DHand(newChoice);
        currentHand.scale.set(1.2, 1.2, 1.2);
        scene.add(currentHand);
        return currentHand;
      }
    };
  }

  return {
    init3DReveal,
    init3DButton,
    init3DCountdown
  };
})();
