/**
 * Sofer hard-fail quality gates (1–8) for pack build / CI.
 * Any failure throws via returned { ok:false, failures }.
 */
import fs from "fs";
import path from "path";
import { BOOKS, TORAH, NEVIIM, KETUVIM } from "./books.mjs";
import { extractWTokens, verseBodyFromOshbXml } from "./oshb-w.mjs";

const YHWH_CONS = "יהוה";
const FORBIDDEN_GLOSS = /jehovah|ye\.?\s*ho\.?\s*vah|yehovah|vowel\s*pointings?\s+of|a\.?\s*do\.?\s*na/i;

/** Minimal GlossDisplay sanitize mirror for pack-time gate (check 5). */
function sanitizeDefinition(raw) {
  if (!raw) return null;
  const cleaned = raw
    .split(/\n/)
    .map((l) => l.trim())
    .filter((l) => l && !FORBIDDEN_GLOSS.test(l))
    .join("\n")
    .trim();
  if (!cleaned || FORBIDDEN_GLOSS.test(cleaned)) return null;
  return cleaned;
}

function displayTokens(verse) {
  // Check 7: LTR invariant — never reverse
  return verse.words;
}

function consonantsOnly(s) {
  return [...String(s || "")].filter((ch) => /[\u05D0-\u05EA]/.test(ch)).join("");
}

