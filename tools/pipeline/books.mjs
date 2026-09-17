/**
 * Jewish Tanakh book order + vendor path map.
 * Navigation MUST use this order — never Christian/filename sort for UI.
 * OSHB filenames are morphhb WLC (v.2.2). JPS codes are ebible engjps.
 */
export const JEWISH_TANAKH_ORDER = [
  // Torah
  "Gen", "Exod", "Lev", "Num", "Deut",
  // Nevi'im
  "Josh", "Judg", "1Sam", "2Sam", "1Kgs", "2Kgs",
  "Isa", "Jer", "Ezek",
  "Hos", "Joel", "Amos", "Obad", "Jonah", "Mic",
  "Nah", "Hab", "Zeph", "Hag", "Zech", "Mal",
  // Ketuvim (Jewish order — Daniel in Writings, not Prophets)
  "Ps", "Prov", "Job",
  "Song", "Ruth", "Lam", "Eccl", "Esth",
  "Dan", "Ezra", "Neh", "1Chr", "2Chr",
];

/** Books with Biblical Aramaic sections — do NOT silently apply Hebrew SBL-Learner. */
export const ARAMAIC_FLAG_BOOKS = new Set(["Dan", "Ezra"]);

export const TORAH = ["Gen", "Exod", "Lev", "Num", "Deut"];

export const BOOKS = {
  Gen: {
    osis: "Gen",
    title: "Genesis",
    division: "Torah",
    oshbFile: "Gen.xml",
    jps: { num: "002", code: "GEN", titleHints: [/^The First Book/i, /^Genesis\.?$/i] },
    aramaic: false,
  },
  Exod: {
    osis: "Exod",
    title: "Exodus",
    division: "Torah",
    oshbFile: "Exod.xml",
    jps: { num: "003", code: "EXO", titleHints: [/^The Second Book/i, /^Exodus\.?$/i] },
    aramaic: false,
  },
  Lev: {
    osis: "Lev",
    title: "Leviticus",
    division: "Torah",
    oshbFile: "Lev.xml",
    jps: { num: "004", code: "LEV", titleHints: [/^The Third Book/i, /^Leviticus\.?$/i] },
    aramaic: false,
  },
  Num: {
    osis: "Num",
    title: "Numbers",
    division: "Torah",
    oshbFile: "Num.xml",
    jps: { num: "005", code: "NUM", titleHints: [/^The Fourth Book/i, /^Numbers\.?$/i] },
    aramaic: false,
  },
  Deut: {
    osis: "Deut",
    title: "Deuteronomy",
    division: "Torah",
    oshbFile: "Deut.xml",
    jps: { num: "006", code: "DEU", titleHints: [/^The Fifth Book/i, /^Deuteronomy\.?$/i] },
    aramaic: false,
  },
};

export function jewishSortKey(osis) {
  const i = JEWISH_TANAKH_ORDER.indexOf(osis);
  return i < 0 ? 9999 : i;
}
