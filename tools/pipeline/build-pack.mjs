#!/usr/bin/env node
/**
 * Offline pack builder — Torah hotfix (v0.2.2-poc) with Torah+Nevi'im hooks.
 *
 * - Ingests OSHB morphhb WLC book XML (pin v.2.2), not Gen-only
 * - Jewish Tanakh nav order (see books.mjs)
 * - JPS 1917 via engjps readaloud; Hebrew (WLC) verse IDs primary;
 *   OSHB VerseMap.xml maps WLC→KJV/engjps file refs (mismatches are real)
 * - Do NOT NFC-normalize Hebrew surfaces
 * - Aramaic books (Dan/Ezra): flagged; Hebrew SBL-Learner must not be applied
 *   silently (N/A for Torah body; pipeline supports the gate)
 * - Per-book compact JSON (+ optional gzip) for lazy load
 * - Hard-fail Sofer checks 1–8; auto K/Q report
 */
import fs from "fs";
import path from "path";
import zlib from "zlib";
import { fileURLToPath } from "url";
import { createRequire } from "module";
import { BOOKS, TORAH, NEVIIM, KETUVIM, ARAMAIC_FLAG_BOOKS, jewishSortKey } from "./books.mjs";
import { createSoferSblLearnerSchema } from "./sofer-sbl-learner.mjs";
import { runHardFailChecks } from "./hard-fail-checks.mjs";
import { extractWTokens } from "./oshb-w.mjs";
import { isAramaicMorph } from "./morph-lang.mjs";

const require = createRequire(import.meta.url);
const { transliterate } = require("hebrew-transliteration");

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../..");
const VENDOR = path.join(ROOT, "vendor");
const OUT_DIR = path.join(ROOT, "app/src/main/assets/data");
const REPORTS = path.join(ROOT, "reports");
const PACK_VERSION = "0.4.1-poc";
const schema = createSoferSblLearnerSchema();

const CANTILLATION = /[\u0591-\u05AF\u05BD\u05BF\u05C0\u05C3\u05C6]/g;
const HEBREW_LETTER = /[\u05D0-\u05EA]/;

const args = process.argv.slice(2);
function argVal(flag, fallback) {
  const i = args.indexOf(flag);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
}
const SCOPE = argVal("--scope", "torah+neviim"); // torah | neviim | ketuvim | torah+neviim | all | book=Gen
const WRITE_GZIP = !args.includes("--no-gzip");
const PRETTY = args.includes("--pretty");

function booksForScope() {
  if (SCOPE === "torah") return [...TORAH];
  if (SCOPE === "neviim") return [...NEVIIM];
  if (SCOPE === "ketuvim") return [...KETUVIM];
  if (SCOPE === "torah+neviim" || SCOPE === "torah-neviim") return [...TORAH, ...NEVIIM];
  if (SCOPE === "all" || SCOPE === "tanakh" || SCOPE === "torah+neviim+ketuvim") {
    return [...TORAH, ...NEVIIM, ...KETUVIM];
  }
  if (SCOPE.startsWith("book=")) return [SCOPE.slice(5)];
  return SCOPE.split(",").map((s) => s.trim()).filter(Boolean);
}

function scopeLabel() {
  if (SCOPE === "torah") return "Torah (Gen–Deut)";
  if (SCOPE === "neviim") return "Nevi'im (Josh–Mal)";
  if (SCOPE === "ketuvim") return "Ketuvim (Ps–2Chr)";
  if (SCOPE === "torah+neviim" || SCOPE === "torah-neviim") return "Torah + Nevi'im";
  if (SCOPE === "all" || SCOPE === "tanakh" || SCOPE === "torah+neviim+ketuvim") {
    return "Full Tanakh (Torah + Nevi'im + Ketuvim)";
  }
  return SCOPE;
}

function reportPrefix() {
  if (SCOPE === "neviim") return "neviim";
  if (SCOPE === "ketuvim") return "ketuvim";
  if (SCOPE === "torah+neviim" || SCOPE === "torah-neviim") return "tanakh-tn";
  if (SCOPE === "all" || SCOPE === "tanakh" || SCOPE === "torah+neviim+ketuvim") return "tanakh";
  return "torah";
}

