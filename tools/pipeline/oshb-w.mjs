/**
 * OSHB <w> helpers — flatten nested <seg> (e.g. type="x-large") so surface
 * forms keep all text nodes under <w>. Used by pack build + hard-fail gates.
 */

/** Concatenate text nodes under a <w> inner HTML; drop seg/other tags. */
export function flattenWInner(inner) {
  if (!inner) return "";
  return String(inner)
    .replace(/<\/?seg\b[^>]*>/gi, "")
    .replace(/<[^>]+>/g, "");
}

/**
 * Extract non-ketiv/non-qere <w> tokens from a verse body, flattening nested seg.
 * Returns [{ order, heRaw, lemma, morph, id, ketiv, qere }, ...] in document order.
 */
export function extractWTokens(body) {
  const tokens = [];

  const ketivRe =
    /<w type="x-ketiv"[^>]*lemma="([^"]*)"[^>]*morph="([^"]*)"[^>]*id="([^"]*)"[^>]*>([\s\S]*?)<\/w>\s*<note type="variant">[\s\S]*?<rdg type="x-qere"><w([^>]*)>([\s\S]*?)<\/w>/g;
  let km;
  while ((km = ketivRe.exec(body))) {
    const qereAttrs = km[5];
    const qereLemma = (qereAttrs.match(/lemma="([^"]*)"/) || [])[1] || km[1];
    const qereMorph = (qereAttrs.match(/morph="([^"]*)"/) || [])[1] || km[2];
    const qereId = (qereAttrs.match(/id="([^"]*)"/) || [])[1] || km[3];
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
      lemma: (attrs.match(/lemma="([^"]*)"/) || [])[1] || "",
      morph: (attrs.match(/morph="([^"]*)"/) || [])[1] || "",
      id: (attrs.match(/id="([^"]*)"/) || [])[1] || "",
      ketiv: null,
      qere: false,
    });
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
