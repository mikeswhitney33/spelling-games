"use client";

import { useEffect, useState } from "react";
import { Lightbulb, Volume2 } from "lucide-react";

import { BankPicker, NotEnoughWords } from "@/components/bank-picker";
import { FeedbackPanel, GameFrame } from "@/components/game-frame";
import { SpellingInput } from "@/components/spelling-input";
import { Button } from "@/components/ui/button";
import { useWordBank } from "@/hooks/use-bank";
import { useSpeechSupported } from "@/hooks/use-speech-supported";
import { useGameRound } from "@/hooks/use-spelling-round";
import type { BankEntry } from "@/lib/banks";
import { speak } from "@/lib/game-utils";
import { GAMES } from "@/lib/games";

const game = GAMES.find((g) => g.slug === "listen-and-spell")!;

export function ListenAndSpellGame() {
  const { bank, banks, setActive } = useWordBank();
  const { state, record, advance, restart, roundId } = useGameRound(bank.entries);
  const entry = state?.phase === "playing" ? state.words[state.index] : null;

  return (
    <GameFrame
      game={game}
      icon={<Volume2 className="h-7 w-7" aria-hidden="true" />}
      instructions="Press the speaker, listen closely, then type the word you hear."
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
        <ListenWord
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

function ListenWord({
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
  const [typed, setTyped] = useState("");
  const [outcome, setOutcome] = useState<boolean | null>(null);
  const [retrying, setRetrying] = useState(false);
  const [showHint, setShowHint] = useState(false);
  const soundWorks = useSpeechSupported();

  useEffect(() => {
    if (!soundWorks) setShowHint(true);
  }, [soundWorks]);

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
          <SpellingInput
            id="spelling-input"
            value={typed}
            onChange={(e) => setTyped(e.target.value)}
            placeholder="Type the word…"
            autoFocus
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
            {entry.hint && (
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
            )}
          </div>
          {showHint && entry.hint && (
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
