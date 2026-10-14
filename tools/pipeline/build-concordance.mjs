/**
 * Concordance index (Sofer gate spec 2026-10-13). Build-time only, from shipped
 * pack JSON (+ TBESH / HebrewStrong for the header headword). No runtime lookup.
 *
 * Output: app/src/main/assets/data/concordance.json.gz
 *   { schema, packVersion, packHash, totals, keys: { "H:H1121a": {c, p, hw, r} } }
 *   - key  = lang:lemmaId, lang = first char of morph (H|A)
 *   - c    = count (== rows length; the ONLY source of "Appears N times")
 *   - p    = isParticle (always false in keys; particles live in `particles`)
 *   - hw   = dictionary headword (TBESH col 4 → HebrewStrong fallback; DN = bare consonants)
 *   - r    = ordered rows "verseId:wordIndex[k]" joined by "," (k = ketiv/qere flag).
 *            Compact string encoding of the [verseId, wordIndex] list so the
 *            loader can page without materialising every row (H3068 perf).
 *   particles: { key: count } (G6 rule) — function-word card, no list.
 * packHash = sha256 over (osis + "\n" + decompressed pack bytes + "\n") in catalog order.
 * Also stamps catalog.json `concordance` {asset, assetGz, packHash} and writes
 * reports/concordance_gate_G{1,2,3,5,6,7}.json (G4 is rendered-string, see JVM test).
 * Exit 1 on any HARD-FAIL.
 */
import fs from "fs";
import path from "path";
import zlib from "zlib";
import crypto from "crypto";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../..");
const DATA = path.join(ROOT, "app/src/main/assets/data");
const REPORTS = path.join(ROOT, "reports");
const VENDOR = path.join(ROOT, "vendor");
const SEED = 20261013;

function gunzipMaybe(buf) {
  return buf[0] === 0x1f && buf[1] === 0x8b ? zlib.gunzipSync(buf) : buf;
}
function readPackBytes(osis) {
  const gz = path.join(DATA, "books", `${osis}.json.gz`);
  const plain = path.join(DATA, "books", `${osis}.json`);
  return gunzipMaybe(fs.readFileSync(fs.existsSync(gz) ? gz : plain));
}

export function isParticleToken(w) {
  return /^H9\d{3}/.test(w.lemmaId || "") || (w.glossId || "").startsWith("pfx:");
}
export function langOf(w) {
  const c = (w.morph || "H")[0];
  return c === "A" ? "A" : "H";
}
const stripPoints = (s) => s.replace(/[\u0591-\u05C7]/g, "");
const cons = (s) => [...s].filter((ch) => ch >= "\u05D0" && ch <= "\u05EA").join("");

function headwords() {
  const tb = new Map();
  for (const line of fs.readFileSync(path.join(VENDOR, "TBESH.txt"), "utf8").split(/\r?\n/)) {
    if (!line.startsWith("H")) continue;
    const c = line.split("\t");
    if (c.length > 3 && c[3] && !tb.has(c[0].trim())) tb.set(c[0].trim(), c[3].trim());
  }
  const st = new Map();
  const xml = fs.readFileSync(path.join(VENDOR, "HebrewStrong.xml"), "utf8");
  for (const m of xml.matchAll(/<entry id="(H\d+)">\s*<w[^>]*>([^<]+)<\/w>/g)) st.set(m[1], m[2]);
  return (lemmaId) => {
    let hw = tb.get(lemmaId);
    if (!hw) {
      const n = (lemmaId.match(/^H0*(\d+)/) || [])[1];
      if (n) hw = st.get(`H${n}`);
    }
    if (!hw) return null;
    if (/^H306[89]/.test(lemmaId) || cons(hw).includes("יהוה")) hw = stripPoints(hw);
    return hw;
  };
}

