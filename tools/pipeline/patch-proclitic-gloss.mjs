/**
 * Small pack/gloss patch: compound proclitic lemmas (c/l, c/b, c/m, s/l, i/l, …).
 * Does NOT full-rebuild corpus — only rewrites matching tokens + gloss catalog entries.
 * Policy: functional procliticNote line; never fake TBESH content senses.
 */
import fs from "fs";
import path from "path";
import zlib from "zlib";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../..");
const DATA = path.join(ROOT, "app/src/main/assets/data");
const BOOKS_DIR = path.join(DATA, "books");

const PREFIX_MAP = {
  b: "בְּ (bet) proclitic — often 'in/with'",
  c: "וְ (vav) proclitic — often 'and'",
  d: "הַ (he) article/proclitic — often 'the'",
  l: "לְ (lamed) proclitic — often 'to/for'",
  m: "מִ (mem) proclitic — often 'from'",
  s: "שׁ (shin) relative proclitic",
  k: "כְּ (kaph) proclitic — often 'like/as'",
  i: "הֲ (he) interrogative proclitic",
};

function isCompoundProclitic(raw) {
  if (!raw) return false;
  const parts = String(raw).trim().split("/");
  return parts.length >= 2 && parts.every((p) => /^[bciklmds]$/i.test(p));
}

function prefixNote(raw) {
  const parts = String(raw).trim().split("/").map((p) => p.toLowerCase());
  return parts.map((p) => PREFIX_MAP[p] || `proclitic marker '${p}'`).join("; ");
}

function readJsonMaybeGz(filePath) {
  const buf = fs.readFileSync(filePath);
  if (filePath.endsWith(".gz") || (buf[0] === 0x1f && buf[1] === 0x8b)) {
    return JSON.parse(zlib.gunzipSync(buf).toString("utf8"));
  }
  return JSON.parse(buf.toString("utf8"));
}

function writeJsonGz(filePath, obj) {
  const json = JSON.stringify(obj);
  if (filePath.endsWith(".gz")) {
    fs.writeFileSync(filePath, zlib.gzipSync(Buffer.from(json, "utf8")));
  } else {
    fs.writeFileSync(filePath, json);
  }
  return Buffer.byteLength(json);
}

const glossPathGz = path.join(DATA, "glosses.json.gz");
const glossPathPlain = path.join(DATA, "glosses.json");
const glossPath = fs.existsSync(glossPathGz) ? glossPathGz : glossPathPlain;
const glosses = readJsonMaybeGz(glossPath);

let tokensPatched = 0;
let booksTouched = 0;
const lemmaCounts = {};

for (const name of fs.readdirSync(BOOKS_DIR).sort()) {
  if (!name.endsWith(".json") && !name.endsWith(".json.gz")) continue;
  const fp = path.join(BOOKS_DIR, name);
  const pack = readJsonMaybeGz(fp);
  let changed = false;
  for (const v of pack.verses || []) {
    for (const w of v.words || []) {
      if (!isCompoundProclitic(w.lemmaRaw)) continue;
      const functional = prefixNote(w.lemmaRaw);
      const gid = `pfx:${w.lemmaRaw}`;
      w.glossId = gid;
      w.lemmaId = null;
      w.procliticNote = functional;
      if (!glosses[gid]) {
        glosses[gid] = {
          id: gid,
          primary: functional,
          senses: [],
          source: "proclitic-functional",
          definition: null,
          note:
            "OSHB proclitic-only compound (no content lemma); functional role — not a TBESH lexical sense",
        };
      } else {
        glosses[gid].primary = functional;
        glosses[gid].source = "proclitic-functional";
        glosses[gid].senses = [];
        glosses[gid].note =
          "OSHB proclitic-only compound (no content lemma); functional role — not a TBESH lexical sense";
      }
      // Drop stale shared "unknown" pollution only if unused later
      lemmaCounts[w.lemmaRaw] = (lemmaCounts[w.lemmaRaw] || 0) + 1;
      tokensPatched++;
      changed = true;
    }
  }
  if (changed) {
    writeJsonGz(fp, pack);
    booksTouched++;
    console.log(`patched ${name}`);
  }
}

// If "unknown" only held (no lemma) for these, refresh or leave; safe to keep for other gaps.
if (glosses.unknown && glosses.unknown.primary === "(no lemma)") {
  // leave — still used if any true missing lemmas remain
}

writeJsonGz(glossPath, glosses);
if (glossPath.endsWith(".gz") && fs.existsSync(glossPathPlain)) {
  // Keep plain sibling in sync if present (aapt2 / local)
  fs.writeFileSync(glossPathPlain, JSON.stringify(glosses));
}

console.log(
  JSON.stringify(
    {
      tokensPatched,
      booksTouched,
      lemmaCounts,
      glossEntries: Object.keys(glosses).length,
      pfxEntries: Object.keys(glosses).filter((k) => k.startsWith("pfx:")).length,
    },
    null,
    2
  )
);
