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
    scissors: 'M32 65 Q28 58 30 48 Q40 42 55 45 Q65 48 68 58 Q70 68 55 72 Q42 74 32 65 Z' // Base only for scissors extrusion
  };

  // More complex paths for scissors since it has separate fingers
  const SCISSORS_PATHS = [
    'M32 65 Q28 58 30 48 Q40 42 55 45 Q65 48 68 58 Q70 68 55 72 Q42 74 32 65 Z',
    'M38 42 L44 14 Q46 8 52 10 Q58 12 54 20 L48 45',
    'M52 45 L66 22 Q68 16 74 18 Q80 20 76 28 L62 52'
  ];

  function create3DHand(choice) {
    const group = new THREE.Group();
    const loader = new THREE.SVGLoader();
    
    let pathsToProcess = choice === 'scissors' ? SCISSORS_PATHS : [PATHS[choice]];
    
    pathsToProcess.forEach(pathStr => {
      const path = loader.parse(` <svg xmlns="http://www.w3.org/2000/svg"><path d="${pathStr}" /></svg>`).paths[0];
      const shapes = THREE.SVGLoader.createShapes(path);
      
      shapes.forEach(shape => {
        const extrudeSettings = {
          steps: 1,
          depth: 4,
          bevelEnabled: true,
          bevelThickness: 1,
          bevelSize: 1,
          bevelOffset: 0,
          bevelSegments: 1
        };

        const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings);
        geometry.center();
        
        // Hand material (white/paper)
        const material = new THREE.MeshToonMaterial({ 
          color: PAPER,
          side: THREE.DoubleSide
        });

        // Outline material (ink)
        const wireframe = new THREE.MeshBasicMaterial({ 
          color: INK, 
          wireframe: true,
          transparent: true,
          opacity: 0.3
        });

        const mesh = new THREE.Mesh(geometry, material);
        const outline = new THREE.Mesh(geometry, wireframe);
        outline.scale.multiplyScalar(1.02);

        group.add(mesh);
        group.add(outline);
      });
    });

    // Flip Y because SVGs are Y-down
    group.rotation.x = Math.PI;
    return group;
  }

  function init3DReveal(container, choice) {
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

    const hand = create3DHand(choice);
    scene.add(hand);

    // Initial animation state
    hand.rotation.y = -Math.PI * 2;
    hand.scale.set(0.1, 0.1, 0.1);

    let frame = 0;
    function animate() {
      if (!container.isConnected) return; // Stop if element removed
      requestAnimationFrame(animate);
      
      frame++;
      
      // Entrance animation
      if (frame < 60) {
        hand.rotation.y += (0 - hand.rotation.y) * 0.1;
        const s = hand.scale.x + (1.2 - hand.scale.x) * 0.1;
        hand.scale.set(s, s, s);
      } else {
        // Subtle floating / wobble
        hand.rotation.z = Math.sin(frame * 0.05) * 0.05;
        hand.rotation.y = Math.cos(frame * 0.03) * 0.08;
        const s = 1.2 + Math.sin(frame * 0.04) * 0.02;
        hand.scale.set(s, s, s);
      }
      
      renderer.render(scene, camera);
    }

    animate();
    return { scene, camera, renderer, hand };
  }

  return {
    init3DReveal
  };
})();