function mulberry32(a) {
  return () => {
    a |= 0; a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function buildConcordance() {
  const catalogPath = path.join(DATA, "catalog.json");
  const catalog = JSON.parse(fs.readFileSync(catalogPath, "utf8"));
  const hash = crypto.createHash("sha256");
  const packs = [];
  for (const b of catalog.books) {
    const bytes = readPackBytes(b.osis);
    hash.update(b.osis + "\n"); hash.update(bytes); hash.update("\n");
    packs.push(JSON.parse(bytes.toString("utf8")));
  }
  const packHash = hash.digest("hex");
  const hwOf = headwords();

  const rows = new Map(); // key -> [[verseId, wi, k]]
  const particles = new Map();
  let total = 0, excludedParticle = 0, excludedNoLemma = 0, ketivWords = 0, ketivExcluded = 0;
  const wordAt = new Map(); // verseId -> words
  for (const p of packs) {
    for (const v of p.verses) {
      wordAt.set(v.id, v.words);
      v.words.forEach((w, wi) => {
        total++;
        if (w.ketiv) ketivWords++;
        const lang = langOf(w);
        if (isParticleToken(w)) {
          excludedParticle++;
          if (w.ketiv) ketivExcluded++;
          const k = `${lang}:${w.lemmaId || w.glossId}`;
          particles.set(k, (particles.get(k) || 0) + 1);
          return;
        }
        if (!w.lemmaId) { excludedNoLemma++; if (w.ketiv) ketivExcluded++; return; }
        const key = `${lang}:${w.lemmaId}`;
        if (!rows.has(key)) rows.set(key, []);
        rows.get(key).push([v.id, wi, w.ketiv ? 1 : 0]);
      });
    }
  }

  const keys = {};
  for (const [key, list] of rows) {
    const lemmaId = key.slice(2);
    keys[key] = {
      c: list.length,
      p: false,
      hw: hwOf(lemmaId),
      r: list.map(([v, i, k]) => `${v}:${i}${k ? "k" : ""}`).join(","),
    };
  }
  const indexed = [...rows.values()].reduce((a, l) => a + l.length, 0);
  const index = {
    schema: 1,
    packVersion: catalog.version,
    packHash,
    totals: { words: total, indexed, excludedParticle, excludedNoLemma, keys: rows.size, particleKeys: particles.size },
    keys,
    particles: Object.fromEntries([...particles].sort()),
  };
  return { index, catalog, catalogPath, packHash, packs, wordAt, rows, particles, ketivWords, ketivExcluded };
}

function parseRows(r) {
  return r ? r.split(",").map((s) => { const m = s.match(/^(.+):(\d+)(k?)$/); return [m[1], +m[2], m[3] === "k"]; }) : [];
}

export function runGates(ctx) {
  const { index, wordAt, ketivWords, ketivExcluded } = ctx;
  const fails = {};
  const f = (g, msg) => (fails[g] ||= []).push(msg);
  const t = index.totals;
  // G1
  if (t.indexed + t.excludedParticle + t.excludedNoLemma !== t.words) f("G1", `a: ${t.indexed}+${t.excludedParticle}+${t.excludedNoLemma} != ${t.words}`);
  const seen = new Set(); let dup = 0;
  for (const [k, e] of Object.entries(index.keys)) {
    const rs = parseRows(e.r);
    if (rs.length !== e.c) f("G1", `b: ${k} count ${e.c} != ${rs.length}`);
    for (const [v, i] of rs) { const id = `${v}:${i}`; if (seen.has(id)) { dup++; f("G1", `c: dup ${id}`); } seen.add(id); }
  }
  // G2
  let g2checked = 0;
  const mixed = new Map();
  let kqRows = 0;
  for (const [k, e] of Object.entries(index.keys)) {
    const lang = k[0], lemma = k.slice(2);
    for (const [v, i, kq] of parseRows(e.r)) {
      g2checked++;
      const w = wordAt.get(v)?.[i];
      if (!w) { f("G2", `${k}: ${v}:${i} does not resolve`); continue; }
      if (w.lemmaId !== lemma) f("G2", `${k}: ${v}:${i} lemma ${w.lemmaId}`);
      const wl = (w.morph || "H")[0] === "A" ? "A" : "H";
      if (wl !== lang) f("G3", `${k}: ${v}:${i} morph lang ${wl} mixed into ${lang}`);
      if (kq) kqRows++;
      if (!!w.ketiv !== kq) f("G5", `${k}: ${v}:${i} K/Q flag ${kq} vs ketiv ${!!w.ketiv}`);
    }
  }
  if (!index.packHash || index.packHash !== ctx.packHash) f("G2", "packHash stamp mismatch");
  // Sofer v0.7.1: storage-format guard — every row verseId must be Book.C.V.
  const VID = /^[1-3]?[A-Za-z]+\.\d+\.\d+$/;
  let badIds = 0;
  for (const [k, e] of Object.entries(index.keys)) for (const [v] of parseRows(e.r)) if (!VID.test(v)) { badIds++; f("G2", `${k}: malformed verseId ${v}`); }
  // Sofer v0.7.1: no JPS line may carry a YHWH vocalization (Adonai-x titles allowed).
  const JPS_DN = /jehovah|yehovah|yahweh/i;
  const jpsHits = [];
  for (const p of ctx.packs) for (const v of p.verses) if (JPS_DN.test(v.english.text)) { jpsHits.push(v.id); f("G4", `JPS vocalization in ${v.id}`); }
  // G3
  const byLemma = new Map();
  for (const k of Object.keys(index.keys)) { const l = k.slice(2); byLemma.set(l, [...(byLemma.get(l) || []), k]); }
  const both = [...byLemma].filter(([, ks]) => ks.length > 1).map(([l]) => ({ lemmaId: l, H: index.keys[`H:${l}`].c, A: index.keys[`A:${l}`].c }));
  const spot = {};
  const checkAram = (vid, idxs) => {
    for (const i of idxs) {
      const w = wordAt.get(vid)[i];
      if (!w.lemmaId || isParticleToken(w)) continue;
      const inA = parseRows(index.keys[`A:${w.lemmaId}`]?.r).some(([v, j]) => v === vid && j === i);
      const inH = parseRows(index.keys[`H:${w.lemmaId}`]?.r).some(([v, j]) => v === vid && j === i);
      spot[`${vid}:${i}`] = { lemmaId: w.lemmaId, inA, inH };
      if (!inA || inH) f("G3", `${vid}:${i} Aramaic token not in A: key only`);
    }
  };
  const aramIdx = (vid) => wordAt.get(vid).map((w, i) => ((w.morph || "")[0] === "A" ? i : -1)).filter((i) => i >= 0);
  for (const vid of ["Gen.31.47", "Dan.2.4", "Dan.7.28", "Ezra.4.8", "Ezra.6.18", "Ezra.7.12", "Jer.10.11"]) {
    const ix = aramIdx(vid);
    if (!ix.length && vid !== "Jer.10.11") f("G3", `${vid}: no Aramaic tokens found (expected)`);
    checkAram(vid, ix);
  }
  // Dan 2:4b–7:28, Ezra 4:8–6:18, 7:12–26: every A-morph token must be in A keys (bulk)
  let aramTotal = 0, aramInA = 0;
  for (const [vid, ws] of wordAt) {
    ws.forEach((w, i) => { if ((w.morph || "")[0] === "A" && w.lemmaId && !isParticleToken(w)) { aramTotal++; } });
  }
  for (const [k, e] of Object.entries(index.keys)) if (k[0] === "A") aramInA += e.c;
  if (aramTotal !== aramInA) f("G3", `A-morph tokens ${aramTotal} != rows under A: keys ${aramInA}`);
  // G5
  const expectedKq = ketivWords - ketivExcluded;
  if (kqRows !== expectedKq) f("G5", `K/Q rows ${kqRows} != ketiv words ${ketivWords} - excluded ${ketivExcluded}`);
  const jer = wordAt.get("Jer.48.44")[0];
  const jerRow = parseRows(index.keys[`H:${jer.lemmaId}`]?.r).find(([v, i]) => v === "Jer.48.44" && i === 0);
  if (!jerRow || !jerRow[2] || cons(jer.he) !== "הנס" || jer.ketiv !== "הניס") f("G5", "Jer.48.44 catchWord row wrong");
  // G6
  for (const k of ["H:H0853", "H:H3605"]) if (!index.keys[k] || k in index.particles) f("G6", `${k} must be counted, not a particle`);
  for (const k of Object.keys(index.keys)) if (/^[HA]:H9\d{3}/.test(k) || k.includes("pfx:")) f("G6", `${k} particle leaked into keys`);

  const pass = (g) => !(fails[g] && fails[g].length);
  const w = (g, body) => fs.writeFileSync(path.join(REPORTS, `concordance_gate_${g}.json`), JSON.stringify({ gate: g, ok: pass(g), packHash: index.packHash, failures: (fails[g] || []).slice(0, 200), ...body }, null, 2));
  w("G1", { totals: index.totals, duplicateEntries: dup, note: "UI label reads ConcordanceEntry.count (stored c); see ConcordanceGateTest.G1d" });
  w("G2", { rowsChecked: g2checked, verseIdFormat: "^[1-3]?[A-Za-z]+\\.\\d+\\.\\d+$", malformedVerseIds: badIds, packHashStampedInCatalog: true, loader: "ConcordanceRepository refuses index.packHash != catalog.concordance.packHash; JVM test recomputes hash from packs" });
  w("G3", { aramaicTokensIndexed: aramTotal, rowsUnderAKeys: aramInA, aKeys: Object.keys(index.keys).filter((k) => k[0] === "A").length, INFO_lemmasUnderBothHandA: both, spot });
  w("G5", { ketivWords, ketivExcludedAsParticleOrNoLemma: ketivExcluded, kqFlaggedRows: kqRows, jer_48_44: { he: jer.he, ketiv: jer.ketiv, lemmaId: jer.lemmaId, row: jerRow } });
  w("G6", { rule: "isParticle = lemmaId H9xxx OR glossId pfx:*", INFO_particleKeys: index.particles, H0853: index.keys["H:H0853"]?.c, H3605: index.keys["H:H3605"]?.c });
  // G7
  const rnd = mulberry32(SEED);
  const pool = Object.keys(index.keys).sort();
  const picked = new Set();
  while (picked.size < 20) picked.add(pool[Math.floor(rnd() * pool.length)]);
  const aramKey = Object.keys(index.keys).filter((k) => k[0] === "A").sort((a, b) => index.keys[b].c - index.keys[a].c)[0];
  const fixed = ["H:H3068", "H:H0430", "H:H1121a", aramKey];
  const dump = (k) => {
    const rs = parseRows(index.keys[k].r);
    const show = ([v, i, kq]) => ({ verseId: v, wordIndex: i, he: wordAt.get(v)[i].he, kq });
    return { key: k, headword: index.keys[k].hw, count: index.keys[k].c, first3: rs.slice(0, 3).map(show), last3: rs.slice(-3).map(show) };
  };
  fs.writeFileSync(path.join(REPORTS, "concordance_gate_G7.json"), JSON.stringify({ gate: "G7", seed: SEED, prng: "mulberry32 over sorted non-particle keys", fixed: fixed.map(dump), seeded: [...picked].map(dump) }, null, 2));
  return { ok: Object.values(fails).every((l) => !l.length), fails };
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const ctx = buildConcordance();
  const text = JSON.stringify(ctx.index);
  fs.writeFileSync(path.join(DATA, "concordance.json.gz"), zlib.gzipSync(Buffer.from(text, "utf8"), { level: 9 }));
  ctx.catalog.concordance = { asset: "data/concordance.json", assetGz: "data/concordance.json.gz", packHash: ctx.packHash, schema: 1 };
  fs.writeFileSync(ctx.catalogPath, JSON.stringify(ctx.catalog, null, 2));
  const res = runGates(ctx);
  console.log(`concordance: keys=${ctx.index.totals.keys} indexed=${ctx.index.totals.indexed} particles=${ctx.index.totals.excludedParticle} noLemma=${ctx.index.totals.excludedNoLemma} hash=${ctx.packHash.slice(0, 12)} gz=${fs.statSync(path.join(DATA, "concordance.json.gz")).size}B`);
  if (res.fails.G4?.length) console.error("G4 (JPS) failures:", res.fails.G4);
  if (!res.ok) { console.error("CONCORDANCE HARD FAIL", JSON.stringify(res.fails, null, 1).slice(0, 4000)); process.exit(1); }
  console.log("concordance gates G1/G2/G3/G5/G6 PASSED; G7 dump written");
}
