// Renders an HTML file to a transparent PNG with the pre-installed Chromium.
// Usage: node tools/rpg/render.cjs <in.html> <out.png> <width> <height>
const { chromium } = require('playwright');
const path = require('path');

(async () => {
  const [src, out, width, height] = process.argv.slice(2);
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: Number(width), height: Number(height) }, deviceScaleFactor: 1 });
  await page.goto('file://' + path.resolve(src));
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: out, omitBackground: true, clip: { x: 0, y: 0, width: Number(width), height: Number(height) } });
  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
