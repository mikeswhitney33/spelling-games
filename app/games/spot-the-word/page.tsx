import type { Metadata } from "next";
import { SpotTheWordGame } from "@/components/games/spot-the-word-game";

export const metadata: Metadata = {
  title: "Spot the Word — Spell It!",
  description:
    "Pick the one correct spelling out of four look-alikes. A proofreading spelling game for kids in grades K–6+.",
};

export default function SpotTheWordPage() {
  return <SpotTheWordGame />;
}
