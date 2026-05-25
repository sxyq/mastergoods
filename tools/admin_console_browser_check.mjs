import fs from "node:fs/promises";
import path from "node:path";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices } = require("playwright");

const baseUrl = process.argv[2] || "http://127.0.0.1:18080";
const outputDir = process.argv[3] || path.resolve("backend/build/reports/admin-console/browser");

await fs.mkdir(outputDir, { recursive: true });

async function resolveBrowserExecutable() {
  const candidates = [
    "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
    "C:/Program Files/Microsoft/Edge/Application/msedge.exe",
    "C:/Program Files/Google/Chrome/Application/chrome.exe",
    "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
  ];
  for (const candidate of candidates) {
    try {
      await fs.access(candidate);
      return candidate;
    } catch {
    }
  }
  return null;
}

const executablePath = await resolveBrowserExecutable();
if (!executablePath) {
  throw new Error("No local Edge/Chrome executable found for browser check");
}

const browser = await chromium.launch({ headless: true, executablePath });
const checks = [];

async function runViewportCheck(name, contextOptions) {
  const context = await browser.newContext(contextOptions);
  const page = await context.newPage();
  await page.goto(`${baseUrl}/admin-console/index.html`, { waitUntil: "networkidle", timeout: 30000 });

  await page.waitForSelector("text=仓管 Pro 管理后台");
  await page.waitForSelector("text=用户列表");
  await page.waitForSelector("text=Smoke 结果");
  await page.waitForSelector("text=默认登录身份");

  const rawText = await page.locator("body").innerText();
  if (rawText.includes("�")) {
    throw new Error(`${name}: page text contains replacement character`);
  }

  const summaryCards = await page.locator(".summary-card").count();
  if (summaryCards !== 8) {
    throw new Error(`${name}: expected 8 summary cards, got ${summaryCards}`);
  }

  const tableWrapOverflow = await page.locator(".table-wrap").evaluate((node) => getComputedStyle(node).overflowX);
  if (tableWrapOverflow !== "auto") {
    throw new Error(`${name}: expected table-wrap overflow-x to be auto, got ${tableWrapOverflow}`);
  }

  const formBox = await page.locator(".form-panel").boundingBox();
  const smokeBox = await page.locator(".smoke-panel").boundingBox();
  const tableBox = await page.locator(".table-panel").boundingBox();
  if (!formBox || !smokeBox || !tableBox) {
    throw new Error(`${name}: one or more key panels were not rendered`);
  }
  if (formBox.height < 200 || smokeBox.height < 160 || tableBox.width < 320) {
    throw new Error(`${name}: key panels rendered with unexpected dimensions`);
  }

  const screenshotPath = path.join(outputDir, `${name}.png`);
  await page.screenshot({ path: screenshotPath, fullPage: true });

  checks.push({
    viewport: name,
    url: page.url(),
    summaryCards,
    screenshotPath,
    formBox,
    smokeBox,
    tableBox,
  });

  await context.close();
}

await runViewportCheck("desktop", { viewport: { width: 1440, height: 1080 } });
await runViewportCheck("mobile", { ...devices["iPhone 13"] });

await browser.close();

const reportPath = path.join(outputDir, "browser-check.json");
await fs.writeFile(reportPath, JSON.stringify({ baseUrl, executablePath, checks }, null, 2), "utf8");
console.log(reportPath);
