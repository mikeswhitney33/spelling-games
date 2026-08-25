"use client";

import { useEffect, useRef, useState } from "react";
import { Eye, Lightbulb, Volume2 } from "lucide-react";

import { FeedbackPanel, GameFrame } from "@/components/game-frame";
import { Tile, tileSizeForWord } from "@/components/tile";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useGrade, useSpellingRound } from "@/hooks/use-spelling-round";
import { speak } from "@/lib/game-utils";
import { GAMES } from "@/lib/games";
import type { GradeBand, WordEntry } from "@/lib/words";

const game = GAMES.find((g) => g.slug === "flash-spell")!;

// How long the word stays on screen before hiding (first look / re-look).
const SHOW_MS: Record<GradeBand, number> = {
  "k-1": 4000,
  "2-3": 3500,
  "4-5": 3000,
  "6-plus": 3000,
};
const RESHOW_MS = 2000;

export function FlashSpellGame() {
  const [grade, setGrade] = useGrade();
  const { state, record, advance, restart, roundId } = useSpellingRound(grade);
  const entry = state?.phase === "playing" ? state.words[state.index] : null;

  return (
    <GameFrame
      game={game}
      icon={<Eye className="h-7 w-7" aria-hidden="true" />}
      instructions="Look closely while the word is showing — then spell it from memory."
      grade={grade}
      onGradeChange={setGrade}
      round={state}
      onRestart={restart}
    >
      {state && entry && (
        <FlashWord
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

function FlashWord({
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
  const [phase, setPhase] = useState<"show" | "type">("show");
  const [typed, setTyped] = useState("");
  const [outcome, setOutcome] = useState<boolean | null>(null);
  const [retrying, setRetrying] = useState(false);
  const [showHint, setShowHint] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (phase !== "show") return;
    const ms = retrying ? RESHOW_MS : SHOW_MS[grade];
    const timer = window.setTimeout(() => setPhase("type"), ms);
    return () => window.clearTimeout(timer);
  }, [phase, retrying, grade]);

  useEffect(() => {
    if (phase === "type") inputRef.current?.focus();
  }, [phase]);

  const submit = () => {
    if (outcome !== null || phase !== "type") return;
    const attempt = typed.trim().toLowerCase();
    if (!attempt) return;
    if (attempt === entry.word.toLowerCase()) {
      setOutcome(true);
      onJudged(true);
    } else if (!retrying) {
      // One more look, then one more try.
      setRetrying(true);
      setTyped("");
      setPhase("show");
    } else {
      setOutcome(false);
      onJudged(false);
    }
  };

  return (
    <div className="text-center">
      <p className="text-sm text-muted-foreground">Clue: {entry.hint}</p>

      {phase === "show" && (
        <>
          <div className="mt-5 flex flex-wrap justify-center gap-1.5">
            {entry.word.split("").map((letter, i) => (
              <Tile key={i} size={size} className="wobble-in bg-coral-soft">
                {letter}
              </Tile>
            ))}
          </div>
          <p
            className="font-heading mt-4 text-sm font-medium text-muted-foreground"
            role="status"
          >
            {retrying
              ? "One more look — you've got this!"
              : "Look closely… it's about to hide!"}
          </p>
        </>
      )}

      {phase === "type" && outcome === null && (
        <form
          className="mx-auto mt-5 max-w-sm"
          onSubmit={(e) => {
            e.preventDefault();
            submit();
          }}
        >
          <div className="flex justify-center gap-1.5" aria-hidden="true">
            {entry.word.split("").map((_, i) => (
              <Tile
                key={i}
                size={size}
                className="border-dashed opacity-30 shadow-none"
              />
            ))}
          </div>
          <label htmlFor="flash-input" className="sr-only">
            Type the word you saw
          </label>
          <Input
            id="flash-input"
            ref={inputRef}
            value={typed}
            onChange={(e) => setTyped(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                submit();
              }
            }}
            placeholder="Type it from memory…"
            autoComplete="off"
            autoCapitalize="off"
            autoCorrect="off"
            spellCheck={false}
            className="font-heading mt-4 h-14 border-[3px] border-ink text-center !text-2xl lowercase tracking-wide shadow-[0_4px_0_var(--ink)]"
          />
          {retrying && (
            <p
              className="font-heading mt-3 text-sm font-medium text-coral"
              role="status"
            >
              Not quite — try once more!
            </p>
          )}
          <div className="mt-4 flex flex-wrap justify-center gap-2">
            <Button
              type="submit"
              size="lg"
              className="font-heading"
              disabled={typed.trim().length === 0}
            >
              Check my spelling
            </Button>
            <Button
              type="button"
              variant="outline"
              size="lg"
              className="font-heading"
              onClick={() => speak(entry.word)}
            >
              <Volume2 aria-hidden="true" /> Hear it
            </Button>
            <Button
              type="button"
              variant="outline"
              size="lg"
              className="font-heading"
              onClick={() => setShowHint(true)}
              disabled={showHint}
            >
              <Lightbulb aria-hidden="true" /> Clue
            </Button>
          </div>
        </form>
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
