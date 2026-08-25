import type { Metadata } from "next";
import { WordScrambleGame } from "@/components/games/word-scramble-game";

export const metadata: Metadata = {
  title: "Word Scramble — Spell It!",
  description:
    "Unscramble mixed-up letter tiles to rebuild the word. A spelling game for kids in grades K–6+.",
};

export default function WordScramblePage() {
  return <WordScrambleGame />;
}
