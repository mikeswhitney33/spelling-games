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
 * Small guard list of common English words so single-edit misspellings of easy
 * words (cat → act, cut) never show up as "fake" options in Spot the Word.
 * Not exhaustive — just the likely collisions for short words.
 */
const REAL_WORDS = new Set(
  (
    "act ant art ate bat bet bit bad bag ban bar bin bun bus but bud cab can cap car " +
    "cob cod cop cot cub cut dab dam den dig dim din dip dot dug fan fat fig fin fit " +
    "fog fun fur gap gas get got gum gun gut ham has hem hid him hip hit hog hot hub " +
    "hug hum hut jab jet jig job jog jot jug keg kin kit lab lag lap led let lid lip " +
    "lit lot mad man mat men met mob mop mug nab nag net nip nod not nut pad pal pat " +
    "peg pen pet pin pit pod pop pot pun pup put rag ram ran rap rat rib rid rig rim " +
    "rip rob rod rot rub rug rum rut sad sag sap sat set sin sip sir sit sob son sub " +
    "sum tab tag tan tap tar tin tip ton tot tub tug van vat vet wag web wed wig wit " +
    "won yak yam yet zap net pit ten den one two three four five nine here hare hear " +
    "wear were three there their form from trail trial quite quiet"
  ).split(/\s+/),
);

/** Generate plausible misspellings of a word using common error patterns. */
export function makeMisspellings(
  word: string,
  count: number,
  rng: Rng = Math.random,
  avoid?: ReadonlySet<string>,
): string[] {
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
  const pool = [...candidates].filter(
    (c) => c.length >= 2 && !REAL_WORDS.has(c) && !avoid?.has(c),
  );
  return pickN(pool, count, rng);
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
