import type { Metadata } from "next";
import { BalloonPopGame } from "@/components/games/balloon-pop-game";

export const metadata: Metadata = {
  title: "Balloon Pop — Spell It!",
  description:
    "Guess letters to spell the hidden word before all the balloons pop. A kid-friendly word-guessing game for grades K–6+.",
};

export default function BalloonPopPage() {
  return <BalloonPopGame />;
}
