/**
 * Unit tests: nested <seg> flatten + Deut.6.4 token count/content.
 * Run: node --test oshb-w.test.mjs
 */
import test from "node:test";
import assert from "node:assert/strict";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import {
  flattenWInner,
  extractWTokens,
  verseBodyFromOshbXml,
} from "./oshb-w.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const VENDOR = path.resolve(__dirname, "../../vendor");

const CANTILLATION = /[\u0591-\u05AF\u05BD\u05BF\u05C0\u05C3\u05C6]/g;
function surface(heRaw) {
  return String(heRaw).replace(CANTILLATION, "").replace(/\//g, "");
}
function consonants(s) {
  return [...s].filter((ch) => /[\u05D0-\u05EA]/.test(ch)).join("");
}

test("flattenWInner concatenates nested x-large seg text", () => {
  assert.equal(
    flattenWInner('שְׁמַ֖<seg type="x-large">ע</seg>'),
    "שְׁמַ֖ע"
  );
  assert.equal(
    flattenWInner('אֶחָֽ<seg type="x-large">ד</seg>'),
    "אֶחָֽד"
  );
  assert.equal(
    flattenWInner('גָּח֜<seg type="x-large">וֹ</seg>ן'),
    "גָּח֜וֹן"
  );
  assert.equal(
    flattenWInner('מִשְׁפָּטָ֖/<seg type="x-large">ן</seg>'),
    "מִשְׁפָּטָ֖/ן"
  );
  // plain (no seg) unchanged
  assert.equal(flattenWInner("יִשְׂרָאֵ֑ל"), "יִשְׂרָאֵ֑ל");
});

test("extractWTokens keeps words that old [^<]* regex would drop", () => {
  const body =
    '<w lemma="8085" n="1.0" morph="HVqv2ms" id="05WH5">שְׁמַ֖<seg type="x-large">ע</seg></w>' +
    '<seg type="x-maqqef">־</seg>' +
    '<w lemma="3478" morph="HNp" id="05xxx">יִשְׂרָאֵ֑ל</w>';
  const toks = extractWTokens(body);
  assert.equal(toks.length, 2);
  assert.equal(surface(toks[0].heRaw), "שְׁמַע");
  assert.equal(consonants(surface(toks[0].heRaw)), "שמע");
  assert.equal(consonants(surface(toks[1].heRaw)), "ישראל");
});

test("Deut.6.4 OSHB has 6 flattened tokens including שמע and אחד", () => {
  const xml = fs.readFileSync(path.join(VENDOR, "oshb", "Deut.xml"), "utf8");
  const body = verseBodyFromOshbXml(xml, "Deut.6.4");
  assert.ok(body, "verse body present");
  const toks = extractWTokens(body);
  assert.equal(toks.length, 6);
  const cons = toks.map((t) => consonants(surface(t.heRaw)));
  assert.deepEqual(cons, ["שמע", "ישראל", "יהוה", "אלהינו", "יהוה", "אחד"]);
  assert.ok(cons.includes("שמע"));
  assert.ok(cons.includes("אחד"));
});

test("Lev.11.42 includes גחון; Num.27.5 includes משפטן", () => {
  const lev = fs.readFileSync(path.join(VENDOR, "oshb", "Lev.xml"), "utf8");
  const num = fs.readFileSync(path.join(VENDOR, "oshb", "Num.xml"), "utf8");
  const levToks = extractWTokens(verseBodyFromOshbXml(lev, "Lev.11.42"));
  const numToks = extractWTokens(verseBodyFromOshbXml(num, "Num.27.5"));
  assert.ok(
    levToks.map((t) => consonants(surface(t.heRaw))).includes("גחון"),
    "Lev.11.42 missing גחון"
  );
  assert.ok(
    numToks.map((t) => consonants(surface(t.heRaw))).includes("משפטן"),
    "Num.27.5 missing משפטן"
  );
  assert.equal(numToks.length, 6);
});
