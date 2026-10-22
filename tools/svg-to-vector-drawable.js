// Converts the Sakuin Figma SVG sources (icons, logo, illustrations) into Android VectorDrawables.
// Usage: node tools/svg-to-vector-drawable.js app/src/main/res/drawable
const fs = require('fs');
const path = require('path');
const out = process.argv[2];
if (!out) throw new Error('output dir required');

const n = (v) => Number(parseFloat(v).toFixed(3)).toString();
const attrs = (s) => Object.fromEntries([...s.matchAll(/([\w-]+)="([^"]*)"/g)].map((m) => [m[1], m[2]]));
const color = (c) => (c && c !== 'none' ? '#FF' + c.replace('#', '').toUpperCase() : null);

function rectPath(a) {
  const x = +a.x || 0, y = +a.y || 0, w = +a.width, h = +a.height, r = Math.min(+a.rx || 0, w / 2, h / 2);
  if (!r) return `M${n(x)},${n(y)}H${n(x + w)}V${n(y + h)}H${n(x)}Z`;
  return `M${n(x + r)},${n(y)}H${n(x + w - r)}A${n(r)},${n(r)} 0,0 1,${n(x + w)},${n(y + r)}` +
    `V${n(y + h - r)}A${n(r)},${n(r)} 0,0 1,${n(x + w - r)},${n(y + h)}` +
    `H${n(x + r)}A${n(r)},${n(r)} 0,0 1,${n(x)},${n(y + h - r)}` +
    `V${n(y + r)}A${n(r)},${n(r)} 0,0 1,${n(x + r)},${n(y)}Z`;
}
function ellipsePath(cx, cy, rx, ry) {
  return `M${n(cx - rx)},${n(cy)}A${n(rx)},${n(ry)} 0,1 0,${n(cx + rx)},${n(cy)}` +
    `A${n(rx)},${n(ry)} 0,1 0,${n(cx - rx)},${n(cy)}Z`;
}

function element(tag, a, defaults, indent) {
  let d;
  if (tag === 'path') d = a.d;
  else if (tag === 'rect') d = rectPath(a);
  else if (tag === 'circle') d = ellipsePath(+a.cx, +a.cy, +a.r, +a.r);
  else if (tag === 'ellipse') d = ellipsePath(+a.cx, +a.cy, +a.rx, +a.ry);
  const p = { ...defaults, ...a };
  const lines = [`${indent}<path`, `${indent}    android:pathData="${d}"`];
  const fill = color(p.fill);
  if (fill) lines.push(`${indent}    android:fillColor="${fill}"`);
  if (p['fill-rule'] === 'evenodd') lines.push(`${indent}    android:fillType="evenOdd"`);
  const stroke = color(p.stroke);
  if (stroke) {
    lines.push(`${indent}    android:strokeColor="${stroke}"`);
    lines.push(`${indent}    android:strokeWidth="${p['stroke-width'] || 1}"`);
    if (p['stroke-linecap']) lines.push(`${indent}    android:strokeLineCap="${p['stroke-linecap']}"`);
    if (p['stroke-linejoin']) lines.push(`${indent}    android:strokeLineJoin="${p['stroke-linejoin']}"`);
  }
  lines[lines.length - 1] += ' />';
  let xml = lines.join('\n');
  const rot = a.transform && a.transform.match(/rotate\(([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\)/);
  if (rot) {
    xml = `${indent}<group\n${indent}    android:rotation="${rot[1]}"\n${indent}    android:pivotX="${rot[2]}"\n` +
      `${indent}    android:pivotY="${rot[3]}">\n${xml.replace(/^/gm, '    ')}\n${indent}</group>`;
  }
  return xml;
}

function vector({ name, comment, width, height, viewport, body, defaults = {}, group }) {
  const [vw, vh] = viewport || [width, height];
  const indent = group ? '        ' : '    ';
  const els = [...body.matchAll(/<(path|rect|circle|ellipse)\s([^>]*?)\/?>/g)]
    .map((m) => element(m[1], attrs(m[2]), defaults, indent));
  let inner = els.join('\n');
  if (group) inner = `    <group\n${Object.entries(group).map(([k, v]) => `        android:${k}="${v}"`).join('\n')}>\n${inner}\n    </group>`;
  const xml = `<?xml version="1.0" encoding="utf-8"?>\n<!-- ${comment} -->\n` +
    `<vector xmlns:android="http://schemas.android.com/apk/res/android"\n` +
    `    android:width="${width}dp"\n    android:height="${height}dp"\n` +
    `    android:viewportWidth="${vw}"\n    android:viewportHeight="${vh}">\n${inner}\n</vector>\n`;
  fs.writeFileSync(path.join(out, name + '.xml'), xml);
}

const ICON_DEFAULTS = { stroke: '#000000', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', fill: 'none' };
const ICONS = {
  home: '<path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>',
  receipt: '<path d="M5 3h14v18l-3-2-2 2-2-2-2 2-2-2-3 2z"/><path d="M9 8h6M9 12h6M9 16h4"/>',
  chart: '<path d="M21 12A9 9 0 1 1 12 3v9z"/><path d="M15 3.5A9 9 0 0 1 20.5 9H15z"/>',
  wallet: '<rect x="3" y="6" width="18" height="14" rx="2"/><path d="M3 10h18M16 15h2M6 6V5a2 2 0 0 1 2-2h9"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  scan: '<path d="M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2M7 12h10"/>',
  camera: '<path d="M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z"/><circle cx="12" cy="13.5" r="3.5"/>',
  utensils: '<path d="M7 3v8M4 3v5a3 3 0 0 0 6 0V3M7 11v10M17 21V3c-2 1-3 4-3 7v3h3"/>',
  bus: '<rect x="4" y="3" width="16" height="15" rx="2"/><path d="M4 11h16M8 18v3M16 18v3"/><circle cx="8" cy="14.5" r="1"/><circle cx="16" cy="14.5" r="1"/>',
  graduation: '<path d="M2 9l10-5 10 5-10 5z"/><path d="M6 11v5c0 1.5 3 3 6 3s6-1.5 6-3v-5M22 9v6"/>',
  bag: '<path d="M5 7h14l-1 14H6z"/><path d="M9 10V6a3 3 0 0 1 6 0v4"/>',
  gamepad: '<rect x="2" y="7" width="20" height="11" rx="4"/><path d="M7 11v3M5.5 12.5h3"/><circle cx="16" cy="11.5" r="1"/><circle cx="18" cy="14" r="1"/>',
  health: '<path d="M20.8 6.6a5 5 0 0 0-8.8-1.4 5 5 0 0 0-8.8 1.4C2 11 7 15.5 12 20c5-4.5 10-9 8.8-13.4z"/><path d="M3.5 12h4l2-3 3 6 2-3h6"/>',
  bill: '<path d="M14 3H6a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8z"/><path d="M14 3v5h5M9 13h6M9 17h6"/>',
  more: '<circle cx="5" cy="12" r="1.2"/><circle cx="12" cy="12" r="1.2"/><circle cx="19" cy="12" r="1.2"/>',
  briefcase: '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2M3 13h18"/>',
  coins: '<circle cx="9" cy="9" r="6"/><path d="M14.9 9.2A6 6 0 1 1 9.2 14.9"/>',
  gift: '<rect x="3" y="8" width="18" height="4" rx="1"/><path d="M5 12v8h14v-8M12 8v12M12 8C10 4 6 4 7 7c.5 1 2 1 5 1zM12 8c2-4 6-4 5-1-.5 1-2 1-5 1z"/>',
  transfer: '<path d="M7 4 3 8l4 4M3 8h14M17 12l4 4-4 4M21 16H7"/>',
  card: '<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/>',
  cash: '<rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2.5"/><path d="M6 12h.01M18 12h.01"/>',
  ewallet: '<rect x="6" y="2" width="12" height="20" rx="2"/><path d="M11 18h2"/>',
  chevron_left: '<path d="M15 18 9 12 15 6"/>',
  chevron_right: '<path d="M9 18 15 12 9 6"/>',
  chevron_down: '<path d="M6 9 12 15 18 9"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
  filter: '<path d="M3 5h18l-7 8v6l-4 2v-8z"/>',
  settings: '<path d="M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12"/><circle cx="16" cy="6" r="2"/><circle cx="10" cy="12" r="2"/><circle cx="18" cy="18" r="2"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>',
  image: '<rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-5-5L5 21"/>',
  wifi_off: '<path d="M2 2l20 20M8.5 16.5a5 5 0 0 1 7 0M5 13a10 10 0 0 1 5.2-2.8M19 13a10 10 0 0 0-2.4-1.7M2 8.8a15 15 0 0 1 4.2-2.7M22 8.8A15 15 0 0 0 10.7 5M12 20h.01"/>',
  check: '<path d="M20 6 9 17 4 12"/>',
  close: '<path d="M18 6 6 18M6 6l12 12"/>',
  trash: '<path d="M3 6h18M8 6V4h8v2M6 6l1 15h10l1-15"/>',
  edit: '<path d="M16 3l5 5L8 21H3v-5z"/>',
  sparkles: '<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z"/>',
  tag: '<path d="M3 3h8l10 10-8 8L3 11z"/><circle cx="7.5" cy="7.5" r="1.5"/>',
  eye: '<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
  arrow_up: '<path d="M12 19V5M6 11l6-6 6 6"/>',
  arrow_down: '<path d="M12 5v14M6 13l6 6 6-6"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-7 8-7s8 3 8 7"/>',
};
for (const [k, body] of Object.entries(ICONS)) {
  vector({ name: 'ic_' + k, comment: 'Stroke icon from the Sakuin Figma icon set; tinted at runtime.', width: 24, height: 24, body, defaults: ICON_DEFAULTS });
}

const LOGO = `<rect width="120" height="120" rx="30" fill="#1F5BDD"/>
<circle cx="35.8" cy="26.7" r="9.9" fill="#FFD54F"/>
<rect x="24" y="31.2" width="64" height="13" rx="4" transform="rotate(-21 56 37.7)" fill="#4CB87A"/>
<rect x="31" y="38" width="64" height="12.5" rx="4" transform="rotate(-16 63 44.2)" fill="#FFCB3D"/>
<rect x="35" y="43.5" width="64" height="11.5" rx="4" transform="rotate(-8.5 67 49.2)" fill="#FF8552"/>
<rect x="18.6" y="47" width="83.4" height="58" rx="11" fill="#F9FAFC"/>
<path d="M86 63.5H104A2.5 2.5 0 0 1 106.5 66V86.7A2.5 2.5 0 0 1 104 89.2H86A12.85 12.85 0 0 1 86 63.5Z" fill="#5E91F7"/>
<circle cx="85.6" cy="76.35" r="4.9" fill="#F9FAFC"/>`;
// Android 12 splash icons are 240dp with a 160dp visible circle: scale the logo to fit inside it.
vector({ name: 'ic_splash_logo', comment: 'Splash screen icon: the logo centered inside the 160dp safe circle.', width: 240, height: 240, viewport: [240, 240], body: LOGO, group: { translateX: '66', translateY: '66', scaleX: '0.9', scaleY: '0.9' } });

// Adaptive launcher icon (108dp canvas, ~72dp visible): the logo art without its blue tile, scaled to
// about 44dp and centred; the tile colour becomes the background layer.
const LOGO_ART = LOGO.split('\n').slice(1).join('\n');
const LAUNCHER_GROUP = { translateX: '22.72', translateY: '23.55', scaleX: '0.5', scaleY: '0.5' };
vector({ name: 'ic_launcher_foreground', comment: 'Adaptive launcher icon foreground: the Sakuin logo art.', width: 108, height: 108, viewport: [108, 108], body: LOGO_ART, group: LAUNCHER_GROUP });
vector({ name: 'ic_launcher_background', comment: 'Adaptive launcher icon background: the logo tile colour.', width: 108, height: 108, body: '<rect width="108" height="108" fill="#1F5BDD"/>' });
// Themed (monochrome) icon: one colour, so the clasp is cut out of the wallet with an even-odd fill.
const walletWithClasp = rectPath({ x: 18.6, y: 47, width: 83.4, height: 58, rx: 11 }) + ellipsePath(85.6, 76.35, 4.9, 4.9);
// Coin + three cards, then the wallet body with the clasp hole.
const LOGO_MONO = LOGO_ART.split('\n').slice(0, 4).join('\n') + `\n<path fill-rule="evenodd" d="${walletWithClasp}" fill="#000000"/>`;
vector({ name: 'ic_launcher_monochrome', comment: 'Themed launcher icon (Android 13+): single-colour logo silhouette.', width: 108, height: 108, viewport: [108, 108], body: LOGO_MONO, group: LAUNCHER_GROUP });

const WALLET = `<rect x="80" y="70" width="120" height="32" rx="10" transform="rotate(-14 140 86)" fill="#4CB87A"/>
<rect x="86" y="84" width="120" height="30" rx="10" transform="rotate(-8 146 99)" fill="#FFCB3D"/>
<rect x="90" y="98" width="120" height="28" rx="10" transform="rotate(-3 150 112)" fill="#FF8552"/>
<rect x="56" y="112" width="168" height="82" rx="22" fill="#1F5BDD"/>
<path d="M202 134H228A6 6 0 0 1 234 140V166A6 6 0 0 1 228 172H202A19 19 0 0 1 202 134Z" fill="#5E91F7"/>
<circle cx="203" cy="153" r="7" fill="#FFFFFF"/>`;
const dashed = Array.from({ length: 7 }, (_, i) => `M${114 + i * 8} 136H${118 + i * 8}`).join('');
const ART = {
  track: [220, `<ellipse cx="140" cy="202" rx="104" ry="10" fill="#EDEFF2"/>
<circle cx="60" cy="58" r="18" fill="#FFCB3D"/><circle cx="60" cy="58" r="10" stroke="#F2B630" stroke-width="4"/>
${WALLET}
<rect x="206" y="10" width="64" height="54" rx="12" fill="#FFFFFF" stroke="#E1E4E9" stroke-width="2"/>
<rect x="216" y="40" width="8" height="14" rx="2" fill="#4CB87A"/><rect x="228" y="32" width="8" height="22" rx="2" fill="#D13A2C"/><rect x="240" y="24" width="8" height="30" rx="2" fill="#1F5BDD"/><rect x="252" y="36" width="8" height="18" rx="2" fill="#FFCB3D"/>
<circle cx="32" cy="132" r="5" fill="#FF8552"/><circle cx="254" cy="104" r="6" fill="#4CB87A"/>
<path d="M28 82V94M22 88H34" stroke="#1F5BDD" stroke-width="3" stroke-linecap="round"/>`],
  offline: [220, `<ellipse cx="140" cy="206" rx="96" ry="10" fill="#EDEFF2"/>
<rect x="92" y="14" width="96" height="184" rx="20" fill="#0E1014"/>
<rect x="99" y="26" width="82" height="160" rx="14" fill="#FFFFFF"/>
<rect x="108" y="38" width="64" height="34" rx="8" fill="#1F5BDD"/>
<rect x="114" y="46" width="24" height="5" rx="2.5" fill="#FFFFFF"/><rect x="114" y="58" width="40" height="7" rx="3.5" fill="#FFFFFF"/>
<circle cx="114" cy="90" r="6" fill="#EDEFF2"/><rect x="124" y="87" width="28" height="6" rx="3" fill="#E1E4E9"/><rect x="158" y="87" width="14" height="6" rx="3" fill="#D13A2C"/>
<circle cx="114" cy="112" r="6" fill="#EDEFF2"/><rect x="124" y="109" width="24" height="6" rx="3" fill="#E1E4E9"/><rect x="158" y="109" width="14" height="6" rx="3" fill="#4CB87A"/>
<circle cx="114" cy="134" r="6" fill="#EDEFF2"/><rect x="124" y="131" width="30" height="6" rx="3" fill="#E1E4E9"/><rect x="158" y="131" width="14" height="6" rx="3" fill="#D13A2C"/>
<path d="M206 112L236 123V148C236 168 222 182 206 188C190 182 176 168 176 148V123Z" fill="#4CB87A"/>
<path d="M193 150L202 159L220 139" stroke="#FFFFFF" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
<circle cx="62" cy="70" r="28" fill="#FFCB3D"/>
<path d="M46 68Q62 54 78 68M52 75Q62 67 72 75" stroke="#0E1014" stroke-width="3.5" stroke-linecap="round"/><circle cx="62" cy="82" r="3" fill="#0E1014"/>
<path d="M48 54L76 86" stroke="#0E1014" stroke-width="3.5" stroke-linecap="round"/>
<circle cx="246" cy="60" r="6" fill="#FF8552"/><circle cx="40" cy="152" r="5" fill="#1F5BDD"/>`],
  scan: [220, `<ellipse cx="140" cy="206" rx="96" ry="10" fill="#EDEFF2"/>
<path d="M100 26H180V186L170 178L160 186L150 178L140 186L130 178L120 186L110 178L100 186Z" fill="#FFFFFF" stroke="#E1E4E9" stroke-width="3" stroke-linejoin="round"/>
<rect x="116" y="42" width="48" height="8" rx="4" fill="#0E1014"/><rect x="124" y="56" width="32" height="5" rx="2.5" fill="#E1E4E9"/>
<rect x="114" y="76" width="34" height="5" rx="2.5" fill="#E1E4E9"/><rect x="154" y="76" width="12" height="5" rx="2.5" fill="#E1E4E9"/>
<rect x="114" y="90" width="28" height="5" rx="2.5" fill="#E1E4E9"/><rect x="154" y="90" width="12" height="5" rx="2.5" fill="#E1E4E9"/>
<rect x="114" y="118" width="38" height="5" rx="2.5" fill="#E1E4E9"/><rect x="154" y="118" width="12" height="5" rx="2.5" fill="#E1E4E9"/>
<path d="${dashed}" stroke="#E1E4E9" stroke-width="3"/>
<rect x="114" y="148" width="22" height="7" rx="3.5" fill="#0E1014"/><rect x="142" y="148" width="24" height="7" rx="3.5" fill="#D13A2C"/>
<path d="M80 34V22A8 8 0 0 1 88 14H100M180 14H192A8 8 0 0 1 200 22V34M200 170V182A8 8 0 0 1 192 190H180M100 190H88A8 8 0 0 1 80 182V170" stroke="#1F5BDD" stroke-width="5" stroke-linecap="round"/>
<rect x="84" y="104" width="112" height="4" rx="2" fill="#1F5BDD"/>
<path d="M234 36L239 50L253 55L239 60L234 74L229 60L215 55L229 50Z" fill="#FFCB3D"/>
<path d="M50 120L53 128L61 131L53 134L50 142L47 134L39 131L47 128Z" fill="#4CB87A"/>
<rect x="206" y="128" width="62" height="28" rx="14" fill="#FF8552"/><circle cx="221" cy="142" r="6" fill="#FFFFFF"/><rect x="232" y="139" width="24" height="6" rx="3" fill="#FFFFFF"/>`],
  profile: [200, `<ellipse cx="140" cy="190" rx="92" ry="9" fill="#EDEFF2"/>
<rect x="58" y="78" width="164" height="100" rx="18" fill="#FFFFFF" stroke="#E1E4E9" stroke-width="3"/>
<circle cx="102" cy="128" r="28" fill="#1F5BDD"/>
<circle cx="102" cy="119" r="10" fill="#FFFFFF"/>
<path d="M84 145C86 135 93 131 102 131C111 131 118 135 120 145" fill="#FFFFFF"/>
<rect x="142" y="110" width="58" height="8" rx="4" fill="#0E1014"/><rect x="142" y="126" width="42" height="6" rx="3" fill="#E1E4E9"/><rect x="142" y="140" width="50" height="6" rx="3" fill="#E1E4E9"/>
<path d="M176 16H236A12 12 0 0 1 248 28V50A12 12 0 0 1 236 62H206L194 74V62H176A12 12 0 0 1 164 50V28A12 12 0 0 1 176 16Z" fill="#4CB87A"/>
<circle cx="188" cy="39" r="4" fill="#FFFFFF"/><circle cx="206" cy="39" r="4" fill="#FFFFFF"/><circle cx="224" cy="39" r="4" fill="#FFFFFF"/>
<circle cx="50" cy="50" r="12" fill="#FFCB3D"/><circle cx="252" cy="150" r="6" fill="#FF8552"/><circle cx="34" cy="120" r="4" fill="#1F5BDD"/>`],
  done: [220, `<ellipse cx="140" cy="204" rx="96" ry="10" fill="#EDEFF2"/>
${WALLET}
<circle cx="222" cy="62" r="26" fill="#4CB87A"/><path d="M210 62L218 70L234 54" stroke="#FFFFFF" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
<rect x="40" y="30" width="10" height="16" rx="3" transform="rotate(-20 45 38)" fill="#D13A2C"/>
<rect x="96" y="12" width="10" height="16" rx="3" transform="rotate(25 101 20)" fill="#1F5BDD"/>
<rect x="150" y="26" width="10" height="16" rx="3" transform="rotate(-35 155 34)" fill="#FFCB3D"/>
<rect x="250" y="118" width="10" height="16" rx="3" transform="rotate(30 255 126)" fill="#FF8552"/>
<rect x="24" y="108" width="10" height="16" rx="3" transform="rotate(15 29 116)" fill="#4CB87A"/>
<circle cx="70" cy="76" r="5" fill="#FF8552"/><circle cx="262" cy="24" r="5" fill="#D13A2C"/><circle cx="128" cy="50" r="4" fill="#4CB87A"/>`],
};
for (const [k, [h, body]] of Object.entries(ART)) {
  vector({ name: 'ill_' + k, comment: `Onboarding illustration "${k}" (Figma component Illustration/${k}).`, width: 280, height: h, body });
}
console.log(`icons: ${Object.keys(ICONS).length}, illustrations: ${Object.keys(ART).length}, logo: 1, launcher: 3`);
