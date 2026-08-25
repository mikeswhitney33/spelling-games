import { pickN, type Rng } from "./game-utils";
import type { WordEntry } from "./words";

export type Direction = "across" | "down";

export interface CrosswordPlacement {
  word: string;
  hint: string;
  row: number;
  col: number;
  dir: Direction;
  /** Standard crossword clue number, assigned after layout. */
  number: number;
}

export interface CrosswordPuzzle {
  placements: CrosswordPlacement[];
  width: number;
  height: number;
}

const key = (row: number, col: number) => `${row},${col}`;

/**
 * Greedy crossword layout: place the first word across, then cross each
 * following word through a shared letter, keeping standard adjacency rules.
 * Retries with fresh word picks and keeps the densest attempt.
 */
const MAX_WORD_LENGTH = 9;

export function generateCrossword(
  pool: readonly WordEntry[],
  target = 5,
  rng: Rng = Math.random,
): CrosswordPuzzle {
  // Very long words make sprawling grids that don't fit small screens.
  const usable = pool.filter((e) => e.word.length <= MAX_WORD_LENGTH);
  let best: { word: string; hint: string; row: number; col: number; dir: Direction }[] =
    [];
  let bestArea = Infinity;
  const areaOf = (placed: typeof best) => {
    const rows = placed.flatMap((p) => [
      p.row,
      p.dir === "down" ? p.row + p.word.length - 1 : p.row,
    ]);
    const cols = placed.flatMap((p) => [
      p.col,
      p.dir === "across" ? p.col + p.word.length - 1 : p.col,
    ]);
    return (
      (Math.max(...rows) - Math.min(...rows) + 1) *
      (Math.max(...cols) - Math.min(...cols) + 1)
    );
  };
  // A comfortably phone-sized grid; stop early once an attempt fits it.
  const COZY_AREA = 120;

  for (let attempt = 0; attempt < 30; attempt++) {
    const words = pickN(usable, Math.min(usable.length, 12), rng);
    const grid = new Map<string, string>();
    // Directions already covering each cell, so two same-direction words can
    // never overlap end-to-end (e.g. "art" extending into "cart").
    const dirs = new Map<string, Set<Direction>>();
    const placed: typeof best = [];

    const setWord = (entry: WordEntry, row: number, col: number, dir: Direction) => {
      for (let i = 0; i < entry.word.length; i++) {
        const r = dir === "down" ? row + i : row;
        const c = dir === "across" ? col + i : col;
        grid.set(key(r, c), entry.word[i]);
        const set = dirs.get(key(r, c)) ?? new Set<Direction>();
        set.add(dir);
        dirs.set(key(r, c), set);
      }
      placed.push({ word: entry.word, hint: entry.hint, row, col, dir });
    };

    const canPlace = (word: string, row: number, col: number, dir: Direction) => {
      const dr = dir === "down" ? 1 : 0;
      const dc = dir === "across" ? 1 : 0;
      // The cells just before the start and after the end must be empty.
      if (grid.has(key(row - dr, col - dc))) return false;
      if (grid.has(key(row + dr * word.length, col + dc * word.length))) return false;
      let crossings = 0;
      for (let i = 0; i < word.length; i++) {
        const r = row + dr * i;
        const c = col + dc * i;
        const existing = grid.get(key(r, c));
        if (existing !== undefined) {
          if (existing !== word[i]) return false;
          // Only perpendicular crossings — never ride along a same-direction word.
          if (dirs.get(key(r, c))?.has(dir)) return false;
          crossings++;
        } else {
          // An empty cell may not touch another word side-on.
          if (grid.has(key(r + dc, c + dr)) || grid.has(key(r - dc, c - dr))) {
            return false;
          }
        }
      }
      return crossings > 0;
    };

    setWord(words[0], 0, 0, "across");
    for (const entry of words.slice(1)) {
      if (placed.length >= target) break;
      const options: { row: number; col: number; dir: Direction }[] = [];
      for (const p of placed) {
        for (let i = 0; i < p.word.length; i++) {
          for (let j = 0; j < entry.word.length; j++) {
            if (p.word[i] !== entry.word[j]) continue;
            const option =
              p.dir === "across"
                ? { row: p.row - j, col: p.col + i, dir: "down" as const }
                : { row: p.row + i, col: p.col - j, dir: "across" as const };
            if (canPlace(entry.word, option.row, option.col, option.dir)) {
              options.push(option);
            }
          }
        }
      }
      if (options.length > 0) {
        // Prefer placements that keep the grid compact; pick randomly among
        // the tightest few for variety.
        const bounds = placed.reduce(
          (b, p) => {
            const endRow = p.dir === "down" ? p.row + p.word.length - 1 : p.row;
            const endCol = p.dir === "across" ? p.col + p.word.length - 1 : p.col;
            return {
              minRow: Math.min(b.minRow, p.row),
              maxRow: Math.max(b.maxRow, endRow),
              minCol: Math.min(b.minCol, p.col),
              maxCol: Math.max(b.maxCol, endCol),
            };
          },
          { minRow: 0, maxRow: 0, minCol: 0, maxCol: 0 },
        );
        const area = (o: { row: number; col: number; dir: Direction }) => {
          const endRow = o.dir === "down" ? o.row + entry.word.length - 1 : o.row;
          const endCol = o.dir === "across" ? o.col + entry.word.length - 1 : o.col;
          return (
            (Math.max(bounds.maxRow, endRow) - Math.min(bounds.minRow, o.row) + 1) *
            (Math.max(bounds.maxCol, endCol) - Math.min(bounds.minCol, o.col) + 1)
          );
        };
        options.sort((a, b) => area(a) - area(b));
        const tight = options.slice(0, 3);
        const pick = tight[Math.floor(rng() * tight.length)];
        setWord(entry, pick.row, pick.col, pick.dir);
      }
    }

    const area = areaOf(placed);
    if (
      placed.length > best.length ||
      (placed.length === best.length && area < bestArea)
    ) {
      best = placed;
      bestArea = area;
    }
    if (best.length >= target && bestArea <= COZY_AREA) break;
  }

  // Normalize to 0-based coordinates.
  const minRow = Math.min(...best.map((p) => p.row));
  const minCol = Math.min(...best.map((p) => p.col));
  const normalized = best.map((p) => ({
    ...p,
    row: p.row - minRow,
    col: p.col - minCol,
  }));
  const height =
    Math.max(
      ...normalized.map((p) => (p.dir === "down" ? p.row + p.word.length - 1 : p.row)),
    ) + 1;
  const width =
    Math.max(
      ...normalized.map((p) =>
        p.dir === "across" ? p.col + p.word.length - 1 : p.col,
      ),
    ) + 1;

  // Assign standard clue numbers: start cells in row-major order.
  const startKeys = [...new Set(normalized.map((p) => key(p.row, p.col)))].sort(
    (a, b) => {
      const [ar, ac] = a.split(",").map(Number);
      const [br, bc] = b.split(",").map(Number);
      return ar - br || ac - bc;
    },
  );
  const numberByCell = new Map(startKeys.map((k, i) => [k, i + 1]));

  return {
    placements: normalized.map((p) => ({
      ...p,
      number: numberByCell.get(key(p.row, p.col))!,
    })),
    width,
    height,
  };
}
