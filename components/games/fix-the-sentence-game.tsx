"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Pencil } from "lucide-react";

import { BankPicker, NotEnoughWords } from "@/components/bank-picker";
import { FeedbackPanel, GameFrame } from "@/components/game-frame";
import { SpellingInput } from "@/components/spelling-input";
import { Button } from "@/components/ui/button";
import { useWordBank } from "@/hooks/use-bank";
import { useGameRound } from "@/hooks/use-spelling-round";
import { ALL_BUILT_IN_WORDS, type BankEntry } from "@/lib/banks";
import { makeMisspellings, matchCase } from "@/lib/game-utils";
import { GAMES } from "@/lib/games";
import { cn } from "@/lib/utils";

const game = GAMES.find((g) => g.slug === "fix-the-sentence")!;

interface Token {
  /** Punctuation before the word, the word itself, punctuation after. */
  prefix: string;
  core: string;
  suffix: string;
  isTarget: boolean;
}

function tokenize(sentence: string, word: string): Token[] {
  let targetFound = false;
  return sentence.split(" ").map((raw) => {
    const match = raw.match(/^([^A-Za-z']*)([A-Za-z'][A-Za-z']*)([^A-Za-z']*)$/);
    const [prefix, core, suffix] = match
      ? [match[1], match[2], match[3]]
      : ["", raw, ""];
    const isTarget =
      !targetFound && core.toLowerCase() === word.toLowerCase();
    if (isTarget) targetFound = true;
    return { prefix, core, suffix, isTarget };
  });
}

export function FixTheSentenceGame() {
  const { bank, banks, setActive } = useWordBank();
  const pool = useMemo(
    () => bank.entries.filter((e) => e.sentence),
    [bank],
  );
  const { state, record, advance, restart, roundId } = useGameRound(pool);
  const entry = state?.phase === "playing" ? state.words[state.index] : null;

  return (
    <GameFrame
      game={game}
      icon={<Pencil className="h-7 w-7" aria-hidden="true" />}
      instructions="One word is spelled wrong. Tap it, then type the correct spelling."
      picker={<BankPicker bank={bank} banks={banks} onChange={setActive} />}
      notice={
        pool.length < 4 ? (
          <NotEnoughWords need={4} requirement="words with sentences" />
        ) : undefined
      }
      round={state}
      onRestart={restart}
    >
      {state && entry && (
        <FixSentenceWord
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

function FixSentenceWord({
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
  const tokens = useMemo(() => {
    const parsed = tokenize(entry.sentence ?? "", entry.word);
    return parsed.map((token) => {
      if (!token.isTarget) return { ...token, shown: token.core };
      const cautious = !ALL_BUILT_IN_WORDS.has(entry.word.toLowerCase());
      const fake =
        makeMisspellings(
          entry.word.toLowerCase(),
          1,
          Math.random,
          ALL_BUILT_IN_WORDS,
          cautious,
        )[0] ?? entry.word.toLowerCase() + entry.word.slice(-1).toLowerCase();
      return { ...token, shown: matchCase(token.core, fake) };
    });
  }, [entry.sentence, entry.word]);

  const [stage, setStage] = useState<"find" | "fix">("find");
  const [typed, setTyped] = useState("");
  const [outcome, setOutcome] = useState<boolean | null>(null);
  const [retrying, setRetrying] = useState(false);
  const [findMisses, setFindMisses] = useState(0);
  const [shakingIndex, setShakingIndex] = useState<number | null>(null);
  const [lastMissWord, setLastMissWord] = useState<string | null>(null);
  const shakeTimer = useRef<number | undefined>(undefined);

  // Safety net: if the sentence data ever fails to contain the target word,
  // skip the word (with credit) instead of soft-locking the round.
  const hasTarget = tokens.some((t) => t.isTarget);
  const skipped = useRef(false);
  useEffect(() => {
    if (!hasTarget && !skipped.current) {
      skipped.current = true;
      onJudged(true);
      onNext();
    }
  }, [hasTarget, onJudged, onNext]);

  useEffect(() => () => window.clearTimeout(shakeTimer.current), []);

  const tapWord = (index: number) => {
    if (stage !== "find") return;
    if (tokens[index].isTarget) {
      setStage("fix");
    } else {
      window.clearTimeout(shakeTimer.current);
      setShakingIndex(index);
      setLastMissWord(tokens[index].shown);
      setFindMisses((n) => n + 1);
      shakeTimer.current = window.setTimeout(() => setShakingIndex(null), 400);
    }
  };

  const submit = () => {
    if (outcome !== null || stage !== "fix") return;
    const attempt = typed.trim().toLowerCase();
    if (!attempt) return;
    if (attempt === entry.word.toLowerCase()) {
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

  return (
    <div className="text-center">
      {/* The sentence */}
      <p className="flex flex-wrap items-baseline justify-center gap-x-1 gap-y-2 text-xl leading-relaxed">
        {tokens.map((token, i) =>
          stage === "find" && outcome === null ? (
            <button
              key={i}
              type="button"
              onClick={() => tapWord(i)}
              className={cn(
                "cursor-pointer rounded-lg border-2 border-transparent px-1 py-0.5 transition-colors hover:border-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring",
                shakingIndex === i && "shake border-coral bg-coral-soft",
                token.isTarget &&
                  findMisses >= 2 &&
                  "border-dashed border-sky bg-sky-soft",
              )}
            >
              {token.prefix}
              {token.shown}
              {token.suffix}
            </button>
          ) : (
            <span
              key={i}
              className={cn(
                "px-1 py-0.5",
                token.isTarget &&
                  outcome === null &&
                  "rounded-lg bg-coral-soft font-semibold line-through decoration-coral decoration-2",
                token.isTarget &&
                  outcome !== null &&
                  "rounded-lg bg-leaf-soft font-semibold",
              )}
            >
              {token.prefix}
              {token.isTarget && outcome !== null
                ? matchCase(token.shown, entry.word.toLowerCase())
                : token.shown}
              {token.suffix}
            </span>
          ),
        )}
      </p>

      {stage === "find" && (
        <p className="font-heading mt-4 text-sm font-medium text-muted-foreground" role="status">
          {findMisses === 0
            ? "Tap the word that's spelled wrong."
            : findMisses === 1
              ? `"${lastMissWord}" is spelled fine — keep hunting!`
              : `"${lastMissWord}" is spelled fine. Psst — the wrong word starts with "${
                  tokens.find((t) => t.isTarget)?.shown[0] ?? "?"
                }".`}
        </p>
      )}

      {stage === "fix" && outcome === null && (
        <form
          className="mx-auto mt-6 max-w-sm"
          onSubmit={(e) => {
            e.preventDefault();
            submit();
          }}
        >
          <p className="font-heading text-sm font-medium text-muted-foreground">
            You found it! Now type it the right way.
          </p>
          <label htmlFor="fix-input" className="sr-only">
            Type the correct spelling
          </label>
          <SpellingInput
            id="fix-input"
            value={typed}
            onChange={(e) => setTyped(e.target.value)}
            placeholder="Type the fix…"
            autoFocus
            className="mt-3"
          />
          {retrying && (
            <p className="font-heading mt-3 text-sm font-medium text-coral" role="status">
              Not quite — look at the clue and try again!
            </p>
          )}
          <p className="mt-3 text-sm text-muted-foreground">Clue: {entry.hint}</p>
          <Button
            type="submit"
            size="lg"
            className="font-heading mt-4"
            disabled={typed.trim().length === 0}
          >
            Fix it
          </Button>
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