function stripCantillation(s) {
  return s.replace(CANTILLATION, "");
}
const FORBIDDEN_GLOSS_RE = /jehovah|ye\.?\s*ho\.?\s*vah|yehovah|vowel\s*pointings?\s+of|a\.?\s*do\.?\s*na/i;
function sanitizeGlossPrimary(primary, senses = []) {
  if (!primary) return "—";
  if (!FORBIDDEN_GLOSS_RE.test(primary)) return primary;
  const rewritten = String(primary).replace(/jehovah|yehovah|ye\.?\s*ho\.?\s*vah/gi, "LORD");
  if (!FORBIDDEN_GLOSS_RE.test(rewritten)) return rewritten;
  for (const s of senses) {
    if (s && !FORBIDDEN_GLOSS_RE.test(s)) return s;
    const r = String(s || "").replace(/jehovah|yehovah/gi, "LORD");
    if (r && !FORBIDDEN_GLOSS_RE.test(r)) return r;
  }
  return "—";
}
function consonantsOnly(s) {
  return [...s].filter((ch) => HEBREW_LETTER.test(ch)).join("");
}
function isYhwhLemma(lemmaId) {
  if (!lemmaId) return false;
  return (
    lemmaId === "H3068" ||
    lemmaId === "H3069" ||
    lemmaId.startsWith("H3068") ||
    lemmaId.startsWith("H3069")
  );
}
function isYhwhNumeric(lemmaInfo) {
  return lemmaInfo?.numeric === "3068" || lemmaInfo?.numeric === "3069";
}
/** Consonantal יהוה (optionally with proclitic letters before it). */
function yhwhConsMatch(heSurface) {
  const chars = [...heSurface];
  const cons = consonantsOnly(heSurface);
  if (cons === "יהוה") {
    return { proclitic: "", procliticSurf: "", yhwh: true };
  }
  if (cons.endsWith("יהוה") && cons.length > 4) {
    const proclitic = cons.slice(0, -4);
    // Slice pointed proclitic from surface (include niqqud after last prefix letter)
    let taken = 0;
    let i = 0;
    for (; i < chars.length && taken < proclitic.length; i++) {
      if (HEBREW_LETTER.test(chars[i])) taken++;
    }
    while (i < chars.length && !HEBREW_LETTER.test(chars[i])) i++;
    return {
      proclitic,
      procliticSurf: chars.slice(0, i).join(""),
      yhwh: true,
    };
  }
  return { proclitic: "", procliticSurf: "", yhwh: false };
}

const PREFIX_ONLY_TO_TBESH = {
  b: "H9003",
  c: "H9002",
  d: "H9009",
  l: "H9005",
  m: "H9006",
  k: "H9004",
  s: "H9007",
  i: "H9008",
};

function parseLemma(lemmaAttr) {
  if (!lemmaAttr) return { baseId: null, prefixes: [], aug: null, raw: null };
  const trimmed = lemmaAttr.trim();
  if (/^[a-z]$/i.test(trimmed)) {
    const letter = trimmed.toLowerCase();
    return {
      baseId: PREFIX_ONLY_TO_TBESH[letter] || null,
      prefixes: [letter],
      aug: null,
      raw: lemmaAttr,
      numeric: PREFIX_ONLY_TO_TBESH[letter]
        ? PREFIX_ONLY_TO_TBESH[letter].slice(1)
        : null,
      prefixOnly: true,
    };
  }
  const parts = trimmed.split("/");
  const prefixes = [];
  let core = parts[parts.length - 1];
  for (let i = 0; i < parts.length - 1; i++) prefixes.push(parts[i]);
  // OSHB compound continuation uses trailing + (e.g. 3071+ on Exod.17.15 YHWH half)
  const m = core.match(/^(\d+)\s*([a-zA-Z])?\+?$/);
  if (!m) return { baseId: null, prefixes, aug: null, raw: lemmaAttr };
  const num = m[1];
  const aug = m[2] ? m[2].toLowerCase() : null;
  const padded = num.padStart(4, "0");
  const baseId = "H" + padded + (aug || "");
  return { baseId, prefixes, aug, raw: lemmaAttr, numeric: padded };
}

function loadTbesh(filePath) {
  const text = fs.readFileSync(filePath, "utf8");
  const byExact = new Map();
  const byNumeric = new Map();
  for (const line of text.split(/\r?\n/)) {
    if (!line.startsWith("H")) continue;
    const cols = line.split("\t");
    if (cols.length < 7) continue;
    const eStrong = cols[0].trim();
    const dStrongField = cols[1] || "";
    const gloss = (cols[6] || "").trim();
    const definition = (cols[7] || "")
      .replace(/<br\s*\/?>/gi, "\n")
      .replace(/<\/?[^>]+>/g, "")
      .replace(/&nbsp;/g, " ")
      .trim();
    if (!gloss && !definition) continue;
    const entry = {
      id: eStrong,
      dStrong: dStrongField.split("=")[0].trim(),
      gloss,
      definition,
      source: "TBESH",
    };
    const list = byExact.get(eStrong) || [];
    list.push(entry);
    byExact.set(eStrong, list);
    const numKey = eStrong.replace(/[a-zA-Z]+$/, "");
    const nlist = byNumeric.get(numKey) || [];
    nlist.push(entry);
    byNumeric.set(numKey, nlist);
  }
  return { byExact, byNumeric };
}

