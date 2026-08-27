"use client";

import { useMemo, useState } from "react";
import { Check, Search, X } from "lucide-react";

import { BankPicker, NotEnoughWords } from "@/components/bank-picker";
import { FeedbackPanel, GameFrame } from "@/components/game-frame";
import { useWordBank } from "@/hooks/use-bank";
import { useGameRound } from "@/hooks/use-spelling-round";
import { ALL_BUILT_IN_WORDS, type BankEntry } from "@/lib/banks";
import { makeMisspellings, matchCase, shuffle } from "@/lib/game-utils";
import { GAMES } from "@/lib/games";
import { cn } from "@/lib/utils";

const game = GAMES.find((g) => g.slug === "spot-the-word")!;

export function SpotTheWordGame() {
  const { bank, banks, setActive } = useWordBank();
  const { state, record, advance, restart, roundId } = useGameRound(bank.entries);
  const entry = state?.phase === "playing" ? state.words[state.index] : null;

  return (
    <GameFrame
      game={game}
      icon={<Search className="h-7 w-7" aria-hidden="true" />}
      instructions="Read the clue, then tap the one spelling that's really right."
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
        <SpotWord
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

export function SpotWord({
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
  const options = useMemo(() => {
    // Generate from the lowercased word so the case-keyed swap tables apply
    // to every letter, then restore a leading capital ("February") so the
    // real answer's casing doesn't give it away. Custom words fall back to
    // the cautious rules the dictionary guard can't vet.
    const cautious = !ALL_BUILT_IN_WORDS.has(entry.word.toLowerCase());
    const fakes = makeMisspellings(
      entry.word.toLowerCase(),
      3,
      Math.random,
      ALL_BUILT_IN_WORDS,
      cautious,
    ).map((fake) => matchCase(entry.word, fake));
    return shuffle([entry.word, ...fakes]);
  }, [entry.word]);
  const [chosen, setChosen] = useState<string | null>(null);

  const choose = (option: string) => {
    if (chosen !== null) return;
    setChosen(option);
    onJudged(option === entry.word);
  };

  return (
    <div className="text-center">
      {entry.hint && (
        <p className="text-sm text-muted-foreground">Clue: {entry.hint}</p>
      )}

      <div className="mx-auto mt-6 grid max-w-md grid-cols-1 gap-3 sm:grid-cols-2">
        {options.map((option) => {
          const isReal = option === entry.word;
          const isPicked = option === chosen;
          const revealed = chosen !== null;
          return (
            <button
              key={option}
              type="button"
              onClick={() => choose(option)}
              disabled={revealed}
              className={cn(
                "font-heading rounded-xl border-[3px] border-ink bg-card px-4 py-4 text-xl font-medium tracking-wide transition-all",
                !revealed &&
                  "cursor-pointer shadow-[0_4px_0_var(--ink)] hover:-translate-y-0.5 hover:shadow-[0_6px_0_var(--ink)] active:translate-y-1 active:shadow-[0_1px_0_var(--ink)]",
                "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring",
                revealed && isReal && "bg-leaf-soft",
                revealed && isPicked && !isReal && "bg-coral-soft shake",
                revealed && !isPicked && !isReal && "opacity-40",
              )}
            >
              <span className="inline-flex items-center gap-2">
                {revealed && isReal && (
                  <Check className="h-5 w-5 text-leaf" aria-hidden="true" />
                )}
                {revealed && isPicked && !isReal && (
                  <X className="h-5 w-5 text-coral" aria-hidden="true" />
                )}
                {option}
              </span>
            </button>
          );
        })}
      </div>

      {chosen !== null && (
        <FeedbackPanel
          correct={chosen === entry.word}
          word={entry.word}
          isLast={isLast}
          onNext={onNext}
        />
      )}
    </div>
  );
}
