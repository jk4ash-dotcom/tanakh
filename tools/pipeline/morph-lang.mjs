/**
 * OSHB morph language: the language code is the FIRST character of the morph
 * string only. Do NOT scan `/`-separated POS segments.
 *
 * Examples:
 *   "ANcmsd/Td"  → Aramaic (starts with A)
 *   "HTd/Aamsa"  → Hebrew adjective (starts with H; /A… is POS, not language)
 *   "HVqv2ms"    → Hebrew
 */
export function isAramaicMorph(morph) {
  if (!morph) return false;
  return /^A/.test(String(morph));
}
