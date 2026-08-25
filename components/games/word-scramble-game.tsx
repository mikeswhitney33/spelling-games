"use client";

import { useMemo, useState } from "react";
import { Eraser, Shuffle, Volume2 } from "lucide-react";

import { FeedbackPanel, GameFrame } from "@/components/game-frame";
import { Tile, TileButton, tileSizeForWord } from "@/components/tile";
import { Button } from "@/components/ui/button";
import { useGrade, useSpellingRound } from "@/hooks/use-spelling-round";
import { scrambleWord, speak } from "@/lib/game-utils";
import { GAMES } from "@/lib/games";
import type { WordEntry } from "@/lib/words";
import { cn } from "@/lib/utils";

const game = GAMES.find((g) => g.slug === "word-scramble")!;

export function WordScrambleGame() {
  const [grade, setGrade] = useGrade();
  const { state, record, advance, restart, roundId } = useSpellingRound(grade);
  const entry = state?.phase === "playing" ? state.words[state.index] : null;

  return (
    <GameFrame
      game={game}
      icon={<Shuffle className="h-7 w-7" aria-hidden="true" />}
      instructions="Tap the tiles in the right order to unscramble the word."
      grade={grade}
      onGradeChange={setGrade}
      round={state}
      onRestart={restart}
    >
      {state && entry && (
        <ScrambleWord
          key={`${roundId}-${state.index}-${grade}`}
          entry={entry}
          isLast={state.index + 1 === state.words.length}
          onJudged={record}
          onNext={advance}
        />
      )}
    </GameFrame>
  );
}

function ScrambleWord({
  entry,
  isLast,
  onJudged,
  onNext,
}: {
  entry: WordEntry;
  isLast: boolean;
  onJudged: (correct: boolean) => void;
  onNext: () => void;
}) {
  const size = tileSizeForWord(entry.word);
  const letters = useMemo(
    () => scrambleWord(entry.word).split(""),
    [entry.word],
  );
  const [picked, setPicked] = useState<number[]>([]);
  const [outcome, setOutcome] = useState<boolean | null>(null);
  const [retrying, setRetrying] = useState(false);
  const [shaking, setShaking] = useState(false);

  const pickTile = (index: number) => {
    if (outcome !== null || picked.includes(index)) return;
    const next = [...picked, index];
    if (next.length < letters.length) {
      setPicked(next);
      setRetrying(false);
      return;
    }
    // Last tile placed — judge the attempt.
    const attempt = next.map((i) => letters[i]).join("");
    if (attempt === entry.word) {
      setPicked(next);
      setOutcome(true);
      onJudged(true);
    } else if (!retrying) {
      setPicked(next);
      setShaking(true);
      window.setTimeout(() => {
        setShaking(false);
        setPicked([]);
        setRetrying(true);
      }, 650);
    } else {
      setPicked(next);
      setOutcome(false);
      onJudged(false);
    }
  };

  const unpickTile = (position: number) => {
    if (outcome !== null || shaking) return;
    setPicked(picked.filter((_, i) => i !== position));
  };

  return (
    <div className="text-center">
      <p className="text-sm text-muted-foreground">Clue: {entry.hint}</p>

      {/* Answer slots */}
      <div
        className={cn(
          "mt-5 flex min-h-14 flex-wrap justify-center gap-1.5",
          shaking && "shake",
        )}
        aria-label="Your answer"
      >
        {Array.from({ length: letters.length }, (_, i) =>
          i < picked.length ? (
            <TileButton
              key={i}
              size={size}
              className={cn(
                "wobble-in",
                outcome === true && "bg-leaf-soft",
                shaking && "bg-coral-soft",
              )}
              onClick={() => unpickTile(i)}
              aria-label={`Remove letter ${letters[picked[i]]}`}
            >
              {letters[picked[i]]}
            </TileButton>
          ) : (
            <Tile
              key={i}
              size={size}
              className="border-dashed opacity-30 shadow-none"
            />
          ),
        )}
      </div>

      {retrying && outcome === null && (
        <p className="font-heading mt-3 text-sm font-medium text-coral" role="status">
          Not quite — try again!
        </p>
      )}

      {/* Letter bank */}
      {outcome === null && (
        <>
          <div className="mt-6 flex flex-wrap justify-center gap-1.5">
            {letters.map((letter, i) => (
              <TileButton
                key={i}
                size={size}
                className="bg-coral-soft"
                disabled={picked.includes(i) || shaking}
                onClick={() => pickTile(i)}
                aria-label={`Pick letter ${letter}`}
              >
                {letter}
              </TileButton>
            ))}
          </div>
          <div className="mt-6 flex justify-center gap-2">
            <Button
              variant="outline"
              className="font-heading"
              onClick={() => speak(entry.word)}
            >
              <Volume2 aria-hidden="true" /> Hear it
            </Button>
            <Button
              variant="outline"
              className="font-heading"
              onClick={() => setPicked([])}
              disabled={picked.length === 0 || shaking}
            >
              <Eraser aria-hidden="true" /> Clear
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
