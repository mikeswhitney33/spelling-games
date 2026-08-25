import type { GradeBand } from "./words";

/**
 * A word + ending task for the Ending Machine.
 * `word` is the finished answer and `hint` is the rule, matching the RoundItem
 * shape the shared round engine expects.
 */
export interface EndingTask {
  base: string;
  suffix: string;
  /** The correctly spelled combined word. */
  word: string;
  /** The spelling rule, shown as feedback after each attempt. */
  hint: string;
  /** Alternate spellings that are also correct (e.g. British variants). */
  also?: string[];
}

export const ENDING_TASKS: Record<GradeBand, EndingTask[]> = {
  "k-1": [
    { base: "cat", suffix: "s", word: "cats", hint: "Most words just add s." },
    { base: "dog", suffix: "s", word: "dogs", hint: "Most words just add s." },
    { base: "hat", suffix: "s", word: "hats", hint: "Most words just add s." },
    { base: "cup", suffix: "s", word: "cups", hint: "Most words just add s." },
    { base: "hen", suffix: "s", word: "hens", hint: "Most words just add s." },
    { base: "pig", suffix: "s", word: "pigs", hint: "Most words just add s." },
    { base: "sing", suffix: "s", word: "sings", hint: "Most words just add s." },
    { base: "box", suffix: "es", word: "boxes", hint: "Words that end in x add es." },
    { base: "fox", suffix: "es", word: "foxes", hint: "Words that end in x add es." },
    { base: "bus", suffix: "es", word: "buses", hint: "Words that end in s add es." },
    { base: "dish", suffix: "es", word: "dishes", hint: "Words that end in sh add es." },
    { base: "wish", suffix: "es", word: "wishes", hint: "Words that end in sh add es." },
    { base: "jump", suffix: "ing", word: "jumping", hint: "Just add ing — no changes needed." },
    { base: "play", suffix: "ing", word: "playing", hint: "Just add ing — no changes needed." },
    { base: "eat", suffix: "ing", word: "eating", hint: "Just add ing — no changes needed." },
    { base: "go", suffix: "ing", word: "going", hint: "Just add ing — no changes needed." },
  ],
  "2-3": [
    { base: "baby", suffix: "es", word: "babies", hint: "Change the y to i, then add es." },
    { base: "puppy", suffix: "es", word: "puppies", hint: "Change the y to i, then add es." },
    { base: "story", suffix: "es", word: "stories", hint: "Change the y to i, then add es." },
    { base: "party", suffix: "es", word: "parties", hint: "Change the y to i, then add es." },
    { base: "boy", suffix: "s", word: "boys", hint: "A vowel before the y? Just add s." },
    { base: "day", suffix: "s", word: "days", hint: "A vowel before the y? Just add s." },
    { base: "key", suffix: "s", word: "keys", hint: "A vowel before the y? Just add s." },
    { base: "monkey", suffix: "s", word: "monkeys", hint: "A vowel before the y? Just add s." },
    { base: "run", suffix: "ing", word: "running", hint: "Short vowel, one consonant: double it before ing." },
    { base: "hop", suffix: "ing", word: "hopping", hint: "Short vowel, one consonant: double it before ing." },
    { base: "sit", suffix: "ing", word: "sitting", hint: "Short vowel, one consonant: double it before ing." },
    { base: "swim", suffix: "ing", word: "swimming", hint: "Short vowel, one consonant: double it before ing." },
    { base: "stop", suffix: "ed", word: "stopped", hint: "Double the last letter before ed." },
    { base: "clap", suffix: "ed", word: "clapped", hint: "Double the last letter before ed." },
    { base: "make", suffix: "ing", word: "making", hint: "Drop the silent e before ing." },
    { base: "ride", suffix: "ing", word: "riding", hint: "Drop the silent e before ing." },
  ],
  "4-5": [
    { base: "carry", suffix: "ed", word: "carried", hint: "Change the y to i before ed." },
    { base: "hurry", suffix: "ed", word: "hurried", hint: "Change the y to i before ed." },
    { base: "study", suffix: "ed", word: "studied", hint: "Change the y to i before ed." },
    { base: "cry", suffix: "ed", word: "cried", hint: "Change the y to i before ed." },
    { base: "city", suffix: "es", word: "cities", hint: "Change the y to i, then add es." },
    { base: "family", suffix: "es", word: "families", hint: "Change the y to i, then add es." },
    { base: "berry", suffix: "es", word: "berries", hint: "Change the y to i, then add es." },
    { base: "leaf", suffix: "es", word: "leaves", hint: "Change the f to v, then add es." },
    { base: "wolf", suffix: "es", word: "wolves", hint: "Change the f to v, then add es." },
    { base: "knife", suffix: "es", word: "knives", hint: "Change the fe to v, then add es." },
    { base: "shelf", suffix: "es", word: "shelves", hint: "Change the f to v, then add es." },
    { base: "begin", suffix: "ing", word: "beginning", hint: "The stress is at the end: double the n before ing." },
    { base: "forget", suffix: "ing", word: "forgetting", hint: "The stress is at the end: double the t before ing." },
    { base: "big", suffix: "er", word: "bigger", hint: "Double the last letter before er." },
    { base: "hot", suffix: "est", word: "hottest", hint: "Double the last letter before est." },
    { base: "write", suffix: "ing", word: "writing", hint: "Drop the silent e before ing." },
  ],
  "6-plus": [
    { base: "occur", suffix: "ed", word: "occurred", hint: "Stressed last syllable: double the r before ed." },
    { base: "refer", suffix: "ed", word: "referred", hint: "Stressed last syllable: double the r before ed." },
    { base: "commit", suffix: "ed", word: "committed", hint: "Stressed last syllable: double the t before ed." },
    { base: "control", suffix: "ed", word: "controlled", hint: "Stressed last syllable: double the l before ed." },
    { base: "panic", suffix: "ed", word: "panicked", hint: "Add a k so the c stays hard." },
    { base: "picnic", suffix: "ing", word: "picnicking", hint: "Add a k so the c stays hard." },
    { base: "happy", suffix: "ly", word: "happily", hint: "Change the y to i before ly." },
    { base: "easy", suffix: "ly", word: "easily", hint: "Change the y to i before ly." },
    { base: "terrible", suffix: "y", word: "terribly", hint: "Words ending in le drop the e: just add y." },
    { base: "sincere", suffix: "ly", word: "sincerely", hint: "Keep the e — just add ly." },
    { base: "immediate", suffix: "ly", word: "immediately", hint: "Keep the e — just add ly." },
    { base: "argue", suffix: "ment", word: "argument", hint: "Argument drops the e — a famous exception!" },
    { base: "judge", suffix: "ment", word: "judgment", also: ["judgement"], hint: "American English drops the e — British English keeps it. Both are real!" },
    { base: "achieve", suffix: "ment", word: "achievement", hint: "Keep the e — just add ment." },
    { base: "notice", suffix: "able", word: "noticeable", hint: "Keep the e so the c stays soft." },
    { base: "fame", suffix: "ous", word: "famous", hint: "Drop the e before ous." },
  ],
};
