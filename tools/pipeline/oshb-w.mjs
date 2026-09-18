/**
 * OSHB <w> helpers — flatten nested <seg> (e.g. type="x-large") so surface
 * forms keep all text nodes under <w>. Used by pack build + hard-fail gates.
 *
 * Also includes "orphan qere" adaptations: <rdg type="x-qere"> with no
 * preceding x-ketiv (L/BHS do not mark ketiv). Those qere-only words must
 * still appear in the token stream to match OSHB / Sofer HIGH.
 */

/** Concatenate text nodes under a <w> inner HTML; drop seg/other tags. */
export function flattenWInner(inner) {
  if (!inner) return "";
  return String(inner)
    .replace(/<\/?seg\b[^>]*>/gi, "")
    .replace(/<[^>]+>/g, "");
}

/** Hebrew letters only (for catchWord / surface compare). */
export function consonantsOnly(s) {
  return [...String(s)].filter((ch) => /[\u05D0-\u05EA]/.test(ch)).join("");
}

function attrsField(attrs, name) {
  return (String(attrs).match(new RegExp(`${name}="([^"]*)"`)) || [])[1] || "";
}

function isKetivPairedNote(body, noteIndex) {
  const before = body.slice(Math.max(0, noteIndex - 400), noteIndex);
  return /<w type="x-ketiv"[^>]*>[\s\S]*?<\/w>\s*$/.test(before);
}

/**
 * Extract non-ketiv/non-qere <w> tokens from a verse body, flattening nested seg.
 * Returns [{ order, heRaw, lemma, morph, id, ketiv, qere }, ...] in document order.
 *
 * Orphan qere (no x-ketiv partner): inserted as normal tokens at note order.
 * catchWord + qere: replaces the matching preceding <w> with qere surface and
 * stores the catchWord form as ketiv (Jer.48.44 pattern).
 */
export function extractWTokens(body) {
  const tokens = [];

  const ketivRe =
    /<w type="x-ketiv"[^>]*lemma="([^"]*)"[^>]*morph="([^"]*)"[^>]*id="([^"]*)"[^>]*>([\s\S]*?)<\/w>\s*<note type="variant">[\s\S]*?<rdg type="x-qere"><w([^>]*)>([\s\S]*?)<\/w>/g;
  let km;
  while ((km = ketivRe.exec(body))) {
    const qereAttrs = km[5];
    const qereLemma = attrsField(qereAttrs, "lemma") || km[1];
    const qereMorph = attrsField(qereAttrs, "morph") || km[2];
    const qereId = attrsField(qereAttrs, "id") || km[3];
    tokens.push({
      order: km.index,
      heRaw: flattenWInner(km[6]),
      lemma: qereLemma,
      morph: qereMorph,
      id: qereId,
      ketiv: flattenWInner(km[4]).replace(/\//g, ""),
      qere: true,
    });
  }

  const wRe = /<w(?![^>]*type="x-ketiv")([^>]*)>([\s\S]*?)<\/w>/g;
  let wm;
  while ((wm = wRe.exec(body))) {
    if (/type="x-qere"/.test(wm[0]) || /type="x-ketiv"/.test(wm[0])) continue;
    const before = body.slice(Math.max(0, wm.index - 80), wm.index);
    if (/x-qere[^>]*>\s*$/.test(before) || /<rdg type="x-qere">\s*$/.test(before)) {
      continue;
    }
    const attrs = wm[1];
    tokens.push({
      order: wm.index,
      heRaw: flattenWInner(wm[2]),
      lemma: attrsField(attrs, "lemma"),
      morph: attrsField(attrs, "morph"),
      id: attrsField(attrs, "id"),
      ketiv: null,
      qere: false,
    });
  }

  // Orphan qere / catchWord adaptations (no x-ketiv partner)
  const noteRe = /<note type="variant">([\s\S]*?)<\/note>/g;
  let nm;
  while ((nm = noteRe.exec(body))) {
    const noteInner = nm[1];
    if (!/<rdg type="x-qere">/i.test(noteInner)) continue;
    if (isKetivPairedNote(body, nm.index)) continue;

    const qereWm = /<rdg type="x-qere">\s*<w([^>]*)>([\s\S]*?)<\/w>/i.exec(noteInner);
    if (!qereWm) continue;
    const qereAttrs = qereWm[1];
    const heRaw = flattenWInner(qereWm[2]);
    const lemma = attrsField(qereAttrs, "lemma");
    const morph = attrsField(qereAttrs, "morph");
    const id = attrsField(qereAttrs, "id");
    const catchWord = (noteInner.match(/<catchWord>([^<]*)<\/catchWord>/i) || [])[1];

    if (catchWord) {
      const want = consonantsOnly(catchWord);
      // Prefer token immediately preceding this note with matching consonants
      let targetIdx = -1;
      for (let i = tokens.length - 1; i >= 0; i--) {
        if (tokens[i].order >= nm.index) continue;
        if (consonantsOnly(tokens[i].heRaw) === want) {
          targetIdx = i;
          break;
        }
      }
      if (targetIdx >= 0) {
        const prev = tokens[targetIdx];
        tokens[targetIdx] = {
          order: prev.order,
          heRaw,
          lemma: lemma || prev.lemma,
          morph: morph || prev.morph,
          id: id || prev.id,
          ketiv: flattenWInner(prev.heRaw).replace(/\//g, ""),
          qere: true,
        };
      } else {
        tokens.push({
          order: nm.index,
          heRaw,
          lemma,
          morph,
          id,
          ketiv: catchWord.replace(/\//g, ""),
          qere: true,
        });
      }
    } else {
      // Qere-only insertion (L/BHS unmarked) — emit as normal surface token
      tokens.push({
        order: nm.index,
        heRaw,
        lemma,
        morph,
        id,
        ketiv: null,
        qere: false,
      });
    }
  }

  tokens.sort((a, b) => a.order - b.order);
  const seen = new Set();
  const uniq = [];
  for (const t of tokens) {
    const key = t.id || `${t.order}`;
    if (seen.has(key)) continue;
    seen.add(key);
    uniq.push(t);
  }
  return uniq;
}

/** Count flattened OSHB <w> tokens in a verse body (same rules as pack emit). */
export function countOshbWTokens(body) {
  return extractWTokens(body).length;
}

/**
 * Pull one verse body from OSHB book XML by osisID.
 * Returns null if not found.
 */
export function verseBodyFromOshbXml(xml, osisId) {
  const book = osisId.split(".")[0];
  const ch = osisId.split(".")[1];
  const chRe = new RegExp(
    `<chapter osisID="${book}\\.${ch}">([\\s\\S]*?)</chapter>`
  );
  const chm = chRe.exec(xml);
  if (!chm) return null;
  const verseRe = new RegExp(
    `<verse osisID="${osisId.replace(/\./g, "\\.")}">([\\s\\S]*?)</verse>`
  );
  const vm = verseRe.exec(chm[1]);
  return vm ? vm[1] : null;
}
