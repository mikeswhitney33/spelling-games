"use client";

import { useCallback, useEffect, useState } from "react";
import { pickN, ROUND_LENGTH } from "@/lib/game-utils";
import { WORD_LISTS, type GradeBand, type WordEntry } from "@/lib/words";

/** The minimum shape a round item needs: the answer word and a hint/rule. */
export interface RoundItem {
  word: string;
  hint: string;
}

export interface RoundState<T extends RoundItem = RoundItem> {
  words: T[];
  index: number;
  score: number;
  streak: number;
  bestStreak: number;
  results: boolean[];
  phase: "playing" | "done";
  /** Overrides the default "You spelled X of Y words right" summary line. */
  summaryText?: string;
  /** What one round item is called in the score bar (default "Word"). */
  unit?: string;
}

/** Shared 10-item round engine over any pool of round items. */
export function useGameRound<T extends RoundItem>(pool: readonly T[]) {
  const [roundId, setRoundId] = useState(0);
  // Round is built client-side only (random picks), so it starts null to keep
  // the prerendered HTML and the first client render identical.
  const [state, setState] = useState<RoundState<T> | null>(null);

  useEffect(() => {
    setState({
      words: pickN(pool, ROUND_LENGTH),
      index: 0,
      score: 0,
      streak: 0,
      bestStreak: 0,
      results: [],
      phase: "playing",
    });
  }, [pool, roundId]);

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

/** A round drawn from the grade band's spelling word list. */
export function useSpellingRound(grade: GradeBand) {
  return useGameRound(WORD_LISTS[grade]);
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
