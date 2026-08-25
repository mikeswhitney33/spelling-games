"use client";

import { useEffect, useRef, useState } from "react";
import { Flame } from "lucide-react";

import { FlashWord } from "@/components/games/flash-spell-game";
import { MissingLettersWord } from "@/components/games/missing-letters-game";
import { SpotWord } from "@/components/games/spot-the-word-game";
import { ScrambleWord } from "@/components/games/word-scramble-game";
import { GameFrame } from "@/components/game-frame";
import { useGameRound, useGrade } from "@/hooks/use-spelling-round";
import { mulberry32, pickN } from "@/lib/game-utils";
import { GAMES } from "@/lib/games";
import { WORD_LISTS, type WordEntry } from "@/lib/words";

const game = GAMES.find((g) => g.slug === "daily-bee")!;

const MECHANICS = ["scramble", "missing", "spot", "flash"] as const;
type Mechanic = (typeof MECHANICS)[number];

const MECHANIC_LABELS: Record<Mechanic, string> = {
  scramble: "Unscramble it!",
  missing: "Fill the gaps!",
  spot: "Spot the real spelling!",
  flash: "Memorize it!",
};

const EMPTY: WordEntry[] = [];
const STORAGE_KEY = "spell-it-daily";

interface StreakData {
  lastPlayed: string;
  streak: number;
  best: number;
}

function hashString(text: string): number {
  let hash = 5381;
  for (const ch of text) hash = ((hash * 33) ^ ch.charCodeAt(0)) >>> 0;
  return hash;
}

function formatDate(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(
    date.getDate(),
  ).padStart(2, "0")}`;
}

function readStreak(): StreakData {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as StreakData;
  } catch {
    // Corrupt or unavailable storage — start fresh.
  }
  return { lastPlayed: "", streak: 0, best: 0 };
}

/** Record today's completion; only the first finish of the day counts. */
function saveStreak(today: string): StreakData {
  const data = readStreak();
  if (data.lastPlayed === today) return data;
  const yesterday = formatDate(new Date(Date.now() - 24 * 60 * 60 * 1000));
  const streak = data.lastPlayed === yesterday ? data.streak + 1 : 1;
  const next: StreakData = {
    lastPlayed: today,
    streak,
    best: Math.max(data.best, streak),
  };
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    // Private browsing — the streak just won't persist.
  }
  return next;
}

interface Daily {
  pool: WordEntry[];
  mechanics: Mechanic[];
  date: string;
}

export function DailyBeeGame() {
  const [grade, setGrade] = useGrade();
  const [daily, setDaily] = useState<Daily | null>(null);
  const [streak, setStreak] = useState<StreakData | null>(null);
  const recordedFor = useRef<string | null>(null);

  useEffect(() => {
    const today = formatDate(new Date());
    const rng = mulberry32(hashString(`${today}-${grade}`));
    const pool = pickN(WORD_LISTS[grade], 10, rng);
    const mechanics = pool.map(
      () => MECHANICS[Math.floor(rng() * MECHANICS.length)],
    );
    setDaily({ pool, mechanics, date: today });
    setStreak(readStreak());
  }, [grade]);

  const { state, record, advance, restart, roundId } = useGameRound(
    daily?.pool ?? EMPTY,
  );

  // First completion of the day extends the streak.
  useEffect(() => {
    if (!daily || !state || state.phase !== "done") return;
    if (recordedFor.current === daily.date) return;
    recordedFor.current = daily.date;
    setStreak(saveStreak(daily.date));
  }, [state, daily]);

  const entry = state?.phase === "playing" ? state.words[state.index] : null;
  const mechanic = daily && state ? daily.mechanics[state.index] : null;
  const playedToday = streak?.lastPlayed === daily?.date;

  const round =
    daily && state
      ? {
          ...state,
          summaryText: `${state.score} of ${state.words.length} on today's challenge${
            streak && streak.streak > 0
              ? ` — ${streak.streak}-day streak (best: ${streak.best})`
              : ""
          }!`,
        }
      : null;

  const wordProps =
    state && entry
      ? {
          entry,
          isLast: state.index + 1 === state.words.length,
          onJudged: record,
          onNext: advance,
        }
      : null;

  return (
    <GameFrame
      game={game}
      icon={<Flame className="h-7 w-7" aria-hidden="true" />}
      instructions="Ten words, a mix of every challenge — one fresh round each day."
      grade={grade}
      onGradeChange={setGrade}
      round={round}
      onRestart={restart}
    >
      {daily && state && entry && mechanic && wordProps && (
        <div>
          <div className="mb-4 flex flex-wrap items-center justify-center gap-x-4 gap-y-1 text-sm text-muted-foreground">
            <span className="font-heading font-medium">
              {new Date(`${daily.date}T12:00:00`).toLocaleDateString(undefined, {
                weekday: "long",
                month: "long",
                day: "numeric",
              })}
            </span>
            {streak && streak.streak > 0 && (
              <span className="flex items-center gap-1 font-heading font-medium text-coral">
                <Flame className="h-4 w-4" aria-hidden="true" />
                {streak.streak}-day streak
              </span>
            )}
            {playedToday && state.phase === "playing" && (
              <span>Already done today — this replay is just for fun!</span>
            )}
          </div>
          <p className="font-heading mb-2 text-center text-sm font-semibold uppercase tracking-wide text-sky">
            {MECHANIC_LABELS[mechanic]}
          </p>
          {mechanic === "scramble" && (
            <ScrambleWord
              key={`${roundId}-${state.index}-${grade}-scramble`}
              {...wordProps}
            />
          )}
          {mechanic === "missing" && (
            <MissingLettersWord
              key={`${roundId}-${state.index}-${grade}-missing`}
              grade={grade}
              {...wordProps}
            />
          )}
          {mechanic === "spot" && (
            <SpotWord
              key={`${roundId}-${state.index}-${grade}-spot`}
              {...wordProps}
            />
          )}
          {mechanic === "flash" && (
            <FlashWord
              key={`${roundId}-${state.index}-${grade}-flash`}
              grade={grade}
              {...wordProps}
            />
          )}
        </div>
      )}
    </GameFrame>
  );
}
