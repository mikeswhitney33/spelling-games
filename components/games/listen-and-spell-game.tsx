"use client";

import { useEffect, useState } from "react";
import { Lightbulb, Volume2 } from "lucide-react";

import { FeedbackPanel, GameFrame } from "@/components/game-frame";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useGrade, useSpellingRound } from "@/hooks/use-spelling-round";
import { speak } from "@/lib/game-utils";
import { GAMES } from "@/lib/games";
import type { WordEntry } from "@/lib/words";

const game = GAMES.find((g) => g.slug === "listen-and-spell")!;

export function ListenAndSpellGame() {
  const [grade, setGrade] = useGrade();
  const { state, record, advance, restart, roundId } = useSpellingRound(grade);
  const entry = state?.phase === "playing" ? state.words[state.index] : null;

  return (
    <GameFrame
      game={game}
      icon={<Volume2 className="h-7 w-7" aria-hidden="true" />}
      instructions="Press the speaker, listen closely, then type the word you hear."
      grade={grade}
      onGradeChange={setGrade}
      round={state}
      onRestart={restart}
    >
      {state && entry && (
        <ListenWord
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

function ListenWord({
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
  const [typed, setTyped] = useState("");
  const [outcome, setOutcome] = useState<boolean | null>(null);
  const [retrying, setRetrying] = useState(false);
  const [showHint, setShowHint] = useState(false);
  const [soundWorks, setSoundWorks] = useState(true);

  useEffect(() => {
    const supported =
      typeof window !== "undefined" && "speechSynthesis" in window;
    setSoundWorks(supported);
    if (!supported) setShowHint(true);
  }, []);

  const submit = () => {
    if (outcome !== null) return;
    const attempt = typed.trim().toLowerCase();
    if (!attempt) return;
    if (attempt === entry.word.toLowerCase()) {
      setOutcome(true);
      onJudged(true);
    } else if (!retrying) {
      setRetrying(true);
      setTyped("");
      speak(entry.word);
    } else {
      setOutcome(false);
      onJudged(false);
    }
  };

  return (
    <div className="text-center">
      <button
        type="button"
        onClick={() => speak(entry.word)}
        disabled={!soundWorks}
        className="tile tile-press h-24 w-24 cursor-pointer bg-sky-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:cursor-default disabled:opacity-40"
        aria-label="Play the word out loud"
      >
        <Volume2 className="h-12 w-12" aria-hidden="true" />
      </button>
      <p className="mt-2 text-sm text-muted-foreground">
        {soundWorks
          ? "Tap the speaker to hear your word."
          : "Sound isn't available in this browser — use the clue instead."}
      </p>

      {outcome === null && (
        <form
          className="mx-auto mt-6 max-w-sm"
          onSubmit={(e) => {
            e.preventDefault();
            submit();
          }}
        >
          <label htmlFor="spelling-input" className="sr-only">
            Type the word you heard
          </label>
          <Input
            id="spelling-input"
            value={typed}
            onChange={(e) => setTyped(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                submit();
              }
            }}
            placeholder="Type the word…"
            autoComplete="off"
            autoCapitalize="off"
            autoCorrect="off"
            spellCheck={false}
            autoFocus
            className="font-heading h-14 border-[3px] border-ink text-center !text-2xl lowercase tracking-wide shadow-[0_4px_0_var(--ink)]"
          />
          {retrying && (
            <p className="font-heading mt-3 text-sm font-medium text-coral" role="status">
              Not quite — listen again and give it one more try!
            </p>
          )}
          <div className="mt-4 flex justify-center gap-2">
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
              onClick={() => setShowHint(true)}
              disabled={showHint}
            >
              <Lightbulb aria-hidden="true" /> Clue
            </Button>
          </div>
          {showHint && (
            <p className="mt-4 text-sm text-muted-foreground" role="status">
              Clue: {entry.hint}
            </p>
          )}
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
