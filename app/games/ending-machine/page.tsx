import type { Metadata } from "next";
import { EndingMachineGame } from "@/components/games/ending-machine-game";

export const metadata: Metadata = {
  title: "Ending Machine — Spell It!",
  description:
    "Add endings like -ing, -es, and -ed to words and learn the spelling rules for letters that double, drop, or change. For grades K–6+.",
};

export default function EndingMachinePage() {
  return <EndingMachineGame />;
}
