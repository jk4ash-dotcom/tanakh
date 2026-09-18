/**
 * Unit tests: OSHB morph language = first character only.
 * Run: node --test morph-lang.test.mjs
 */
import test from "node:test";
import assert from "node:assert/strict";
import { isAramaicMorph } from "./morph-lang.mjs";

test("Hebrew adjective morph HTd/Aamsa is NOT Aramaic", () => {
  assert.equal(isAramaicMorph("HTd/Aamsa"), false);
  assert.equal(isAramaicMorph("HTd/Aampa"), false);
  assert.equal(isAramaicMorph("HTd/Aafsa"), false);
});

test("true Aramaic morph starting with A is Aramaic", () => {
  assert.equal(isAramaicMorph("ANcmsd/Td"), true);
  assert.equal(isAramaicMorph("AVqp3ms"), true);
  assert.equal(isAramaicMorph("A"), true);
});

test("plain Hebrew morph is NOT Aramaic", () => {
  assert.equal(isAramaicMorph("HVqv2ms"), false);
  assert.equal(isAramaicMorph("HNcmsc"), false);
});

test("empty / null morph is NOT Aramaic", () => {
  assert.equal(isAramaicMorph(""), false);
  assert.equal(isAramaicMorph(null), false);
  assert.equal(isAramaicMorph(undefined), false);
});
