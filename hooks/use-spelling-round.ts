"use client";

import { useCallback, useEffect, useState } from "react";
import { pickN, ROUND_LENGTH } from "@/lib/game-utils";
import { WORD_LISTS, type GradeBand, type WordEntry } from "@/lib/words";

export interface RoundState {
  words: WordEntry[];
  index: number;
  score: number;
  streak: number;
  bestStreak: number;
  results: boolean[];
  phase: "playing" | "done";
}

export function useSpellingRound(grade: GradeBand) {
  const [roundId, setRoundId] = useState(0);
  // Round is built client-side only (random picks), so it starts null to keep
  // the prerendered HTML and the first client render identical.
  const [state, setState] = useState<RoundState | null>(null);

  useEffect(() => {
    setState({
      words: pickN(WORD_LISTS[grade], ROUND_LENGTH),
      index: 0,
      score: 0,
      streak: 0,
      bestStreak: 0,
      results: [],
      phase: "playing",
    });
  }, [grade, roundId]);

  const record = useCallback((correct: boolean) => {
    setState((s) => {
      if (!s) return s;
      const streak = correct ? s.streak + 1 : 0;
      return {
        ...s,
        score: s.score + (correct ? 1 : 0),
        streak,
        bestStreak: Math.max(s.bestStreak, streak),
        results: [...s.results, correct],
      };
    });
  }, []);

  const advance = useCallback(() => {
    setState((s) => {
      if (!s) return s;
      return s.index + 1 >= s.words.length
        ? { ...s, phase: "done" }
        : { ...s, index: s.index + 1 };
    });
  }, []);

  const restart = useCallback(() => setRoundId((n) => n + 1), []);

  return { state, record, advance, restart, roundId };
}

const GRADE_STORAGE_KEY = "spell-it-grade";

export function useGrade(): [GradeBand, (grade: GradeBand) => void] {
  const [grade, setGrade] = useState<GradeBand>("2-3");

  useEffect(() => {
    const saved = window.localStorage.getItem(GRADE_STORAGE_KEY);
    if (saved && saved in WORD_LISTS) setGrade(saved as GradeBand);
  }, []);

  const update = useCallback((next: GradeBand) => {
    setGrade(next);
    try {
      window.localStorage.setItem(GRADE_STORAGE_KEY, next);
    } catch {
      // Private browsing — grade just won't persist.
    }
  }, []);

  return [grade, update];
}

export type { WordEntry };
