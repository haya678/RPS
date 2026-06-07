/**
 * 3D Illustrated hands using Three.js.
 * Converts detailed SVG illustrations into thin 3D "paper" models with clean outlines.
 */
const Rps3D = (() => {
  const INK = 0x0a295c;
  const PAPER = 0xffffff;

  const PATHS = {
    rock: [
      'M30 70 Q25 65 25 55 Q25 45 35 40 Q40 35 50 35 Q60 35 65 40 Q70 45 70 55 Q70 65 65 70 Q60 75 50 75 Q40 75 30 70 Z',
      'M35 40 Q38 48 35 55',
      'M45 38 Q48 46 45 53',
      'M55 38 Q58 46 55 53',
      'M65 40 Q68 48 65 55'
    ],
    paper: [
      'M35 75 Q30 70 30 60 L25 30 Q25 25 30 25 Q35 25 35 35 L35 55',
      'M35 55 L40 20 Q40 15 45 15 Q50 15 50 25 L50 55',
      'M50 55 L55 20 Q55 15 60 15 Q65 15 65 25 L65 55',
      'M65 55 L70 30 Q70 25 75 25 Q80 25 80 35 L75 65 Q73 75 55 78 Q45 80 35 75 Z',
      'M30 60 Q28 50 32 45',
      'M75 65 Q78 55 74 50'
    ],
    scissors: [
      'M35 75 Q30 70 30 60 Q35 55 45 55 Q55 55 60 60 Q65 70 55 75 Q45 80 35 75 Z',
      'M45 55 L40 25 Q40 20 45 20 Q50 20 50 25 L55 55',
      'M55 55 L75 30 Q78 25 83 25 Q88 25 85 35 L70 65',
      'M30 60 Q28 50 32 45'
    ],
    fist: [
      'M30 70 Q25 65 25 55 Q25 45 35 40 Q40 35 50 35 Q60 35 65 40 Q70 45 70 55 Q70 65 65 70 Q60 75 50 75 Q40 75 30 70 Z',
      'M35 40 Q38 48 35 55',
      'M45 38 Q48 46 45 53',
      'M55 38 Q58 46 55 53',
      'M65 40 Q68 48 65 55'
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
