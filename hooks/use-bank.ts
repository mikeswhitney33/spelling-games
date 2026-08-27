"use client";

import { useCallback, useEffect, useState } from "react";

import {
  BUILT_IN_BANKS,
  DEFAULT_BANK_ID,
  type BankEntry,
  type WordBank,
} from "@/lib/banks";

const CUSTOM_KEY = "spell-it-custom-banks";
const ACTIVE_KEY = "spell-it-active-bank";

interface StoredCustomBank {
  id: string;
  name: string;
  entries: BankEntry[];
}

export function loadCustomBanks(): WordBank[] {
  try {
    const raw = window.localStorage.getItem(CUSTOM_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as StoredCustomBank[];
    return parsed
      .filter((b) => b && typeof b.id === "string" && Array.isArray(b.entries))
      .map((b) => ({
        id: b.id,
        name: b.name || "My list",
        blurb: `Your custom list · ${b.entries.length} words`,
        builtIn: false,
        entries: b.entries.filter((e) => typeof e?.word === "string" && e.word),
      }));
  } catch {
    return [];
  }
}

export function saveCustomBanks(banks: WordBank[]) {
  try {
    const stored: StoredCustomBank[] = banks
      .filter((b) => !b.builtIn)
      .map((b) => ({ id: b.id, name: b.name, entries: b.entries }));
    window.localStorage.setItem(CUSTOM_KEY, JSON.stringify(stored));
  } catch {
    // Private browsing — lists just won't persist.
  }
}

export function setActiveBankId(id: string) {
  try {
    window.localStorage.setItem(ACTIVE_KEY, id);
  } catch {
    // Ignore.
  }
}

/**
 * The active word bank plus everything selectable. Starts on the built-in
 * default for hydration safety, then loads the saved choice and custom banks.
 */
export function useWordBank() {
  const [banks, setBanks] = useState<WordBank[]>(BUILT_IN_BANKS);
  const [activeId, setActiveId] = useState(DEFAULT_BANK_ID);

  useEffect(() => {
    const all = [...BUILT_IN_BANKS, ...loadCustomBanks()];
    setBanks(all);
    const saved = window.localStorage.getItem(ACTIVE_KEY);
    if (saved && all.some((b) => b.id === saved)) setActiveId(saved);
  }, []);

  const setActive = useCallback((id: string) => {
    setActiveId(id);
    setActiveBankId(id);
  }, []);

  const bank =
    banks.find((b) => b.id === activeId) ??
    BUILT_IN_BANKS.find((b) => b.id === DEFAULT_BANK_ID)!;

  return { bank, banks, setActive };
}
