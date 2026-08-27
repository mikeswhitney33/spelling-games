import { REAL_WORD_GUARD } from "./real-word-guard";

export type Rng = () => number;

export function mulberry32(seed: number): Rng {
  let a = seed >>> 0;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function shuffle<T>(items: readonly T[], rng: Rng = Math.random): T[] {
  const out = [...items];
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1));
    [out[i], out[j]] = [out[j], out[i]];
  }
  return out;
}

export function pickN<T>(items: readonly T[], n: number, rng: Rng = Math.random): T[] {
  return shuffle(items, rng).slice(0, n);
}

/** Shuffle a word's letters, guaranteed different from the original when possible. */
export function scrambleWord(word: string, rng: Rng = Math.random): string {
  const letters = word.split("");
  for (let attempt = 0; attempt < 20; attempt++) {
    const mixed = shuffle(letters, rng).join("");
    if (mixed !== word) return mixed;
  }
  return letters.reverse().join("");
}

const VOWEL_SWAPS: Record<string, string[]> = {
  a: ["e", "u"],
  e: ["a", "i"],
  i: ["e", "y"],
  o: ["u", "a"],
  u: ["o", "e"],
  y: ["i", "e"],
};

const PHONETIC_SWAPS: Record<string, string[]> = {
  c: ["k", "s"],
  k: ["c"],
  s: ["z", "c"],
  z: ["s"],
  f: ["v"],
  v: ["f"],
  g: ["j"],
  j: ["g"],
};

/**
 * Every real-word collision the candidate generator can produce for the
 * current word lists, precomputed from the system dictionary by
 * scripts/generate-real-word-guard.ts. Regenerate with
 * `npm run generate:guard` after editing lib/words.ts or the edit rules.
 */
const REAL_WORDS = new Set(REAL_WORD_GUARD);

/** All single-edit misspelling candidates for a word, before filtering. */
export function misspellingCandidates(word: string): string[] {
  const candidates = new Set<string>();

  // Transpose adjacent letters
  for (let i = 0; i < word.length - 1; i++) {
    if (word[i] === word[i + 1]) continue;
    candidates.add(word.slice(0, i) + word[i + 1] + word[i] + word.slice(i + 2));
  }
  // Swap a vowel for a near-sounding one
  for (let i = 0; i < word.length; i++) {
    const swaps = VOWEL_SWAPS[word[i]];
    if (!swaps) continue;
    for (const s of swaps) {
      candidates.add(word.slice(0, i) + s + word.slice(i + 1));
    }
  }
  // Swap a consonant for a near-sounding one (cat → kat)
  for (let i = 0; i < word.length; i++) {
    const swaps = PHONETIC_SWAPS[word[i]];
    if (!swaps) continue;
    for (const s of swaps) {
      candidates.add(word.slice(0, i) + s + word.slice(i + 1));
    }
  }
  // Undouble a doubled letter
  for (let i = 0; i < word.length - 1; i++) {
    if (word[i] === word[i + 1]) {
      candidates.add(word.slice(0, i) + word.slice(i + 1));
    }
  }
  // Double a letter (cat → catt, caat)
  for (let i = 0; i < word.length; i++) {
    candidates.add(word.slice(0, i + 1) + word[i] + word.slice(i + 1));
  }
  // Drop a silent-ish letter
  for (let i = 1; i < word.length - 1; i++) {
    if ("aeiouhgk".includes(word[i])) {
      candidates.add(word.slice(0, i) + word.slice(i + 1));
    }
  }

  candidates.delete(word);
  return [...candidates].filter((c) => c.length >= 2);
}

/** Generate plausible misspellings of a word using common error patterns. */
export function makeMisspellings(
  word: string,
  count: number,
  rng: Rng = Math.random,
  avoid?: ReadonlySet<string>,
): string[] {
  const pool = misspellingCandidates(word).filter(
    (c) => !REAL_WORDS.has(c.toLowerCase()) && !avoid?.has(c.toLowerCase()),
  );
  return pickN(pool, count, rng);
}

/** Copy the model word's leading capital (if any) onto text. */
export function matchCase(model: string, text: string): string {
  return model[0] === model[0].toUpperCase() && model[0] !== model[0].toLowerCase()
    ? text[0].toUpperCase() + text.slice(1)
    : text;
}

/** Choose which letter positions to blank out for Missing Letters. */
export function pickBlankPositions(word: string, blanks: number, rng: Rng = Math.random): number[] {
  const positions = Array.from({ length: word.length }, (_, i) => i);
  return pickN(positions, Math.min(blanks, word.length), rng).sort((a, b) => a - b);
}

/** Speak a word or sentence aloud using the browser's speech synthesis. */
export function speak(text: string, rate = 0.8): boolean {
  if (typeof window === "undefined" || !("speechSynthesis" in window)) return false;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.rate = rate;
  utterance.lang = "en-US";
  const voice = window.speechSynthesis
    .getVoices()
    .find((v) => v.lang.startsWith("en") && v.localService);
  if (voice) utterance.voice = voice;
  window.speechSynthesis.speak(utterance);
  return true;
}

export const ROUND_LENGTH = 10;

export function starsForScore(score: number, total: number): number {
  const ratio = score / total;
  if (ratio >= 0.9) return 3;
  if (ratio >= 0.7) return 2;
  if (ratio >= 0.5) return 1;
  return 0;
}
