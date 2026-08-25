"use client";

import type { ComponentProps } from "react";

import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

/** The big chunky text input kids type spellings into, shared across games. */
export function SpellingInput({
  className,
  onKeyDown,
  ...props
}: ComponentProps<typeof Input>) {
  return (
    <Input
      autoComplete="off"
      autoCapitalize="off"
      autoCorrect="off"
      spellCheck={false}
      onKeyDown={(e) => {
        onKeyDown?.(e);
        // The Base UI input doesn't perform implicit form submission on
        // Enter, so trigger the surrounding form's onSubmit ourselves.
        if (e.key === "Enter" && !e.defaultPrevented) {
          e.preventDefault();
          e.currentTarget.form?.requestSubmit();
        }
      }}
      className={cn(
        "font-heading h-14 border-[3px] border-ink text-center !text-2xl lowercase tracking-wide shadow-[0_4px_0_var(--ink)]",
        className,
      )}
      {...props}
    />
  );
}
