"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Check, Compass } from "lucide-react";

import { BankPicker, NotEnoughWords } from "@/components/bank-picker";
import { GameFrame } from "@/components/game-frame";
import { useWordBank } from "@/hooks/use-bank";
import type { RoundState } from "@/hooks/use-spelling-round";
import {
  WORD_SEARCH_MAX_LENGTH,
  generateWordSearch,
  type WordSearchPuzzle,
} from "@/lib/word-search";
import { GAMES } from "@/lib/games";
import { cn } from "@/lib/utils";

const game = GAMES.find((g) => g.slug === "word-search")!;

const cellKey = (row: number, col: number) => `${row},${col}`;

function placementCells(p: WordSearchPuzzle["placements"][number]): string[] {
  return p.word
    .split("")
    .map((_, i) => cellKey(p.row + p.dRow * i, p.col + p.dCol * i));
}

/** All cells on the straight line from a to b, or null if not a line. */
function lineBetween(a: string, b: string): string[] | null {
  const [ar, ac] = a.split(",").map(Number);
  const [br, bc] = b.split(",").map(Number);
  const dr = Math.sign(br - ar);
  const dc = Math.sign(bc - ac);
  const steps = Math.max(Math.abs(br - ar), Math.abs(bc - ac));
  if (dr !== 0 && dc !== 0 && Math.abs(br - ar) !== Math.abs(bc - ac)) {
    return null; // not horizontal, vertical, or 45° diagonal
  }
  return Array.from({ length: steps + 1 }, (_, i) =>
    cellKey(ar + dr * i, ac + dc * i),
  );
}

export function WordSearchGame() {
  const { bank, banks, setActive } = useWordBank();
  const pool = useMemo(
    () =>
      bank.entries.filter(
        (e) => e.word.length >= 3 && e.word.length <= WORD_SEARCH_MAX_LENGTH,
      ),
    [bank],
  );
  const [roundId, setRoundId] = useState(0);
  const [puzzle, setPuzzle] = useState<WordSearchPuzzle | null>(null);
  const [found, setFound] = useState<boolean[]>([]);
  const [firstTap, setFirstTap] = useState<string | null>(null);
  const [flashCells, setFlashCells] = useState<Set<string>>(new Set());
  const flashTimer = useRef<number | undefined>(undefined);

  useEffect(() => {
    const next = generateWordSearch(pool);
    setPuzzle(next);
    setFound(next.placements.map(() => false));
    setFirstTap(null);
    window.clearTimeout(flashTimer.current);
    setFlashCells(new Set());
  }, [pool, roundId]);

  useEffect(() => () => window.clearTimeout(flashTimer.current), []);

  const foundCells = useMemo(() => {
    if (!puzzle) return new Set<string>();
    const cells = new Set<string>();
    puzzle.placements.forEach((p, i) => {
      if (found[i]) placementCells(p).forEach((k) => cells.add(k));
    });
    return cells;
  }, [puzzle, found]);

  const round: RoundState | null = useMemo(() => {
    if (!puzzle) return null;
    const total = puzzle.placements.length;
    const foundCount = found.filter(Boolean).length;
    return {
      words: puzzle.placements.map((p) => ({ word: p.word, hint: p.hint })),
      index: Math.min(foundCount, total - 1),
      // Word searches are about completion — every found word scores.
      score: foundCount,
      streak: 0,
      bestStreak: 0,
      results: puzzle.placements.map(() => true),
      phase: foundCount === total ? "done" : "playing",
    };
  }, [puzzle, found]);

  const tapCell = (k: string) => {
    if (!puzzle) return;
    if (firstTap === null) {
      setFirstTap(k);
      return;
    }
    if (firstTap === k) {
      setFirstTap(null);
      return;
    }
    const line = lineBetween(firstTap, k);
    setFirstTap(null);
    if (!line) {
      flashWrong([firstTap, k]);
      return;
    }
    const lineSet = line.join("|");
    const reversedSet = [...line].reverse().join("|");
    const hit = puzzle.placements.findIndex((p) => {
      const cells = placementCells(p).join("|");
      return cells === lineSet || cells === reversedSet;
    });
    if (hit >= 0) {
      // Re-selecting an already-found word is a harmless no-op.
      if (!found[hit]) setFound(found.map((f, i) => (i === hit ? true : f)));
    } else {
      flashWrong(line);
    }
  };

  const flashWrong = (cells: string[]) => {
    window.clearTimeout(flashTimer.current);
    setFlashCells(new Set(cells));
    flashTimer.current = window.setTimeout(() => setFlashCells(new Set()), 500);
  };

  const cellSize =
    puzzle && puzzle.size > 10
      ? "h-7 w-7 text-xs sm:h-8 sm:w-8 sm:text-sm"
      : puzzle && puzzle.size > 7
        ? "h-8 w-8 text-sm sm:h-9 sm:w-9 sm:text-base"
        : "h-10 w-10 text-lg";

  return (
    <GameFrame
      game={game}
      icon={<Compass className="h-7 w-7" aria-hidden="true" />}
      instructions="Tap the first letter of a hidden word, then its last letter."
      picker={<BankPicker bank={bank} banks={banks} onChange={setActive} />}
      notice={
        pool.length < 5 ? (
          <NotEnoughWords need={5} requirement="words of 3–12 letters" />
        ) : undefined
      }
      round={round}
      onRestart={() => setRoundId((n) => n + 1)}
    >
      {puzzle && round?.phase === "playing" && (
        <div>
          <div className="overflow-x-auto">
            <div
              className="mx-auto grid w-fit gap-0.5"
              style={{
                gridTemplateColumns: `repeat(${puzzle.size}, minmax(0, 1fr))`,
              }}
              aria-label="Letter grid"
            >
              {puzzle.grid.map((rowLetters, r) =>
                rowLetters.map((letter, c) => {
                  const k = cellKey(r, c);
                  const isFound = foundCells.has(k);
                  return (
                    <button
                      key={k}
                      type="button"
                      onClick={() => tapCell(k)}
                      aria-label={`${letter}, row ${r + 1}, column ${c + 1}${
                        firstTap === k ? ", selected as first letter" : ""
                      }`}
                      className={cn(
                        "font-heading cursor-pointer rounded-md border-2 border-transparent text-center font-medium uppercase transition-colors",
                        "hover:border-ink focus-visible:outline-2 focus-visible:outline-ring",
                        cellSize,
                        isFound && "bg-leaf-soft border-leaf text-foreground",
                        firstTap === k && "border-ink bg-sky-soft",
                        flashCells.has(k) && "shake bg-coral-soft",
                      )}
                    >
                      {letter}
                    </button>
                  );
                }),
              )}
            </div>
          </div>

          <p
            className="font-heading mt-4 text-center text-sm font-medium text-muted-foreground"
            role="status"
          >
            {firstTap
              ? "Now tap the LAST letter of that word."
              : "Tap the FIRST letter of a word you spot."}
          </p>

          {/* Words to find */}
          <ul className="mt-4 flex flex-wrap justify-center gap-2">
            {puzzle.placements.map((p, i) => (
              <li
                key={p.word}
                className={cn(
                  "font-heading flex items-center gap-1 rounded-lg border-2 border-ink bg-card px-3 py-1 text-sm font-medium",
                  found[i] &&
                    "border-leaf bg-leaf-soft text-muted-foreground line-through",
                )}
              >
                {found[i] && <Check className="h-3.5 w-3.5 text-leaf" aria-hidden="true" />}
                {p.word}
              </li>
            ))}
          </ul>
        </div>
      )}
    </GameFrame>
  );
}
