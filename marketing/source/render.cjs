// Renders the listing artwork in marketing/images/ from real in-game screenshots, the mod logo and the bundled OFL fonts.
// Usage (from the repository root): NODE_PATH="$(npm root -g)" node marketing/source/render.cjs [name...]
// Requires the `playwright` package with a Chromium build available.
const path = require('path');
const fs = require('fs');
const { chromium } = require('playwright');

// The release and the supported Minecraft versions. Adding a version is one line here
// (plus one row in the compatibility table of curseforge/description.md).
const RELEASE = { version: '1.0', name: 'Remember or Forget' };
const VERSIONS = [
  { minecraft: '1.20.1', loaders: ['Fabric', 'Forge'] },
  { minecraft: '1.21.1', loaders: ['Fabric', 'NeoForge'] },
  { minecraft: '26.1.2', loaders: ['Fabric', 'NeoForge'] },
  { minecraft: '26.2', loaders: ['Fabric', 'NeoForge'] },
  { minecraft: '26.3', loaders: ['Fabric', 'NeoForge'] },
];
const LOADERS = ['Fabric', 'Forge', 'NeoForge'].filter((loader) => VERSIONS.some((v) => v.loaders.includes(loader)));

const root = path.resolve(__dirname, '..', '..');
const out = path.join(root, 'marketing', 'images');
const shots = path.join(out, 'screenshots');
const fonts = path.join(__dirname, 'fonts');
const url = (file) => 'file://' + file;
const shot = (name) => url(path.join(shots, name));
const source = (name) => url(path.join(__dirname, name));
const gui = (name) => url(path.join(root, 'shared/common/src/main/resources/assets/tbos/textures/gui', name));

// Rarity colours match relic/Rarity.java; Distortion and Insight icons are cells of the GUI icon sheet.
const rarity = { common: '#c9c4ba', uncommon: '#7fd67a', rare: '#62a9f2', epic: '#b97af0', legendary: '#f2a43a', remnant: '#ea4b3f' };

