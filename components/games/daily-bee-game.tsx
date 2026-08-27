"use client";

import { useEffect, useRef, useState } from "react";
import { Flame } from "lucide-react";

import { FlashWord } from "@/components/games/flash-spell-game";
import { MissingLettersWord } from "@/components/games/missing-letters-game";
import { SpotWord } from "@/components/games/spot-the-word-game";
import { ScrambleWord } from "@/components/games/word-scramble-game";
import { GameFrame } from "@/components/game-frame";
import { GradePicker } from "@/components/grade-picker";
import { useGameRound, useGrade } from "@/hooks/use-spelling-round";
import {
  blanksForWord,
  flashMsForWord,
  mulberry32,
  pickN,
} from "@/lib/game-utils";
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
  // Yesterday relative to the day being credited, not the wall clock —
  // otherwise finishing just after midnight (or across DST) resets streaks.
  const reference = new Date(`${today}T12:00:00`);
  reference.setDate(reference.getDate() - 1);
  const yesterday = formatDate(reference);
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
  /** Keyed by word so the pairing survives the round's presentation shuffle. */
  mechanicByWord: Record<string, Mechanic>;
  date: string;
}

export function DailyBeeGame() {
  const [grade, setGrade] = useGrade();
  const [daily, setDaily] = useState<Daily | null>(null);
  const [streak, setStreak] = useState<StreakData | null>(null);
  const [dateTick, setDateTick] = useState(0);
  const recordedFor = useRef<string | null>(null);
  const dailyDateRef = useRef<string | null>(null);

  useEffect(() => {
    const today = formatDate(new Date());
    const rng = mulberry32(hashString(`${today}-${grade}`));
    const pool = pickN(WORD_LISTS[grade], 10, rng);
    const mechanicByWord = Object.fromEntries(
      pool.map((e) => [e.word, MECHANICS[Math.floor(rng() * MECHANICS.length)]]),
    );
    setDaily({ pool, mechanicByWord, date: today });
    dailyDateRef.current = today;
    setStreak(readStreak());
  }, [grade, dateTick]);

  // A tab left open across midnight should roll over to the new day's round.
  useEffect(() => {
    const checkDate = () => {
      if (
        dailyDateRef.current &&
        formatDate(new Date()) !== dailyDateRef.current
      ) {
        setDateTick((n) => n + 1);
      }
    };
    window.addEventListener("visibilitychange", checkDate);
    window.addEventListener("focus", checkDate);
    return () => {
      window.removeEventListener("visibilitychange", checkDate);
      window.removeEventListener("focus", checkDate);
    };
  }, []);

  const { state, record, advance, restart, roundId } = useGameRound(
    daily?.pool ?? EMPTY,
  );

  // First completion of the day extends the streak. Credit the actual current
  // day, in case the round straddled midnight.
  useEffect(() => {
    if (!daily || !state || state.phase !== "done") return;
    const today = formatDate(new Date());
    if (recordedFor.current === today) return;
    recordedFor.current = today;
    setStreak(saveStreak(today));
  }, [state, daily]);

  // Ignore round state until it was built from the current daily pool.
  const roundReady =
    daily !== null &&
    state !== null &&
    state.words.length > 0 &&
    daily.pool.includes(state.words[0]);

  const entry =
    roundReady && state.phase === "playing" ? state.words[state.index] : null;
  const mechanic = entry && daily ? daily.mechanicByWord[entry.word] : null;
  const playedToday = streak?.lastPlayed === daily?.date;

  const round =
    roundReady && daily && state
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
      picker={<GradePicker value={grade} onChange={setGrade} />}
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
              blanks={blanksForWord(entry.word)}
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
              showMs={flashMsForWord(entry.word)}
              {...wordProps}
            />
          )}
        </div>
      )}
    </GameFrame>
  );
}
