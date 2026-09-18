/** Standalone hard-fail against shipped assets (no pack rebuild). */
import fs from "fs";
import path from "path";
import zlib from "zlib";
import { fileURLToPath } from "url";
import { TORAH, NEVIIM, KETUVIM } from "./books.mjs";
import { runHardFailChecks } from "./hard-fail-checks.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../..");
const DATA = path.join(ROOT, "app/src/main/assets/data");
const REPORTS = path.join(ROOT, "reports");
const VENDOR = path.join(ROOT, "vendor");

function readJsonMaybeGz(fp) {
  const buf = fs.readFileSync(fp);
  if (fp.endsWith(".gz") || (buf[0] === 0x1f && buf[1] === 0x8b)) {
    return JSON.parse(zlib.gunzipSync(buf).toString("utf8"));
  }
  return JSON.parse(buf.toString("utf8"));
}

function loadPack(osis) {
  const gz = path.join(DATA, "books", `${osis}.json.gz`);
  const plain = path.join(DATA, "books", `${osis}.json`);
  if (fs.existsSync(gz)) return readJsonMaybeGz(gz);
  if (fs.existsSync(plain)) return readJsonMaybeGz(plain);
  throw new Error(`missing pack ${osis}`);
}

const catalog = readJsonMaybeGz(path.join(DATA, "catalog.json"));
const glossPath = fs.existsSync(path.join(DATA, "glosses.json.gz"))
  ? path.join(DATA, "glosses.json.gz")
  : path.join(DATA, "glosses.json");
const glossCatalog = readJsonMaybeGz(glossPath);

function ketivFromReport(name) {
  const fp = path.join(REPORTS, name);
  if (!fs.existsSync(fp)) return [];
  const raw = JSON.parse(fs.readFileSync(fp, "utf8"));
  if (Array.isArray(raw)) return raw;
  return raw.entries || [];
}

function runFor(label, books, reportName, ketivName) {
  const packByBook = Object.fromEntries(books.map((b) => [b, loadPack(b)]));
  const cat = {
    ...catalog,
    books: catalog.books.filter((b) => books.includes(b.osis)),
  };
  const ketivQere = ketivFromReport(ketivName).filter((e) => books.includes(e.book));
  const result = runHardFailChecks({
    catalog: cat,
    packByBook,
    glossCatalog,
    ketivQere,
    vendorRoot: VENDOR,
  });
  fs.writeFileSync(path.join(REPORTS, reportName), JSON.stringify(result, null, 2));
  console.log(
    `${label}: ${result.ok ? "PASSED" : "FAILED"} failures=${result.failures.length} → reports/${reportName}`
  );
  if (!result.ok) {
    for (const f of result.failures.slice(0, 30)) console.log("  -", f);
    if (result.failures.length > 30) console.log(`  … +${result.failures.length - 30} more`);
  }
  return result;
}

const allBooks = catalog.books.map((b) => b.osis);
const ket = allBooks.filter((b) => KETUVIM.includes(b));
const tn = allBooks.filter((b) => TORAH.includes(b) || NEVIIM.includes(b));

const rKet = runFor("ketuvim", ket, "ketuvim-hard-fail.json", "ketuvim-ketiv-qere.json");
const rAll = runFor("tanakh", allBooks, "tanakh-hard-fail.json", "tanakh-ketiv-qere.json");

if (!rKet.ok || !rAll.ok) process.exit(1);
console.log("hard-fail green");
