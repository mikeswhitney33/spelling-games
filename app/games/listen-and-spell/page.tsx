import type { Metadata } from "next";
import { ListenAndSpellGame } from "@/components/games/listen-and-spell-game";

export const metadata: Metadata = {
  title: "Listen & Spell — Spell It!",
  description:
    "Hear a word out loud, then type its spelling. A listening spelling game for kids in grades K–6+.",
};

export default function ListenAndSpellPage() {
  return <ListenAndSpellGame />;
}
