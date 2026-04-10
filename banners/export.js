const puppeteer = require('puppeteer');
const path = require('path');

const BANNERS = [
  { id: 'banner-2400x250', width: 2400, height: 250, file: '2400x250.png' },
  { id: 'banner-1456x180', width: 1456, height: 180, file: '1456x180.png' },
  { id: 'banner-640x200',  width: 640,  height: 200, file: '640x200.png' },
  { id: 'banner-600x500',  width: 600,  height: 500, file: '600x500.png' },
];

(async () => {
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });

  const page = await browser.newPage();

  // Set a large viewport so all banners render without wrapping
  await page.setViewport({ width: 2600, height: 2000, deviceScaleFactor: 1 });

  const htmlPath = path.resolve(__dirname, 'banner.html');
  await page.goto(`file://${htmlPath}`, { waitUntil: 'networkidle0' });

  // Wait for logo images to load
  await page.evaluate(() => {
    return Promise.all(
      Array.from(document.images)
        .filter(img => !img.complete)
        .map(img => new Promise((resolve, reject) => {
          img.onload = resolve;
          img.onerror = reject;
        }))
    );
  });

  for (const banner of BANNERS) {
    const element = await page.$(`#${banner.id}`);
    if (!element) {
      console.error(`Element #${banner.id} not found!`);
      continue;
    }

    const outputPath = path.resolve(__dirname, banner.file);

    await element.screenshot({
      path: outputPath,
      type: 'png',
      omitBackground: false,
    });

    // Verify dimensions
    const box = await element.boundingBox();
    const match = box.width === banner.width && box.height === banner.height;
    console.log(
      `${match ? '✓' : '✗'} ${banner.file} — rendered ${box.width}x${box.height}` +
      (match ? '' : ` (expected ${banner.width}x${banner.height})`)
    );
  }

  await browser.close();
  console.log('\nDone. Banners saved to:', path.resolve(__dirname));
})();