const css = `
@font-face { font-family: RdSerif; src: url('${url(path.join(fonts, 'eb-garamond.ttf'))}'); }
@font-face { font-family: RdDisplay; src: url('${url(path.join(fonts, 'cinzel.ttf'))}'); }
:root { --text:#e3dac5; --dim:#a59d8a; --faint:#6f695d; --header:#f1e7cd; --gold:#d6aa5e; --gold-hi:#f3d796;
        --teal:#5fd0c8; --teal-hi:#9cf2e6; --red:#e0645a; --bg:#06080a; --line:rgba(216,207,184,.32); }
* { box-sizing: border-box; }
html, body { margin: 0; background: var(--bg); color: var(--text); font-family: RdSerif, serif; }
.stage { position: relative; overflow: hidden; background: radial-gradient(ellipse at 30% 40%, #12191b 0%, #06080a 70%); }
.display { font-family: RdDisplay, serif; letter-spacing: .04em; }
.gold { background: linear-gradient(180deg, #fff3cf 0%, #f3d796 35%, #d6aa5e 70%, #8a6a38 100%);
        -webkit-background-clip: text; background-clip: text; color: transparent; }
.teal { color: var(--teal-hi); }
.glow { filter: drop-shadow(0 0 18px rgba(95,208,200,.32)); }
.kicker { font-family: RdDisplay, serif; color: var(--teal); letter-spacing: .22em; font-size: 22px; }
.rule { height: 1px; background: linear-gradient(90deg, rgba(214,170,94,0), rgba(214,170,94,.9) 20%, rgba(214,170,94,.9) 70%, rgba(214,170,94,0)); }
.diamond { display: inline-block; width: 9px; height: 9px; background: var(--gold); transform: rotate(45deg); margin: 0 12px 3px 0; }
.frame { position: relative; border: 1px solid rgba(216,207,184,.45); box-shadow: 0 0 0 1px rgba(0,0,0,.6), 0 30px 80px rgba(0,0,0,.65), 0 0 60px rgba(95,208,200,.14); background: #000; }
.frame > img { display: block; width: 100%; }
canvas.motes { position: absolute; left: 0; top: 0; width: 100%; height: 100%; pointer-events: none; }
.vignette { position: absolute; inset: 0; background: radial-gradient(ellipse at center, rgba(0,0,0,0) 45%, rgba(0,0,0,.78) 100%); }
.backdrop { position: absolute; inset: -40px; background-size: cover; background-position: center; filter: blur(16px) brightness(.4) saturate(1.1); }
.pill { display: inline-block; border: 1px solid rgba(216,207,184,.55); padding: 6px 14px 4px; margin-right: 10px;
        font-family: RdDisplay, serif; font-size: 18px; letter-spacing: .12em; color: var(--header); background: rgba(0,0,0,.35); }
.label { position: absolute; font-family: RdDisplay, serif; font-size: 20px; letter-spacing: .14em; color: var(--header);
         background: rgba(7,10,11,.88); border: 1px solid rgba(95,208,200,.75); padding: 7px 14px 5px; white-space: nowrap;
         box-shadow: 0 8px 26px rgba(0,0,0,.6); z-index: 2; }
.label small { display: block; font-family: RdSerif, serif; letter-spacing: 0; font-size: 18px; color: var(--dim); margin-top: 1px; }
ul.points { list-style: none; padding: 0; margin: 0; }
ul.points li { position: relative; padding-left: 26px; margin: 0 0 14px; font-size: 27px; line-height: 1.32; color: var(--text); }
ul.points li::before { content: ''; position: absolute; left: 2px; top: 13px; width: 8px; height: 8px; background: var(--gold); transform: rotate(45deg); }
.stats { display: flex; margin-top: 22px; border: 1px solid rgba(216,207,184,.35); background: rgba(7,10,11,.84); box-shadow: 0 18px 50px rgba(0,0,0,.55); }
.stats > div { flex: 1; padding: 14px 24px 12px; border-left: 1px solid rgba(216,207,184,.2); }
.stats > div:first-child { border-left: 0; }
.stats .t { font-family: RdDisplay, serif; font-size: 16px; letter-spacing: .18em; color: var(--dim); }
.stats .v { font-family: RdDisplay, serif; font-size: 30px; color: var(--header); margin-top: 4px; white-space: nowrap; }
b.hi { color: var(--header); font-weight: normal; }
.tile { background: rgba(7,10,11,.9); border: 1px solid rgba(216,207,184,.32); box-shadow: 0 18px 50px rgba(0,0,0,.5); }
.icon { display: inline-block; background-image: url('${gui('rpg/icons.png')}'); background-repeat: no-repeat; flex: none; }
`;

// Drifting memory motes, teal with a few bronze ones. Deterministic so renders are reproducible.
const motes = `<canvas class="motes"></canvas><script>
(() => { const c = document.querySelector('canvas.motes'); const r = c.getBoundingClientRect();
  c.width = r.width; c.height = r.height; const g = c.getContext('2d'); let s = 11;
  const rnd = () => (s = (s * 16807) % 2147483647) / 2147483647;
  for (let i = 0; i < 90; i++) { const x = rnd() * c.width, y = rnd() * c.height, a = .25 + rnd() * .6, z = 1 + rnd() * 2.2;
    const col = rnd() < .75 ? '156,242,230' : '243,215,150';
    const h = g.createRadialGradient(x, y, 0, x, y, z * 5); h.addColorStop(0, 'rgba(' + col + ',' + a * .5 + ')'); h.addColorStop(1, 'rgba(' + col + ',0)');
    g.fillStyle = h; g.beginPath(); g.arc(x, y, z * 5, 0, Math.PI * 2); g.fill();
    g.fillStyle = 'rgba(' + col + ',' + a + ')'; g.fillRect(x - z / 2, y - z / 2, z, z); } })();
</script>`;

