import type { Metadata } from "next";
import { MemoryMatchGame } from "@/components/games/memory-match-game";

export const metadata: Metadata = {
  title: "Memory Match — Spell It!",
  description:
    "Flip cards to match each spelling word with its meaning. A memory game for grades K–6+.",
};

export default function MemoryMatchPage() {
  return <MemoryMatchGame />;
}
