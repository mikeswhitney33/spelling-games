import { BLOCKED_WORDS } from "./blocked-words";
import { pickN, shuffle, type Rng } from "./game-utils";
import type { GradeBand, WordEntry } from "./words";

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

export const WORD_SEARCH_CONFIG: Record<
  GradeBand,
  { size: number; count: number; diagonals: boolean }
> = {
  "k-1": { size: 7, count: 5, diagonals: false },
  "2-3": { size: 9, count: 6, diagonals: false },
  "4-5": { size: 11, count: 6, diagonals: true },
  "6-plus": { size: 12, count: 6, diagonals: true },
};

const ALPHABET = "abcdefghijklmnopqrstuvwxyz";

function gridContainsBlockedWord(grid: string[][]): boolean {
  const size = grid.length;
  const lines: string[] = [];
  for (let r = 0; r < size; r++) {
    lines.push(grid[r].join(""));
  }
  for (let c = 0; c < size; c++) {
    lines.push(grid.map((row) => row[c]).join(""));
  }
  for (let d = -size + 1; d < size; d++) {
    let diag = "";
    let antiDiag = "";
    for (let r = 0; r < size; r++) {
      const c = r + d;
      if (c >= 0 && c < size) diag += grid[r][c];
      const ac = size - 1 - r + d;
      if (ac >= 0 && ac < size) antiDiag += grid[r][ac];
    }
    if (diag.length >= 3) lines.push(diag);
    if (antiDiag.length >= 3) lines.push(antiDiag);
  }
  return lines.some((line) => {
    const reversed = [...line].reverse().join("");
    return BLOCKED_WORDS.some(
      (bad) => line.includes(bad) || reversed.includes(bad),
    );
  });
}

/** Build a word-search grid with words running right, down, or diagonally. */
export function generateWordSearch(
  pool: readonly WordEntry[],
  grade: GradeBand,
  rng: Rng = Math.random,
): WordSearchPuzzle {
  const { size, count, diagonals } = WORD_SEARCH_CONFIG[grade];
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
        placements.push({ word, hint: entry.hint, row, col, dRow, dCol });
        placed = true;
      }
    }
    if (placements.length > best.placements.length) best = { placements, cells };
  }

  // Fill the empty cells, biased toward the placed words' letters so the fill
  // looks plausible; reroll the whole fill if it accidentally spells anything
  // from the blocked list.
  const wordLetters = best.placements.flatMap((p) => p.word.split(""));
  for (let reroll = 0; reroll < 20; reroll++) {
    const grid: string[][] = Array.from({ length: size }, (_, r) =>
      Array.from({ length: size }, (_, c) => {
        const placedLetter = best.cells.get(`${r},${c}`);
        if (placedLetter) return placedLetter;
        return rng() < 0.5 && wordLetters.length > 0
          ? wordLetters[Math.floor(rng() * wordLetters.length)]
          : ALPHABET[Math.floor(rng() * ALPHABET.length)];
      }),
    );
    if (!gridContainsBlockedWord(grid)) {
      return { grid, placements: shuffle(best.placements, rng), size };
    }
  }

  // Last resort: all-consonant fill can't spell anything on the blocked list.
  const grid: string[][] = Array.from({ length: size }, (_, r) =>
    Array.from({ length: size }, (_, c) => {
      const placedLetter = best.cells.get(`${r},${c}`);
      if (placedLetter) return placedLetter;
      return "bcdfghjklmnpqrstvwxz"[Math.floor(rng() * 20)];
    }),
  );
  return { grid, placements: shuffle(best.placements, rng), size };
}
