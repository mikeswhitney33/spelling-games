import { ANIMALS_NATURE, COMMONLY_MISSPELLED, SIGHT_WORDS } from "./prefilled-banks";
import { GRADE_BANDS, WORD_LISTS } from "./words";

/** A word in a bank. Hint and sentence are optional for custom words. */
export interface BankEntry {
  word: string;
  hint?: string;
  sentence?: string;
}

export interface WordBank {
  id: string;
  name: string;
  blurb: string;
  builtIn: boolean;
  entries: BankEntry[];
}

export const BUILT_IN_BANKS: WordBank[] = [
  ...GRADE_BANDS.map((band) => ({
    id: `band-${band.id}`,
    name: band.label,
    blurb: band.blurb,
    builtIn: true,
    entries: WORD_LISTS[band.id] as BankEntry[],
  })),
  {
    id: "sight-words",
    name: "Sight Words",
    blurb: "The everyday little words early readers meet constantly.",
    builtIn: true,
    entries: SIGHT_WORDS,
  },
  {
    id: "commonly-misspelled",
    name: "Commonly Misspelled",
    blurb: "The words that trip everyone up — even grown-ups.",
    builtIn: true,
    entries: COMMONLY_MISSPELLED,
  },
  {
    id: "animals-nature",
    name: "Animals & Nature",
    blurb: "Creatures and wild places kids love to spell.",
    builtIn: true,
    entries: ANIMALS_NATURE,
  },
];

export const DEFAULT_BANK_ID = "band-2-3";

/** Every built-in word, lowercase — used to filter generated misspellings
 * and to decide when the cautious fake-generation rules apply. */
export const ALL_BUILT_IN_WORDS: ReadonlySet<string> = new Set(
  BUILT_IN_BANKS.flatMap((bank) => bank.entries.map((e) => e.word.toLowerCase())),
);