const page = (width, height, body, transparent = false) => `<!doctype html><html><head><meta charset="utf-8"><style>${css}
body { width: ${width}px; height: ${height}px; } .stage { width: ${width}px; height: ${height}px; }
${transparent ? 'html, body, .stage { background: transparent; }' : ''}</style></head>
<body><div class="stage">${body}</div></body></html>`;

// One cell of the 1.0 GUI icon sheet (8 columns of 128 px), drawn at `size` pixels.
const icon = (index, size, style = '') => {
  const scale = size / 128;
  return `<span class="icon" style="width:${size}px;height:${size}px;background-size:${1024 * scale}px ${1024 * scale}px;`
    + `background-position:${-(index % 8) * 128 * scale}px ${-Math.floor(index / 8) * 128 * scale}px;${style}"></span>`;
};
const ICON = { distortion: (i) => i, insight: (i) => 8 + i, rarity: (i) => 24 + i, renown: 30, key: 31, hourglass: 32, party: 33,
  weekly: 34, records: 35, bestiary: 36, relic: 37, remember: 38, forget: 39, momentum: 40, insightPoint: 41, leader: 42, archive: 43, sealed: 44 };

const header = (title) => page(1400, 120, `
  <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:28px;background:linear-gradient(90deg,rgba(0,0,0,0),rgba(10,14,15,.9) 30%,rgba(10,14,15,.9) 70%,rgba(0,0,0,0))">
    <div class="rule" style="width:300px;transform:scaleX(-1)"></div>
    <span class="diamond" style="margin:0"></span>
    <div class="display gold glow" style="font-size:44px;white-space:nowrap">${title}</div>
    <span class="diamond" style="margin:0"></span>
    <div class="rule" style="width:300px"></div>
  </div>`, true);

// A feature card: copy on the left, artwork on the right. Bullets never end on a one-word line.
const card = ({ kicker, title, points, art, width = 1600, height = 900, artWidth = 980, backdrop = 'backdrop.jpg' }) => page(width, height, `
  <div class="backdrop" style="background-image:url('${source(backdrop)}');opacity:.55"></div>
  <div class="vignette"></div>${motes}
  <div style="position:absolute;left:80px;top:0;bottom:0;width:${width - artWidth - 150}px;display:flex;flex-direction:column;justify-content:center">
    <div class="kicker"${kicker.length > 24 ? ' style="font-size:18px;letter-spacing:.12em"' : ''}><span class="diamond"></span>${kicker}</div>
    <div class="display gold glow" style="font-size:58px;line-height:1.08;margin:18px 0 22px">${title}</div>
    <div class="rule" style="width:80%;margin-bottom:30px"></div>
    <ul class="points">${points.map((point) => `<li>${point.replace(/ (\S+)$/, '&nbsp;$1')}</li>`).join('')}</ul>
  </div>
  <div style="position:absolute;right:60px;top:50%;transform:translateY(-50%);width:${artWidth}px">${art}</div>`);

// An exact region (x, y, w, h in 1920x1080 pixels) of a capture, scaled to W pixels wide.
const region = (file, x, y, w, h, W) => `<div style="position:relative;width:${W}px;height:${Math.round(W * h / w)}px;overflow:hidden">
    <img src="${shot(file)}" style="position:absolute;max-width:none;width:${W * 1920 / w}px;left:${-x * W / w}px;top:${-y * W / w}px"></div>`;
const framed = (inner, style = '', color = '') => `<div class="frame" style="${color ? `border-color:${color}99;` : ''}${style}">${inner}</div>`;
const label = (text, style, small = '') => `<div class="label" style="${style}">${text}${small ? `<small>${small}</small>` : ''}</div>`;
const stats = (items, color) => `<div class="stats"${color ? ` style="border-top:2px solid ${color}"` : ''}>${items.map(([t, v, flex]) =>
  `<div${flex ? ` style="flex:${flex}"` : ''}><div class="t">${t}</div><div class="v">${v}</div></div>`).join('')}</div>`;
