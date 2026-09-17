/**
 * Sofer SBL-Learner schema for hebrew-transliteration.
 * Biblical / Tiberian base with learner digraphs (sh/kh/ts/ʾ/ʿ),
 * vocal shewa as ĕ — NOT Modern Israeli.
 * Divine name → YHWH (never invent a vocalization).
 */
import { createRequire } from "module";
const require = createRequire(import.meta.url);
const { SBL } = require("hebrew-transliteration");

export function createSoferSblLearnerSchema() {
  return new SBL({
    VOCAL_SHEVA: "ĕ",
    HATAF_SEGOL: "ĕ",
    HATAF_PATAH: "ă",
    HATAF_QAMATS: "ŏ",
    HIRIQ: "i",
    TSERE: "ē",
    SEGOL: "e",
    PATAH: "a",
    QAMATS: "ā",
    QAMATS_QATAN: "o",
    HOLAM: "ō",
    HOLAM_HASER: "ō",
    QUBUTS: "u",
    SHUREQ: "û",
    HOLAM_VAV: "ô",
    HIRIQ_YOD: "î",
    TSERE_YOD: "ê",
    SEGOL_YOD: "ê",
    QAMATS_HE: "â",
    FURTIVE_PATAH: "a",

    ALEF: "ʾ",
    AYIN: "ʿ",
    SHIN: "sh",
    SIN: "s",
    TSADI: "ts",
    FINAL_TSADI: "ts",
    HET: "ḥ",
    TET: "ṭ",
    QOF: "q",
    VAV: "w",
    YOD: "y",

    BET: "v",
    BET_DAGESH: "b",
    GIMEL: "g",
    GIMEL_DAGESH: "g",
    DALET: "d",
    DALET_DAGESH: "d",
    KAF: "kh",
    KAF_DAGESH: "k",
    FINAL_KAF: "kh",
    PE: "f",
    PE_DAGESH: "p",
    FINAL_PE: "f",
    TAV: "th",
    TAV_DAGESH: "t",

    HE: "h",
    ZAYIN: "z",
    LAMED: "l",
    MEM: "m",
    FINAL_MEM: "m",
    NUN: "n",
    FINAL_NUN: "n",
    SAMEKH: "s",
    RESH: "r",

    DIVINE_NAME: "YHWH",
    DAGESH_CHAZAQ: true,
    MAQAF: "-",
    PASEQ: "",
    SOF_PASUQ: "",

    longVowels: true,
    qametsQatan: true,
    shevaAfterMeteg: true,
    shevaWithMeteg: false,
    sqnmlvy: true,
    wawShureq: true,
    article: true,
    allowNoNiqqud: true,
    strict: false,
    holemHaser: "remove",
  });
}
