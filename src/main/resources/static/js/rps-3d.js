/**
 * 3D Illustrated hands using Three.js.
 * Converts detailed SVG illustrations into thin 3D "paper" models with clean outlines.
 */
const Rps3D = (() => {
  const INK = 0x0a295c;
  const PAPER = 0xffffff;

  const PATHS = {
    rock: [
      // Anime fist outline: Tense, slightly angular
      'M30 75 Q20 70 20 55 Q20 40 35 35 Q40 30 55 30 Q70 30 75 40 Q80 50 80 60 Q80 75 65 80 Q50 85 30 75 Z',
      // Thumb tucked over: Elegant curve
      'M22 55 Q35 50 45 55 Q55 60 50 75',
      // Knuckle definitions: Sharp stylized lines
      'M38 32 L38 45',
      'M52 30 L52 43',
      'M66 32 L66 45'
    ],
    paper: [
      // Anime open hand: Slender fingers, elegant taper
      'M35 85 Q25 80 28 65 L20 35 Q20 28 28 28 Q36 28 35 45 L38 20 Q38 12 46 12 Q54 12 52 45 L58 15 Q58 8 66 8 Q74 8 72 45 L80 25 Q82 18 88 22 Q94 26 88 50 L75 80 Q65 90 45 88 Q38 87 35 85 Z',
      // Palm creases
      'M40 65 Q50 62 65 68',
      'M45 75 Q55 72 70 78'
    ],
    scissors: [
      // Anime scissors: Dynamic "V" sign
      'M35 85 Q25 80 28 65 Q35 60 45 60 L40 25 Q40 18 48 18 Q56 18 55 55 L75 25 Q78 18 86 22 Q94 26 82 55 L70 75 Q65 85 45 88 Q38 87 35 85 Z',
      // Folded fingers detail
      'M30 65 Q45 62 55 68',
      // Thumb tuck
      'M32 75 Q40 70 48 75'
    ],
    fist: [
      'M30 75 Q20 70 20 55 Q20 40 35 35 Q40 30 55 30 Q70 30 75 40 Q80 50 80 60 Q80 75 65 80 Q50 85 30 75 Z',
      'M22 55 Q35 50 45 55 Q55 60 50 75',
      'M38 32 L38 45',
      'M52 30 L52 43',
      'M66 32 L66 45'
    ]
  };

  function create3DHand(choice) {
    const group = new THREE.Group();
    const loader = new THREE.SVGLoader();
    
    const pathsToProcess = PATHS[choice] || PATHS.rock;
    
    pathsToProcess.forEach(pathStr => {
      const pathData = loader.parse(` <svg xmlns="http://www.w3.org/2000/svg"><path d="${pathStr}" /></svg>`).paths[0];
      const shapes = THREE.SVGLoader.createShapes(pathData);
      
      shapes.forEach(shape => {
        // Very thin extrusion to look like a "drawing on paper"
        const extrudeSettings = {
          steps: 1,
          depth: 0.5,
          bevelEnabled: false
        };

        const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings);
        geometry.center();
        
        const material = new THREE.MeshToonMaterial({ 
          color: PAPER,
          side: THREE.DoubleSide
        });

        const mesh = new THREE.Mesh(geometry, material);
        
        // Clean solid outline using a slightly larger mesh
        const outlineMaterial = new THREE.MeshBasicMaterial({ 
          color: INK,
          side: THREE.BackSide
        });
        const outline = new THREE.Mesh(geometry, outlineMaterial);
        outline.scale.multiplyScalar(1.05);

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

    const hand = create3DHand(choice);
    scene.add(hand);

    function animate() {
      if (!container.isConnected) return;
      requestAnimationFrame(animate);
      hand.rotation.y += 0.03;
      hand.rotation.z = Math.sin(Date.now() * 0.002) * 0.15;
      renderer.render(scene, camera);
    }
    animate();
  }

  function init3DCountdown(container, text, color = 0x0a295c) {
    const width = container.clientWidth;
    const height = container.clientHeight;

    container.innerHTML = '';
    const div = document.createElement('div');
    div.className = 'countdown-3d-text drawing-text';
    div.textContent = text;
    div.style.color = '#' + color.toString(16).padStart(6, '0');
    container.appendChild(div);

    div.style.transform = 'scale(0) rotate(-5deg)';
    div.style.opacity = '0';
    div.style.transition = 'all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
    
    requestAnimationFrame(() => {
      div.style.transform = 'scale(1.1) rotate(2deg)';
      div.style.opacity = '1';
      setTimeout(() => {
        div.style.transform = 'scale(1) rotate(0deg)';
      }, 400);
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

    const ambientLight = new THREE.AmbientLight(0xffffff, 1);
    scene.add(ambientLight);

    let currentHand = create3DHand(isCountdown ? 'fist' : choice);
    scene.add(currentHand);

    currentHand.scale.set(1.3, 1.3, 1.3);

    let frame = 0;
    let revealChoice = null;

    function animate() {
      if (!container.isConnected) return;
      requestAnimationFrame(animate);
      
      frame++;
      
      if (isCountdown && !revealChoice) {
        const pump = Math.abs(Math.sin(frame * 0.2));
        currentHand.position.y = pump * 15;
        currentHand.rotation.x = Math.PI + pump * 0.2;
      } else if (revealChoice) {
        if (currentHand.choice !== revealChoice) {
           scene.remove(currentHand);
           currentHand = create3DHand(revealChoice);
           currentHand.choice = revealChoice;
           currentHand.scale.set(0.1, 0.1, 0.1);
           scene.add(currentHand);
        }
        
        const s = currentHand.scale.x + (1.5 - currentHand.scale.x) * 0.2;
        currentHand.scale.set(s, s, s);
        currentHand.position.y *= 0.8;
        currentHand.rotation.x = currentHand.rotation.x + (Math.PI - currentHand.rotation.x) * 0.2;
        currentHand.rotation.z = Math.sin(frame * 0.05) * 0.03;
        currentHand.rotation.y = Math.cos(frame * 0.03) * 0.05;
      } else {
        currentHand.rotation.z = Math.sin(frame * 0.05) * 0.03;
        currentHand.rotation.y = Math.cos(frame * 0.03) * 0.05;
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
        currentHand.scale.set(1.3, 1.3, 1.3);
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