const pair = (first, second, firstLabel, secondLabel, firstSpot = 'left:20px;top:20px', height = 700) => `<div style="position:relative;height:${height}px">
    <div class="frame" style="position:absolute;left:0;top:0;width:74%"><img src="${shot(first)}" style="display:block;width:100%"></div>
    <div class="frame" style="position:absolute;right:0;bottom:0;width:74%"><img src="${shot(second)}" style="display:block;width:100%"></div>
    ${label(firstLabel, firstSpot)}${label(secondLabel, 'right:20px;bottom:20px')}</div>`;
// A row of icon tiles: [icon index, title, line].
const tiles = (rows, columns = 2, size = 64) => `<div style="display:grid;grid-template-columns:repeat(${columns},1fr);gap:14px">${rows.map(([index, title, line, color]) =>
  `<div class="tile" style="display:flex;align-items:center;gap:18px;padding:14px 18px;${color ? `border-left:3px solid ${color}` : ''}">${icon(index, size)}
    <div><div class="display" style="font-size:23px;color:var(--header)">${title}</div><div style="font-size:21px;color:var(--dim);line-height:1.25;margin-top:2px">${line}</div></div></div>`).join('')}</div>`;

const versionText = VERSIONS.map((v) => v.minecraft).join(' · ');

const assets = {
  icon: () => page(512, 512, `
    <div style="position:absolute;inset:0;background:radial-gradient(circle at 50% 45%,#1a2526 0%,#0b1012 55%,#050708 100%)"></div>${motes}
    <div style="position:absolute;left:78px;top:78px;width:356px;height:356px;border-radius:50%;overflow:hidden;box-shadow:inset 0 0 30px rgba(0,0,0,.8)">
      <img src="${source('logo.png')}" style="position:absolute;left:-14px;top:-6px;width:384px;height:384px;image-rendering:pixelated"></div>
    <div style="position:absolute;left:56px;top:56px;width:400px;height:400px;border-radius:50%;border:3px solid var(--gold);box-shadow:0 0 40px rgba(95,208,200,.4), inset 0 0 40px rgba(214,170,94,.25)"></div>
    <div style="position:absolute;left:78px;top:78px;width:356px;height:356px;border-radius:50%;border:1px solid rgba(214,170,94,.55)"></div>
    ${[[256, 56], [456, 256], [256, 456], [56, 256]].map(([x, y]) => `<div style="position:absolute;left:${x - 12}px;top:${y - 12}px;width:24px;height:24px;background:var(--gold);transform:rotate(45deg);box-shadow:0 0 16px rgba(156,242,230,.75)"></div>`).join('')}`),
};

const headers = {};
for (const [name, title] of Object.entries(headers)) assets[name] = () => header(title);
assets.divider = () => page(1400, 40, `<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:18px">
  <div class="rule" style="width:520px;transform:scaleX(-1)"></div><span class="diamond" style="margin:0"></span><div class="rule" style="width:520px"></div></div>`, true);

module.exports = { assets, headers, header, card, region, framed, label, stats, pair, tiles, icon, ICON, rarity, shot, source, motes, page, versionText, LOADERS, RELEASE };

if (require.main === module) {
  (async () => {
    const wanted = process.argv.slice(2);
    const browser = await chromium.launch();
    fs.mkdirSync(out, { recursive: true });
    for (const [name, build] of Object.entries(assets)) {
      if (wanted.length && !wanted.includes(name)) continue;
      const html = build();
      const size = html.match(/body \{ width: (\d+)px; height: (\d+)px; \}/);
      const tab = await browser.newPage({ viewport: { width: +size[1], height: +size[2] }, deviceScaleFactor: 1 });
      const file = path.join(__dirname, `.render-${name}.html`);
      fs.writeFileSync(file, html);
      await tab.goto(url(file));
      await tab.evaluate(() => document.fonts.ready);
      await tab.waitForLoadState('networkidle');
      await tab.screenshot({ path: path.join(out, `${name}.png`), omitBackground: name === 'divider' || name.startsWith('header-') });
      fs.unlinkSync(file);
      await tab.close();
      console.log('rendered', name);
    }
    await browser.close();
  })().catch((error) => { console.error(error); process.exit(1); });
}