function pickTbesh(tbesh, baseId) {
  if (!baseId) return null;
  const exact = tbesh.byExact.get(baseId) || [];
  const numericKey = "H" + (baseId.match(/\d+/) || [""])[0].padStart(4, "0");
  const numeric = tbesh.byNumeric.get(numericKey) || [];
  const pool = exact.length ? exact : numeric;
  if (!pool.length) return null;
  const general = pool.find((e) => /G\s*=/.test(e.dStrong) || /G$/.test(e.dStrong));
  return general || pool[0];
}

function loadHebrewStrong(filePath) {
  const text = fs.readFileSync(filePath, "utf8");
  const map = new Map();
  const entryRe = /<entry id="(H\d+)">([\s\S]*?)<\/entry>/g;
  let m;
  while ((m = entryRe.exec(text))) {
    const id = m[1];
    const body = m[2];
    const defs = [...body.matchAll(/<def>([^<]*)<\/def>/g)].map((x) => x[1].trim());
    const usage = (body.match(/<usage>([\s\S]*?)<\/usage>/) || [])[1] || "";
    const usageClean = usage.replace(/<[^>]+>/g, "").replace(/\s+/g, " ").trim();
    const meaning = (body.match(/<meaning>([\s\S]*?)<\/meaning>/) || [])[1] || "";
    const meaningClean = meaning.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
    map.set(id, {
      id,
      gloss: defs[0] || meaningClean.split(/[,;]/)[0] || usageClean.split(/[,;]/)[0] || "",
      definition: meaningClean || usageClean,
      senses: defs.length
        ? defs
        : usageClean
          ? usageClean.split(/[,;]/).map((s) => s.trim()).filter(Boolean)
          : [],
      source: "HebrewStrong",
    });
  }
  return map;
}

function resolveGloss(tbesh, strong, lemmaInfo) {
  if (!lemmaInfo.baseId) {
    return {
      id: "unknown",
      primary: "(no lemma)",
      senses: [],
      source: "none",
      note: "OSHB lemma missing",
    };
  }
  const candidates = [];
  if (lemmaInfo.aug) candidates.push("H" + lemmaInfo.numeric + lemmaInfo.aug);
  candidates.push("H" + lemmaInfo.numeric);

  let picked = null;
  for (const c of candidates) {
    picked = pickTbesh(tbesh, c);
    if (picked) break;
  }
  if (picked) {
    const senses = [];
    if (picked.gloss) senses.push(picked.gloss);
    for (const line of picked.definition.split("\n")) {
      const mm = line.match(/^\d+[a-z]?\)\s*(.+)/);
      if (mm) {
        const s = mm[1].trim();
        if (s && !senses.includes(s)) senses.push(s);
      }
    }
    return {
      id: picked.id,
      primary: picked.gloss || senses[0] || picked.id,
      senses: senses.slice(0, 8),
      source: "TBESH",
      definition: picked.definition.slice(0, 500),
    };
  }

  const hs =
    strong.get("H" + parseInt(lemmaInfo.numeric, 10)) ||
    strong.get("H" + lemmaInfo.numeric);
  if (hs) {
    return {
      id: hs.id,
      primary: hs.gloss || hs.id,
      senses: (hs.senses || []).slice(0, 8),
      source: "HebrewStrong",
      definition: (hs.definition || "").slice(0, 500),
    };
  }
  return {
    id: candidates[0],
    primary: "(gloss unavailable)",
    senses: [],
    source: "none",
    note: "Not found in TBESH or HebrewStrong",
  };
}

function prefixNote(prefixes) {
  if (!prefixes.length) return null;
  const map = {
    b: "בְּ (bet) proclitic — often 'in/with'",
    c: "וְ (vav) proclitic — often 'and'",
    d: "הַ (he) article/proclitic — often 'the'",
    l: "לְ (lamed) proclitic — often 'to/for'",
    m: "מִ (mem) proclitic — often 'from'",
    s: "שׁ (shin) relative proclitic",
  };
  return prefixes.map((p) => map[p] || `proclitic marker '${p}'`).join("; ");
}

