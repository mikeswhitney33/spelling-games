import type { Metadata } from "next";
import { BankManager } from "@/components/settings/bank-manager";

export const metadata: Metadata = {
  title: "Word Lists — Spell It!",
  description:
    "Manage the word lists every game draws from: pick a built-in list, or build custom lists with your own words, hints, and sentences.",
};

export default function SettingsPage() {
  return <BankManager />;
}
