"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Brain } from "lucide-react";

import { BankPicker, NotEnoughWords } from "@/components/bank-picker";
import { GameFrame } from "@/components/game-frame";
import { useWordBank } from "@/hooks/use-bank";
import type { RoundState } from "@/hooks/use-spelling-round";
import type { BankEntry } from "@/lib/banks";
import { pickN, shuffle } from "@/lib/game-utils";
import { GAMES } from "@/lib/games";
import { cn } from "@/lib/utils";

const game = GAMES.find((g) => g.slug === "memory-match")!;

const PAIR_COUNT = 6;

interface MemoryCard {
  pairId: number;
  kind: "word" | "clue";
  text: string;
}

/**
 * Efficiency-based score: fewer tries, more stars — but finishing the board
 * always earns at least one star.
 *
 * Even a player who never forgets a card needs roughly 1.5 tries per pair to
 * clear the board, so the three-star window starts from there rather than from
 * a perfect run; the old thresholds sat below optimal play and handed out one
 * or two stars almost every game.
 */
function scoreForAttempts(attempts: number, total: number): number {
  if (attempts <= Math.round(total * 2.4)) return total; // 3 stars
  if (attempts <= Math.round(total * 3.4)) return Math.ceil(total * 0.75); // 2 stars
  return Math.ceil(total * 0.6); // 1 star
}

export function MemoryMatchGame() {
  const { bank, banks, setActive } = useWordBank();
  const pool = useMemo(
    () => bank.entries.filter((e) => e.hint),
    [bank],
  );
  const [roundId, setRoundId] = useState(0);
  const [entries, setEntries] = useState<BankEntry[] | null>(null);
  const [cards, setCards] = useState<MemoryCard[]>([]);
  const [faceUp, setFaceUp] = useState<number[]>([]);
  const [matched, setMatched] = useState<Set<number>>(new Set());
  const [attempts, setAttempts] = useState(0);
  const resolveTimer = useRef<number | undefined>(undefined);

  useEffect(() => {
    const picked = pickN(pool, PAIR_COUNT);
    setEntries(picked);
    setCards(
      shuffle(
        picked.flatMap((entry, pairId): MemoryCard[] => [
          { pairId, kind: "word", text: entry.word },
          { pairId, kind: "clue", text: entry.hint ?? "" },
        ]),
      ),
    );
    setFaceUp([]);
    setMatched(new Set());
    setAttempts(0);
    window.clearTimeout(resolveTimer.current);
  }, [pool, roundId]);

  useEffect(() => () => window.clearTimeout(resolveTimer.current), []);

  const round: RoundState | null = useMemo(() => {
    if (!entries) return null;
    const done = matched.size === PAIR_COUNT;
    return {
      words: entries.map((e) => ({ word: e.word, hint: e.hint })),
      index: Math.min(matched.size, PAIR_COUNT - 1),
      score: done ? scoreForAttempts(attempts, PAIR_COUNT) : matched.size,
      streak: 0,
      bestStreak: 0,
      results: entries.map(() => true),
      phase: done ? "done" : "playing",
      unit: "Pair",
      summaryText: `You matched all ${PAIR_COUNT} pairs in ${attempts} ${
        attempts === 1 ? "try" : "tries"
      }!`,
    };
  }, [entries, matched, attempts]);

  const flip = (index: number) => {
    if (
      faceUp.length === 2 ||
      faceUp.includes(index) ||
      matched.has(cards[index].pairId)
    ) {
      return;
    }
    const next = [...faceUp, index];
    setFaceUp(next);
    if (next.length < 2) return;

    setAttempts((n) => n + 1);
    const [a, b] = next.map((i) => cards[i]);
    if (a.pairId === b.pairId && a.kind !== b.kind) {
      // A match locks immediately.
      setMatched(new Set(matched).add(a.pairId));
      setFaceUp([]);
    } else {
      resolveTimer.current = window.setTimeout(() => setFaceUp([]), 1100);
    }
  };

  return (
    <GameFrame
      game={game}
      icon={<Brain className="h-7 w-7" aria-hidden="true" />}
      instructions="Flip two cards at a time to match each word with its clue."
      picker={<BankPicker bank={bank} banks={banks} onChange={setActive} />}
      notice={
        pool.length < PAIR_COUNT ? (
          <NotEnoughWords need={PAIR_COUNT} requirement="words with hints" />
        ) : undefined
      }
      round={round}
      onRestart={() => setRoundId((n) => n + 1)}
    >
      {entries && round?.phase === "playing" && (
        <div>
          <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 sm:gap-3">
            {cards.map((card, index) => {
              const isMatched = matched.has(card.pairId);
              const isUp = isMatched || faceUp.includes(index);
              return (
                <button
                  key={index}
                  type="button"
                  onClick={() => flip(index)}
                  aria-disabled={isMatched}
                  aria-label={
                    isUp
                      ? `${card.kind === "word" ? "Word" : "Clue"}: ${card.text}${
                          isMatched ? ", matched" : ""
                        }`
                      : "Hidden card"
                  }
                  className={cn(
                    "flex min-h-28 items-center justify-center rounded-xl border-[3px] border-ink p-2 text-center transition-all sm:min-h-24",
                    !isUp &&
                      "tile-press cursor-pointer bg-primary shadow-[0_4px_0_var(--ink)]",
                    isUp && !isMatched && "wobble-in bg-sky-soft",
                    isMatched && "bg-leaf-soft opacity-80",
                    "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring",
                  )}
                >
                  {isUp ? (
                    card.kind === "word" ? (
                      <span className="font-heading text-lg font-semibold sm:text-xl">
                        {card.text}
                      </span>
                    ) : (
                      <span className="text-xs leading-snug sm:text-sm">
                        {card.text}
                      </span>
                    )
                  ) : (
                    <span
                      className="font-heading text-2xl font-semibold text-primary-foreground"
                      aria-hidden="true"
                    >
                      ?
                    </span>
                  )}
                </button>
              );
            })}
          </div>
          <p
            className="font-heading mt-4 text-center text-sm font-medium text-muted-foreground"
            role="status"
          >
            {attempts === 0 && faceUp.length === 0
              ? "Flip a card to start!"
              : attempts === 0
                ? "Now find its partner!"
                : `${matched.size} of ${PAIR_COUNT} pairs matched · ${attempts} ${
                    attempts === 1 ? "try" : "tries"
                  }`}
          </p>
        </div>
      )}
      {/* Persistent live region so the final match is announced before the
          board swaps to the summary. */}
      <p className="sr-only" role="status">
        {round?.phase === "done" ? "All pairs matched — great job!" : ""}
      </p>
    </GameFrame>
  );
}
