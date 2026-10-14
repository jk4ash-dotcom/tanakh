/**
 * v0.7.1 in-place pack patch (no full OSHB rebuild): apply the same JPS DN normalization
 * build-pack.mjs now does (Jehovah/Yehovah/Yahweh → YHWH; Adonai-x titles verbatim) to the
 * shipped book packs, and set catalog.version. Re-run build-concordance.mjs afterwards.
 */
import fs from "fs";
import path from "path";
import zlib from "zlib";
import { fileURLToPath } from "url";
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const DATA = path.join(ROOT, "app/src/main/assets/data");
const VERSION = "0.7.1-concordance-poc";
const RE = /\b(jehovah|yehovah|yahweh)\b/gi;
const catalogPath = path.join(DATA, "catalog.json");
const catalog = JSON.parse(fs.readFileSync(catalogPath, "utf8"));
for (const b of catalog.books) {
  const fp = path.join(DATA, "books", `${b.osis}.json.gz`);
  const pack = JSON.parse(zlib.gunzipSync(fs.readFileSync(fp)).toString("utf8"));
  let n = 0;
  for (const v of pack.verses) {
    const t = v.english.text.replace(RE, "YHWH");
    if (t !== v.english.text) { console.log(`${v.id}: ${v.english.text} → ${t}`); v.english.text = t; n++; }
  }
  if (n) fs.writeFileSync(fp, zlib.gzipSync(Buffer.from(JSON.stringify(pack), "utf8"), { level: 9 }));
}
catalog.version = VERSION;
fs.writeFileSync(catalogPath, JSON.stringify(catalog, null, 2));
