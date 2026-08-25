"use client";

import Link from "next/link";
import { ArrowLeft, Flame, RotateCcw, Star } from "lucide-react";
import type { ReactNode } from "react";

import { GradePicker } from "@/components/grade-picker";
import { Tile, tileSizeForWord } from "@/components/tile";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import type { RoundState } from "@/hooks/use-spelling-round";
import { starsForScore } from "@/lib/game-utils";
import { COLOR_STYLES, type GameInfo } from "@/lib/games";
import type { GradeBand } from "@/lib/words";
import { cn } from "@/lib/utils";

export function GameFrame({
  game,
  icon,
  instructions,
  grade,
  onGradeChange,
  round,
  onRestart,
  children,
}: {
  game: GameInfo;
  icon: ReactNode;
  instructions: string;
  grade: GradeBand;
  onGradeChange: (grade: GradeBand) => void;
  round: RoundState | null;
  onRestart: () => void;
  children: ReactNode;
}) {
  const colors = COLOR_STYLES[game.color];
  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-8 sm:px-6">
      <Link
        href="/#games"
        className="inline-flex items-center gap-1.5 font-heading text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        All games
      </Link>

      <div className="mt-4 flex items-center gap-4">
        <span className={cn("tile h-14 w-14", colors.soft)} aria-hidden="true">
          {icon}
        </span>
        <div>
          <h1 className="font-heading text-3xl font-semibold sm:text-4xl">
            {game.title}
          </h1>
          <p className="text-sm text-muted-foreground">{instructions}</p>
        </div>
      </div>

      <div className="mt-6">
        <GradePicker value={grade} onChange={onGradeChange} />
      </div>

      <div className="mt-6">
        {round === null ? (
          <Card className={cn("border-t-8", colors.borderT)}>
            <CardContent className="space-y-4 py-10">
              <Skeleton className="mx-auto h-6 w-48" />
              <Skeleton className="mx-auto h-14 w-64" />
              <Skeleton className="mx-auto h-14 w-72" />
            </CardContent>
          </Card>
        ) : round.phase === "done" ? (
          <RoundSummary round={round} game={game} onRestart={onRestart} />
        ) : (
          <>
            <ScoreBar round={round} />
            <Card className={cn("mt-3 border-t-8", colors.borderT)}>
              <CardContent className="py-6">{children}</CardContent>
            </Card>
          </>
        )}
      </div>
    </div>
  );
}

function ScoreBar({ round }: { round: RoundState }) {
  return (
    <div className="flex items-center gap-4">
      <div className="flex-1">
        <div className="flex items-baseline justify-between font-heading text-sm font-medium text-muted-foreground">
          <span>
            Word {round.index + 1} of {round.words.length}
          </span>
          <span className="flex items-center gap-3">
            {round.streak >= 2 && (
              <span className="flex items-center gap-1 text-coral">
                <Flame className="h-4 w-4" aria-hidden="true" />
                {round.streak} in a row!
              </span>
            )}
            <span className="flex items-center gap-1 text-foreground">
              <Star className="h-4 w-4 fill-sun text-sun" aria-hidden="true" />
              {round.score}
            </span>
          </span>
        </div>
        <Progress
          className="mt-1.5 h-3"
          value={(round.index / round.words.length) * 100}
        />
      </div>
    </div>
  );
}

const SUMMARY_HEADLINES = [
  "Keep practicing — you'll get there!",
  "Good effort — try for more stars!",
  "Nice spelling! One more round?",
  "Wow! Spelling superstar!",
];

function RoundSummary({
  round,
  game,
  onRestart,
}: {
  round: RoundState;
  game: GameInfo;
  onRestart: () => void;
}) {
  const stars = starsForScore(round.score, round.words.length);
  const missed = round.words.filter((_, i) => round.results[i] === false);
  const colors = COLOR_STYLES[game.color];
  return (
    <Card className={cn("border-t-8 text-center", colors.borderT)}>
      <CardContent className="py-8">
        <div className="flex justify-center gap-2" aria-hidden="true">
          {[0, 1, 2].map((i) => (
            <Tile
              key={i}
              size="lg"
              className={cn(
                "wobble-in",
                i === 1 ? "" : i === 0 ? "-rotate-6" : "rotate-6",
                i < stars ? "bg-sun-soft" : "bg-muted opacity-60",
              )}
            >
              <Star
                className={cn(
                  "h-7 w-7",
                  i < stars ? "fill-sun text-sun" : "text-muted-foreground",
                )}
              />
            </Tile>
          ))}
        </div>
        <p className="sr-only">{stars} out of 3 stars</p>
        <h2 className="font-heading mt-4 text-2xl font-semibold">
          {SUMMARY_HEADLINES[stars]}
        </h2>
        <p className="mt-1 text-muted-foreground">
          You spelled {round.score} of {round.words.length} words right
          {round.bestStreak >= 3 && (
            <> — best streak: {round.bestStreak} in a row</>
          )}
          .
        </p>

        {missed.length > 0 && (
          <div className="mx-auto mt-6 max-w-md rounded-xl bg-secondary p-4 text-left">
            <h3 className="font-heading text-sm font-medium text-muted-foreground">
              Words to practice
            </h3>
            <ul className="mt-2 flex flex-wrap gap-2">
              {missed.map((entry) => (
                <li
                  key={entry.word}
                  className="font-heading rounded-lg border-2 border-ink bg-card px-3 py-1 text-sm font-medium"
                >
                  {entry.word}
                </li>
              ))}
            </ul>
          </div>
        )}

        <div className="mt-8 flex flex-wrap justify-center gap-3">
          <Button size="lg" onClick={onRestart} className="font-heading">
            <RotateCcw aria-hidden="true" />
            Play again
          </Button>
          <Button
            size="lg"
            variant="outline"
            className="font-heading"
            nativeButton={false}
            render={<Link href="/#games" />}
          >
            Try another game
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export function FeedbackPanel({
  correct,
  word,
  isLast,
  onNext,
}: {
  correct: boolean;
  word: string;
  isLast: boolean;
  onNext: () => void;
}) {
  return (
    <div
      className={cn(
        "mt-6 rounded-xl p-4 text-center",
        correct ? "bg-leaf-soft" : "bg-coral-soft",
      )}
      role="status"
    >
      {correct ? (
        <p className="font-heading pop text-lg font-semibold text-foreground">
          Nailed it! ⭐
        </p>
      ) : (
        <div>
          <p className="font-heading text-lg font-semibold text-foreground">
            Almost! It&apos;s spelled:
          </p>
          <div className="mt-3 flex flex-wrap justify-center gap-1.5">
            {word.split("").map((letter, i) => (
              <Tile key={i} size={tileSizeForWord(word)} className="bg-card">
                {letter}
              </Tile>
            ))}
          </div>
        </div>
      )}
      <Button size="lg" className="font-heading mt-4" onClick={onNext} autoFocus>
        {isLast ? "See my score" : "Next word"}
      </Button>
    </div>
  );
}
