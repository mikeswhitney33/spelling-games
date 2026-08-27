"use client";

import Link from "next/link";
import { Settings } from "lucide-react";

import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { WordBank } from "@/lib/banks";

export function BankPicker({
  bank,
  banks,
  onChange,
}: {
  bank: WordBank;
  banks: WordBank[];
  onChange: (id: string) => void;
}) {
  const bands = banks.filter((b) => b.builtIn && b.id.startsWith("band-"));
  const collections = banks.filter((b) => b.builtIn && !b.id.startsWith("band-"));
  const custom = banks.filter((b) => !b.builtIn);

  return (
    <div>
      <div className="flex flex-wrap items-center gap-3">
        <div>
          <label
            htmlFor="bank-picker"
            className="font-heading text-sm font-medium text-muted-foreground"
          >
            Word list
          </label>
          <Select
            value={bank.id}
            onValueChange={(value) => {
              if (typeof value === "string") onChange(value);
            }}
          >
            <SelectTrigger
              id="bank-picker"
              className="font-heading mt-1 w-56 border-[3px] border-ink bg-card font-medium shadow-[0_3px_0_var(--ink)]"
            >
              <SelectValue>{bank.name}</SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                <SelectLabel>Levels</SelectLabel>
                {bands.map((b) => (
                  <SelectItem key={b.id} value={b.id}>
                    {b.name}
                  </SelectItem>
                ))}
              </SelectGroup>
              <SelectGroup>
                <SelectLabel>Collections</SelectLabel>
                {collections.map((b) => (
                  <SelectItem key={b.id} value={b.id}>
                    {b.name}
                  </SelectItem>
                ))}
              </SelectGroup>
              {custom.length > 0 && (
                <SelectGroup>
                  <SelectLabel>My lists</SelectLabel>
                  {custom.map((b) => (
                    <SelectItem key={b.id} value={b.id}>
                      {b.name}
                    </SelectItem>
                  ))}
                </SelectGroup>
              )}
            </SelectContent>
          </Select>
        </div>
        <Link
          href="/settings"
          className="font-heading mt-5 inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
        >
          <Settings className="h-4 w-4" aria-hidden="true" />
          Manage lists
        </Link>
      </div>
      <p className="mt-2 text-sm text-muted-foreground">{bank.blurb}</p>
    </div>
  );
}

/** Shown when the active bank lacks enough usable words for a game. */
export function NotEnoughWords({
  need,
  requirement,
}: {
  need: number;
  requirement: string;
}) {
  return (
    <div className="py-6 text-center">
      <p className="font-heading text-lg font-semibold text-foreground">
        This list needs more words for this game.
      </p>
      <p className="mx-auto mt-2 max-w-md text-sm text-muted-foreground">
        It takes at least {need} {requirement} to play. Add some in{" "}
        <Link href="/settings" className="font-semibold underline">
          Settings
        </Link>
        , or pick a different list above.
      </p>
    </div>
  );
}
