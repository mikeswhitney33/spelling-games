import type { Metadata } from "next";
import { DailyBeeGame } from "@/components/games/daily-bee-game";

export const metadata: Metadata = {
  title: "Daily Bee — Spell It!",
  description:
    "A fresh ten-word spelling challenge every day, mixing scrambles, missing letters, spot-the-word, and flash rounds. Keep your streak alive!",
};

export default function DailyBeePage() {
  return <DailyBeeGame />;
}
