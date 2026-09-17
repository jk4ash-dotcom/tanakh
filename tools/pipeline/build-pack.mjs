#!/usr/bin/env node
/**
 * Build offline pack for Gen 1–3:
 * OSHB v.2.2 tokens + Sofer SBL-Learner phonetics + JPS 1917 verse English
 * + TBESH primary glosses (HebrewStrong.xml fallback).
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { createRequire } from "module";

const require = createRequire(import.meta.url);
const { transliterate } = require("hebrew-transliteration");
const { createSoferSblLearnerSchema } = await import("./sofer-sbl-learner.mjs");

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../..");
const VENDOR = path.join(ROOT, "vendor");
const OUT_DIR = path.join(ROOT, "app/src/main/assets/data");

const CHAPTERS = [1, 2, 3];
const schema = createSoferSblLearnerSchema();

const CANTILLATION = /[\u0591-\u05AF\u05BD\u05BF\u05C0\u05C3\u05C6]/g;
const NIQQUD_AND_MARKS = /[\u0591-\u05C7]/g;
const HEBREW_LETTER = /[\u05D0-\u05EA]/;

function stripCantillation(s) {
  return s.replace(CANTILLATION, "");
}

function consonantsOnly(s) {
  return [...s].filter((ch) => HEBREW_LETTER.test(ch)).join("");
}

function isYhwhLemma(lemmaId) {
  return lemmaId === "H3068" || lemmaId.startsWith("H3068");
}

/** Parse OSHB lemma attr → { baseId: "H07225", prefixes: ["b"], aug: "a"|null } */
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
  // Standalone proclitic letter lemmas (e.g. lemma="b" on בּוֹ)
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
  for (let i = 0; i < parts.length - 1; i++) {
    prefixes.push(parts[i]);
  }
  // e.g. "1254 a" or "430" or "5921 a"
  const m = core.match(/^(\d+)\s*([a-zA-Z])?$/);
  if (!m) {
    return { baseId: null, prefixes, aug: null, raw: lemmaAttr };
  }
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
    const eStrong = cols[0].trim(); // H0001 or H1254a
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
  // Prefer exact (incl. letter aug), then numeric; prefer *G general rows
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
      senses: defs.length ? defs : usageClean ? usageClean.split(/[,;]/).map((s) => s.trim()).filter(Boolean) : [],
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
  // Normalize: H7225 vs H07225 — TBESH uses 4-digit
  const candidates = [];
  if (lemmaInfo.aug) {
    candidates.push("H" + lemmaInfo.numeric + lemmaInfo.aug);
  }
  candidates.push("H" + lemmaInfo.numeric);

  let picked = null;
  for (const c of candidates) {
    picked = pickTbesh(tbesh, c);
    if (picked) break;
  }
  if (picked) {
    const senses = [];
    if (picked.gloss) senses.push(picked.gloss);
    // pull numbered senses from definition lightly
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

  const hsId = "H" + String(parseInt(lemmaInfo.numeric, 10)); // HebrewStrong uses H1, H430 not padded
  const hsAlt = "H" + lemmaInfo.numeric.replace(/^0+/, "") || "H0";
  const hs =
    strong.get("H" + parseInt(lemmaInfo.numeric, 10)) ||
    strong.get("H" + lemmaInfo.numeric) ||
    strong.get(hsAlt);
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
  // Remove OSHB morph slash separators; keep niqqud; drop cantillation for UI
  return stripCantillation(raw.replace(/\//g, ""));
}

function phoneticForToken(heSurface, lemmaInfo) {
  if (lemmaInfo.numeric === "3068" || isYhwhLemma(lemmaInfo.baseId || "")) {
    return { phonetic: "YHWH", ketivQere: null, divineName: true };
  }
  // Transliterate from pointed surface (niqqud intact; cantillation ok for lib)
  const input = heSurface.replace(/־/g, "");
  try {
    const phonetic = transliterate(input, schema);
    return { phonetic, ketivQere: null, divineName: false };
  } catch (e) {
    return { phonetic: "[transliteration-error]", ketivQere: null, divineName: false, error: String(e) };
  }
}

function displayHebrew(heSurface, lemmaInfo) {
  if (lemmaInfo.numeric === "3068") {
    return consonantsOnly(heSurface); // יהוה only
  }
  return heSurface;
}

function parseOshbGenChapters(xmlPath, chapters) {
  const xml = fs.readFileSync(xmlPath, "utf8");
  const verses = [];
  for (const ch of chapters) {
    const chRe = new RegExp(
      `<chapter osisID="Gen\\.${ch}">([\\s\\S]*?)</chapter>`
    );
    const chm = xml.match(chRe);
    if (!chm) throw new Error("Missing chapter Gen." + ch);
    const verseRe = /<verse osisID="(Gen\.\d+\.\d+)">([\s\S]*?)<\/verse>/g;
    let vm;
    while ((vm = verseRe.exec(chm[1]))) {
      const osisId = vm[1];
      const body = vm[2];
      const tokens = [];
      // Walk tokens: prefer qere when ketiv present
      // Pattern 1: ketiv + note/qere
      const ketivRe =
        /<w type="x-ketiv"[^>]*lemma="([^"]*)"[^>]*morph="([^"]*)"[^>]*id="([^"]*)"[^>]*>([^<]*)<\/w>\s*<note type="variant">[\s\S]*?<rdg type="x-qere"><w([^>]*)>([^<]*)<\/w>/g;
      const consumed = new Set();
      let km;
      while ((km = ketivRe.exec(body))) {
        const qereAttrs = km[5];
        const qereLemma = (qereAttrs.match(/lemma="([^"]*)"/) || [])[1] || km[1];
        const qereMorph = (qereAttrs.match(/morph="([^"]*)"/) || [])[1] || km[2];
        const qereId = (qereAttrs.match(/id="([^"]*)"/) || [])[1] || km[3];
        const qereText = km[6];
        const ketivText = km[4];
        tokens.push({
          order: km.index,
          heRaw: qereText,
          lemma: qereLemma,
          morph: qereMorph,
          id: qereId,
          ketiv: stripCantillation(ketivText.replace(/\//g, "")),
          qere: true,
        });
        consumed.add(km.index);
      }
      // Regular <w> not ketiv
      const wRe =
        /<w(?![^>]*type="x-ketiv")([^>]*)>([^<]*)<\/w>/g;
      let wm;
      while ((wm = wRe.exec(body))) {
        // skip if inside a qere we already handled — qere w tags are inside note
        if (/type="x-qere"/.test(wm[0]) || /type="x-ketiv"/.test(wm[0])) continue;
        // skip w that are children of rdg (already captured)
        const before = body.slice(Math.max(0, wm.index - 80), wm.index);
        if (/x-qere[^>]*>\s*$/.test(before) || /<rdg type="x-qere">\s*$/.test(before)) {
          continue;
        }
        const attrs = wm[1];
        const lemma = (attrs.match(/lemma="([^"]*)"/) || [])[1] || "";
        const morph = (attrs.match(/morph="([^"]*)"/) || [])[1] || "";
        const id = (attrs.match(/id="([^"]*)"/) || [])[1] || "";
        tokens.push({
          order: wm.index,
          heRaw: wm[2],
          lemma,
          morph,
          id,
          ketiv: null,
          qere: false,
        });
      }
      tokens.sort((a, b) => a.order - b.order);
      // Deduplicate accidental overlaps by id
      const seen = new Set();
      const uniq = [];
      for (const t of tokens) {
        const key = t.id || t.order;
        if (seen.has(key)) continue;
        seen.add(key);
        uniq.push(t);
      }
      verses.push({ osisId, chapter: ch, verse: parseInt(osisId.split(".")[2], 10), tokens: uniq });
    }
  }
  return verses;
}

function loadJpsChapter(ch) {
  const p = path.join(
    VENDOR,
    "jps1917",
    `engjps_002_GEN_${String(ch).padStart(2, "0")}_read.txt`
  );
  const lines = fs
    .readFileSync(p, "utf8")
    .replace(/^\uFEFF/, "")
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean);
  // drop title + "Chapter N."
  const verses = [];
  for (const line of lines) {
    if (/^Chapter\s+\d+\.?$/i.test(line)) continue;
    if (/^The First Book/i.test(line)) continue;
    if (/^Genesis\.?$/i.test(line)) continue;
    verses.push(line);
  }
  return verses;
}

function main() {
  console.log("Loading lexicons…");
  const tbesh = loadTbesh(path.join(VENDOR, "TBESH.txt"));
  const strong = loadHebrewStrong(path.join(VENDOR, "HebrewStrong.xml"));
  console.log("Parsing OSHB Gen.xml…");
  const oshbVerses = parseOshbGenChapters(
    path.join(VENDOR, "oshb", "Gen.xml"),
    CHAPTERS
  );
  const jpsByChapter = Object.fromEntries(
    CHAPTERS.map((ch) => [ch, loadJpsChapter(ch)])
  );

  const glossCatalog = {};
  const packVerses = [];
  const gaps = [];

  for (const v of oshbVerses) {
    const jpsLines = jpsByChapter[v.chapter] || [];
    const english = jpsLines[v.verse - 1] || "";
    if (!english) {
      gaps.push(`Missing JPS for ${v.osisId}`);
    }
    const words = [];
    for (let i = 0; i < v.tokens.length; i++) {
      const t = v.tokens[i];
      const lemmaInfo = parseLemma(t.lemma);
      const he = displayHebrew(surfaceHebrew(t.heRaw), lemmaInfo);
      const ph = phoneticForToken(surfaceHebrew(t.heRaw), lemmaInfo);
      const gloss = resolveGloss(tbesh, strong, lemmaInfo);
      const glossId = gloss.id || `tok-${t.id}`;
      if (!glossCatalog[glossId]) {
        glossCatalog[glossId] = {
          id: glossId,
          primary: gloss.primary,
          senses: gloss.senses.length ? gloss.senses : [gloss.primary],
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
      }
      if (pfx) {
        word.procliticNote = pfx;
      }
      if (ph.error) {
        gaps.push(`Phonetic error ${v.osisId}#${i}: ${ph.error}`);
      }
      if (gloss.source === "none") {
        gaps.push(`Gloss gap ${v.osisId} lemma=${lemmaInfo.raw}`);
      }
      words.push(word);
    }
    packVerses.push({
      id: v.osisId,
      book: "Gen",
      chapter: v.chapter,
      verse: v.verse,
      english: {
        text: english,
        source: "JPS 1917",
        license: "Public Domain",
      },
      words,
    });
  }

  const pack = {
    meta: {
      name: "tanakh-learner-poc-gen1-3",
      version: "0.1.0-poc",
      generatedAt: new Date().toISOString(),
      scope: "Genesis 1–3",
      hebrew: {
        source: "OSHB / morphhb WLC",
        pin: "v.2.2",
        file: "vendor/oshb/Gen.xml",
        license: "WLC text Public Domain; lemma/morphology CC BY 4.0 (Open Scriptures Hebrew Bible)",
      },
      english: {
        source: "JPS TaNaKH 1917",
        license: "Public Domain",
        note: "Verse-level only — not word-aligned to Hebrew tokens",
        provider: "ebible.org engjps",
      },
      phonetics: {
        engine: "hebrew-transliteration@2.11.0",
        schema: "Sofer SBL-Learner",
        policy:
          "Derived from OSHB/WLC niqqud. Biblical/Tiberian + digraphs sh/kh/ts/ʾ/ʿ; vocal shewa ĕ; NOT Modern Israeli. יהוה → YHWH only (no invented vocalization). Phonetics follow qere when ketiv/qere present.",
        mappingDoc: "docs/SOFER_SBL_LEARNER.md",
      },
      glosses: {
        primary: "TBESH (STEPBible) CC BY 4.0",
        fallback: "OSHB HebrewStrong.xml CC BY 4.0",
        uiPolicy: "Header: Possible sense(s). Footer: Gloss ≠ verse translation.",
      },
      display: {
        tokenOrder:
          "Single token array preserves OSHB order. UI may show LTR paired chips (RTL Hebrew glyph direction inside chip + LTR phonetic) without reversing the array.",
      },
      gaps,
    },
    chapters: CHAPTERS.map((ch) => ({
      book: "Gen",
      chapter: ch,
      verseIds: packVerses.filter((x) => x.chapter === ch).map((x) => x.id),
    })),
    verses: packVerses,
    glosses: glossCatalog,
  };

  fs.mkdirSync(OUT_DIR, { recursive: true });
  const outFile = path.join(OUT_DIR, "pack_gen_1_3.json");
  fs.writeFileSync(outFile, JSON.stringify(pack, null, 2));
  const catalogLite = path.join(OUT_DIR, "gloss_catalog.json");
  fs.writeFileSync(catalogLite, JSON.stringify(glossCatalog, null, 2));

  console.log(
    `Wrote ${outFile} — ${packVerses.length} verses, ${Object.keys(glossCatalog).length} glosses, ${gaps.length} gap notes`
  );
  if (gaps.length) {
    console.log("Gaps (first 20):");
    gaps.slice(0, 20).forEach((g) => console.log(" -", g));
  }
  // sanity Gen 1:1
  const g11 = packVerses.find((v) => v.id === "Gen.1.1");
  console.log("Gen.1.1 sample:", JSON.stringify(g11, null, 2).slice(0, 1200));
}

main();
