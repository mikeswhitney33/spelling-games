"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Check, LayoutGrid } from "lucide-react";

import { BankPicker, NotEnoughWords } from "@/components/bank-picker";
import { GameFrame } from "@/components/game-frame";
import { useWordBank } from "@/hooks/use-bank";
import type { RoundState } from "@/hooks/use-spelling-round";
import {
  CROSSWORD_MAX_LENGTH,
  generateCrossword,
  type CrosswordPlacement,
  type CrosswordPuzzle,
} from "@/lib/crossword";
import { GAMES } from "@/lib/games";
import { cn } from "@/lib/utils";

const game = GAMES.find((g) => g.slug === "mini-crossword")!;

const cellKey = (row: number, col: number) => `${row},${col}`;

function placementCells(p: CrosswordPlacement): string[] {
  return p.word.split("").map((_, i) =>
    p.dir === "down" ? cellKey(p.row + i, p.col) : cellKey(p.row, p.col + i),
  );
}

export function MiniCrosswordGame() {
  const { bank, banks, setActive } = useWordBank();
  const pool = useMemo(
    () =>
      bank.entries.filter(
        (e) => e.hint && e.word.length >= 3 && e.word.length <= CROSSWORD_MAX_LENGTH,
      ),
    [bank],
  );
  const [roundId, setRoundId] = useState(0);
  const [puzzle, setPuzzle] = useState<CrosswordPuzzle | null>(null);
  const [letters, setLettersState] = useState<Record<string, string>>({});
  // Ref mirror so rapid keystrokes each build on the latest letters, not a
  // stale render closure.
  const lettersRef = useRef<Record<string, string>>({});
  const setLetters = (next: Record<string, string>) => {
    lettersRef.current = next;
    setLettersState(next);
  };
  const [solved, setSolved] = useState<boolean[]>([]);
  const [results, setResults] = useState<(boolean | null)[]>([]);
  const [selected, setSelectedState] = useState(0);
  const [activeCell, setActiveCell] = useState<string | null>(null);
  // Ref mirror so focus-advance logic sees the selection synchronously,
  // even between rapid keystrokes before state commits.
  const selectedRef = useRef(0);
  const setSelected = (index: number) => {
    selectedRef.current = index;
    setSelectedState(index);
  };
  const [shakeIndex, setShakeIndex] = useState<number | null>(null);
  const filledBefore = useRef<Set<number>>(new Set());
  const shakeTimer = useRef<number | undefined>(undefined);
  const inputs = useRef<Map<string, HTMLInputElement>>(new Map());

  useEffect(() => {
    const next = generateCrossword(pool, 5);
    setPuzzle(next);
    setLetters({});
    setSolved(next.placements.map(() => false));
    setResults(next.placements.map(() => null));
    setSelected(0);
    setActiveCell(null);
    window.clearTimeout(shakeTimer.current);
    setShakeIndex(null);
    filledBefore.current = new Set();
  }, [pool, roundId]);

  useEffect(() => () => window.clearTimeout(shakeTimer.current), []);

  // Which placements pass through each cell, plus its clue number.
  const cellMap = useMemo(() => {
    if (!puzzle) return new Map<string, { indices: number[]; number?: number }>();
    const map = new Map<string, { indices: number[]; number?: number }>();
    puzzle.placements.forEach((p, index) => {
      placementCells(p).forEach((k, i) => {
        const info = map.get(k) ?? { indices: [] };
        info.indices.push(index);
        if (i === 0) info.number = Math.min(info.number ?? p.number, p.number);
        map.set(k, info);
      });
    });
    return map;
  }, [puzzle]);

  // Adapt crossword progress to the shared round frame.
  const round: RoundState | null = useMemo(() => {
    if (!puzzle) return null;
    const solvedCount = solved.filter(Boolean).length;
    const total = puzzle.placements.length;
    return {
      words: puzzle.placements.map((p) => ({ word: p.word, hint: p.hint })),
      index: Math.min(solvedCount, total - 1),
      score: results.filter((r) => r === true).length,
      streak: 0,
      bestStreak: 0,
      results: results.map((r) => r === true),
      phase: solvedCount === total ? "done" : "playing",
    };
  }, [puzzle, solved, results]);

  const checkWords = (nextLetters: Record<string, string>, changedKey: string) => {
    if (!puzzle) return;
    const nextSolved = [...solved];
    const nextResults = [...results];
    let shook: number | null = null;
    puzzle.placements.forEach((p, index) => {
      if (nextSolved[index]) return;
      const cells = placementCells(p);
      if (!cells.every((k) => nextLetters[k])) return;
      const attempt = cells.map((k) => nextLetters[k]).join("");
      // Typed letters are lowercased on input; lowercase the target so
      // capitalized entries like "February" stay solvable.
      if (attempt === p.word.toLowerCase()) {
        // A correct word counts no matter which word the kid was working on.
        nextSolved[index] = true;
        nextResults[index] = nextResults[index] ?? true;
      } else if (
        // A wrong fill only counts against the word the kid is actually
        // typing — a stray crossing letter shouldn't dock two words at once.
        index === selectedRef.current &&
        cells.includes(changedKey) &&
        !filledBefore.current.has(index)
      ) {
        filledBefore.current.add(index);
        nextResults[index] = nextResults[index] ?? false;
        shook = index;
      }
    });
    setSolved(nextSolved);
    setResults(nextResults);
    if (shook !== null) {
      window.clearTimeout(shakeTimer.current);
      setShakeIndex(shook);
      shakeTimer.current = window.setTimeout(() => setShakeIndex(null), 450);
    }
  };

  const isLocked = (k: string) => {
    const info = cellMap.get(k);
    return !!info && info.indices.some((i) => solved[i]);
  };

  const selectCell = (k: string) => {
    const info = cellMap.get(k);
    if (!info || !puzzle) return;
    setActiveCell(k);
    if (!info.indices.includes(selectedRef.current)) {
      // Prefer the across word when the cell is a crossing.
      const across = info.indices.find(
        (i) => puzzle.placements[i].dir === "across",
      );
      setSelected(across ?? info.indices[0]);
    }
  };

  /** Clicking a cell that's already focused flips to the crossing word. */
  const toggleDirection = (k: string) => {
    const info = cellMap.get(k);
    if (!info || info.indices.length < 2) return;
    const other = info.indices.find((i) => i !== selectedRef.current);
    if (other !== undefined) setSelected(other);
  };

  /** Move along the selected word, skipping locked cells in that direction. */
  const moveFocus = (k: string, delta: 1 | -1): string | null => {
    if (!puzzle) return null;
    const cells = placementCells(puzzle.placements[selectedRef.current]);
    let at = cells.indexOf(k);
    if (at === -1) return null;
    for (at += delta; at >= 0 && at < cells.length; at += delta) {
      if (!isLocked(cells[at])) {
        const next = cells[at];
        inputs.current.get(next)?.focus();
        inputs.current.get(next)?.select();
        setActiveCell(next);
        return next;
      }
    }
    return null;
  };

  const handleChange = (k: string, raw: string) => {
    if (isLocked(k)) return;
    const char = raw.toLowerCase().replace(/[^a-z]/g, "").slice(-1);
    const next = { ...lettersRef.current, [k]: char };
    if (!char) delete next[k];
    setLetters(next);
    checkWords(next, k);
    if (char) moveFocus(k, 1);
  };

  const ARROW_DELTAS: Record<string, [number, number]> = {
    ArrowUp: [-1, 0],
    ArrowDown: [1, 0],
    ArrowLeft: [0, -1],
    ArrowRight: [0, 1],
  };

  const handleKeyDown = (k: string, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Backspace" && !lettersRef.current[k]) {
      e.preventDefault();
      const prev = moveFocus(k, -1);
      if (prev) {
        const next = { ...lettersRef.current };
        delete next[prev];
        setLetters(next);
      }
      return;
    }
    const arrow = ARROW_DELTAS[e.key];
    if (arrow && puzzle) {
      e.preventDefault();
      const [r, c] = k.split(",").map(Number);
      // Walk in the arrow direction until the next cell that exists.
      const limit = Math.max(puzzle.width, puzzle.height);
      for (let step = 1; step <= limit; step++) {
        const target = cellKey(r + arrow[0] * step, c + arrow[1] * step);
        const input = inputs.current.get(target);
        if (input) {
          input.focus();
          input.select();
          setActiveCell(target);
          break;
        }
      }
    }
  };

  const cellSize =
    puzzle && Math.max(puzzle.width, puzzle.height) > 12
      ? "h-7 w-7 text-sm"
      : puzzle && Math.max(puzzle.width, puzzle.height) > 8
        ? "h-8 w-8 text-base"
        : "h-10 w-10 text-xl";

  // A pool with nothing usable lays out an empty grid, so there is no
  // placement to highlight — the "not enough words" notice shows instead.
  const selectedPlacement = puzzle?.placements[selected];
  const selectedCells = selectedPlacement
    ? new Set(placementCells(selectedPlacement))
    : new Set<string>();
  const shakeCells =
    puzzle && shakeIndex !== null && puzzle.placements[shakeIndex]
      ? new Set(placementCells(puzzle.placements[shakeIndex]))
      : new Set<string>();

  /** Screen-reader label tying a cell to its clue and letter position. */
  const cellLabel = (k: string): string => {
    if (!puzzle) return "";
    const info = cellMap.get(k);
    if (!info) return "";
    const primary =
      info.indices.find((i) => i === selectedRef.current) ??
      info.indices.find((i) => puzzle.placements[i].dir === "across") ??
      info.indices[0];
    const p = puzzle.placements[primary];
    const position = placementCells(p).indexOf(k) + 1;
    return `Clue ${p.number} ${p.dir}, letter ${position} of ${p.word.length}. ${p.hint}${
      solved[primary] ? " Solved." : ""
    }`;
  };

  return (
    <GameFrame
      game={game}
      icon={<LayoutGrid className="h-7 w-7" aria-hidden="true" />}
      instructions="Use the clues to fill the grid — words cross and share letters."
      picker={<BankPicker bank={bank} banks={banks} onChange={setActive} />}
      notice={
        pool.length < 5 ? (
          <NotEnoughWords need={5} requirement="words with hints" />
        ) : undefined
      }
      round={round}
      onRestart={() => setRoundId((n) => n + 1)}
    >
      {puzzle && round?.phase === "playing" && (
        <div>
          {/* Grid */}
          <div className="overflow-x-auto">
            <div
              className="mx-auto grid w-fit gap-1"
              style={{
                gridTemplateColumns: `repeat(${puzzle.width}, minmax(0, 1fr))`,
              }}
            >
              {Array.from({ length: puzzle.height }, (_, r) =>
                Array.from({ length: puzzle.width }, (_, c) => {
                  const k = cellKey(r, c);
                  const info = cellMap.get(k);
                  if (!info) {
                    return <div key={k} className={cellSize} aria-hidden="true" />;
                  }
                  const locked = isLocked(k);
                  return (
                    <div key={k} className="relative">
                      {info.number !== undefined && (
                        <span
                          className="pointer-events-none absolute top-0 left-0.5 z-10 text-[0.55rem] font-bold text-muted-foreground"
                          aria-hidden="true"
                        >
                          {info.number}
                        </span>
                      )}
                      <input
                        ref={(el) => {
                          if (el) inputs.current.set(k, el);
                          else inputs.current.delete(k);
                        }}
                        value={(letters[k] ?? "").toUpperCase()}
                        onChange={(e) => handleChange(k, e.target.value)}
                        onKeyDown={(e) => handleKeyDown(k, e)}
                        onFocus={(e) => {
                          e.currentTarget.select();
                          selectCell(k);
                        }}
                        onMouseDown={(e) => {
                          // Fires before focus: only an already-focused cell
                          // re-click should flip to the crossing word.
                          if (document.activeElement === e.currentTarget) {
                            toggleDirection(k);
                          }
                        }}
                        readOnly={locked}
                        tabIndex={locked ? -1 : 0}
                        autoComplete="off"
                        autoCapitalize="off"
                        spellCheck={false}
                        aria-label={cellLabel(k)}
                        className={cn(
                          "font-heading block rounded-md border-2 border-ink bg-card text-center font-semibold caret-transparent outline-none",
                          cellSize,
                          selectedCells.has(k) && !locked && "bg-sky-soft",
                          activeCell === k && !locked && "ring-2 ring-ring",
                          locked && "bg-leaf-soft",
                          shakeCells.has(k) && "shake bg-coral-soft",
                        )}
                      />
                    </div>
                  );
                }),
              )}
            </div>
          </div>

          {/* Clues */}
          <div className="mt-6 grid gap-4 text-left sm:grid-cols-2">
            {(["across", "down"] as const).map((dir) => {
              const clues = puzzle.placements
                .map((p, index) => ({ p, index }))
                .filter(({ p }) => p.dir === dir)
                .sort((a, b) => a.p.number - b.p.number);
              if (clues.length === 0) return null;
              return (
                <div key={dir}>
                  <h3 className="font-heading text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                    {dir}
                  </h3>
                  <ul className="mt-2 space-y-1.5">
                    {clues.map(({ p, index }) => (
                      <li key={index}>
                        <button
                          type="button"
                          onClick={() => {
                            setSelected(index);
                            const first = placementCells(p).find(
                              (k) => !letters[k] && !isLocked(k),
                            );
                            const focusKey = first ?? placementCells(p)[0];
                            inputs.current.get(focusKey)?.focus();
                            setActiveCell(focusKey);
                          }}
                          className={cn(
                            "flex w-full cursor-pointer items-baseline gap-2 rounded-lg px-2 py-1 text-left text-sm transition-colors hover:bg-secondary focus-visible:outline-2 focus-visible:outline-ring",
                            selected === index && !solved[index] && "bg-sky-soft",
                            solved[index] && "text-muted-foreground line-through",
                          )}
                        >
                          <span className="font-heading font-semibold">
                            {p.number}.
                          </span>
                          <span className="flex-1">
                            {p.hint}{" "}
                            <span className="text-muted-foreground">
                              ({p.word.length} letters)
                            </span>
                          </span>
                          {solved[index] && (
                            <Check
                              className="h-4 w-4 shrink-0 text-leaf"
                              aria-label="solved"
                            />
                          )}
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </GameFrame>
  );
}
