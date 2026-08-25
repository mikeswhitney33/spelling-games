"use client";

import { useMemo, useState } from "react";
import { Puzzle, Volume2 } from "lucide-react";

import { FeedbackPanel, GameFrame } from "@/components/game-frame";
import { Tile, TileButton, tileSizeForWord } from "@/components/tile";
import { Button } from "@/components/ui/button";
import { useGrade, useSpellingRound } from "@/hooks/use-spelling-round";
import { pickBlankPositions, pickN, shuffle, speak } from "@/lib/game-utils";
import { GAMES } from "@/lib/games";
import type { GradeBand, WordEntry } from "@/lib/words";
import { cn } from "@/lib/utils";

const game = GAMES.find((g) => g.slug === "missing-letters")!;

const BLANKS_PER_GRADE: Record<GradeBand, number> = {
  "k-1": 1,
  "2-3": 2,
  "4-5": 3,
  "6-plus": 4,
};

export function MissingLettersGame() {
  const [grade, setGrade] = useGrade();
  const { state, record, advance, restart, roundId } = useSpellingRound(grade);
  const entry = state?.phase === "playing" ? state.words[state.index] : null;

  return (
    <GameFrame
      game={game}
      icon={<Puzzle className="h-7 w-7" aria-hidden="true" />}
      instructions="Some letters are missing. Tap letters from the bank to finish the word."
      grade={grade}
      onGradeChange={setGrade}
      round={state}
      onRestart={restart}
    >
      {state && entry && (
        <MissingLettersWord
          key={`${roundId}-${state.index}-${grade}`}
          entry={entry}
          grade={grade}
          isLast={state.index + 1 === state.words.length}
          onJudged={record}
          onNext={advance}
        />
      )}
    </GameFrame>
  );
}

function MissingLettersWord({
  entry,
  grade,
  isLast,
  onJudged,
  onNext,
}: {
  entry: WordEntry;
  grade: GradeBand;
  isLast: boolean;
  onJudged: (correct: boolean) => void;
  onNext: () => void;
}) {
  const size = tileSizeForWord(entry.word);
  const setup = useMemo(() => {
    const positions = pickBlankPositions(entry.word, BLANKS_PER_GRADE[grade]);
    const needed = positions.map((p) => entry.word[p]);
    const distractors = pickN(
      "abcdefghijklmnopqrstuvwxyz".split("").filter((c) => !needed.includes(c)),
      3,
    );
    return { positions, bank: shuffle([...needed, ...distractors]) };
  }, [entry.word, grade]);

  // For each blank, the index into the bank of the letter placed there.
  const [placed, setPlaced] = useState<(number | null)[]>(() =>
    setup.positions.map(() => null),
  );
  const [outcome, setOutcome] = useState<boolean | null>(null);
  const [retrying, setRetrying] = useState(false);
  const [shaking, setShaking] = useState(false);

  const pickFromBank = (bankIndex: number) => {
    if (outcome !== null || shaking || placed.includes(bankIndex)) return;
    const firstEmpty = placed.indexOf(null);
    if (firstEmpty === -1) return;
    const next = [...placed];
    next[firstEmpty] = bankIndex;
    if (next.includes(null)) {
      setPlaced(next);
      return;
    }
    // All blanks filled — judge.
    const correct = setup.positions.every(
      (pos, i) => setup.bank[next[i]!] === entry.word[pos],
    );
    setPlaced(next);
    if (correct) {
      setOutcome(true);
      onJudged(true);
    } else if (!retrying) {
      setShaking(true);
      window.setTimeout(() => {
        setShaking(false);
        setPlaced(setup.positions.map(() => null));
        setRetrying(true);
      }, 650);
    } else {
      setOutcome(false);
      onJudged(false);
    }
  };

  const clearBlank = (blankIndex: number) => {
    if (outcome !== null || shaking) return;
    const next = [...placed];
    next[blankIndex] = null;
    setPlaced(next);
  };

  return (
    <div className="text-center">
      <p className="text-sm text-muted-foreground">Clue: {entry.hint}</p>

      {/* The word with gaps */}
      <div
        className={cn(
          "mt-5 flex flex-wrap justify-center gap-1.5",
          shaking && "shake",
        )}
      >
        {entry.word.split("").map((letter, pos) => {
          const blankIndex = setup.positions.indexOf(pos);
          if (blankIndex === -1) {
            return (
              <Tile key={pos} size={size} className="bg-secondary">
                {letter}
              </Tile>
            );
          }
          const bankIndex = placed[blankIndex];
          return bankIndex === null ? (
            <Tile
              key={pos}
              size={size}
              className="border-dashed border-sun bg-sun-soft/50 shadow-none"
            />
          ) : (
            <TileButton
              key={pos}
              size={size}
              className={cn(
                "wobble-in bg-sun-soft",
                outcome === true && "bg-leaf-soft",
                shaking && "bg-coral-soft",
              )}
              onClick={() => clearBlank(blankIndex)}
              aria-label={`Remove letter ${setup.bank[bankIndex]}`}
            >
              {setup.bank[bankIndex]}
            </TileButton>
          );
        })}
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
            {setup.bank.map((letter, i) => (
              <TileButton
                key={i}
                size="md"
                disabled={placed.includes(i) || shaking}
                onClick={() => pickFromBank(i)}
                aria-label={`Pick letter ${letter}`}
              >
                {letter}
              </TileButton>
            ))}
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
