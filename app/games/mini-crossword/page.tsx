import type { Metadata } from "next";
import { MiniCrosswordGame } from "@/components/games/mini-crossword-game";

export const metadata: Metadata = {
  title: "Mini Crossword — Spell It!",
  description:
    "A five-word crossword built from grade-level spelling words — use the clues to fill the grid. For grades K–6+.",
};

export default function MiniCrosswordPage() {
  return <MiniCrosswordGame />;
}
