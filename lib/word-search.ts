import type { BankEntry } from "./banks";
import { BLOCKED_WORDS } from "./blocked-words";
import { pickN, shuffle, type Rng } from "./game-utils";

export interface WordSearchPlacement {
  word: string;
  hint: string;
  row: number;
  col: number;
  dRow: 0 | 1;
  dCol: 0 | 1;
}

export interface WordSearchPuzzle {
  grid: string[][];
  placements: WordSearchPlacement[];
  size: number;
}

export interface WordSearchConfig {
  size: number;
  count: number;
  diagonals: boolean;
}

/** The longest word a word-search grid can hold. */
export const WORD_SEARCH_MAX_LENGTH = 12;

/** Grid size scales with the pool's word lengths instead of a grade band. */
export function configForPool(pool: readonly BankEntry[]): WordSearchConfig {
  const lengths = pool
    .map((e) => e.word.length)
    .filter((l) => l <= WORD_SEARCH_MAX_LENGTH);
  const maxLen = lengths.length > 0 ? Math.max(...lengths) : 5;
  const size = Math.min(12, Math.max(7, maxLen + 2));
  return { size, count: size <= 8 ? 5 : 6, diagonals: size >= 10 };
}

const ALPHABET = "abcdefghijklmnopqrstuvwxyz";

type Cell = { r: number; c: number };

/** Every straight line of the grid: rows, columns, and both diagonal families. */
function collectLines(size: number): Cell[][] {
  const lines: Cell[][] = [];
  for (let r = 0; r < size; r++) {
    lines.push(Array.from({ length: size }, (_, c) => ({ r, c })));
  }
  for (let c = 0; c < size; c++) {
    lines.push(Array.from({ length: size }, (_, r) => ({ r, c })));
  }
  for (let d = -size + 1; d < size; d++) {
    const diag: Cell[] = [];
    const antiDiag: Cell[] = [];
    for (let r = 0; r < size; r++) {
      const c = r + d;
      if (c >= 0 && c < size) diag.push({ r, c });
      const ac = size - 1 - r + d;
      if (ac >= 0 && ac < size) antiDiag.push({ r, c: ac });
    }
    if (diag.length >= 3) lines.push(diag);
    if (antiDiag.length >= 3) lines.push(antiDiag);
  }
  return lines;
}

/**
 * True when the filled grid has a problem the fill should be rerolled for:
 * a blocked word touching at least one fill cell (a blocked substring living
 * entirely inside a legitimately placed word, like the "ass" in "embarrass",
 * is fine), or a stray copy of an answer word away from its real placement.
 */
function gridHasProblem(
  grid: string[][],
  placements: WordSearchPlacement[],
): boolean {
  const size = grid.length;
  const placedCells = new Set<string>();
  const placementPaths = new Map<string, Set<string>>();
  for (const p of placements) {
    // Grid cells are lowercase, so scan with the lowercased word.
    const word = p.word.toLowerCase();
    const path = word
      .split("")
      .map((_, i) => `${p.row + p.dRow * i},${p.col + p.dCol * i}`);
    path.forEach((k) => placedCells.add(k));
    const reversed = [...path].reverse();
    const set = placementPaths.get(word) ?? new Set<string>();
    set.add(path.join("|"));
    set.add(reversed.join("|"));
    placementPaths.set(word, set);
  }

  const answerWords = [...placementPaths.keys()];
  for (const line of collectLines(size)) {
    for (const oriented of [line, [...line].reverse()]) {
      const text = oriented.map(({ r, c }) => grid[r][c]).join("");
      for (const bad of BLOCKED_WORDS) {
        for (let at = text.indexOf(bad); at !== -1; at = text.indexOf(bad, at + 1)) {
          const window = oriented.slice(at, at + bad.length);
          if (window.some(({ r, c }) => !placedCells.has(`${r},${c}`))) {
            return true;
          }
        }
      }
      for (const word of answerWords) {
        for (
          let at = text.indexOf(word);
          at !== -1;
          at = text.indexOf(word, at + 1)
        ) {
          const path = oriented
            .slice(at, at + word.length)
            .map(({ r, c }) => `${r},${c}`)
            .join("|");
          if (!placementPaths.get(word)!.has(path)) return true;
        }
      }
    }
  }
  return false;
}

