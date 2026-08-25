import type { Metadata } from "next";
import { FlashSpellGame } from "@/components/games/flash-spell-game";

export const metadata: Metadata = {
  title: "Flash Spell — Spell It!",
  description:
    "The word flashes on screen, then hides — spell it from memory. A look-cover-write-check spelling game for grades K–6+.",
};

export default function FlashSpellPage() {
  return <FlashSpellGame />;
}
