"use client";

import { useState } from "react";
import { Cog } from "lucide-react";

import { FeedbackPanel, GameFrame } from "@/components/game-frame";
import { SpellingInput } from "@/components/spelling-input";
import { Tile, tileSizeForWord } from "@/components/tile";
import { Button } from "@/components/ui/button";
import { useGameRound, useGrade } from "@/hooks/use-spelling-round";
import { ENDING_TASKS, type EndingTask } from "@/lib/endings";
import { GAMES } from "@/lib/games";
import { cn } from "@/lib/utils";

const game = GAMES.find((g) => g.slug === "ending-machine")!;

export function EndingMachineGame() {
  const [grade, setGrade] = useGrade();
  const { state, record, advance, restart, roundId } = useGameRound(
    ENDING_TASKS[grade],
  );
  const task = state?.phase === "playing" ? state.words[state.index] : null;

  return (
    <GameFrame
      game={game}
      icon={<Cog className="h-7 w-7" aria-hidden="true" />}
      instructions="Add the ending to the word — watch for letters that double, drop, or change!"
      grade={grade}
      onGradeChange={setGrade}
      round={state}
      onRestart={restart}
    >
      {state && task && (
        <EndingWord
          key={`${roundId}-${state.index}-${grade}`}
          task={task}
          isLast={state.index + 1 === state.words.length}
          onJudged={record}
          onNext={advance}
        />
      )}
    </GameFrame>
  );
}

function EndingWord({
  task,
  isLast,
  onJudged,
  onNext,
}: {
  task: EndingTask;
  isLast: boolean;
  onJudged: (correct: boolean) => void;
  onNext: () => void;
}) {
  const [typed, setTyped] = useState("");
  const [outcome, setOutcome] = useState<boolean | null>(null);
  const [retrying, setRetrying] = useState(false);

  const submit = () => {
    if (outcome !== null) return;
    const attempt = typed.trim().toLowerCase();
    if (!attempt) return;
    if (attempt === task.word.toLowerCase() || task.also?.includes(attempt)) {
      setOutcome(true);
      onJudged(true);
    } else if (!retrying) {
      setRetrying(true);
      setTyped("");
    } else {
      setOutcome(false);
      onJudged(false);
    }
  };

  const size = tileSizeForWord(task.base + task.suffix);

  return (
    <div className="text-center">
      {/* The machine: base + suffix = ? */}
      <div className="flex flex-wrap items-center justify-center gap-2">
        <span className="flex flex-wrap justify-center gap-1">
          {task.base.split("").map((letter, i) => (
            <Tile key={i} size={size} className="bg-secondary">
              {letter}
            </Tile>
          ))}
        </span>
        <span className="font-heading text-2xl font-semibold text-muted-foreground">
          +
        </span>
        <span className="flex gap-1">
          {task.suffix.split("").map((letter, i) => (
            <Tile key={i} size={size} className="bg-sun-soft">
              {letter}
            </Tile>
          ))}
        </span>
        <span className="font-heading text-2xl font-semibold text-muted-foreground">
          =
        </span>
        <Tile size={size} className="bg-sun-soft font-semibold">
          ?
        </Tile>
      </div>

      {outcome === null && (
        <form
          className="mx-auto mt-6 max-w-sm"
          onSubmit={(e) => {
            e.preventDefault();
            submit();
          }}
        >
          <label htmlFor="ending-input" className="sr-only">
            Type {task.base} with the ending {task.suffix} added
          </label>
          <SpellingInput
            id="ending-input"
            value={typed}
            onChange={(e) => setTyped(e.target.value)}
            placeholder="What comes out?"
            autoFocus
          />
          {retrying && (
            <p
              className="font-heading mt-3 text-sm font-medium text-coral"
              role="status"
            >
              Not quite! Hint: {task.hint}
            </p>
          )}
          <Button
            type="submit"
            size="lg"
            className="font-heading mt-4"
            disabled={typed.trim().length === 0}
          >
            Crank the machine
          </Button>
        </form>
      )}

      {outcome !== null && (
        <>
          <p
            className={cn(
              "font-heading mx-auto mt-5 max-w-md rounded-xl p-3 text-sm font-medium",
              "bg-sun-soft text-foreground",
            )}
          >
            Rule: {task.hint}
          </p>
          <FeedbackPanel
            correct={outcome}
            word={task.word}
            isLast={isLast}
            onNext={onNext}
          />
        </>
      )}
    </div>
  );
}
