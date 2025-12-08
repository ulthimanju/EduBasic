/**
 * Avatar Generation Utility
 * Generates deterministic, unique avatar SVGs from usernames and emails
 */

// --- 1. Utility: Deterministic Random Number Generator ---
// Creates a seeded RNG so the same input always produces the same avatar.
const createRNG = (seedStr) => {
  let hash = 0;
  for (let i = 0; i < seedStr.length; i++) {
    hash = seedStr.charCodeAt(i) + ((hash << 5) - hash);
  }
  return () => {
    let t = hash += 0x6D2B79F5;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
};

// --- 2. Utility: Color Generation ---
// Generates a consistent HSL color from a string.
const stringToHslColor = (str, s = 65, l = 50) => {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  const h = hash % 360;
  return { h, s, l, css: `hsl(${h}, ${s}%, ${l}%)` };
};

// --- 3. Utility: Initials Extraction ---
const getInitials = (name) => {
  if (!name) return '';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
};

// --- 4. Core Logic: Organic Blob Path Generation ---
// Generates a smooth, closed shape using Quadratic Bezier curves.
const generateBlobPath = (rng, cx, cy, radius, complexity) => {
  const points = [];
  const angleStep = (Math.PI * 2) / complexity;

  // 1. Generate random points around a circle
  for (let i = 0; i < complexity; i++) {
    const angle = i * angleStep;
    const dist = radius * (0.6 + rng() * 0.8); // Randomize radius
    points.push({
      x: cx + Math.cos(angle) * dist,
      y: cy + Math.sin(angle) * dist
    });
  }

  // 2. Helper to find midpoint between two points
  const getMid = (p1, p2) => ({
    x: (p1.x + p2.x) / 2,
    y: (p1.y + p2.y) / 2
  });

  // 3. Construct SVG Path command
  const p0 = points[0];
  const pLast = points[points.length - 1];
  const start = getMid(pLast, p0);

  let d = `M ${start.x} ${start.y}`;

  for (let i = 0; i < points.length; i++) {
    const p1 = points[i];
    const p2 = points[(i + 1) % points.length];
    const mid = getMid(p1, p2);
    // Use the point as control point, and midpoint as end point
    d += ` Q ${p1.x} ${p1.y} ${mid.x} ${mid.y}`;
  }

  return d;
};

// --- 5. Main Function: Generate Avatar SVG ---
/**
 * Generates an SVG string for an avatar.
 * @param {Object} config - { username, email, size, type, isRounded }
 * @returns {string} The raw SVG string.
 */
export function generateAvatarSVG({ 
  username = '', 
  email = '', 
  size = 64, 
  type = 'organic', // 'initials' | 'organic'
  isRounded = true 
}) {
  const seedStr = (email.toLowerCase().trim() + username.toLowerCase().trim()) || 'default';
  const baseColor = stringToHslColor(seedStr);
  const safeId = seedStr.replace(/[^a-z0-9]/gi, ''); // Safe ID for SVG defs
  
  // Common SVG attributes
  const viewBox = `0 0 ${size} ${size}`;
  const clipRadius = isRounded ? size / 2 : size / 8;

  let content = '';
  let defs = '';
  const bgColor = `hsl(${baseColor.h}, ${baseColor.s * 0.3}%, 95%)`;

  if (type === 'initials') {
    // --- Initials Logic ---
    const fontSize = size * 0.4;
    const initials = getInitials(username);
    
    content = `
      <rect width="${size}" height="${size}" fill="${baseColor.css}" />
      <text 
        x="50%" y="50%" dy=".1em" 
        text-anchor="middle" dominant-baseline="middle"
        font-size="${fontSize}" fill="white" 
        font-family="Arial, sans-serif" font-weight="bold"
      >${initials}</text>
    `;
  } else {
    // --- Organic Pattern Logic ---
    const rng = createRNG(seedStr);
    const layerCount = 3 + Math.floor(rng() * 2); // 3 to 4 layers
    let layersSvg = '';

    for (let i = 0; i < layerCount; i++) {
      const complexity = 4 + Math.floor(rng() * 4); // 4-7 points
      
      // Map logic: 0..100 coordinate space scaled to size
      const cx = (50 + (rng() - 0.5) * 40) * (size / 100); 
      const cy = (50 + (rng() - 0.5) * 40) * (size / 100);
      const r = (25 + rng() * 25) * (size / 100);

      const pathData = generateBlobPath(rng, cx, cy, r, complexity);

      // Color variation
      const hueShift = (rng() - 0.5) * 60;
      const lightShift = (rng() - 0.5) * 30;
      const fill = `hsl(${baseColor.h + hueShift}, ${baseColor.s}%, ${Math.max(20, Math.min(90, baseColor.l + lightShift))}%)`;
      const opacity = 0.4 + rng() * 0.4;

      layersSvg += `<path d="${pathData}" fill="${fill}" opacity="${opacity}" style="mix-blend-mode: multiply;" />`;
    }

    // Add Noise Filter
    defs = `
      <filter id="noise-${safeId}">
        <feTurbulence type="fractalNoise" baseFrequency="0.8" numOctaves="3" result="noise" />
        <feColorMatrix type="matrix" values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 0.4 0" in="noise" />
      </filter>
      <clipPath id="clip-${safeId}">
        <rect width="${size}" height="${size}" rx="${clipRadius}" />
      </clipPath>
    `;

    content = `
      <g clip-path="url(#clip-${safeId})">
        <rect width="${size}" height="${size}" fill="${bgColor}" />
        ${layersSvg}
        <rect width="${size}" height="${size}" filter="url(#noise-${safeId})" opacity="0.15" fill="transparent"/>
      </g>
    `;
  }

  // Assemble Final SVG
  return `
    <svg 
      width="${size}" height="${size}" viewBox="${viewBox}" 
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>${defs}</defs>
      ${type === 'initials' 
          ? `<mask id="mask-${safeId}"><rect width="${size}" height="${size}" fill="white" rx="${clipRadius}" /></mask><g mask="url(#mask-${safeId})">${content}</g>` 
          : content 
      }
    </svg>
  `.trim();
}

/**
 * Convert SVG string to Data URL for use in img src
 * @param {string} svgString - The SVG string
 * @returns {string} Data URL
 */
export function svgToDataUrl(svgString) {
  const blob = new Blob([svgString], { type: 'image/svg+xml' });
  return URL.createObjectURL(blob);
}

/**
 * Alternative: Base64 encoding for more compatibility
 * @param {string} svgString - The SVG string
 * @returns {string} Data URL with base64 encoding
 */
export function svgToDataUrlBase64(svgString) {
  const encoded = btoa(svgString);
  return `data:image/svg+xml;base64,${encoded}`;
}
