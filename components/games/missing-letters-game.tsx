"use client";

import { useMemo, useState } from "react";
import { Puzzle, Volume2 } from "lucide-react";

import { BankPicker, NotEnoughWords } from "@/components/bank-picker";
import { FeedbackPanel, GameFrame } from "@/components/game-frame";
import { Tile, TileButton, TileRow } from "@/components/tile";
import { Button } from "@/components/ui/button";
import { useWordBank } from "@/hooks/use-bank";
import { useGameRound } from "@/hooks/use-spelling-round";
import type { BankEntry } from "@/lib/banks";
import {
  blanksForWord,
  pickBlankPositions,
  pickN,
  shuffle,
  speak,
} from "@/lib/game-utils";
import { GAMES } from "@/lib/games";
import { cn } from "@/lib/utils";

const game = GAMES.find((g) => g.slug === "missing-letters")!;

export function MissingLettersGame() {
  const { bank, banks, setActive } = useWordBank();
  const pool = useMemo(
    () => bank.entries.filter((e) => e.word.length >= 3),
    [bank],
  );
  const { state, record, advance, restart, roundId } = useGameRound(pool);
  const entry = state?.phase === "playing" ? state.words[state.index] : null;

  return (
    <GameFrame
      game={game}
      icon={<Puzzle className="h-7 w-7" aria-hidden="true" />}
      instructions="Some letters are missing. Tap letters from the bank to finish the word."
      picker={<BankPicker bank={bank} banks={banks} onChange={setActive} />}
      notice={
        pool.length < 4 ? (
          <NotEnoughWords need={4} requirement="words of three or more letters" />
        ) : undefined
      }
      round={state}
      onRestart={restart}
    >
      {state && entry && (
        <MissingLettersWord
          key={`${roundId}-${state.index}-${bank.id}`}
          entry={entry}
          blanks={blanksForWord(entry.word)}
          isLast={state.index + 1 === state.words.length}
          onJudged={record}
          onNext={advance}
        />
      )}
    </GameFrame>
  );
}

export function MissingLettersWord({
  entry,
  blanks,
  isLast,
  onJudged,
  onNext,
}: {
  entry: BankEntry;
  blanks: number;
  isLast: boolean;
  onJudged: (correct: boolean) => void;
  onNext: () => void;
}) {
  const setup = useMemo(() => {
    const positions = pickBlankPositions(entry.word, blanks);
    const needed = positions.map((p) => entry.word[p]);
    // Compare lowercased so a needed capital ("F" in February) can't draw
    // its lowercase twin as a distractor.
    const neededLower = needed.map((l) => l.toLowerCase());
    const distractors = pickN(
      "abcdefghijklmnopqrstuvwxyz".split("").filter((c) => !neededLower.includes(c)),
      3,
    );
    return { positions, bank: shuffle([...needed, ...distractors]) };
  }, [entry.word, blanks]);

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
      {entry.hint && (
        <p className="text-sm text-muted-foreground">Clue: {entry.hint}</p>
      )}

      {/* The word with gaps */}
      <TileRow className={cn("mt-5", shaking && "shake")}>
        {entry.word.split("").map((letter, pos) => {
          const blankIndex = setup.positions.indexOf(pos);
          if (blankIndex === -1) {
            return (
              <Tile key={pos} className="bg-secondary">
                {letter}
              </Tile>
            );
          }
          const bankIndex = placed[blankIndex];
          return bankIndex === null ? (
            <Tile
              key={pos}
              className="border-dashed border-sun bg-sun-soft/50 shadow-none"
            />
          ) : (
            <TileButton
              key={pos}
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
      </TileRow>

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