/** Build a word-search grid with words running right, down, or diagonally. */
export function generateWordSearch(
  pool: readonly BankEntry[],
  rng: Rng = Math.random,
  config: WordSearchConfig = configForPool(pool),
): WordSearchPuzzle {
  const { size, count, diagonals } = config;
  const usable = pool.filter((e) => e.word.length <= size);
  const directions: [0 | 1, 0 | 1][] = diagonals
    ? [
        [0, 1],
        [1, 0],
        [1, 1],
      ]
    : [
        [0, 1],
        [1, 0],
      ];

  let best: { placements: WordSearchPlacement[]; cells: Map<string, string> } = {
    placements: [],
    cells: new Map(),
  };

  for (let attempt = 0; attempt < 10 && best.placements.length < count; attempt++) {
    const entries = pickN(usable, Math.min(usable.length, count + 4), rng);
    const cells = new Map<string, string>();
    const placements: WordSearchPlacement[] = [];

    for (const entry of entries) {
      if (placements.length >= count) break;
      const word = entry.word.toLowerCase();
      // Reverse pairs (nap/pan) make selections ambiguous — keep one.
      const reversed = [...word].reverse().join("");
      if (placements.some((p) => p.word.toLowerCase() === reversed)) continue;
      let placed = false;
      for (let attempt2 = 0; attempt2 < 60 && !placed; attempt2++) {
        const [dRow, dCol] = directions[Math.floor(rng() * directions.length)];
        const maxRow = size - (dRow ? word.length : 1);
        const maxCol = size - (dCol ? word.length : 1);
        const row = Math.floor(rng() * (maxRow + 1));
        const col = Math.floor(rng() * (maxCol + 1));
        let ok = true;
        for (let i = 0; i < word.length; i++) {
          const existing = cells.get(`${row + dRow * i},${col + dCol * i}`);
          if (existing !== undefined && existing !== word[i]) {
            ok = false;
            break;
          }
        }
        if (!ok) continue;
        for (let i = 0; i < word.length; i++) {
          cells.set(`${row + dRow * i},${col + dCol * i}`, word[i]);
        }
        // Keep the entry's casing for the word-chip list; the grid cells
        // above already hold the lowercase letters.
        placements.push({
          word: entry.word,
          hint: entry.hint ?? "",
          row,
          col,
          dRow,
          dCol,
        });
        placed = true;
      }
    }
    if (placements.length > best.placements.length) best = { placements, cells };
  }

  // Fill the empty cells, biased toward the placed words' letters so the fill
  // looks plausible; reroll the fill if it forms a blocked word or a stray
  // copy of an answer word.
  const wordLetters = best.placements.flatMap((p) =>
    p.word.toLowerCase().split(""),
  );
  const buildGrid = (fill: (r: number, c: number) => string): string[][] =>
    Array.from({ length: size }, (_, r) =>
      Array.from({ length: size }, (_, c) => {
        const placedLetter = best.cells.get(`${r},${c}`);
        return placedLetter ?? fill(r, c);
      }),
    );

  for (let reroll = 0; reroll < 20; reroll++) {
    const grid = buildGrid(() =>
      rng() < 0.5 && wordLetters.length > 0
        ? wordLetters[Math.floor(rng() * wordLetters.length)]
        : ALPHABET[Math.floor(rng() * ALPHABET.length)],
    );
    if (!gridHasProblem(grid, best.placements)) {
      return { grid, placements: shuffle(best.placements, rng), size };
    }
  }

  // Last resort: uniform-random fill without the answer-letter bias; take the
  // first clean one, and after that accept the small remaining risk.
  for (let reroll = 0; ; reroll++) {
    const grid = buildGrid(
      () => ALPHABET[Math.floor(rng() * ALPHABET.length)],
    );
    if (reroll >= 20 || !gridHasProblem(grid, best.placements)) {
      return { grid, placements: shuffle(best.placements, rng), size };
    }
  }
}
