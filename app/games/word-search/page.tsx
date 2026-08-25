import type { Metadata } from "next";
import { WordSearchGame } from "@/components/games/word-search-game";

export const metadata: Metadata = {
  title: "Word Search — Spell It!",
  description:
    "Find grade-level spelling words hidden in a letter grid — tap the first and last letter of each. For grades K–6+.",
};

export default function WordSearchPage() {
  return <WordSearchGame />;
}
