"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ArrowLeft, Check, Copy, Pencil, Plus, Trash2, X } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  loadCustomBanks,
  saveCustomBanks,
  setActiveBankId,
} from "@/hooks/use-bank";
import {
  BUILT_IN_BANKS,
  DEFAULT_BANK_ID,
  type BankEntry,
  type WordBank,
} from "@/lib/banks";
import { BLOCKED_WORDS } from "@/lib/blocked-words";
import { cn } from "@/lib/utils";

/** Validate a new custom word; returns an error message or null. */
function validateWord(word: string, existing: BankEntry[]): string | null {
  if (!/^[A-Za-z]{2,20}$/.test(word)) {
    return "Words are 2–20 letters, no spaces or symbols.";
  }
  if (BLOCKED_WORDS.includes(word.toLowerCase())) {
    return "Let's pick a different word.";
  }
  if (existing.some((e) => e.word.toLowerCase() === word.toLowerCase())) {
    return "That word is already in this list.";
  }
  return null;
}

/** A sentence must use the word exactly once so Fix the Sentence works. */
function validateSentence(word: string, sentence: string): string | null {
  if (!sentence) return null;
  const tokens = sentence
    .split(/\s+/)
    .map((t) => (t.match(/[A-Za-z']+/)?.[0] ?? "").toLowerCase());
  const count = tokens.filter((t) => t === word.toLowerCase()).length;
  if (count !== 1) {
    return "The sentence needs to use the word exactly once.";
  }
  return null;
}

export function BankManager() {
  const [customBanks, setCustomBanks] = useState<WordBank[]>([]);
  const [activeId, setActiveId] = useState(DEFAULT_BANK_ID);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const custom = loadCustomBanks();
    setCustomBanks(custom);
    const saved = window.localStorage.getItem("spell-it-active-bank");
    if (
      saved &&
      [...BUILT_IN_BANKS, ...custom].some((b) => b.id === saved)
    ) {
      setActiveId(saved);
    }
    setLoaded(true);
  }, []);

  const persist = (next: WordBank[]) => {
    setCustomBanks(next);
    saveCustomBanks(next);
  };

  const setActive = (id: string) => {
    setActiveId(id);
    setActiveBankId(id);
  };

  const createBank = (entries: BankEntry[] = [], name = "My new list") => {
    const bank: WordBank = {
      id: crypto.randomUUID(),
      name,
      blurb: "",
      builtIn: false,
      entries,
    };
    persist([...customBanks, bank]);
    setEditingId(bank.id);
  };

  const updateBank = (id: string, update: Partial<WordBank>) => {
    persist(customBanks.map((b) => (b.id === id ? { ...b, ...update } : b)));
  };

  const deleteBank = (id: string) => {
    persist(customBanks.filter((b) => b.id !== id));
    if (editingId === id) setEditingId(null);
    if (activeId === id) setActive(DEFAULT_BANK_ID);
  };

  const editing = customBanks.find((b) => b.id === editingId) ?? null;

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-8 sm:px-6">
      <Link
        href="/"
        className="font-heading inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Home
      </Link>

      <h1 className="font-heading mt-4 text-3xl font-semibold sm:text-4xl">
        Word lists
      </h1>
      <p className="mt-1 max-w-xl text-sm text-muted-foreground">
        Every game draws from one list at a time. Use a built-in list, or build
        your own — a hint powers the clue games, and a sentence (using the word
        once) powers Fix the Sentence.
      </p>

      {editing ? (
        <BankEditor
          bank={editing}
          onChange={(update) => updateBank(editing.id, update)}
          onClose={() => setEditingId(null)}
        />
      ) : (
        <>
          {/* Custom lists */}
          <section className="mt-8">
            <div className="flex items-center justify-between">
              <h2 className="font-heading text-xl font-semibold">My lists</h2>
              <Button className="font-heading" onClick={() => createBank()}>
                <Plus aria-hidden="true" /> New list
              </Button>
            </div>
            {loaded && customBanks.length === 0 && (
              <p className="mt-3 text-sm text-muted-foreground">
                No custom lists yet. Start one from scratch, or duplicate a
                built-in list below and make it yours.
              </p>
            )}
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {customBanks.map((bank) => (
                <BankCard
                  key={bank.id}
                  bank={bank}
                  active={bank.id === activeId}
                  onUse={() => setActive(bank.id)}
                  actions={
                    <>
                      <Button
                        variant="outline"
                        size="sm"
                        className="font-heading"
                        onClick={() => setEditingId(bank.id)}
                      >
                        <Pencil aria-hidden="true" /> Edit
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className="font-heading text-destructive"
                        onClick={() => deleteBank(bank.id)}
                      >
                        <Trash2 aria-hidden="true" /> Delete
                      </Button>
                    </>
                  }
                />
              ))}
            </div>
          </section>

          {/* Built-in lists */}
          <section className="mt-10">
            <h2 className="font-heading text-xl font-semibold">
              Built-in lists
            </h2>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {BUILT_IN_BANKS.map((bank) => (
                <BankCard
                  key={bank.id}
                  bank={bank}
                  active={bank.id === activeId}
                  onUse={() => setActive(bank.id)}
                  actions={
                    <Button
                      variant="outline"
                      size="sm"
                      className="font-heading"
                      onClick={() =>
                        createBank([...bank.entries], `${bank.name} (my copy)`)
                      }
                    >
                      <Copy aria-hidden="true" /> Duplicate to customize
                    </Button>
                  }
                />
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  );
}

function BankCard({
  bank,
  active,
  onUse,
  actions,
}: {
  bank: WordBank;
  active: boolean;
  onUse: () => void;
  actions: React.ReactNode;
}) {
  return (
    <Card className={cn(active && "border-leaf ring-2 ring-leaf/40")}>
      <CardContent className="flex h-full flex-col gap-2 pt-4">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-heading text-lg font-semibold">{bank.name}</h3>
          {active && (
            <Badge className="bg-leaf-soft text-foreground">
              <Check className="h-3 w-3" aria-hidden="true" /> In use
            </Badge>
          )}
        </div>
        <p className="text-sm text-muted-foreground">
          {bank.entries.length} words
          {bank.blurb ? ` · ${bank.blurb}` : ""}
        </p>
        <div className="mt-auto flex flex-wrap gap-2 pt-2">
          {!active && (
            <Button size="sm" className="font-heading" onClick={onUse}>
              Use this list
            </Button>
          )}
          {actions}
        </div>
      </CardContent>
    </Card>
  );
}

function BankEditor({
  bank,
  onChange,
  onClose,
}: {
  bank: WordBank;
  onChange: (update: Partial<WordBank>) => void;
  onClose: () => void;
}) {
  const [word, setWord] = useState("");
  const [hint, setHint] = useState("");
  const [sentence, setSentence] = useState("");
  const [error, setError] = useState<string | null>(null);

  const addWord = () => {
    const trimmed = word.trim();
    const wordError = validateWord(trimmed, bank.entries);
    if (wordError) {
      setError(wordError);
      return;
    }
    const sentenceError = validateSentence(trimmed, sentence.trim());
    if (sentenceError) {
      setError(sentenceError);
      return;
    }
    onChange({
      entries: [
        ...bank.entries,
        {
          word: trimmed.toLowerCase(),
          hint: hint.trim() || undefined,
          sentence: sentence.trim() || undefined,
        },
      ],
    });
    setWord("");
    setHint("");
    setSentence("");
    setError(null);
  };

  const removeWord = (index: number) => {
    onChange({ entries: bank.entries.filter((_, i) => i !== index) });
  };

  return (
    <section className="mt-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Input
          value={bank.name}
          onChange={(e) => onChange({ name: e.target.value })}
          aria-label="List name"
          className="font-heading h-11 max-w-xs border-[3px] border-ink !text-lg font-semibold shadow-[0_3px_0_var(--ink)]"
        />
        <Button className="font-heading" onClick={onClose}>
          Done
        </Button>
      </div>

      {bank.entries.length < 6 && (
        <p className="mt-3 text-sm text-muted-foreground">
          Tip: games work best with at least 6–10 words. Words with hints
          unlock Mini Crossword and Memory Match; sentences unlock Fix the
          Sentence.
        </p>
      )}

      {/* Add word form */}
      <Card className="mt-4">
        <CardContent className="grid gap-3 pt-4 sm:grid-cols-[1fr_2fr]">
          <div>
            <label htmlFor="new-word" className="font-heading text-sm font-medium">
              Word
            </label>
            <Input
              id="new-word"
              value={word}
              onChange={(e) => setWord(e.target.value)}
              placeholder="e.g. galaxy"
              autoComplete="off"
              autoCapitalize="off"
              spellCheck={false}
            />
          </div>
          <div>
            <label htmlFor="new-hint" className="font-heading text-sm font-medium">
              Hint <span className="font-normal text-muted-foreground">(optional)</span>
            </label>
            <Input
              id="new-hint"
              value={hint}
              onChange={(e) => setHint(e.target.value)}
              placeholder="A clue that doesn't give the spelling away"
            />
          </div>
          <div className="sm:col-span-2">
            <label htmlFor="new-sentence" className="font-heading text-sm font-medium">
              Sentence{" "}
              <span className="font-normal text-muted-foreground">
                (optional — must use the word exactly once)
              </span>
            </label>
            <Input
              id="new-sentence"
              value={sentence}
              onChange={(e) => setSentence(e.target.value)}
              placeholder="e.g. The galaxy is full of stars."
            />
          </div>
          {error && (
            <p className="font-heading text-sm font-medium text-destructive sm:col-span-2" role="alert">
              {error}
            </p>
          )}
          <div className="sm:col-span-2">
            <Button className="font-heading" onClick={addWord} disabled={!word.trim()}>
              <Plus aria-hidden="true" /> Add word
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Word list */}
      <ul className="mt-4 space-y-2">
        {bank.entries.map((entry, index) => (
          <li
            key={`${entry.word}-${index}`}
            className="flex items-start justify-between gap-3 rounded-xl border-2 border-ink/15 bg-card px-4 py-2.5"
          >
            <div className="min-w-0">
              <span className="font-heading font-semibold">{entry.word}</span>
              {entry.hint && (
                <span className="ml-2 text-sm text-muted-foreground">
                  {entry.hint}
                </span>
              )}
              {entry.sentence && (
                <p className="truncate text-sm text-muted-foreground italic">
                  {entry.sentence}
                </p>
              )}
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => removeWord(index)}
              aria-label={`Remove ${entry.word}`}
            >
              <X aria-hidden="true" />
            </Button>
          </li>
        ))}
      </ul>
      {bank.entries.length === 0 && (
        <p className="mt-4 text-sm text-muted-foreground">
          No words yet — add your first one above.
        </p>
      )}
    </section>
  );
}
