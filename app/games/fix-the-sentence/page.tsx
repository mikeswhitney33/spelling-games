import type { Metadata } from "next";
import { FixTheSentenceGame } from "@/components/games/fix-the-sentence-game";

export const metadata: Metadata = {
  title: "Fix the Sentence — Spell It!",
  description:
    "One word in each sentence is spelled wrong — find it and type the fix. A proofreading spelling game for grades K–6+.",
};

export default function FixTheSentencePage() {
  return <FixTheSentenceGame />;
}
