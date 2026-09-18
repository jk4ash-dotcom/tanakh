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

test("orphan qere insertions appear (Nevi'im Sofer HIGH ×8 + Ruth)", () => {
  const cases = [
    ["Judg", "Judg.20.13", "בני"],
    ["2Sam", "2Sam.8.3", "פרת"],
    ["2Sam", "2Sam.16.23", "איש"],
    ["2Kgs", "2Kgs.19.31", "צבאות"],
    ["2Kgs", "2Kgs.19.37", "בניו"],
    ["Jer", "Jer.31.38", "באים"],
    ["Jer", "Jer.50.29", "לה"],
    ["Ruth", "Ruth.3.5", "אלי"],
    ["Ruth", "Ruth.3.17", "אלי"],
  ];
  for (const [book, id, need] of cases) {
    const xml = fs.readFileSync(path.join(VENDOR, "oshb", `${book}.xml`), "utf8");
    const toks = extractWTokens(verseBodyFromOshbXml(xml, id));
    const cons = toks.map((t) => consonants(surface(t.heRaw)));
    assert.ok(cons.includes(need), `${id} missing ${need}; got ${cons.join(",")}`);
  }
});

test("Jer.48.44 catchWord qere replaces הניס with הנס + ketiv", () => {
  const xml = fs.readFileSync(path.join(VENDOR, "oshb", "Jer.xml"), "utf8");
  const toks = extractWTokens(verseBodyFromOshbXml(xml, "Jer.48.44"));
  const cons = toks.map((t) => consonants(surface(t.heRaw)));
  assert.ok(cons.includes("הנס"), `missing הנס; got ${cons.join(",")}`);
  assert.ok(!cons.includes("הניס"), "ketiv הניס must not remain as surface");
  const q = toks.find((t) => consonants(surface(t.heRaw)) === "הנס");
  assert.ok(q.qere);
  assert.equal(consonants(q.ketiv), "הניס");
});

test("Judg.20.13 has אבו→בני→בנימן sequence from orphan qere", () => {
  const xml = fs.readFileSync(path.join(VENDOR, "oshb", "Judg.xml"), "utf8");
  const toks = extractWTokens(verseBodyFromOshbXml(xml, "Judg.20.13"));
  const cons = toks.map((t) => consonants(surface(t.heRaw)));
  const i = cons.indexOf("אבו");
  assert.ok(i >= 0);
  assert.equal(cons[i + 1], "בני");
  assert.equal(cons[i + 2], "בנימן");
});
