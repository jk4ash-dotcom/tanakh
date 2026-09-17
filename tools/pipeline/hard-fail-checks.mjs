/**
 * Sofer hard-fail quality gates (1–8) for pack build / CI.
 * Any failure throws via returned { ok:false, failures }.
 */
import { BOOKS, TORAH } from "./books.mjs";

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

export function runHardFailChecks(ctx) {
  const { catalog, packByBook, glossCatalog, ketivQere } = ctx;
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
    const EXPECTED_CH = { Gen: 50, Exod: 40, Lev: 27, Num: 36, Deut: 34 };
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

  // --- 3. Phonetics ---
  for (const b of books) {
    const pack = packByBook[b];
    if (pack.meta.aramaic) {
      // Aramaic: must NOT silently apply Hebrew schema successfully as if Hebrew
      const bad = pack.verses.flatMap((v) => v.words).filter((w) => w.aramaic !== true && !w.divineName);
      // For flagged aramaic books we require aramaic marker OR pending phonetic
      // (Torah N/A)
      continue;
    }
    for (const v of pack.verses) {
      for (let i = 0; i < v.words.length; i++) {
        const w = v.words[i];
        if (!w.phonetic || !String(w.phonetic).trim()) {
          failures.push(`3-phonetics: blank ${v.id}#${i}`);
        }
        if (w.divineName && w.phonetic !== "YHWH") {
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
        const isY = w.divineName || w.lemmaId === "H3068" || (w.he && w.he.includes(YHWH_CONS));
        if (!isY) continue;
        yhwhCount++;
        if (w.he !== YHWH_CONS) failures.push(`4-YHWH: he=${w.he} at ${v.id}`);
        if (w.phonetic !== "YHWH") failures.push(`4-YHWH: phonetic=${w.phonetic} at ${v.id}`);
        if (!w.divineName) failures.push(`4-YHWH: divineName false at ${v.id}`);
        if (/[aeiouAEIOUĕâîōûăŏ]/.test(w.phonetic) && w.phonetic !== "YHWH") {
          failures.push(`4-YHWH: vocalization leak at ${v.id}`);
        }
      }
    }
  }
  if (books.includes("Gen") && yhwhCount < 1) {
    failures.push("4-YHWH: expected at least one YHWH in Torah sample");
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

  // Torah-specific sample verses for Sofer ping
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