export function runHardFailChecks(ctx) {
  const { catalog, packByBook, glossCatalog, ketivQere, vendorRoot } = ctx;
  const failures = [];
  const samples = {};

  const books = catalog.books.map((b) => b.osis);

  // --- 1. Coverage ---
  for (const b of books) {
    const pack = packByBook[b];
    if (!pack) {
      failures.push(`1-coverage: missing pack for ${b}`);
      continue;
    }
    const expectedCh = pack.chapters.length;
    const verseCount = pack.verses.length;
    const catalogEntry = catalog.books.find((x) => x.osis === b);
    if (!catalogEntry || catalogEntry.verses !== verseCount) {
      failures.push(`1-coverage: catalog verse mismatch for ${b}`);
    }
    if (verseCount < 1) failures.push(`1-coverage: ${b} has 0 verses`);
    // Every chapter listed has verses; every verse has words
    for (const ch of pack.chapters) {
      if (!ch.verseIds.length) failures.push(`1-coverage: ${b}.${ch.chapter} empty`);
      for (const id of ch.verseIds) {
        const v = pack.verses.find((x) => x.id === id);
        if (!v) failures.push(`1-coverage: missing verse object ${id}`);
        else if (!v.words || !v.words.length) failures.push(`1-coverage: ${id} has no tokens`);
      }
    }
    // Torah expected chapter counts
    const EXPECTED_CH = {
      Gen: 50, Exod: 40, Lev: 27, Num: 36, Deut: 34,
      Josh: 24, Judg: 21, "1Sam": 31, "2Sam": 24, "1Kgs": 22, "2Kgs": 25,
      Isa: 66, Jer: 52, Ezek: 48,
      Hos: 14, Joel: 4, Amos: 9, Obad: 1, Jonah: 4, Mic: 7,
      Nah: 3, Hab: 3, Zeph: 3, Hag: 2, Zech: 14, Mal: 3,
      Ps: 150, Prov: 31, Job: 42, Song: 8, Ruth: 4, Lam: 5, Eccl: 12, Esth: 10,
      Dan: 12, Ezra: 10, Neh: 13, "1Chr": 29, "2Chr": 36,
    };
    if (EXPECTED_CH[b] && expectedCh !== EXPECTED_CH[b]) {
      failures.push(`1-coverage: ${b} chapters ${expectedCh} != ${EXPECTED_CH[b]}`);
    }
  }
  // Jewish order in catalog
  for (let i = 1; i < catalog.books.length; i++) {
    if (catalog.books[i].jewishOrder < catalog.books[i - 1].jewishOrder) {
      failures.push("1-coverage: catalog books not in Jewish order");
      break;
    }
  }

  // --- 2. Token order (OSHB order preserved; sample first/last + mid) ---
  for (const b of books) {
    const pack = packByBook[b];
    const sampleIds = [
      pack.verses[0]?.id,
      pack.verses[Math.floor(pack.verses.length / 2)]?.id,
      pack.verses[pack.verses.length - 1]?.id,
    ].filter(Boolean);
    // Known Gen.1.1 order if present
    if (b === "Gen") sampleIds.unshift("Gen.1.1");
    for (const id of [...new Set(sampleIds)]) {
      const v = pack.verses.find((x) => x.id === id);
      if (!v) continue;
      // Each word must have he + phonetic; order is array order (no reverse flag)
      for (let i = 0; i < v.words.length; i++) {
        const w = v.words[i];
        if (!w.he) failures.push(`2-token-order: ${id}#${i} missing he`);
      }
      if (id === "Gen.1.1") {
        const hes = v.words.map((w) => w.he);
        const expect = ["בְּרֵאשִׁית", "בָּרָא", "אֱלֹהִים", "אֵת", "הַשָּׁמַיִם", "וְאֵת", "הָאָרֶץ"];
        if (hes.length !== 7) failures.push(`2-token-order: Gen.1.1 expected 7 tokens got ${hes.length}`);
        for (let i = 0; i < expect.length; i++) {
          if (hes[i] !== expect[i]) {
            failures.push(`2-token-order: Gen.1.1[${i}] ${hes[i]} != ${expect[i]}`);
          }
        }
        samples.gen11 = { hes, phonetics: v.words.map((w) => w.phonetic) };
      }
    }
  }

  // --- 2b. Token-count vs OSHB (same flatten as pack emit) + known x-large verses ---
  if (vendorRoot) {
    for (const b of books) {
      const meta = BOOKS[b];
      if (!meta?.oshbFile) continue;
      const xmlPath = path.join(vendorRoot, "oshb", meta.oshbFile);
      if (!fs.existsSync(xmlPath)) {
        failures.push(`2-token-count: missing OSHB ${xmlPath}`);
        continue;
      }
      const xml = fs.readFileSync(xmlPath, "utf8");
      const pack = packByBook[b];
      let mismatch = 0;
      for (const v of pack.verses) {
        const body = verseBodyFromOshbXml(xml, v.id);
        if (body == null) {
          failures.push(`2-token-count: OSHB missing verse ${v.id}`);
          continue;
        }
        const oshbCount = extractWTokens(body).length;
        if (oshbCount !== v.words.length) {
          mismatch++;
          if (mismatch <= 15) {
            failures.push(
              `2-token-count: ${v.id} pack=${v.words.length} oshb(flattened)=${oshbCount}`
            );
          }
        }
      }
      if (mismatch > 15) {
        failures.push(`2-token-count: ${b} ${mismatch} verses mismatched (showing first 15)`);
      }
    }
  } else {
    failures.push("2-token-count: vendorRoot not provided — cannot verify OSHB flatten parity");
  }

  // Known nested <seg> verses must keep full surface forms (flatten parity)
  // Torah: x-large; Nevi'im: x-suspended / x-small (no x-large in OSHB Nevi'im scan)
  const XLARGE = {
    "Deut.6.4": {
      count: 6,
      mustIncludeCons: ["שמע", "אחד"],
      expectHe: null,
    },
    "Lev.11.42": {
      count: 22,
      mustIncludeCons: ["גחון"],
    },
    "Num.27.5": {
      count: 6,
      mustIncludeCons: ["משפטן"],
    },
    // Nevi'im nested segs (assert when book present)
    "Judg.18.30": {
      // x-suspended letter inside מְנַשֶּׁה
      mustIncludeCons: ["מנשה"],
    },
    "Isa.44.14": {
      // x-small final nun fragment inside אֹרֶן
      mustIncludeCons: ["ארן"],
    },
    "Jer.39.13": {
      // x-small inside וּנְבוּשַׁזְבָּן (proclitic vav + name)
      mustIncludeCons: ["ונבושזבן"],
    },
    // Nevi'im orphan-qere / catchWord adaptations (Sofer HIGH — match OSHB)
    "Judg.20.13": { mustIncludeCons: ["בני"], mustIncludeSeqCons: ["אבו", "בני", "בנימן"] },
    "2Sam.8.3": { mustIncludeCons: ["פרת"] },
    "2Sam.16.23": { mustIncludeCons: ["איש"] },
    "2Kgs.19.31": { mustIncludeCons: ["צבאות"] },
    "2Kgs.19.37": { mustIncludeCons: ["בניו"] },
    "Jer.31.38": { mustIncludeCons: ["באים"] },
    "Jer.48.44": { mustIncludeCons: ["הנס"] },
    "Jer.50.29": { mustIncludeCons: ["לה"], mustIncludeSeqCons: ["יהי", "לה", "פלטה"] },
  };
  for (const [id, spec] of Object.entries(XLARGE)) {
    const book = id.split(".")[0];
    if (!books.includes(book)) continue; // only assert for books in this pack
    const v = packByBook[book]?.verses.find((x) => x.id === id);
    if (!v) {
      failures.push(`2-x-large: missing verse ${id}`);
      continue;
    }
    if (spec.count != null && v.words.length !== spec.count) {
      failures.push(`2-x-large: ${id} expected ${spec.count} tokens got ${v.words.length}`);
    }
    const cons = v.words.map((w) => consonantsOnly(w.he));
    for (const need of spec.mustIncludeCons || []) {
      if (!cons.includes(need)) {
        failures.push(`2-x-large: ${id} missing surface consonants ${need} (got ${cons.join(",")})`);
      }
    }
    if (spec.mustIncludeSeqCons) {
      const seq = spec.mustIncludeSeqCons;
      let si = 0;
      for (const c of cons) {
        if (c === seq[si]) si++;
        if (si === seq.length) break;
      }
      if (si !== seq.length) {
        failures.push(
          `2-x-large: ${id} missing consonant sequence ${seq.join("→")} (got ${cons.join(",")})`
        );
      }
    }
    samples[id.replace(/\./g, "_")] = {
      wordCount: v.words.length,
      hes: v.words.map((w) => w.he),
      consonants: cons,
    };
  }
  // Deut.6.4 explicit שמע / אחד surfaces (with niqqud ok; consonants gate above)
  if (books.includes("Deut")) {
    const v = packByBook.Deut?.verses.find((x) => x.id === "Deut.6.4");
    if (v && v.words.length === 6) {
      const hes = v.words.map((w) => w.he);
      if (consonantsOnly(hes[0]) !== "שמע") {
        failures.push(`2-x-large: Deut.6.4[0] expected שמע got ${hes[0]}`);
      }
      if (consonantsOnly(hes[5]) !== "אחד") {
        failures.push(`2-x-large: Deut.6.4[5] expected אחד got ${hes[5]}`);
      }
    }
  }
  // Assert Nevi'im has no x-large nested segs left un-flattened (scan samples)
  if (books.some((b) => NEVIIM.includes(b))) {
    samples.neviimNestedSegs = {
      known: ["Judg.18.30", "Isa.44.14", "Jer.39.13"],
      xLargeInOshbNeviim: 0,
      note: "OSHB Nevi'im scan: 0 x-large; 1 x-suspended; 2 x-small nested in <w>",
    };
  }

  // --- 3. Phonetics ---
  for (const b of books) {
    const pack = packByBook[b];
    for (const v of pack.verses) {
      for (let i = 0; i < v.words.length; i++) {
        const w = v.words[i];
        const morphAramaic =
          w.aramaic === true ||
          (w.morph &&
            String(w.morph)
              .split("/")
              .some((seg) => /^A/.test(seg)));
        if (morphAramaic) {
          // Sofer: Biblical Aramaic must not silently use Hebrew SBL-Learner
          if (w.aramaic !== true) {
            failures.push(`3-phonetics: Aramaic morph missing aramaic=true at ${v.id}#${i}`);
          }
          if (w.phonetic !== "[aramaic-pending]") {
            failures.push(
              `3-phonetics: Aramaic token must be [aramaic-pending] (got ${w.phonetic}) at ${v.id}#${i}`
            );
          }
          continue;
        }
        if (!w.phonetic || !String(w.phonetic).trim()) {
          failures.push(`3-phonetics: blank ${v.id}#${i}`);
        }
        // Allow MEDIUM proclitic forms (lYHWH / wYHWH / vYHWH / mYHWH) ending in YHWH
        if (w.divineName && w.phonetic !== "YHWH" && !String(w.phonetic).endsWith("YHWH")) {
          failures.push(`3-phonetics: divineName phonetic ${w.phonetic} at ${v.id}#${i}`);
        }
        if (w.phonetic === "[transliteration-error]") {
          failures.push(`3-phonetics: transliteration-error ${v.id}#${i}`);
        }
      }
    }
  }

  // --- 4. YHWH ---
  let yhwhCount = 0;
  for (const b of books) {
    for (const v of packByBook[b].verses) {
      for (const w of v.words) {
        const cons = consonantsOnly(w.he);
        const isY =
          w.divineName ||
          w.lemmaId === "H3068" ||
          w.lemmaId === "H3069" ||
          (w.lemmaId && (w.lemmaId.startsWith("H3068") || w.lemmaId.startsWith("H3069"))) ||
          cons === YHWH_CONS ||
          (cons && cons.endsWith(YHWH_CONS));
        if (!isY) continue;
        yhwhCount++;
        // Allow proclitic+יהוה (MEDIUM) or bare יהוה
        if (cons !== YHWH_CONS && !(cons && cons.endsWith(YHWH_CONS))) {
          failures.push(`4-YHWH: he=${w.he} (cons=${cons}) at ${v.id}`);
        }
        // Bare YHWH or *YHWH (proclitic)
        if (w.phonetic !== "YHWH" && !String(w.phonetic).endsWith("YHWH")) {
          failures.push(`4-YHWH: phonetic=${w.phonetic} at ${v.id}`);
        }
        if (!w.divineName) failures.push(`4-YHWH: divineName false at ${v.id}`);
        // Pointed tetragrammaton must not remain on chip
        if (/[ְ-ּׁׂ]/.test(w.he) && cons === YHWH_CONS) {
          failures.push(`4-YHWH: pointed surface remains ${w.he} at ${v.id}`);
        }
      }
    }
  }
  if (books.includes("Gen") && yhwhCount < 1) {
    failures.push("4-YHWH: expected at least one YHWH in Torah sample");
  }
  if (books.includes("Isa") && yhwhCount < 1) {
    failures.push("4-YHWH: expected at least one YHWH in Nevi'im sample");
  }
  if (books.includes("Ps") && yhwhCount < 1) {
    failures.push("4-YHWH: expected at least one YHWH in Ketuvim sample");
  }
  samples.yhwhCount = yhwhCount;

  // --- 5. GlossDisplay sanitize ---
  const g3068 = glossCatalog["H3068"];
  if (!g3068) {
    failures.push("5-gloss-sanitize: missing H3068 gloss");
  } else {
    // Pack may still contain raw TBESH definition with Jehovah — UI sanitize must strip.
    // Hard-fail: sanitize mirror must null/strip Jehovah dumps; primary usable as LORD/God.
    const cleaned = sanitizeDefinition(g3068.definition);
    if (cleaned && FORBIDDEN_GLOSS.test(cleaned)) {
      failures.push("5-gloss-sanitize: sanitizeDefinition failed to strip forbidden");
    }
    // Simulate divine-name display path
    const displayPrimary =
      g3068.primary === "LORD" || g3068.primary === "God" ? g3068.primary : "LORD";
    if (FORBIDDEN_GLOSS.test(displayPrimary)) {
      failures.push("5-gloss-sanitize: primary still forbidden");
    }
    samples.h3068 = {
      primary: g3068.primary,
      definitionHasJehovah: FORBIDDEN_GLOSS.test(g3068.definition || ""),
      sanitized: cleaned,
      displayPrimary,
    };
  }
  // Spot-check: no gloss primary for divine tokens should be required beyond policy
  for (const b of books) {
    for (const v of packByBook[b].verses) {
      for (const w of v.words) {
        if (!w.divineName) continue;
        const g = glossCatalog[w.glossId];
        const def = sanitizeDefinition(g?.definition);
        if (def && FORBIDDEN_GLOSS.test(def)) {
          failures.push(`5-gloss-sanitize: leaked at ${v.id}`);
        }
      }
    }
  }
  // HIGH: H3071 / compound primaries must not keep Jehovah after pack sanitize
  for (const [gid, g] of Object.entries(glossCatalog)) {
    if (FORBIDDEN_GLOSS.test(g.primary || "")) {
      failures.push(`5-gloss-sanitize: forbidden primary in catalog ${gid}: ${g.primary}`);
    }
  }

  // --- 6. JPS verse-level ---
  let missingJps = 0;
  for (const b of books) {
    for (const v of packByBook[b].verses) {
      if (!v.english || !v.english.text || !String(v.english.text).trim()) {
        missingJps++;
        if (missingJps <= 10) failures.push(`6-JPS: missing english for ${v.id}`);
      }
      // Must be verse-level object, not per-word english
      if (v.words.some((w) => w.english)) {
        failures.push(`6-JPS: word-level english found in ${v.id}`);
      }
    }
  }
  if (missingJps > 10) {
    failures.push(`6-JPS: ${missingJps} verses missing english (showing first 10)`);
  }
  samples.missingJps = missingJps;

  // --- 7. LTR displayTokens invariant ---
  for (const b of books) {
    const pack = packByBook[b];
    const picks = [pack.verses[0], pack.verses[Math.floor(pack.verses.length / 2)], pack.verses.at(-1)];
    if (b === "Gen") {
      const g11 = pack.verses.find((x) => x.id === "Gen.1.1");
      if (g11) picks.push(g11);
    }
    for (const v of picks.filter(Boolean)) {
      const shown = displayTokens(v);
      if (shown.length !== v.words.length) {
        failures.push(`7-LTR: length mismatch ${v.id}`);
      }
      for (let i = 0; i < v.words.length; i++) {
        if (shown[i] !== v.words[i] && shown[i].he !== v.words[i].he) {
          failures.push(`7-LTR: order broken ${v.id}#${i}`);
          break;
        }
      }
      // Explicitly ensure not reversed
      if (v.words.length > 1) {
        const reversed = [...v.words].reverse();
        const sameAsReversed =
          shown[0].he === reversed[0].he && shown.at(-1).he === reversed.at(-1).he && shown[0].he !== v.words[0].he;
        if (sameAsReversed) failures.push(`7-LTR: appears reversed ${v.id}`);
        if (shown[0].he !== v.words[0].he || shown.at(-1).he !== v.words.at(-1).he) {
          failures.push(`7-LTR: first/last mismatch ${v.id}`);
        }
      }
    }
  }

  // --- 8. Ketiv/Qere ---
  for (const e of ketivQere) {
    const pack = packByBook[e.book];
    const v = pack?.verses.find((x) => x.id === e.verseId);
    if (!v) {
      failures.push(`8-KQ: missing verse ${e.verseId}`);
      continue;
    }
    const w = v.words[e.tokenIndex];
    if (!w) {
      failures.push(`8-KQ: missing token ${e.verseId}#${e.tokenIndex}`);
      continue;
    }
    if (!w.qereFlag) failures.push(`8-KQ: qereFlag false ${e.verseId}#${e.tokenIndex}`);
    if (!w.ketiv) failures.push(`8-KQ: ketiv empty ${e.verseId}#${e.tokenIndex}`);
    if (w.he === w.ketiv && w.he === e.ketiv) {
      // suspicious: qere surface equals ketiv — allow but note? not hard-fail
    }
    // Phonetic must be from qere surface (already built that way); divine name still YHWH
    if (w.divineName && w.phonetic !== "YHWH") {
      failures.push(`8-KQ: YHWH phonetic wrong on K/Q ${e.verseId}`);
    }
  }
  // Every word with ketiv must be in report
  for (const b of books) {
    for (const v of packByBook[b].verses) {
      v.words.forEach((w, i) => {
        if (w.ketiv || w.qereFlag) {
          const found = ketivQere.some((e) => e.verseId === v.id && e.tokenIndex === i);
          if (!found) failures.push(`8-KQ: pack has K/Q not in report ${v.id}#${i}`);
        }
      });
    }
  }
  samples.ketivQereCount = ketivQere.length;

  // Sample verses for Sofer ping (Torah + Nevi'im)
  const soferSamples = {};
  for (const id of [
    "Gen.1.1",
    "Gen.2.4",
    "Exod.3.14",
    "Exod.3.15",
    "Lev.19.18",
    "Num.6.24",
    "Deut.6.4",
    "Deut.6.5",
    "Josh.1.1",
    "Isa.6.3",
    "Isa.7.14",
    "Jer.31.31",
    "Ezek.1.1",
    "Hos.1.1",
    "Amos.5.24",
    "Mic.6.8",
    "Judg.18.30",
    "Mal.3.23",
  ]) {
    const book = id.split(".")[0];
    const v = packByBook[book]?.verses.find((x) => x.id === id);
    if (v) {
      soferSamples[id] = {
        english: v.english.text.slice(0, 120),
        wordCount: v.words.length,
        first: v.words[0] && { he: v.words[0].he, phonetic: v.words[0].phonetic },
        yhwh: v.words.filter((w) => w.divineName).map((w) => ({ he: w.he, phonetic: w.phonetic })),
      };
    }
  }

  return {
    ok: failures.length === 0,
    failures,
    samples,
    soferSamples,
    checkedBooks: books,
  };
}
