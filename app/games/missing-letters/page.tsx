import type { Metadata } from "next";
import { MissingLettersGame } from "@/components/games/missing-letters-game";

export const metadata: Metadata = {
  title: "Missing Letters — Spell It!",
  description:
    "Fill in the missing letters to finish each word. A spelling game for kids in grades K–6+.",
};

export default function MissingLettersPage() {
  return <MissingLettersGame />;
}
