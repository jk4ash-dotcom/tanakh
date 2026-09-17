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

export const NEVIIM = [
  "Josh", "Judg", "1Sam", "2Sam", "1Kgs", "2Kgs",
  "Isa", "Jer", "Ezek",
  "Hos", "Joel", "Amos", "Obad", "Jonah", "Mic",
  "Nah", "Hab", "Zeph", "Hag", "Zech", "Mal",
];

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
  Josh: {
    osis: "Josh",
    title: "Joshua",
    division: "Nevi'im",
    oshbFile: "Josh.xml",
    jps: { num: "007", code: "JOS", titleHints: [/^The Book of Joshua/i, /^Joshua\.?$/i] },
    aramaic: false,
  },
  Judg: {
    osis: "Judg",
    title: "Judges",
    division: "Nevi'im",
    oshbFile: "Judg.xml",
    jps: { num: "008", code: "JDG", titleHints: [/^The Book of Judges/i, /^Judges\.?$/i] },
    aramaic: false,
  },
  "1Sam": {
    osis: "1Sam",
    title: "1 Samuel",
    division: "Nevi'im",
    oshbFile: "1Sam.xml",
    jps: {
      num: "010",
      code: "1SA",
      titleHints: [/^The First Book of Samuel/i, /^1 Samuel\.?$/i],
    },
    aramaic: false,
  },
  "2Sam": {
    osis: "2Sam",
    title: "2 Samuel",
    division: "Nevi'im",
    oshbFile: "2Sam.xml",
    jps: {
      num: "011",
      code: "2SA",
      titleHints: [/^The Second Book of Samuel/i, /^2 Samuel\.?$/i],
    },
    aramaic: false,
  },
  "1Kgs": {
    osis: "1Kgs",
    title: "1 Kings",
    division: "Nevi'im",
    oshbFile: "1Kgs.xml",
    jps: {
      num: "012",
      code: "1KI",
      titleHints: [/^The First Book of the Kings/i, /^1 Kings\.?$/i],
    },
    aramaic: false,
  },
  "2Kgs": {
    osis: "2Kgs",
    title: "2 Kings",
    division: "Nevi'im",
    oshbFile: "2Kgs.xml",
    jps: {
      num: "013",
      code: "2KI",
      titleHints: [/^The Second Book of the Kings/i, /^2 Kings\.?$/i],
    },
    aramaic: false,
  },
  Isa: {
    osis: "Isa",
    title: "Isaiah",
    division: "Nevi'im",
    oshbFile: "Isa.xml",
    jps: {
      num: "024",
      code: "ISA",
      titleHints: [/^The Book of the Prophet Isaiah/i, /^Isaiah\.?$/i],
    },
    aramaic: false,
  },
  Jer: {
    osis: "Jer",
    title: "Jeremiah",
    division: "Nevi'im",
    oshbFile: "Jer.xml",
    jps: {
      num: "025",
      code: "JER",
      titleHints: [/^The Book of the Prophet Jeremiah/i, /^Jeremiah\.?$/i],
    },
    aramaic: false,
  },
  Ezek: {
    osis: "Ezek",
    title: "Ezekiel",
    division: "Nevi'im",
    oshbFile: "Ezek.xml",
    jps: {
      num: "027",
      code: "EZK",
      titleHints: [/^The Book of the Prophet Ezekiel/i, /^Ezekiel\.?$/i],
    },
    aramaic: false,
  },
  Hos: {
    osis: "Hos",
    title: "Hosea",
    division: "Nevi'im",
    oshbFile: "Hos.xml",
    jps: { num: "029", code: "HOS", titleHints: [/^Hosea\.?$/i] },
    aramaic: false,
  },
  Joel: {
    osis: "Joel",
    title: "Joel",
    division: "Nevi'im",
    oshbFile: "Joel.xml",
    jps: { num: "030", code: "JOL", titleHints: [/^Joel\.?$/i] },
    aramaic: false,
  },
  Amos: {
    osis: "Amos",
    title: "Amos",
    division: "Nevi'im",
    oshbFile: "Amos.xml",
    jps: { num: "031", code: "AMO", titleHints: [/^Amos\.?$/i] },
    aramaic: false,
  },
  Obad: {
    osis: "Obad",
    title: "Obadiah",
    division: "Nevi'im",
    oshbFile: "Obad.xml",
    jps: { num: "032", code: "OBA", titleHints: [/^Obadiah\.?$/i] },
    aramaic: false,
  },
  Jonah: {
    osis: "Jonah",
    title: "Jonah",
    division: "Nevi'im",
    oshbFile: "Jonah.xml",
    jps: { num: "033", code: "JON", titleHints: [/^Jonah\.?$/i] },
    aramaic: false,
  },
  Mic: {
    osis: "Mic",
    title: "Micah",
    division: "Nevi'im",
    oshbFile: "Mic.xml",
    jps: { num: "034", code: "MIC", titleHints: [/^Micah\.?$/i] },
    aramaic: false,
  },
  Nah: {
    osis: "Nah",
    title: "Nahum",
    division: "Nevi'im",
    oshbFile: "Nah.xml",
    jps: { num: "035", code: "NAM", titleHints: [/^Nahum\.?$/i] },
    aramaic: false,
  },
  Hab: {
    osis: "Hab",
    title: "Habakkuk",
    division: "Nevi'im",
    oshbFile: "Hab.xml",
    jps: { num: "036", code: "HAB", titleHints: [/^Habakkuk\.?$/i] },
    aramaic: false,
  },
  Zeph: {
    osis: "Zeph",
    title: "Zephaniah",
    division: "Nevi'im",
    oshbFile: "Zeph.xml",
    jps: { num: "037", code: "ZEP", titleHints: [/^Zephaniah\.?$/i] },
    aramaic: false,
  },
  Hag: {
    osis: "Hag",
    title: "Haggai",
    division: "Nevi'im",
    oshbFile: "Hag.xml",
    jps: { num: "038", code: "HAG", titleHints: [/^Haggai\.?$/i] },
    aramaic: false,
  },
  Zech: {
    osis: "Zech",
    title: "Zechariah",
    division: "Nevi'im",
    oshbFile: "Zech.xml",
    jps: { num: "039", code: "ZEC", titleHints: [/^Zechariah\.?$/i] },
    aramaic: false,
  },
  Mal: {
    osis: "Mal",
    title: "Malachi",
    division: "Nevi'im",
    oshbFile: "Mal.xml",
    jps: { num: "040", code: "MAL", titleHints: [/^Malachi\.?$/i] },
    aramaic: false,
  },
};

export function jewishSortKey(osis) {
  const i = JEWISH_TANAKH_ORDER.indexOf(osis);
  return i < 0 ? 9999 : i;
}
