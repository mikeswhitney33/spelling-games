export type GameColor = "coral" | "sun" | "leaf" | "sky";

export interface GameInfo {
  slug: string;
  title: string;
  tagline: string;
  description: string;
  color: GameColor;
  skills: string[];
}

export const GAMES: GameInfo[] = [
  {
    slug: "word-scramble",
    title: "Word Scramble",
    tagline: "Untangle the tiles",
    description:
      "The letters got all mixed up! Tap the tiles to put the word back together.",
    color: "coral",
    skills: ["letter order", "word shapes"],
  },
  {
    slug: "missing-letters",
    title: "Missing Letters",
    tagline: "Fill in the gaps",
    description:
      "Some letters ran away. Pick the right ones to finish the word.",
    color: "sun",
    skills: ["tricky letters", "spelling patterns"],
  },
  {
    slug: "listen-and-spell",
    title: "Listen & Spell",
    tagline: "Hear it, spell it",
    description:
      "Press play, listen closely, and type the whole word all by yourself.",
    color: "sky",
    skills: ["sounding out", "memory"],
  },
  {
    slug: "spot-the-word",
    title: "Spot the Word",
    tagline: "Find the real one",
    description:
      "One spelling is right and three are fakes. Can you spot the real word?",
    color: "leaf",
    skills: ["proofreading", "sharp eyes"],
  },
];

export const COLOR_STYLES: Record<
  GameColor,
  { soft: string; solid: string; text: string; borderT: string }
> = {
  coral: {
    soft: "bg-coral-soft",
    solid: "bg-coral",
    text: "text-coral",
    borderT: "border-t-coral",
  },
  sun: {
    soft: "bg-sun-soft",
    solid: "bg-sun",
    text: "text-sun",
    borderT: "border-t-sun",
  },
  leaf: {
    soft: "bg-leaf-soft",
    solid: "bg-leaf",
    text: "text-leaf",
    borderT: "border-t-leaf",
  },
  sky: {
    soft: "bg-sky-soft",
    solid: "bg-sky",
    text: "text-sky",
    borderT: "border-t-sky",
  },
};