function surfaceHebrew(raw) {
  // Strip cantillation only; keep niqqud; DO NOT NFC-normalize
  return stripCantillation(raw.replace(/\//g, ""));
}

function phoneticForToken(heSurface, lemmaInfo, opts = {}) {
  if (opts.aramaic) {
    return {
      phonetic: "[aramaic-pending]",
      ketivQere: null,
      divineName: false,
      aramaic: true,
      error: "Biblical Aramaic — Sofer-approved handling required; Hebrew SBL-Learner not applied",
    };
  }
  const yhwhMatch = yhwhConsMatch(heSurface);
  if (
    isYhwhNumeric(lemmaInfo) ||
    isYhwhLemma(lemmaInfo.baseId || "") ||
    yhwhMatch.yhwh
  ) {
    // MEDIUM: proclitic+YHWH → e.g. laYHWH; bare → YHWH
    let phonetic = "YHWH";
    if (yhwhMatch.proclitic) {
      try {
        const pfxSrc = yhwhMatch.procliticSurf || yhwhMatch.proclitic;
        const pfx = transliterate(pfxSrc, schema)
          .replace(/[\s\-׳ʼ]/g, "")
          .replace(/ʾ/g, "");
        phonetic = `${pfx}YHWH`;
      } catch {
        phonetic = "YHWH";
      }
    }
    return { phonetic, ketivQere: null, divineName: true };
  }
  const input = heSurface.replace(/־/g, "");
  try {
    const phonetic = transliterate(input, schema);
    return { phonetic, ketivQere: null, divineName: false };
  } catch (e) {
    return {
      phonetic: "[transliteration-error]",
      ketivQere: null,
      divineName: false,
      error: String(e),
    };
  }
}

function displayHebrew(heSurface, lemmaInfo) {
  // Sofer HIGH: pointed יהוה / H3068 / H3069 → consonants יהוה (+ optional proclitic)
  const m = yhwhConsMatch(heSurface);
  if (isYhwhNumeric(lemmaInfo) || isYhwhLemma(lemmaInfo.baseId || "") || m.yhwh) {
    // MEDIUM: keep proclitic on chip (ליהוה) when present
    return (m.proclitic || "") + "יהוה";
  }
  return heSurface;
}

/** Load WLC→KJV map from OSHB VerseMap.xml */
function loadVerseMap(xmlPath) {
  const xml = fs.readFileSync(xmlPath, "utf8");
  const wlcToKjv = new Map();
  const verseRe = /<verse\s+wlc="([^"]+)"\s+kjv="([^"]+)"/g;
  let m;
  while ((m = verseRe.exec(xml))) {
    wlcToKjv.set(m[1], m[2]);
  }
  return wlcToKjv;
}

function parseOsisRef(ref) {
  // Gen.32.1 or Exod.8.1
  const parts = ref.split(".");
  return {
    book: parts[0],
    chapter: parseInt(parts[1], 10),
    verse: parseInt(parts[2], 10),
    raw: ref,
  };
}

/**
 * Load all JPS engjps chapter files for a book into Map "Book.CH.V" → text
 * File organization follows KJV/Christian chapter numbers (engjps).
 */
function loadJpsBook(bookMeta) {
  const { num, code, titleHints } = bookMeta.jps;
  const dir = path.join(VENDOR, "jps1917");
  const files = fs
    .readdirSync(dir)
    .filter((f) => f.startsWith(`engjps_${num}_${code}_`) && f.endsWith("_read.txt"))
    .sort();
  const byKjv = new Map(); // "Gen.31.55" style using OSHB osis book id
  const osisBook = bookMeta.osis;

  for (const f of files) {
    const chm = f.match(/_(\d+)_read\.txt$/);
    if (!chm) continue;
    const chapter = parseInt(chm[1], 10);
    const raw = fs.readFileSync(path.join(dir, f), "utf8").replace(/^\uFEFF/, "");
    const lines = raw
      .split(/\r?\n/)
      .map((l) => l.trim())
      .filter(Boolean);
    let vNum = 0;
    for (const line of lines) {
      if (/^Chapter\s+\d+\.?$/i.test(line)) continue;
      if (titleHints.some((re) => re.test(line))) continue;
      vNum += 1;
      // Strip leading Hebrew-ref annotation like "(32-1) " if present — keep body
      const text = line.replace(/^\(\d+-\d+\)\s*/, "");
      byKjv.set(`${osisBook}.${chapter}.${vNum}`, text);
    }
  }
  return byKjv;
}

function jpsForWlc(wlcOsisId, verseMap, jpsByKjv) {
  const kjvRef = verseMap.get(wlcOsisId) || wlcOsisId;
  // VerseMap may use partials (1Kgs.22.43!b); engjps is whole-line only
  const baseRef = String(kjvRef).replace(/![ab]$/i, "");
  let text = jpsByKjv.get(kjvRef) || jpsByKjv.get(baseRef) || "";
  return {
    text,
    kjvRef,
    mapped: verseMap.has(wlcOsisId),
  };
}

function parseOshbBook(xmlPath, bookOsis) {
  const xml = fs.readFileSync(xmlPath, "utf8");
  // Do not NFC-normalize the XML text
  const verses = [];
  const chRe = new RegExp(
    `<chapter osisID="${bookOsis}\\.(\\d+)">([\\s\\S]*?)</chapter>`,
    "g"
  );
  let chm;
  while ((chm = chRe.exec(xml))) {
    const ch = parseInt(chm[1], 10);
    const bodyCh = chm[2];
    const verseRe = new RegExp(
      `<verse osisID="(${bookOsis}\\.\\d+\\.\\d+)">([\\s\\S]*?)</verse>`,
      "g"
    );
    let vm;
    while ((vm = verseRe.exec(bodyCh))) {
      const osisId = vm[1];
      const body = vm[2];
      // Flatten nested <seg> (x-large etc.) inside <w> before emit — [^<]* drops those words
      const uniq = extractWTokens(body).map((t) => ({
        ...t,
        ketiv: t.ketiv != null ? stripCantillation(t.ketiv) : null,
      }));
      const verseNum = parseInt(osisId.split(".")[2], 10);
      verses.push({ osisId, book: bookOsis, chapter: ch, verse: verseNum, tokens: uniq });
    }
  }
  return verses;
}

function writeJson(filePath, obj, { gzipOnly = true } = {}) {
  const text = PRETTY ? JSON.stringify(obj, null, 2) : JSON.stringify(obj);
  const bytes = Buffer.byteLength(text, "utf8");
  if (WRITE_GZIP) {
    fs.writeFileSync(filePath + ".gz", zlib.gzipSync(Buffer.from(text, "utf8"), { level: 9 }));
  }
  if (!gzipOnly || !WRITE_GZIP || PRETTY) {
    fs.writeFileSync(filePath, text);
  } else if (fs.existsSync(filePath)) {
    fs.unlinkSync(filePath);
  }
  return bytes;
}

function main() {
  const bookList = booksForScope().sort((a, b) => jewishSortKey(a) - jewishSortKey(b));
  console.log(`Building scope=${SCOPE} books=${bookList.join(",")} version=${PACK_VERSION}`);

  for (const b of bookList) {
    if (!BOOKS[b]) throw new Error(`Unknown book ${b} — add to books.mjs`);
    if (ARAMAIC_FLAG_BOOKS.has(b)) {
      console.warn(
        `WARN: ${b} has Biblical Aramaic — Hebrew SBL-Learner must not be applied silently`
      );
    }
  }

  console.log("Loading lexicons + VerseMap…");
  const tbesh = loadTbesh(path.join(VENDOR, "TBESH.txt"));
  const strong = loadHebrewStrong(path.join(VENDOR, "HebrewStrong.xml"));
  const verseMap = loadVerseMap(path.join(VENDOR, "oshb", "VerseMap.xml"));

  fs.mkdirSync(OUT_DIR, { recursive: true });
  fs.mkdirSync(path.join(OUT_DIR, "books"), { recursive: true });
  fs.mkdirSync(REPORTS, { recursive: true });

  const glossCatalog = {};
  const catalogBooks = [];
  const allKetivQere = [];
  const allGaps = [];
  const packByBook = {};
  let totalVerses = 0;

  for (const bookOsis of bookList) {
    const meta = BOOKS[bookOsis];
    const aramaicBook = !!meta.aramaic || ARAMAIC_FLAG_BOOKS.has(bookOsis);
    const xmlPath = path.join(VENDOR, "oshb", meta.oshbFile);
    if (!fs.existsSync(xmlPath)) throw new Error(`Missing OSHB file ${xmlPath}`);

    console.log(`Parsing OSHB ${meta.oshbFile}…`);
    const oshbVerses = parseOshbBook(xmlPath, bookOsis);
    console.log(`  ${oshbVerses.length} verses`);
    console.log(`Loading JPS ${meta.jps.code}…`);
    const jpsByKjv = loadJpsBook(meta);

    const packVerses = [];
    const chaptersMap = new Map();

    let verseIdx = 0;
    for (const v of oshbVerses) {
      verseIdx++;
      if (verseIdx === 1 || verseIdx % 100 === 0 || verseIdx === oshbVerses.length) {
        console.log(`  … ${bookOsis} verse ${verseIdx}/${oshbVerses.length}`);
      }
      const jps = jpsForWlc(v.osisId, verseMap, jpsByKjv);
      if (!jps.text) {
        allGaps.push(`Missing JPS for ${v.osisId} (kjvRef=${jps.kjvRef})`);
      }
      const words = [];
      for (let i = 0; i < v.tokens.length; i++) {
        const t = v.tokens[i];
        const lemmaInfo = parseLemma(t.lemma);
        const heSurf = surfaceHebrew(t.heRaw);
        const he = displayHebrew(heSurf, lemmaInfo);
        const tokenAramaic = isAramaicMorph(t.morph);
        const ph = phoneticForToken(heSurf, lemmaInfo, { aramaic: tokenAramaic });
        const gloss = resolveGloss(tbesh, strong, lemmaInfo);
        const glossId = gloss.id || `tok-${t.id}`;
        if (!glossCatalog[glossId]) {
          glossCatalog[glossId] = {
            id: glossId,
            primary: sanitizeGlossPrimary(gloss.primary, gloss.senses),
            senses: (gloss.senses.length ? gloss.senses : [gloss.primary])
              .map((x) => sanitizeGlossPrimary(x, []))
              .filter(Boolean),
            source: gloss.source,
            definition: gloss.definition || null,
            note: gloss.note || null,
          };
        }
        const pfx = prefixNote(lemmaInfo.prefixes);
        const word = {
          he,
          lemmaId: lemmaInfo.baseId,
          lemmaRaw: lemmaInfo.raw,
          morph: t.morph,
          phonetic: ph.phonetic,
          glossId,
          divineName: !!ph.divineName,
        };
        if (t.ketiv) {
          word.ketiv = t.ketiv;
          word.qereFlag = true;
          allKetivQere.push({
            verseId: v.osisId,
            book: bookOsis,
            chapter: v.chapter,
            verse: v.verse,
            tokenIndex: i,
            ketiv: t.ketiv,
            qereHe: he,
            qerePhonetic: ph.phonetic,
            lemmaId: lemmaInfo.baseId,
            morph: t.morph,
            oshbId: t.id,
          });
        }
        if (pfx) word.procliticNote = pfx;
        if (ph.aramaic) word.aramaic = true;
        if (ph.error) allGaps.push(`Phonetic note ${v.osisId}#${i}: ${ph.error}`);
        if (gloss.source === "none") {
          allGaps.push(`Gloss gap ${v.osisId} lemma=${lemmaInfo.raw}`);
        }
        words.push(word);
      }
      const verseObj = {
        id: v.osisId,
        book: bookOsis,
        chapter: v.chapter,
        verse: v.verse,
        english: {
          text: jps.text,
          source: "JPS 1917",
          license: "Public Domain",
          kjvRef: jps.mapped ? jps.kjvRef : undefined,
        },
        words,
      };
      if (jps.mapped) verseObj.english.versificationMapped = true;
      packVerses.push(verseObj);
      if (!chaptersMap.has(v.chapter)) chaptersMap.set(v.chapter, []);
      chaptersMap.get(v.chapter).push(v.osisId);
    }

    const chapters = [...chaptersMap.keys()]
      .sort((a, b) => a - b)
      .map((ch) => ({
        book: bookOsis,
        chapter: ch,
        verseIds: chaptersMap.get(ch),
      }));

    const bookPack = {
      meta: {
        name: `tanakh-learner-${bookOsis}`,
        version: PACK_VERSION,
        generatedAt: new Date().toISOString(),
        scope: meta.title,
        book: bookOsis,
        title: meta.title,
        division: meta.division,
        aramaic: aramaicBook,
        hebrew: {
          source: "OSHB / morphhb WLC",
          pin: "v.2.2",
          file: `vendor/oshb/${meta.oshbFile}`,
          license:
            "WLC text Public Domain; lemma/morphology CC BY 4.0 (Open Scriptures Hebrew Bible)",
          normalization: "none — do not NFC-normalize",
        },
        english: {
          source: "JPS TaNaKH 1917",
          license: "Public Domain",
          note: "Verse-level only — not word-aligned. Hebrew WLC IDs primary; engjps via VerseMap where WLC≠KJV.",
          provider: "ebible.org engjps",
          verseMap: "vendor/oshb/VerseMap.xml",
        },
        phonetics: {
          engine: "hebrew-transliteration@2.11.0",
          schema: "Sofer SBL-Learner",
          policy:
            "Derived from OSHB/WLC niqqud. Biblical/Tiberian + digraphs sh/kh/ts/ʾ/ʿ; vocal shewa ĕ; NOT Modern Israeli. יהוה → YHWH only. Phonetics follow qere when ketiv/qere present. Biblical Aramaic must not silently use this schema.",
          mappingDoc: "docs/SOFER_SBL_LEARNER.md",
        },
        glosses: {
          primary: "TBESH (STEPBible) CC BY 4.0",
          fallback: "OSHB HebrewStrong.xml CC BY 4.0",
          uiPolicy: "Header: Possible sense(s). Footer: Gloss ≠ verse translation.",
        },
        display: {
          tokenOrder:
            "Single token array preserves OSHB order. UI may show LTR paired chips without reversing the array.",
          navOrder: "Jewish Tanakh order",
        },
      },
      chapters,
      verses: packVerses,
    };

    const outName = `books/${bookOsis}.json`;
    const bytes = writeJson(path.join(OUT_DIR, outName), bookPack);
    console.log(
      `  Wrote ${outName} (${(bytes / 1024 / 1024).toFixed(2)} MB, ${packVerses.length} verses)`
    );

    // Avoid retaining full verse graphs during multi-book builds (OOM / thrash).
    // Hard-fail reloads packs from gzip below.
    packByBook[bookOsis] = null;
    if (global.gc) global.gc();
    totalVerses += packVerses.length;
    catalogBooks.push({
      osis: bookOsis,
      title: meta.title,
      division: meta.division,
      jewishOrder: jewishSortKey(bookOsis),
      chapters: chapters.length,
      verses: packVerses.length,
      asset: `data/${outName}`,
      assetGz: WRITE_GZIP ? `data/${outName}.gz` : null,
      aramaic: aramaicBook,
    });
  }

  // Shared gloss catalog
  const glossBytes = writeJson(path.join(OUT_DIR, "glosses.json"), glossCatalog);
  console.log(
    `Wrote glosses.json (${(glossBytes / 1024 / 1024).toFixed(2)} MB, ${Object.keys(glossCatalog).length} entries)`
  );

  const catalog = {
    name: "tanakh-learner",
    version: PACK_VERSION,
    generatedAt: new Date().toISOString(),
    scope: scopeLabel(),
    navOrder: "Jewish Tanakh",
    hebrewPin: "OSHB morphhb v.2.2",
    books: catalogBooks,
    glossesAsset: "data/glosses.json",
    glossesAssetGz: WRITE_GZIP ? "data/glosses.json.gz" : null,
    totals: {
      books: catalogBooks.length,
      verses: totalVerses,
      glosses: Object.keys(glossCatalog).length,
      ketivQere: allKetivQere.length,
      gaps: allGaps.length,
    },
  };
  fs.writeFileSync(path.join(OUT_DIR, "catalog.json"), JSON.stringify(catalog, null, 2));

  function writeKqReport(prefix, title, books, entries) {
    const kqReport = {
      version: PACK_VERSION,
      division: title,
      generatedAt: new Date().toISOString(),
      count: entries.length,
      byBook: Object.fromEntries(
        books.map((b) => [b, entries.filter((x) => x.book === b).length])
      ),
      entries,
    };
    fs.writeFileSync(
      path.join(REPORTS, `${prefix}-ketiv-qere.json`),
      JSON.stringify(kqReport, null, 2)
    );
    const kqMd = [
      `# ${title} Ketiv/Qere report (auto)`,
      ``,
      `Pack version: **${PACK_VERSION}**`,
      `Generated: ${kqReport.generatedAt}`,
      `Total K/Q pairs: **${entries.length}**`,
      ``,
      `| Book | Count |`,
      `|------|------:|`,
      ...books.map((b) => `| ${BOOKS[b].title} | ${kqReport.byBook[b] || 0} |`),
      ``,
      `## Entries`,
      ``,
      ...entries.map(
        (e, i) =>
          `${i + 1}. **${e.verseId}** #${e.tokenIndex} — ketiv \`${e.ketiv}\` → qere \`${e.qereHe}\` / \`${e.qerePhonetic}\` (${e.lemmaId || "?"})`
      ),
      ``,
    ].join("\n");
    fs.writeFileSync(path.join(REPORTS, `${prefix}-ketiv-qere.md`), kqMd);
    console.log(`K/Q report: ${entries.length} entries → reports/${prefix}-ketiv-qere.md`);
    return kqReport;
  }

  const neviimInBuild = bookList.filter((b) => NEVIIM.includes(b));
  const ketuvimInBuild = bookList.filter((b) => KETUVIM.includes(b));
  const torahInBuild = bookList.filter((b) => TORAH.includes(b));
  if (torahInBuild.length) {
    writeKqReport(
      "torah",
      "Torah",
      torahInBuild,
      allKetivQere.filter((e) => TORAH.includes(e.book))
    );
  }
  if (neviimInBuild.length) {
    writeKqReport(
      "neviim",
      "Nevi'im",
      neviimInBuild,
      allKetivQere.filter((e) => NEVIIM.includes(e.book))
    );
  }
  if (ketuvimInBuild.length) {
    writeKqReport(
      "ketuvim",
      "Ketuvim",
      ketuvimInBuild,
      allKetivQere.filter((e) => KETUVIM.includes(e.book))
    );
  }
  writeKqReport(reportPrefix(), scopeLabel(), bookList, allKetivQere);

  // Gaps report (non-fatal notes; hard-fail decides)
  fs.writeFileSync(
    path.join(REPORTS, `${reportPrefix()}-gaps.json`),
    JSON.stringify({ count: allGaps.length, gaps: allGaps }, null, 2)
  );
  if (neviimInBuild.length) {
    const neviGaps = allGaps.filter((g) =>
      neviimInBuild.some((b) => String(g).includes(b + ".") || String(g).endsWith(" " + b))
    );
    fs.writeFileSync(
      path.join(REPORTS, "neviim-gaps.json"),
      JSON.stringify({ count: neviGaps.length, gaps: neviGaps }, null, 2)
    );
  }

  // Remove obsolete Gen 1–3 single pack from assets (replaced by books/)
  for (const stale of ["pack_gen_1_3.json", "gloss_catalog.json"]) {
    const p = path.join(OUT_DIR, stale);
    if (fs.existsSync(p)) fs.unlinkSync(p);
  }

  console.log("Reloading packs from disk for hard-fail…");
  for (const b of bookList) {
    const gzPath = path.join(OUT_DIR, `books/${b}.json.gz`);
    const jsonPath = path.join(OUT_DIR, `books/${b}.json`);
    if (WRITE_GZIP && fs.existsSync(gzPath)) {
      packByBook[b] = JSON.parse(zlib.gunzipSync(fs.readFileSync(gzPath)).toString("utf8"));
    } else if (fs.existsSync(jsonPath)) {
      packByBook[b] = JSON.parse(fs.readFileSync(jsonPath, "utf8"));
    } else {
      throw new Error(`Missing pack on disk for hard-fail: ${b}`);
    }
  }
  console.log("Running hard-fail checks 1–8…");
  const checkResult = runHardFailChecks({
    catalog,
    packByBook,
    glossCatalog,
    ketivQere: allKetivQere,
    gaps: allGaps,
    verseMap,
    vendorRoot: VENDOR,
  });
  fs.writeFileSync(
    path.join(REPORTS, `${reportPrefix()}-hard-fail.json`),
    JSON.stringify(checkResult, null, 2)
  );
  if (neviimInBuild.length) {
    const neviCatalog = {
      ...catalog,
      books: catalog.books.filter((b) => NEVIIM.includes(b.osis)),
    };
    const neviPack = Object.fromEntries(
      neviimInBuild.map((b) => [b, packByBook[b]])
    );
    const neviKq = allKetivQere.filter((e) => NEVIIM.includes(e.book));
    const neviCheck = runHardFailChecks({
      catalog: neviCatalog,
      packByBook: neviPack,
      glossCatalog,
      ketivQere: neviKq,
      gaps: allGaps,
      verseMap,
      vendorRoot: VENDOR,
    });
    fs.writeFileSync(
      path.join(REPORTS, "neviim-hard-fail.json"),
      JSON.stringify(neviCheck, null, 2)
    );
    if (!neviCheck.ok) {
      console.error("NEVIIM HARD FAIL:", neviCheck.failures);
      process.exit(1);
    }
    console.log("Nevi'im hard-fail checks PASSED.");
  }
  if (ketuvimInBuild.length) {
    const ketCatalog = {
      ...catalog,
      books: catalog.books.filter((x) => KETUVIM.includes(x.osis)),
    };
    const ketPack = Object.fromEntries(ketuvimInBuild.map((x) => [x, packByBook[x]]));
    const ketKq = allKetivQere.filter((e) => KETUVIM.includes(e.book));
    const ketCheck = runHardFailChecks({
      catalog: ketCatalog,
      packByBook: ketPack,
      glossCatalog,
      ketivQere: ketKq,
      gaps: allGaps,
      verseMap,
      vendorRoot: VENDOR,
    });
    fs.writeFileSync(
      path.join(REPORTS, "ketuvim-hard-fail.json"),
      JSON.stringify(ketCheck, null, 2)
    );
    if (!ketCheck.ok) {
      console.error("KETUVIM HARD FAIL:", ketCheck.failures);
      process.exit(1);
    }
    console.log("Ketuvim hard-fail checks PASSED.");
  }
  if (!checkResult.ok) {
    console.error("HARD FAIL:", checkResult.failures);
    process.exit(1);
  }
  console.log("Hard-fail checks PASSED.");
  console.log(
    `Done. ${totalVerses} verses across ${bookList.length} books. Gloss sanitize + YHWH policy enforced.`
  );
}

main();
