"use client";

import { useEffect, useState } from "react";
import { PartyPopper, Volume2 } from "lucide-react";

import { BankPicker, NotEnoughWords } from "@/components/bank-picker";
import { FeedbackPanel, GameFrame } from "@/components/game-frame";
import { Tile, TileButton, tileSizeForWord } from "@/components/tile";
import { Button } from "@/components/ui/button";
import { useWordBank } from "@/hooks/use-bank";
import { useGameRound } from "@/hooks/use-spelling-round";
import type { BankEntry } from "@/lib/banks";
import { speak } from "@/lib/game-utils";
import { GAMES } from "@/lib/games";
import { cn } from "@/lib/utils";

const game = GAMES.find((g) => g.slug === "balloon-pop")!;

const MAX_MISSES = 6;
const ALPHABET = "abcdefghijklmnopqrstuvwxyz".split("");
const BALLOON_COLORS = [
  "var(--coral)",
  "var(--sun)",
  "var(--leaf)",
  "var(--sky)",
  "var(--grape)",
  "var(--coral)",
];

export function BalloonPopGame() {
  const { bank, banks, setActive } = useWordBank();
  const { state, record, advance, restart, roundId } = useGameRound(bank.entries);
  const entry = state?.phase === "playing" ? state.words[state.index] : null;

  return (
    <GameFrame
      game={game}
      icon={<PartyPopper className="h-7 w-7" aria-hidden="true" />}
      instructions="Pick letters to spell the hidden word — every wrong guess pops a balloon."
      picker={<BankPicker bank={bank} banks={banks} onChange={setActive} />}
      notice={
        bank.entries.length < 4 ? (
          <NotEnoughWords need={4} requirement="words" />
        ) : undefined
      }
      round={state}
      onRestart={restart}
    >
      {state && entry && (
        <BalloonWord
          key={`${roundId}-${state.index}-${bank.id}`}
          entry={entry}
          isLast={state.index + 1 === state.words.length}
          onJudged={record}
          onNext={advance}
        />
      )}
    </GameFrame>
  );
}

function Balloon({ popped, color }: { popped: boolean; color: string }) {
  return (
    <svg viewBox="0 0 40 60" className="h-14 w-9" aria-hidden="true">
      {popped ? (
        <g
          stroke="var(--muted-foreground)"
          strokeWidth="2.5"
          strokeLinecap="round"
          fill="none"
          opacity="0.5"
        >
          {/* Little burst where the balloon was */}
          <path d="M20 16 v-8" />
          <path d="M20 24 v8" />
          <path d="M16 20 h-8" />
          <path d="M24 20 h8" />
          <path d="M17 17 l-5 -5" />
          <path d="M23 17 l5 -5" />
          <path d="M17 23 l-5 5" />
          <path d="M23 23 l5 5" />
        </g>
      ) : (
        <g>
          <ellipse
            cx="20"
            cy="19"
            rx="14"
            ry="17"
            fill={color}
            stroke="var(--ink)"
            strokeWidth="2.5"
          />
          <path
            d="M20 36 l-4 6 h8 z"
            fill={color}
            stroke="var(--ink)"
            strokeWidth="2"
            strokeLinejoin="round"
          />
          <path
            d="M20 42 q-6 8 2 16"
            stroke="var(--ink)"
            strokeWidth="2"
            fill="none"
          />
        </g>
      )}
    </svg>
  );
}

function BalloonWord({
  entry,
  isLast,
  onJudged,
  onNext,
}: {
  entry: BankEntry;
  isLast: boolean;
  onJudged: (correct: boolean) => void;
  onNext: () => void;
}) {
  const size = tileSizeForWord(entry.word);
  // Guesses are lowercase a–z; compare against the lowercased word so
  // capitalized entries like "February" stay winnable.
  const word = entry.word.toLowerCase();
  const [guessed, setGuessed] = useState<Set<string>>(new Set());
  const [misses, setMisses] = useState(0);
  const [outcome, setOutcome] = useState<boolean | null>(null);
  const [lastMiss, setLastMiss] = useState(false);

  const guess = (letter: string) => {
    if (outcome !== null || guessed.has(letter)) return;
    const nextGuessed = new Set(guessed).add(letter);
    setGuessed(nextGuessed);
    if (word.includes(letter)) {
      setLastMiss(false);
      if (word.split("").every((ch) => nextGuessed.has(ch))) {
        setOutcome(true);
        onJudged(true);
      }
    } else {
      setLastMiss(true);
      const nextMisses = misses + 1;
      setMisses(nextMisses);
      if (nextMisses >= MAX_MISSES) {
        setOutcome(false);
        onJudged(false);
      }
    }
  };

  // Physical keyboard support alongside the on-screen letters.
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.metaKey || e.ctrlKey || e.altKey) return;
      const letter = e.key.toLowerCase();
      if (letter.length === 1 && letter >= "a" && letter <= "z") {
        guess(letter);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  });

  const balloonsLeft = MAX_MISSES - misses;

  return (
    <div className="text-center">
      {entry.hint && (
        <p className="text-sm text-muted-foreground">Clue: {entry.hint}</p>
      )}

      {/* Balloons */}
      <div
        className="mt-4 flex justify-center gap-1"
        role="status"
        aria-label={`${balloonsLeft} of ${MAX_MISSES} balloons left`}
      >
        {BALLOON_COLORS.map((color, i) => (
          <span key={i} className={cn(i >= balloonsLeft && "wobble-in")}>
            <Balloon popped={i >= balloonsLeft} color={color} />
          </span>
        ))}
      </div>

      {/* Hidden word */}
      <div
        className={cn(
          "mt-4 flex flex-wrap justify-center gap-1.5",
          lastMiss && outcome === null && "shake",
        )}
      >
        {entry.word.split("").map((letter, i) => {
          const wasGuessed = guessed.has(letter.toLowerCase());
          const revealed = wasGuessed || outcome !== null;
          return (
            <Tile
              key={i}
              size={size}
              className={cn(
                !revealed && "border-dashed border-grape bg-grape-soft/50 shadow-none",
                revealed && wasGuessed && "bg-grape-soft wobble-in",
                revealed && !wasGuessed && "bg-coral-soft wobble-in",
                outcome === true && "bg-leaf-soft",
              )}
            >
              {revealed ? letter : ""}
            </Tile>
          );
        })}
      </div>

      {/* Letter keyboard */}
      {outcome === null && (
        <>
          <div className="mx-auto mt-6 flex max-w-md flex-wrap justify-center gap-1.5">
            {ALPHABET.map((letter) => {
              const used = guessed.has(letter);
              const hit = used && word.includes(letter);
              return (
                <TileButton
                  key={letter}
                  size="sm"
                  disabled={used}
                  onClick={() => guess(letter)}
                  className={cn(
                    used && hit && "bg-leaf-soft",
                    used && !hit && "bg-coral-soft",
                  )}
                  aria-label={`Guess letter ${letter}`}
                >
                  {letter}
                </TileButton>
              );
            })}
          </div>
          <div className="mt-6 flex justify-center">
            <Button
              variant="outline"
              className="font-heading"
              onClick={() => speak(entry.word)}
            >
              <Volume2 aria-hidden="true" /> Hear it
            </Button>
          </div>
        </>
      )}

      {outcome !== null && (
        <FeedbackPanel
          correct={outcome}
          word={entry.word}
          isLast={isLast}
          onNext={onNext}
        />
      )}
    </div>
  );
}
